package com.jivRas.groceries.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jivRas.groceries.entity.Order;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long>  {

    List<Order> findAllByOrderByOrderDateDesc();

}
