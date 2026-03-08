package com.jivRas.groceries.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jivRas.groceries.entity.Product;
import com.jivRas.groceries.service.ProductService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;

	// Add new product
	@PostMapping
	public ResponseEntity<Product> addProduct(@RequestBody Product product) {
		return ResponseEntity.ok(productService.save(product));
	}

	// Update product (price / stock / name)
	@PutMapping("/{id}")
	public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {

		return ResponseEntity.ok(productService.update(id, product));
	}

	// Upload product image
	@PostMapping("/{id}/image")
	public ResponseEntity<String> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {

		productService.uploadImage(id, file);
		return ResponseEntity.ok("Image uploaded successfully");
	}

	// Get all products (owner view)
	@GetMapping
	public ResponseEntity<List<Product>> getAllProducts() {
		return ResponseEntity.ok(productService.findAll());
	}

	// Get active products (customer view)
	@GetMapping("/active")
	public ResponseEntity<List<Product>> getActiveProducts() {
		return ResponseEntity.ok(productService.findActive());
	}

}