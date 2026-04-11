package com.jivRas.groceries.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jivRas.groceries.annotation.ModuleAction;
import com.jivRas.groceries.entity.Product;
import com.jivRas.groceries.service.DynamicAuthorizationService;
import com.jivRas.groceries.service.ProductService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final DynamicAuthorizationService dynamicAuthorizationService;

    /** Add new product — roles per DB permissions (all non-CUSTOMER). */
    @ModuleAction(module = "PRODUCTS", action = "CREATE")
    @PostMapping
    public ResponseEntity<?> addProduct(
            @RequestBody Product product,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        String role = resolveRole(authentication);
        if (!dynamicAuthorizationService.isAllowed(role, httpRequest.getRequestURI(), httpRequest.getMethod())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        return ResponseEntity.ok(productService.save(product));
    }

    /** Update product — roles per DB permissions. */
    @ModuleAction(module = "PRODUCTS", action = "EDIT")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @RequestBody Product product,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        String role = resolveRole(authentication);
        if (!dynamicAuthorizationService.isAllowed(role, httpRequest.getRequestURI(), httpRequest.getMethod())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        return ResponseEntity.ok(productService.update(id, product));
    }

    /** Upload product image — roles per DB permissions. */
    @ModuleAction(module = "PRODUCTS", action = "EDIT")
    @PostMapping("/{id}/image")
    public ResponseEntity<?> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        String role = resolveRole(authentication);
        if (!dynamicAuthorizationService.isAllowed(role, httpRequest.getRequestURI(), httpRequest.getMethod())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        productService.uploadImage(id, file);
        return ResponseEntity.ok("Image uploaded successfully");
    }

    /** Delete product — roles per DB permissions. */
    @ModuleAction(module = "PRODUCTS", action = "DELETE")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(
            @PathVariable Long id,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        String role = resolveRole(authentication);
        if (!dynamicAuthorizationService.isAllowed(role, httpRequest.getRequestURI(), httpRequest.getMethod())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Get all products (including inactive) — public. */
    @ModuleAction(module = "PRODUCTS", action = "VIEW")
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.findAll());
    }

    /** Get active products only — public. */
    @ModuleAction(module = "PRODUCTS", action = "VIEW")
    @GetMapping("/active")
    public ResponseEntity<List<Product>> getActiveProducts() {
        return ResponseEntity.ok(productService.findActive());
    }

    // ── Utility ─────────────────────────────────────────────────────────────

    private String resolveRole(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return "";
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .findFirst()
                .orElse("");
    }
}