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
import org.springframework.web.bind.annotation.RestController;

import com.jivRas.groceries.dto.RolePermissionRequest;
import com.jivRas.groceries.entity.RolePermission;
import com.jivRas.groceries.repository.RolePermissionRepository;
import com.jivRas.groceries.service.DynamicAuthorizationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * CRUD API for managing role-permission entries at runtime.
 *
 * <p><strong>Security:</strong> All endpoints here are bootstrapped — they
 * CANNOT use the DB-driven {@link DynamicAuthorizationService} to protect
 * themselves (that would be circular). Instead they perform a direct JWT role
 * check ({@code ROLE_ADMIN}) from the Spring Security {@link Authentication}
 * object, which is always populated by {@code JwtAuthFilter} before this
 * controller runs.
 */
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class RolePermissionController {

    private final RolePermissionRepository rolePermissionRepository;
    private final DynamicAuthorizationService dynamicAuthorizationService;

    // ──────────────────────── Admin guard helper ────────────────────────────

    /**
     * Direct JWT-based ADMIN check. Returns {@code true} if the Authentication
     * contains the {@code ROLE_ADMIN} authority.
     * Uses Spring Security context (populated by JwtAuthFilter) — no DB call.
     */
    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())
                            || "ADMIN".equals(a.getAuthority()));
    }

    // ──────────────────────── GET /api/permissions ──────────────────────────

    /**
     * List all role-permission entries.
     * ADMIN only.
     */
    @GetMapping
    public ResponseEntity<?> listAll(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body("Access denied: ADMIN only");
        }
        List<RolePermission> all = rolePermissionRepository.findAll();
        return ResponseEntity.ok(all);
    }

    // ──────────────────────── POST /api/permissions ─────────────────────────

    /**
     * Add a new permission rule.
     * ADMIN only.
     */
    @PostMapping
    public ResponseEntity<?> addPermission(
            @Valid @RequestBody RolePermissionRequest request,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body("Access denied: ADMIN only");
        }

        RolePermission rp = new RolePermission();
        rp.setRole(request.getRole().toUpperCase());
        rp.setEndpoint(request.getEndpoint());
        rp.setHttpMethod(request.getHttpMethod().toUpperCase());
        rp.setAllowed(request.isAllowed());

        RolePermission saved = rolePermissionRepository.save(rp);

        // Bust the cache so the new rule takes effect immediately
        dynamicAuthorizationService.evictPermissionCache();

        return ResponseEntity.ok(saved);
    }

    // ──────────────────────── PUT /api/permissions/{id} ─────────────────────

    /**
     * Update an existing permission rule by ID.
     * ADMIN only.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePermission(
            @PathVariable Long id,
            @Valid @RequestBody RolePermissionRequest request,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body("Access denied: ADMIN only");
        }

        return rolePermissionRepository.findById(id)
                .map(existing -> {
                    existing.setRole(request.getRole().toUpperCase());
                    existing.setEndpoint(request.getEndpoint());
                    existing.setHttpMethod(request.getHttpMethod().toUpperCase());
                    existing.setAllowed(request.isAllowed());
                    RolePermission updated = rolePermissionRepository.save(existing);
                    dynamicAuthorizationService.evictPermissionCache();
                    return ResponseEntity.ok((Object) updated);
                })
                .orElseGet(() -> ResponseEntity.status(404)
                        .body("Permission with id=" + id + " not found"));
    }

    // ──────────────────────── DELETE /api/permissions/{id} ──────────────────

    /**
     * Remove a permission rule by ID.
     * ADMIN only.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePermission(
            @PathVariable Long id,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body("Access denied: ADMIN only");
        }

        if (!rolePermissionRepository.existsById(id)) {
            return ResponseEntity.status(404).body("Permission with id=" + id + " not found");
        }

        rolePermissionRepository.deleteById(id);
        dynamicAuthorizationService.evictPermissionCache();
        return ResponseEntity.ok("Permission deleted successfully");
    }

    // ──────────────────────── GET /api/permissions/role/{roleName} ───────────

    /**
     * List all permission entries for a specific role.
     * Used by the Role Definition tab to lazily load permissions per role.
     * ADMIN only.
     */
    @GetMapping("/role/{roleName}")
    public ResponseEntity<?> getPermissionsByRole(
            @PathVariable String roleName,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body("Access denied: ADMIN only");
        }

        List<RolePermission> permissions = rolePermissionRepository.findByRole(roleName.toUpperCase());
        return ResponseEntity.ok(permissions);
    }

    // ──────────────────────── PUT /api/permissions/{id}/toggle ──────────────

    /**
     * Toggle the isAllowed boolean on a single permission entry.
     * Used by the inline toggles in the Role Definition permissions table.
     * ADMIN only.
     */
    @PutMapping("/{id}/toggle")
    public ResponseEntity<?> togglePermission(
            @PathVariable Long id,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body("Access denied: ADMIN only");
        }

        return rolePermissionRepository.findById(id)
                .map(rp -> {
                    rp.setAllowed(!rp.isAllowed());
                    RolePermission saved = rolePermissionRepository.save(rp);
                    dynamicAuthorizationService.evictPermissionCache();
                    return ResponseEntity.ok((Object) saved);
                })
                .orElseGet(() -> ResponseEntity.status(404)
                        .body("Permission with id=" + id + " not found"));
    }
}
