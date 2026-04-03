package com.jivRas.groceries.service;

import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jivRas.groceries.entity.Product;
import com.jivRas.groceries.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

import com.jivRas.groceries.entity.Category;
import com.jivRas.groceries.repository.CategoryRepository;



@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public Product save(Product product) {
        if (product.getCategory() != null && product.getCategory().getId() != null) {
            Category category = categoryRepository.findById(product.getCategory().getId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategory(category);
        }
        return productRepository.save(product);
    }

    public Product update(Long id, Product updated) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setName(updated.getName());
        product.setDescription(updated.getDescription());
        product.setPricePerKg(updated.getPricePerKg());
        product.setAvailableStockKg(updated.getAvailableStockKg());
        product.setActive(updated.isActive());

        if (updated.getCategory() != null && updated.getCategory().getId() != null) {
            Category category = categoryRepository.findById(updated.getCategory().getId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategory(category);
        }

        return productRepository.save(product);
    }

//    public void uploadImage(Long id, MultipartFile file) {
//        Product product = productRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Product not found"));
//
//        // for now store filename only (later S3 / cloud)
//        product.setImageUrl(file.getOriginalFilename());
//        productRepository.save(product);
//    }

    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        // Soft-delete: mark as inactive (preserves order history)
        product.setActive(false);
        productRepository.save(product);
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public List<Product> findActive() {
        return productRepository.findByActiveTrue();
    }
    
    public void uploadImage(Long id, MultipartFile file) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        try {
            // 1. Create the folder if it doesn't exist
            String uploadDir = "./data/images/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 2. Generate a unique filename (e.g., "product_7.jpg")
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = "product_" + id + extension;

            // 3. Save the file to disk
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 4. Save the FULL URL to the database
            // NOTE: Ensure this matches the backend port (8080)
            String fileUrl = "http://localhost:8080/images/" + newFilename;
            product.setImageUrl(fileUrl);
            
            productRepository.save(product);

        } catch (IOException e) {
            throw new RuntimeException("Could not store file", e);
        }
    }
}

