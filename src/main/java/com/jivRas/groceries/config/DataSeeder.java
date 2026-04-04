package com.jivRas.groceries.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.jivRas.groceries.entity.RolePermission;
import com.jivRas.groceries.repository.RolePermissionRepository;

import jakarta.annotation.PostConstruct;

/**
 * Seeds default role-endpoint permissions into {@code role_permissions} on
 * application startup — only if the table is completely empty.
 *
 * <p>Permission design decisions:
 * <ul>
 *   <li>ADMIN uses httpMethod {@code "*"} + endpoint {@code "/**"} to grant
 *       blanket access to every endpoint.</li>
 *   <li>Specific roles receive fine-grained method+endpoint entries.</li>
 *   <li>CUSTOMER only has read access to public product/category listings.</li>
 * </ul>
 */
@Component
public class DataSeeder {

    private final RolePermissionRepository rolePermissionRepository;

    public DataSeeder(RolePermissionRepository rolePermissionRepository) {
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @PostConstruct
    public void seedPermissions() {
        if (rolePermissionRepository.count() > 0) {
            System.out.println("[DataSeeder] role_permissions table already populated — skipping seed.");
            return;
        }

        System.out.println("[DataSeeder] Seeding default role permissions...");
        List<RolePermission> permissions = new ArrayList<>();

        // ── ADMIN ──────────────────────────────────────────────────────────────
        // ADMIN gets wildcard access to everything
        permissions.add(perm("ADMIN", "/**", "*", true));

        // ── BRANCH_MANAGER ─────────────────────────────────────────────────────
        permissions.add(perm("BRANCH_MANAGER", "/api/inventory/**",      "GET",   true));
        permissions.add(perm("BRANCH_MANAGER", "/api/inventory/**",      "POST",  true));
        permissions.add(perm("BRANCH_MANAGER", "/api/inventory/**",      "PUT",   true));
        permissions.add(perm("BRANCH_MANAGER", "/api/branches/{id}",     "GET",   true));
        permissions.add(perm("BRANCH_MANAGER", "/api/branches/active",   "GET",   true));
        permissions.add(perm("BRANCH_MANAGER", "/api/products/**",       "GET",   true));
        permissions.add(perm("BRANCH_MANAGER", "/api/categories/**",     "GET",   true));
        permissions.add(perm("BRANCH_MANAGER", "/api/orders/admin/**",   "GET",   true));
        permissions.add(perm("BRANCH_MANAGER", "/api/orders/**",         "PATCH", true));  // ← status update
        permissions.add(perm("BRANCH_MANAGER", "/api/users/me",          "GET",   true));
        permissions.add(perm("BRANCH_MANAGER", "/api/users/profile",     "GET",   true));

        // ── EMPLOYEE ────────────────────────────────────────────────────────────
        permissions.add(perm("EMPLOYEE", "/api/inventory/my-branch",  "GET",   true));
        permissions.add(perm("EMPLOYEE", "/api/inventory/low-stock",  "GET",   true));
        permissions.add(perm("EMPLOYEE", "/api/inventory/stock",      "POST",  true));
        permissions.add(perm("EMPLOYEE", "/api/branches/active",      "GET",   true));
        permissions.add(perm("EMPLOYEE", "/api/products/**",          "GET",   true));
        permissions.add(perm("EMPLOYEE", "/api/products/**",          "POST",  true));
        permissions.add(perm("EMPLOYEE", "/api/products/**",          "PUT",   true));
        permissions.add(perm("EMPLOYEE", "/api/categories/**",        "GET",   true));
        permissions.add(perm("EMPLOYEE", "/api/orders/admin/**",      "GET",   true));
        permissions.add(perm("EMPLOYEE", "/api/orders/**",            "PATCH", true));  // ← status update
        permissions.add(perm("EMPLOYEE", "/api/users/me",             "GET",   true));
        permissions.add(perm("EMPLOYEE", "/api/users/profile",        "GET",   true));

        // ── CUSTOMER ────────────────────────────────────────────────────────────
        permissions.add(perm("CUSTOMER", "/api/products/**",      "GET",    true));
        permissions.add(perm("CUSTOMER", "/api/categories/**",    "GET",    true));
        permissions.add(perm("CUSTOMER", "/api/cart/**",          "GET",    true));
        permissions.add(perm("CUSTOMER", "/api/cart/**",          "POST",   true));
        permissions.add(perm("CUSTOMER", "/api/cart/**",          "PUT",    true));
        permissions.add(perm("CUSTOMER", "/api/cart/**",          "DELETE", true));
        permissions.add(perm("CUSTOMER", "/api/orders/checkout",  "POST",   true));
        permissions.add(perm("CUSTOMER", "/api/orders/{id}",      "GET",    true));
        permissions.add(perm("CUSTOMER", "/api/users/me",         "GET",    true));
        permissions.add(perm("CUSTOMER", "/api/users/profile",    "GET",    true));
        permissions.add(perm("CUSTOMER", "/api/locations/**",     "GET",    true));

        rolePermissionRepository.saveAll(permissions);
        System.out.println("[DataSeeder] Seeded " + permissions.size() + " permission entries.");
    }

    /** Helper to build a RolePermission without needing an id. */
    private RolePermission perm(String role, String endpoint, String method, boolean allowed) {
        RolePermission rp = new RolePermission();
        rp.setRole(role);
        rp.setEndpoint(endpoint);
        rp.setHttpMethod(method);
        rp.setAllowed(allowed);
        return rp;
    }
}