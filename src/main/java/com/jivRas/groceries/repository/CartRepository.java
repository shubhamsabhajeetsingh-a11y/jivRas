package com.jivRas.groceries.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jivRas.groceries.entity.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {

    /** Find the cart belonging to a specific user (or guest by their UUID). */
    Optional<Cart> findByUserId(String userId);
}
