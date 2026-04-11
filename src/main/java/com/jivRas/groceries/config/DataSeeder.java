package com.jivRas.groceries.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.jivRas.groceries.entity.RolePermission;
import com.jivRas.groceries.repository.RolePermissionRepository;

import jakarta.annotation.PostConstruct;

/**
 * Seeds default role+module+action permissions into {@code role_permissions} on
 * application startup — only if the table is completely empty.
 *
 * <p>SUPER_ADMIN is intentionally omitted; it is short-circuited directly inside
 * {@link com.jivRas.groceries.service.DynamicAuthorizationService} and never
 * needs DB rows.
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

        System.out.println("[DataSeeder] Seeding default role-module-action permissions...");
        List<RolePermission> permissions = new ArrayList<>();

        // ── BRANCH_MANAGER ────────────────────────────────────────────────────
        permissions.add(perm("BRANCH_MANAGER", "INVENTORY",  "VIEW",   true));
        permissions.add(perm("BRANCH_MANAGER", "INVENTORY",  "CREATE", true));
        permissions.add(perm("BRANCH_MANAGER", "INVENTORY",  "EDIT",   true));
        permissions.add(perm("BRANCH_MANAGER", "ORDERS",     "VIEW",   true));
        permissions.add(perm("BRANCH_MANAGER", "ORDERS",     "EDIT",   true));
        permissions.add(perm("BRANCH_MANAGER", "REPORTS",    "VIEW",   true));
        permissions.add(perm("BRANCH_MANAGER", "PRODUCTS",   "VIEW",   true));
        permissions.add(perm("BRANCH_MANAGER", "CATEGORIES", "VIEW",   true));
        permissions.add(perm("BRANCH_MANAGER", "BRANCHES",   "VIEW",   true));
        permissions.add(perm("BRANCH_MANAGER", "USERS",      "VIEW",   true));

        // ── EMPLOYEE ──────────────────────────────────────────────────────────
        permissions.add(perm("EMPLOYEE", "INVENTORY",  "VIEW",   true));
        permissions.add(perm("EMPLOYEE", "INVENTORY",  "EDIT",   true));
        permissions.add(perm("EMPLOYEE", "INVENTORY",  "CREATE", true));
        permissions.add(perm("EMPLOYEE", "ORDERS",     "VIEW",   true));
        permissions.add(perm("EMPLOYEE", "ORDERS",     "EDIT",   true));
        permissions.add(perm("EMPLOYEE", "PRODUCTS",   "VIEW",   true));
        permissions.add(perm("EMPLOYEE", "PRODUCTS",   "CREATE", true));
        permissions.add(perm("EMPLOYEE", "PRODUCTS",   "EDIT",   true));
        permissions.add(perm("EMPLOYEE", "CATEGORIES", "VIEW",   true));
        permissions.add(perm("EMPLOYEE", "USERS",      "VIEW",   true));

        // ── CUSTOMER ──────────────────────────────────────────────────────────
        permissions.add(perm("CUSTOMER", "PRODUCTS",   "VIEW",   true));
        permissions.add(perm("CUSTOMER", "CATEGORIES", "VIEW",   true));
        permissions.add(perm("CUSTOMER", "CART",       "VIEW",   true));
        permissions.add(perm("CUSTOMER", "CART",       "CREATE", true));
        permissions.add(perm("CUSTOMER", "CART",       "EDIT",   true));
        permissions.add(perm("CUSTOMER", "CART",       "DELETE", true));
        permissions.add(perm("CUSTOMER", "ORDERS",     "CREATE", true));
        permissions.add(perm("CUSTOMER", "ORDERS",     "VIEW",   true));
        permissions.add(perm("CUSTOMER", "USERS",      "VIEW",   true));
        permissions.add(perm("CUSTOMER", "LOCATIONS",  "VIEW",   true));

        rolePermissionRepository.saveAll(permissions);
        System.out.println("[DataSeeder] Seeded " + permissions.size() + " permission entries.");
    }

    /** Helper to build a RolePermission without needing an id. */
    private RolePermission perm(String role, String module, String action, boolean allowed) {
        RolePermission rp = new RolePermission();
        rp.setRole(role);
        rp.setModule(module);
        rp.setAction(action);
        rp.setAllowed(allowed);
        return rp;
    }
}