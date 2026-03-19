package com.jivRas.groceries.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jivRas.groceries.dto.AddToCartRequest;
import com.jivRas.groceries.dto.CartResponse;
import com.jivRas.groceries.dto.UpdateCartRequest;
import com.jivRas.groceries.service.CartService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for shopping cart operations.
 * Supports both logged-in users (JWT) and guests (X-Guest-Id header).
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * Add a product to the cart.
     * Logged-in users are identified by JWT; guests must send X-Guest-Id header.
     */
    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId) {

        String userId = resolveUserId(guestId);
        return ResponseEntity.ok(cartService.addToCart(userId, request));
    }

    /**
     * Get the current cart.
     */
    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId) {

        String userId = resolveUserId(guestId);
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    /**
     * Update the quantity of a specific cart item.
     */
    @PutMapping("/update/{itemId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartRequest request,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId) {

        String userId = resolveUserId(guestId);
        return ResponseEntity.ok(cartService.updateCartItem(itemId, userId, request));
    }

    /**
     * Remove a single item from the cart.
     */
    @DeleteMapping("/{itemId}")
    public ResponseEntity<CartResponse> removeCartItem(
            @PathVariable Long itemId,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId) {

        String userId = resolveUserId(guestId);
        return ResponseEntity.ok(cartService.removeCartItem(itemId, userId));
    }

    // ──────────────────────────── Helper ────────────────────────────

    /**
     * Resolve the userId from JWT (if logged in) or from the X-Guest-Id header.
     * Logged-in users always take priority over guestId.
     */
    private String resolveUserId(String guestId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // If user is authenticated (not anonymous), use their username
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }

        // Otherwise, use the guest ID from the header
        if (guestId != null && !guestId.isBlank()) {
            return "guest_" + guestId;
        }

        throw new RuntimeException("Please log in or provide X-Guest-Id header");
    }
}
