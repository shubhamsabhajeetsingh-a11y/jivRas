package com.jivRas.groceries.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jivRas.groceries.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
