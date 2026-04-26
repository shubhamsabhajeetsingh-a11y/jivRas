package com.jivRas.groceries.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;


import com.jivRas.groceries.dto.CheckoutRequest;
import com.jivRas.groceries.dto.OrderItemResponse;
import com.jivRas.groceries.dto.OrderResponse;
import com.jivRas.groceries.entity.Cart;
import com.jivRas.groceries.entity.CartItem;
import com.jivRas.groceries.entity.Order;
import com.jivRas.groceries.entity.OrderItem;
import com.jivRas.groceries.entity.Product;
import com.jivRas.groceries.dto.AdminOrderResponse;
import java.util.Map;
import com.jivRas.groceries.exception.ResourceNotFoundException;
import com.jivRas.groceries.repository.CartRepository;
import com.jivRas.groceries.repository.OrderRepository;
import com.jivRas.groceries.repository.ProductRepository;
import com.jivRas.groceries.repository.PaymentRepository;
import com.jivRas.groceries.enums.PaymentStatus;
import com.jivRas.groceries.dto.order.CustomerOrderSummaryDto;
import com.jivRas.groceries.dto.order.DeliveryOrderDto;
import com.jivRas.groceries.entity.Payment;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Handles checkout flow:
 *   1. Fetch cart items
 *   2. Validate stock
 *   3. Deduct stock
 *   4. Create Order + OrderItems
 *   5. Clear cart
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final DeliveryService deliveryService;
    private final CartService cartService;
    private final PaymentRepository paymentRepository;

    // TODO (tech debt): Order.userId is String to hold either numeric user IDs or "guest_<uuid>".
    // Cleaner long-term: separate `customer_id Long FK` and `guest_session_id String` columns.
    // Current code handles both formats defensively — see getMyOrders(), getGuestOrdersByPhone(), cancelMyOrder().

    /**
     * Checkout the user's cart — creates an order and clears the cart.
     */
    @Transactional
    public OrderResponse checkout(String userId, CheckoutRequest request) {

        // 1. Fetch the cart
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart is empty. Add items before checkout."));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty. Add items before checkout.");
        }

        // 2. Create the order
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus("CREATED");

        // Set delivery details from request
        order.setCustomerName(request.getCustomerName());
        order.setMobile(request.getMobile());
        order.setAddressLine(request.getAddressLine());
        order.setCity(request.getCity());
        order.setState(request.getState());
        order.setPincode(request.getPincode());

        // Calculate ETA
        order.setEstimatedDeliveryDays(
                deliveryService.calculateETA(request.getPincode()));

        // 3. Process each cart item → create OrderItem + deduct stock
        double totalAmount = 0;

        for (CartItem cartItem : cart.getItems()) {

            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found: " + cartItem.getProductName()));

            // Validate stock availability
            if (product.getAvailableStockKg() < cartItem.getQuantity()) {
                throw new RuntimeException(
                        "Insufficient stock for " + product.getName()
                        + ". Available: " + product.getAvailableStockKg() + " kg");
            }

            // Deduct stock
            product.setAvailableStockKg(
                    product.getAvailableStockKg() - cartItem.getQuantity());
            productRepository.save(product);

            // Create order item
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setQuantityKg(cartItem.getQuantity());
            orderItem.setPricePerKg(product.getPricePerKg());

            order.getItems().add(orderItem);

            totalAmount += cartItem.getQuantity() * product.getPricePerKg();
        }

        order.setTotalAmount(totalAmount);

        // Always populate customer_phone from checkout form mobile field.
        // Required for order history lookup by phone and for guest-to-registered linking.
        order.setCustomerPhone(request.getMobile());

        // Email is optional — guests may leave blank, registered users may already have it on their User record.
        order.setCustomerEmail(request.getEmail());

        // userId starts with "guest_" when X-Guest-Id header was used (see OrderController.resolveUserId).
        // Flag the order as guest so admin UI can filter/badge it; user FK stays null for guests.
        boolean isGuestOrder = userId != null && userId.startsWith("guest_");
        order.setIsGuest(isGuestOrder);

        // 4. Save order
        Order savedOrder = orderRepository.save(order);

        // 5. Clear cart
        cartService.clearCart(userId);

        return toOrderResponse(savedOrder);
    }

    /**
     * Get order details by ID.
     */
    public OrderResponse getOrderById(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with ID: " + orderId));

        return toOrderResponse(order);
    }

    public List<CustomerOrderSummaryDto> getMyOrders(Long customerId) {
        String callingUserIdStr = String.valueOf(customerId);
        List<Order> orders = orderRepository.findByUserIdOrderByOrderDateDesc(callingUserIdStr);
        return orders.stream().map(this::toCustomerOrderSummaryDto).collect(Collectors.toList());
    }

    public List<CustomerOrderSummaryDto> getGuestOrdersByPhone(String phone) {
        // NOTE: The filter to keep only guest orders is critical.
        // If a phone number has BOTH a guest order (no user account at time of order)
        // AND a registered-user order (later signed up and placed a new order),
        // this method must return only the guest orders to avoid exposing the
        // registered user's data to someone who just has the phone number.
        List<Order> orders = orderRepository.findByCustomerPhoneOrderByOrderDateDesc(phone);
        return orders.stream()
                // Guest orders are identified by any of:
                //   - isGuest == true (canonical flag from Phase 1)
                //   - userId is null
                //   - userId starts with "guest_" prefix (legacy before isGuest flag existed)
                .filter(o -> Boolean.TRUE.equals(o.getIsGuest())
                        || o.getUserId() == null
                        || (o.getUserId() != null && o.getUserId().startsWith("guest_")))
                .map(this::toCustomerOrderSummaryDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Order cancelMyOrder(Long orderId, Long customerId) {
        // Cancellation allowed only in the initial order state (CREATED in this codebase —
        // semantically equivalent to "PENDING" in generic e-commerce terminology).
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // auth.getName() returns numeric user ID as String for registered users.
        // Order.userId may be either "42" (registered) or "guest_<uuid>" (guest).
        // Ownership check: order's userId must match the calling user's ID string.
        String callingUserId = String.valueOf(customerId);
        if (!callingUserId.equals(order.getUserId())) {
            throw new org.springframework.security.access.AccessDeniedException("Not your order");
        }

        if (!"CREATED".equals(order.getOrderStatus())) {
            throw new IllegalStateException("Only PENDING orders can be cancelled");
        }

        Payment latestPayment = paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream().findFirst().orElse(null);
        if (latestPayment != null && PaymentStatus.PAID.equals(latestPayment.getStatus())) {
            throw new IllegalStateException("Paid orders cannot be cancelled via self-service; contact support for refund");
        }

        order.setOrderStatus("CANCELLED");
        return orderRepository.save(order);
    }



    // ──────────────────────────── Admin Methods ────────────────────────────

    public List<AdminOrderResponse> getAllOrdersForAdmin(Boolean guestOnly) {
        List<Order> allOrders = orderRepository.findAllByOrderByOrderDateDesc();
        return allOrders.stream()
                .filter(o -> {
                    // guestOnly null = show all (default), true = guests only, false = registered only.
                    // Frontend toggles between all/guest/registered via this single param.
                    if (guestOnly == null) return true;
                    if (guestOnly) return Boolean.TRUE.equals(o.getIsGuest());
                    return !Boolean.TRUE.equals(o.getIsGuest());
                })
                .map(order -> {
                    List<OrderItemResponse> itemResponses = order.getItems().stream()
                            .map(item -> OrderItemResponse.builder()
                                    .id(item.getId())
                                    .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                                    .productName(item.getProductName())
                                    .quantityKg(item.getQuantityKg())
                                    .pricePerKg(item.getPricePerKg())
                                    .subtotal(item.getQuantityKg() * item.getPricePerKg())
                                    .build())
                            .collect(Collectors.toList());

                    // Try to parse userId as Long if it's not a guest
                    Long customerId = null;
                    if (!Boolean.TRUE.equals(order.getIsGuest()) && order.getUserId() != null && !order.getUserId().startsWith("guest_")) {
                        try {
                            customerId = Long.parseLong(order.getUserId());
                        } catch (NumberFormatException ignored) {}
                    }

                    return AdminOrderResponse.builder()
                            .orderId(order.getId())
                            .orderStatus(order.getOrderStatus())
                            .orderDate(order.getOrderDate())
                            .totalAmount(order.getTotalAmount())
                            .estimatedDeliveryDays(order.getEstimatedDeliveryDays())
                            .customerName(order.getCustomerName())
                            .mobile(order.getMobile())
                            .addressLine(order.getAddressLine())
                            .city(order.getCity())
                            .state(order.getState())
                            .pincode(order.getPincode())
                            .isGuest(order.getIsGuest())
                            .customerId(customerId)
                            .items(itemResponses)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public Map<String, List<AdminOrderResponse>> getOrdersGroupedByCategory(Boolean guestOnly) {
        return getAllOrdersForAdmin(guestOnly).stream()
                .flatMap(adminOrder -> adminOrder.getItems().stream()
                        .map(item -> {
                            String categoryName = "Uncategorized";
                            if (item.getProductId() != null) {
                                Product product = productRepository.findById(item.getProductId()).orElse(null);
                                if (product != null && product.getCategory() != null && product.getCategory().getName() != null) {
                                    categoryName = product.getCategory().getName();
                                }
                            }
                            return new java.util.AbstractMap.SimpleEntry<>(categoryName, adminOrder);
                        })
                        .distinct()
                )
                .collect(Collectors.groupingBy(Map.Entry::getKey, 
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    // ──────────────────────────── Mapper ────────────────────────────

    /**
     * Convert Order entity to OrderResponse DTO.
     */
    private OrderResponse toOrderResponse(Order order) {

        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                        .productName(item.getProductName())
                        .quantityKg(item.getQuantityKg())
                        .pricePerKg(item.getPricePerKg())
                        .subtotal(item.getQuantityKg() * item.getPricePerKg())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getOrderStatus())
                .orderDate(order.getOrderDate())
                .estimatedDeliveryDays(order.getEstimatedDeliveryDays())
                .customerName(order.getCustomerName())
                .mobile(order.getMobile())
                .addressLine(order.getAddressLine())
                .city(order.getCity())
                .state(order.getState())
                .pincode(order.getPincode())
                .items(itemResponses)
                .build();
    }

    // ──────────────────────────── Phase 6: Delivery Agent Methods ────────────────────

    /**
     * Assigns a delivery agent to an order and transitions it to OUT_FOR_DELIVERY.
     * Called by admin / branch manager via PATCH /api/orders/{orderId}/assign-agent.
     * Timeline entry is recorded by the controller (same pattern as updateOrderStatus).
     *
     * @param orderId the order to assign
     * @param agentId numeric ID of the EmployeeUser with role DELIVERY_AGENT
     * @param eta     estimated delivery time shown on the agent's dashboard
     * @return the updated Order entity
     */
    public Order assignAgent(Long orderId, Long agentId, LocalDateTime eta) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        // Prevent re-assigning already completed or cancelled orders
        if ("DELIVERED".equals(order.getOrderStatus()) || "CANCELLED".equals(order.getOrderStatus())) {
            throw new IllegalStateException(
                    "Cannot assign agent to an order with status: " + order.getOrderStatus());
        }

        // Stamp the agent and ETA, then move the order into the delivery pipeline
        order.setDeliveryAgentId(agentId);
        order.setEstimatedDeliveryTime(eta);
        order.setOrderStatus("OUT_FOR_DELIVERY");

        return orderRepository.save(order);
    }

    /**
     * Returns all orders currently assigned to the given agent that are still in transit.
     * Only OUT_FOR_DELIVERY orders are returned — delivered orders drop off automatically
     * when markDelivered() transitions them to DELIVERED.
     *
     * @param agentId numeric ID of the authenticated delivery agent
     * @return list of delivery-view DTOs, one per assigned in-transit order
     */
    public List<DeliveryOrderDto> getMyDeliveries(Long agentId) {

        List<Order> orders = orderRepository.findByDeliveryAgentIdAndOrderStatus(
                agentId, "OUT_FOR_DELIVERY");

        List<DeliveryOrderDto> result = new ArrayList<>();
        for (Order order : orders) {
            result.add(toDeliveryOrderDto(order));
        }
        return result;
    }

    /**
     * Marks an order as DELIVERED. Only the assigned agent may call this.
     * Timeline entry ("Delivered by agent") is recorded by the controller.
     *
     * @param orderId the order to mark delivered
     * @param agentId numeric ID of the calling delivery agent — validated against order.deliveryAgentId
     * @return the updated Order entity
     * @throws org.springframework.security.access.AccessDeniedException if the agent is not assigned to this order
     * @throws IllegalStateException if the order is not currently OUT_FOR_DELIVERY
     */
    public Order markDelivered(Long orderId, Long agentId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        // Ownership check — only the assigned agent can mark their own delivery
        if (!agentId.equals(order.getDeliveryAgentId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not assigned to order #" + orderId);
        }

        // Guard: order must be actively out for delivery
        if (!"OUT_FOR_DELIVERY".equals(order.getOrderStatus())) {
            throw new IllegalStateException(
                    "Order #" + orderId + " is not OUT_FOR_DELIVERY (current status: " + order.getOrderStatus() + ")");
        }

        order.setOrderStatus("DELIVERED");
        return orderRepository.save(order);
    }

    /**
     * Maps an Order entity to the slim DeliveryOrderDto shown on the agent dashboard.
     * Uses a plain for-loop (no streams) per project convention for agent-facing code.
     */
    private DeliveryOrderDto toDeliveryOrderDto(Order order) {

        // Build item response list and capture first item name for the summary line
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        String firstItemName = null;

        for (OrderItem item : order.getItems()) {
            if (firstItemName == null) {
                firstItemName = item.getProductName();
            }
            OrderItemResponse ir = OrderItemResponse.builder()
                    .id(item.getId())
                    .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                    .productName(item.getProductName())
                    .quantityKg(item.getQuantityKg())
                    .pricePerKg(item.getPricePerKg())
                    .subtotal(item.getQuantityKg() * item.getPricePerKg())
                    .build();
            itemResponses.add(ir);
        }

        // additionalItemCount = total items minus the one shown as firstItemName
        int additionalItemCount = order.getItems().size() > 1 ? order.getItems().size() - 1 : 0;

        return DeliveryOrderDto.builder()
                .orderId(order.getId())
                .orderStatus(order.getOrderStatus())
                .orderDate(order.getOrderDate())
                .totalAmount(order.getTotalAmount())
                .customerName(order.getCustomerName())
                .mobile(order.getMobile())
                .addressLine(order.getAddressLine())
                .city(order.getCity())
                .state(order.getState())
                .pincode(order.getPincode())
                .estimatedDeliveryTime(order.getEstimatedDeliveryTime())
                .firstItemName(firstItemName)
                .additionalItemCount(additionalItemCount)
                .items(itemResponses)
                .build();
    }

    private CustomerOrderSummaryDto toCustomerOrderSummaryDto(Order order) {
        Payment latestPayment = paymentRepository.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream().findFirst().orElse(null);
        PaymentStatus paymentStatus = latestPayment != null ? latestPayment.getStatus() : PaymentStatus.NOT_APPLICABLE;

        int itemCount = 0;
        String firstItemName = null;
        for (OrderItem item : order.getItems()) {
            if (firstItemName == null) {
                firstItemName = item.getProductName();
            }
            itemCount += item.getQuantityKg(); // Assuming quantity is the count representation here
        }
        int additionalItemCount = order.getItems().size() > 1 ? order.getItems().size() - 1 : 0;
        
        boolean canCancel = "CREATED".equals(order.getOrderStatus()) && !PaymentStatus.PAID.equals(paymentStatus);

        return CustomerOrderSummaryDto.builder()
                .orderId(order.getId())
                .orderDate(order.getOrderDate())
                .status(order.getOrderStatus())
                .paymentStatus(paymentStatus)
                .totalAmount(Math.round(order.getTotalAmount()))
                .itemCount(itemCount)
                .firstItemName(firstItemName)
                .additionalItemCount(additionalItemCount)
                .canCancel(canCancel)
                .build();
    }
}
