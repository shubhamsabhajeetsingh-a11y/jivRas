package com.jivRas.groceries.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jivRas.groceries.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
