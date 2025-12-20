package com.jivRas.groceries.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jivRas.groceries.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long>  {
	List<Product> findByActiveTrue();
}
