package com.jivRas.groceries.service;

import java.time.LocalDateTime;
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



    // ──────────────────────────── Admin Methods ────────────────────────────

    public List<AdminOrderResponse> getAllOrdersForAdmin() {
        return orderRepository.findAllByOrderByOrderDateDesc().stream()
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
                            .items(itemResponses)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public Map<String, List<AdminOrderResponse>> getOrdersGroupedByCategory() {
        return getAllOrdersForAdmin().stream()
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
}
