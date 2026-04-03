package com.jivRas.groceries.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jivRas.groceries.entity.Category;
import com.jivRas.groceries.repository.CategoryRepository;
import com.jivRas.groceries.service.DynamicAuthorizationService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final DynamicAuthorizationService dynamicAuthorizationService;

    /** Public: anyone can read categories. */
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    /** Create category — non-CUSTOMER roles per DB permissions. */
    @PostMapping
    public ResponseEntity<?> createCategory(
            @RequestBody Category category,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        String role = resolveRole(authentication);
        if (!dynamicAuthorizationService.isAllowed(role, httpRequest.getRequestURI(), httpRequest.getMethod())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        return ResponseEntity.ok(categoryRepository.save(category));
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
