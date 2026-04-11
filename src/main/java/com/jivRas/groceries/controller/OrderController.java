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
import org.springframework.web.bind.annotation.RestController;

import com.jivRas.groceries.annotation.ModuleAction;
import com.jivRas.groceries.dto.CheckoutRequest;
import com.jivRas.groceries.dto.OrderResponse;
import com.jivRas.groceries.dto.AdminOrderResponse;
import com.jivRas.groceries.entity.Order;
import com.jivRas.groceries.entity.OrderStatusHistory;
import com.jivRas.groceries.exception.ResourceNotFoundException;
import com.jivRas.groceries.repository.OrderRepository;
import com.jivRas.groceries.repository.OrderStatusHistoryRepository;
import com.jivRas.groceries.service.DynamicAuthorizationService;
import com.jivRas.groceries.service.InvoiceService;
import com.jivRas.groceries.service.OrderService;

import jakarta.servlet.http.HttpServletRequest;
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
    private final DynamicAuthorizationService dynamicAuthorizationService;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final InvoiceService invoiceService;

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
            HttpServletRequest httpRequest,
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
    public ResponseEntity<?> getOrder(
            @PathVariable Long id,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        String role = resolveRole(authentication);
        if (!dynamicAuthorizationService.isAllowed(role, httpRequest.getRequestURI(), httpRequest.getMethod())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    // ──────────────────────────── Admin / Staff Endpoints ──────────────────────

    /**
     * GET /api/orders/admin/all
     * List all orders — ADMIN and EMPLOYEE only (per DB permissions).
     */
    @ModuleAction(module = "ORDERS", action = "VIEW")
    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllOrdersForAdmin(
            HttpServletRequest httpRequest,
            Authentication authentication) {

        String role = resolveRole(authentication);
        if (!dynamicAuthorizationService.isAllowed(role, httpRequest.getRequestURI(), httpRequest.getMethod())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        List<AdminOrderResponse> orders = orderService.getAllOrdersForAdmin();
        return ResponseEntity.ok(orders);
    }

    /**
     * GET /api/orders/admin/grouped-by-category
     * Orders grouped by product category — ADMIN and EMPLOYEE only.
     */
    @ModuleAction(module = "ORDERS", action = "VIEW")
    @GetMapping("/admin/grouped-by-category")
    public ResponseEntity<?> getOrdersGroupedByCategory(
            HttpServletRequest httpRequest,
            Authentication authentication) {

        String role = resolveRole(authentication);
        if (!dynamicAuthorizationService.isAllowed(role, httpRequest.getRequestURI(), httpRequest.getMethod())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        Map<String, List<AdminOrderResponse>> grouped = orderService.getOrdersGroupedByCategory();
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
            HttpServletRequest httpRequest,
            Authentication authentication) {

        String role = resolveRole(authentication);
        if (!dynamicAuthorizationService.isAllowed(role, httpRequest.getRequestURI(), httpRequest.getMethod())) {
            return ResponseEntity.status(403).body("Access denied");
        }

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
    public ResponseEntity<?> downloadInvoice(
            @PathVariable Long id,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        String role = resolveRole(authentication);
        if (!dynamicAuthorizationService.isAllowed(role, httpRequest.getRequestURI(), httpRequest.getMethod())) {
            return ResponseEntity.status(403).body("Access denied");
        }

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
    public ResponseEntity<?> getOrderTimeline(
            @PathVariable Long id,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        String role = resolveRole(authentication);
        if (!dynamicAuthorizationService.isAllowed(role, httpRequest.getRequestURI(), httpRequest.getMethod())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        List<OrderStatusHistory> timeline = orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(id);
        return ResponseEntity.ok(timeline);
    }

    // ──────────────────────────── Helpers ──────────────────────────────────────

    private String resolveUserId(String guestId, Authentication auth) {
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        if (guestId != null && !guestId.isBlank()) {
            return "guest_" + guestId;
        }
        Authentication contextAuth = SecurityContextHolder.getContext().getAuthentication();
        if (contextAuth != null && contextAuth.isAuthenticated()
                && !"anonymousUser".equals(contextAuth.getPrincipal())) {
            return contextAuth.getName();
        }
        throw new RuntimeException("Please log in or provide X-Guest-Id header");
    }

    private String resolveRole(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return "";
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .findFirst()
                .orElse("");
    }
}