package com.jivRas.groceries.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jivRas.groceries.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long>{

}
