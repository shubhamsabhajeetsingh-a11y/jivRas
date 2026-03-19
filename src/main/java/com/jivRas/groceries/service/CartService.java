package com.jivRas.groceries.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.jivRas.groceries.dto.AddToCartRequest;
import com.jivRas.groceries.dto.CartItemResponse;
import com.jivRas.groceries.dto.CartResponse;
import com.jivRas.groceries.dto.UpdateCartRequest;
import com.jivRas.groceries.entity.Cart;
import com.jivRas.groceries.entity.CartItem;
import com.jivRas.groceries.entity.Product;
import com.jivRas.groceries.exception.ResourceNotFoundException;
import com.jivRas.groceries.repository.CartRepository;
import com.jivRas.groceries.repository.ProductRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Service handling all cart operations — add, view, update, remove, and clear.
 * Supports both logged-in users (userId = username) and guests (userId = guest UUID).
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    /**
     * Add a product to the user's cart. If the product already exists in the cart,
     * the quantity is increased instead of adding a duplicate item.
     */
    @Transactional
    public CartResponse addToCart(String userId, AddToCartRequest request) {

        // 1. Validate product exists
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with ID: " + request.getProductId()));

        // 2. Find or create cart for this user
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUserId(userId);
                    return cartRepository.save(newCart);
                });

        // 3. Check if item already exists in cart — if so, increase quantity
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(
                    existingItem.get().getQuantity() + request.getQuantity());
            existingItem.get().setPricePerKg(product.getPricePerKg()); // refresh price
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductId(product.getId());
            newItem.setProductName(product.getName());
            newItem.setQuantity(request.getQuantity());
            newItem.setPricePerKg(product.getPricePerKg());
            cart.getItems().add(newItem);
        }

        Cart savedCart = cartRepository.save(cart);
        return toCartResponse(savedCart);
    }

    /**
     * Get the current cart for a user.
     */
    public CartResponse getCart(String userId) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart is empty"));

        return toCartResponse(cart);
    }

    /**
     * Update the quantity of a specific cart item.
     */
    @Transactional
    public CartResponse updateCartItem(Long cartItemId, String userId, UpdateCartRequest request) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found with ID: " + cartItemId));

        item.setQuantity(request.getQuantity());

        Cart savedCart = cartRepository.save(cart);
        return toCartResponse(savedCart);
    }

    /**
     * Remove a single item from the cart.
     */
    @Transactional
    public CartResponse removeCartItem(Long cartItemId, String userId) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        boolean removed = cart.getItems().removeIf(item -> item.getId().equals(cartItemId));

        if (!removed) {
            throw new ResourceNotFoundException("Cart item not found with ID: " + cartItemId);
        }

        Cart savedCart = cartRepository.save(cart);
        return toCartResponse(savedCart);
    }

    /**
     * Clear all items from the cart (called after successful checkout).
     */
    @Transactional
    public void clearCart(String userId) {

        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    // ──────────────────────────── Mapper ────────────────────────────

    /**
     * Convert a Cart entity into a CartResponse DTO.
     */
    private CartResponse toCartResponse(Cart cart) {

        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> CartItemResponse.builder()
                        .cartItemId(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .pricePerKg(item.getPricePerKg())
                        .subtotal(item.getQuantity() * item.getPricePerKg())
                        .build())
                .collect(Collectors.toList());

        double totalAmount = itemResponses.stream()
                .mapToDouble(CartItemResponse::getSubtotal)
                .sum();

        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(cart.getUserId())
                .items(itemResponses)
                .totalAmount(totalAmount)
                .build();
    }
}
