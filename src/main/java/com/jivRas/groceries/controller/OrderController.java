package com.jivRas.groceries.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jivRas.groceries.annotation.ModuleAction;
import com.jivRas.groceries.dto.AssignAgentRequest;
import com.jivRas.groceries.dto.CheckoutRequest;
import com.jivRas.groceries.dto.AdminOrderResponse;
import com.jivRas.groceries.dto.order.CustomerOrderSummaryDto;
import com.jivRas.groceries.dto.order.DeliveryOrderDto;
import com.jivRas.groceries.entity.EmployeeUser;
import com.jivRas.groceries.entity.Order;
import com.jivRas.groceries.entity.OrderStatusHistory;
import com.jivRas.groceries.exception.ResourceNotFoundException;
import com.jivRas.groceries.repository.EmployeeUserRepository;
import com.jivRas.groceries.repository.OrderRepository;
import com.jivRas.groceries.repository.OrderStatusHistoryRepository;
import com.jivRas.groceries.service.InvoiceService;
import com.jivRas.groceries.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * REST controller for order operations.
 * Supports both logged-in users (JWT) and guests (X-Guest-Id header).
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final InvoiceService invoiceService;
    // Phase 6: used to resolve agent username → numeric ID for delivery endpoints
    private final EmployeeUserRepository employeeUserRepository;

    /**
     * POST /api/orders/checkout
     * Checkout — create an order from the current cart.
     * Open to authenticated users and guests (X-Guest-Id).
     */
    @ModuleAction(module = "ORDERS", action = "CREATE")
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(
            @Valid @RequestBody CheckoutRequest request,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            Authentication authentication) {

        String userId = resolveUserId(guestId, authentication);
        return ResponseEntity.ok(orderService.checkout(userId, request));
    }

    /**
     * GET /api/orders/{id}
     * Get order by ID — any authenticated user.
     */
    @ModuleAction(module = "ORDERS", action = "VIEW")
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    // ──────────────────────────── Customer / Guest Endpoints ──────────────────────

    /**
     * Customer-facing: logged-in CUSTOMER sees their own order history.
     */
    @GetMapping("/my-orders")
    @ModuleAction(module = "ORDERS", action = "VIEW")
    public ResponseEntity<List<CustomerOrderSummaryDto>> getMyOrders(Authentication auth) {
        // auth.getName() returns the numeric user ID from the JWT (per existing convention)
        Long customerId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(orderService.getMyOrders(customerId));
    }

    /**
     * Customer-facing self-cancel endpoint.
     */
    @PostMapping("/{orderId}/cancel")
    @ModuleAction(module = "ORDERS", action = "EDIT")
    public ResponseEntity<Order> cancelMyOrder(@PathVariable Long orderId, Authentication auth) {
        Long customerId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(orderService.cancelMyOrder(orderId, customerId));
    }

    /**
     * Admin-facing: guest orders lookup by phone — used by Phase 5 signup linking
     * AND by the admin Orders tab filter.
     */
    @GetMapping("/guest-by-phone")
    @ModuleAction(module = "ORDERS", action = "VIEW_ALL")
    public ResponseEntity<List<CustomerOrderSummaryDto>> getGuestOrdersByPhone(@RequestParam String phone) {
        return ResponseEntity.ok(orderService.getGuestOrdersByPhone(phone));
    }

    // ──────────────────────────── Admin / Staff Endpoints ──────────────────────

    /**
     * GET /api/orders/admin/all
     * List all orders — ADMIN and EMPLOYEE only (per DB permissions).
     */
    @ModuleAction(module = "ORDERS", action = "VIEW_ALL")
    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllOrdersForAdmin(@RequestParam(required = false) Boolean guestOnly) {
        List<AdminOrderResponse> orders = orderService.getAllOrdersForAdmin(guestOnly);
        return ResponseEntity.ok(orders);
    }

    /**
     * GET /api/orders/admin/grouped-by-category
     * Orders grouped by product category — ADMIN and EMPLOYEE only.
     */
    @ModuleAction(module = "ORDERS", action = "VIEW_ALL")
    @GetMapping("/admin/grouped-by-category")
    public ResponseEntity<?> getOrdersGroupedByCategory(@RequestParam(required = false) Boolean guestOnly) {
        Map<String, List<AdminOrderResponse>> grouped = orderService.getOrdersGroupedByCategory(guestOnly);
        return ResponseEntity.ok(grouped);
    }

    /**
     * PATCH /api/orders/{id}/status
     * Update order status — ADMIN, EMPLOYEE, and BRANCH_MANAGER (per DB permissions).
     * Called by the Orders dashboard status dropdown.
     */
    @ModuleAction(module = "ORDERS", action = "EDIT")
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        String newStatus = body.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            return ResponseEntity.badRequest().body("status field is required");
        }

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        order.setOrderStatus(newStatus);
        orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(id);
        history.setStatus(newStatus);
        history.setChangedBy(authentication.getName());
        orderStatusHistoryRepository.save(history);

        return ResponseEntity.ok(Map.of(
                "orderId", id,
                "newStatus", newStatus,
                "message", "Order status updated successfully"
        ));
    }

    /**
     * GET /api/orders/{id}/invoice
     * Generates and downloads a PDF invoice for the given order.
     */
    @ModuleAction(module = "ORDERS", action = "VIEW")
    @GetMapping("/{id}/invoice")
    public ResponseEntity<?> downloadInvoice(@PathVariable Long id) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        byte[] pdf = invoiceService.generateInvoice(order);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"JivRas_Invoice_#" + id + ".pdf\"");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }

    /**
     * GET /api/orders/{id}/timeline
     * Returns status change history for an order in chronological order.
     */
    @ModuleAction(module = "ORDERS", action = "VIEW")
    @GetMapping("/{id}/timeline")
    public ResponseEntity<?> getOrderTimeline(@PathVariable Long id) {
        List<OrderStatusHistory> timeline = orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(id);
        return ResponseEntity.ok(timeline);
    }

    // ──────────────────────────── Phase 6: Delivery Agent Endpoints ───────────

    /**
     * PATCH /api/orders/{orderId}/assign-agent
     * Admin / branch-manager assigns a delivery agent to an order and sets the ETA.
     * Transitions order status to OUT_FOR_DELIVERY and records a timeline entry.
     * Access: BRANCH_MANAGER and above (ORDERS:EDIT permission).
     */
    @ModuleAction(module = "ORDERS", action = "EDIT")
    @PatchMapping("/{orderId}/assign-agent")
    public ResponseEntity<?> assignAgent(
            @PathVariable Long orderId,
            @RequestBody AssignAgentRequest request,
            Authentication authentication) {

        // Delegate business logic (validation + status change) to the service
        Order updated = orderService.assignAgent(
                orderId, request.getAgentId(), request.getEstimatedDeliveryTime());

        // Record the OUT_FOR_DELIVERY transition in the status timeline
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setStatus("OUT_FOR_DELIVERY");
        history.setChangedBy(authentication.getName());
        orderStatusHistoryRepository.save(history);

        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "assignedAgentId", request.getAgentId(),
                "newStatus", updated.getOrderStatus(),
                "message", "Agent assigned and order is now OUT_FOR_DELIVERY"
        ));
    }

    /**
     * GET /api/orders/my-deliveries
     * Returns orders currently assigned to the calling delivery agent that are OUT_FOR_DELIVERY.
     * Access: DELIVERY_AGENT only (ORDERS:VIEW_DELIVERIES permission).
     */
    @ModuleAction(module = "ORDERS", action = "VIEW_DELIVERIES")
    @GetMapping("/my-deliveries")
    public ResponseEntity<List<DeliveryOrderDto>> getMyDeliveries(Authentication auth) {

        // auth.getName() returns the employee's username — look up their numeric DB id
        // (deliveryAgentId on orders stores the numeric EmployeeUser.id, not the username)
        EmployeeUser agent = employeeUserRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Delivery agent not found: " + auth.getName()));

        return ResponseEntity.ok(orderService.getMyDeliveries(agent.getId()));
    }

    /**
     * PATCH /api/orders/{orderId}/mark-delivered
     * Delivery agent marks their assigned order as DELIVERED.
     * Only the agent whose ID matches order.deliveryAgentId may call this — enforced in the service.
     * Records a timeline entry noting delivery by the agent.
     * Access: DELIVERY_AGENT only (ORDERS:MARK_DELIVERED permission).
     */
    @ModuleAction(module = "ORDERS", action = "MARK_DELIVERED")
    @PatchMapping("/{orderId}/mark-delivered")
    public ResponseEntity<?> markDelivered(
            @PathVariable Long orderId,
            Authentication auth) {

        // Resolve the agent's numeric ID — same lookup pattern as getMyDeliveries()
        EmployeeUser agent = employeeUserRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Delivery agent not found: " + auth.getName()));

        // Service validates ownership (agent == assigned agent) and current status
        orderService.markDelivered(orderId, agent.getId());

        // Record "Delivered by agent" in the timeline for audit trail
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setStatus("DELIVERED");
        history.setChangedBy("Agent:" + auth.getName());
        orderStatusHistoryRepository.save(history);

        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "newStatus", "DELIVERED",
                "message", "Order marked as delivered"
        ));
    }

    // ──────────────────────────── Helpers ──────────────────────────────────────

    private String resolveUserId(String guestId, Authentication auth) {
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())
                && !"guest".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        if (guestId != null && !guestId.isBlank()) {
            return "guest_" + guestId;
        }
        Authentication contextAuth = SecurityContextHolder.getContext().getAuthentication();
        if (contextAuth != null && contextAuth.isAuthenticated()
                && !"anonymousUser".equals(contextAuth.getPrincipal())
                && !"guest".equals(contextAuth.getPrincipal())) {
            return contextAuth.getName();
        }
        throw new RuntimeException("Please log in or provide X-Guest-Id header");
    }

}