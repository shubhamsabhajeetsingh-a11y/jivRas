package com.jivRas.groceries.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jivRas.groceries.dto.CheckoutRequest;
import com.jivRas.groceries.dto.OrderResponse;
import com.jivRas.groceries.dto.AdminOrderResponse;
import com.jivRas.groceries.service.DynamicAuthorizationService;
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
    private final DynamicAuthorizationService dynamicAuthorizationService;

    /**
     * POST /api/orders/checkout
     * Checkout — create an order from the current cart.
     * Open to authenticated users and guests (X-Guest-Id).
     */
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(
            @Valid @RequestBody CheckoutRequest request,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        // Checkout is allowed for any authenticated user or guest — no DB permission check needed
        String userId = resolveUserId(guestId, authentication);
        return ResponseEntity.ok(orderService.checkout(userId, request));
    }

    /**
     * GET /api/orders/{id}
     * Get order by ID — any authenticated user.
     */
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

    // ──────────────────────────── Helpers ──────────────────────────────────────

    /**
     * Resolve the userId from JWT (if logged in) or from the X-Guest-Id header.
     */
    private String resolveUserId(String guestId, Authentication auth) {
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        if (guestId != null && !guestId.isBlank()) {
            return "guest_" + guestId;
        }
        // Fall back to SecurityContextHolder for compatibility
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
