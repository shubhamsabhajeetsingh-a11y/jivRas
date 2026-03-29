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
import com.jivRas.groceries.service.OrderService;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for order operations.
 * Supports both logged-in users (JWT) and guests (X-Guest-Id header).
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Checkout — create an order from the current cart.
     * Cart items are fetched automatically; only delivery details are needed.
     */
    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(
            @Valid @RequestBody CheckoutRequest request,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId) {

        String userId = resolveUserId(guestId);
        return ResponseEntity.ok(orderService.checkout(userId, request));
    }

    /**
     * Get order by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    // ──────────────────────────── Admin Endpoints ────────────────────────────

    @GetMapping("/admin/all")
    public ResponseEntity<List<AdminOrderResponse>> getAllOrdersForAdmin() {
        return ResponseEntity.ok(orderService.getAllOrdersForAdmin());
    }

    @GetMapping("/admin/grouped-by-category")
    public ResponseEntity<Map<String, List<AdminOrderResponse>>> getOrdersGroupedByCategory() {
        return ResponseEntity.ok(orderService.getOrdersGroupedByCategory());
    }

    // ──────────────────────────── Helper ────────────────────────────

    /**
     * Resolve the userId from JWT (if logged in) or from the X-Guest-Id header.
     */
    private String resolveUserId(String guestId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }

        if (guestId != null && !guestId.isBlank()) {
            return "guest_" + guestId;
        }

        throw new RuntimeException("Please log in or provide X-Guest-Id header");
    }
}
