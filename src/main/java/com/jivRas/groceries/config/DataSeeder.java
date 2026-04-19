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
            // Run incremental seeds — each uses its own idempotency check so they
            // are safe to call even when the rest of the table is already populated.
            seedPaymentPermissions();
            seedGuestPermissions();
            seedOrderViewAllPermissions();
            return;
        }

        System.out.println("[DataSeeder] Seeding default role-module-action permissions...");
        List<RolePermission> permissions = new ArrayList<>();

        // These are system default roles. Additional roles can be created via POST /api/role-permissions/roles.

        // ── BRANCH_MANAGER ────────────────────────────────────────────────────
        permissions.add(perm("BRANCH_MANAGER", "INVENTORY",  "VIEW",   true));
        permissions.add(perm("BRANCH_MANAGER", "INVENTORY",  "CREATE", true));
        permissions.add(perm("BRANCH_MANAGER", "INVENTORY",  "EDIT",   true));
        permissions.add(perm("BRANCH_MANAGER", "ORDERS",     "VIEW",   true));
        permissions.add(perm("BRANCH_MANAGER", "ORDERS",     "VIEW_ALL", true));
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
        permissions.add(perm("EMPLOYEE", "ORDERS",     "VIEW_ALL", true));
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
        permissions.add(perm("CUSTOMER", "ORDERS",     "VIEW_ALL", false));
        permissions.add(perm("CUSTOMER", "USERS",      "VIEW",   true));
        permissions.add(perm("CUSTOMER", "LOCATIONS",  "VIEW",   true));

        // ── GUEST ─────────────────────────────────────────────────────────────────
        // Unauthenticated visitors — can browse, cart, checkout, and pay without
        // registering. Identity is tracked via X-Guest-Id header, not a JWT.
        permissions.addAll(buildGuestPermissions());

        // ── PAYMENT (included here so fresh installs get it in a single saveAll) ──
        permissions.addAll(buildPaymentPermissions());

        permissions.addAll(buildOrderViewAllPermissions());

        rolePermissionRepository.saveAll(permissions);
        System.out.println("[DataSeeder] Seeded " + permissions.size() + " permission entries.");
    }

    /**
     * Incrementally seeds PAYMENT permissions — inserts only rows that are absent.
     * Safe to call on every startup; adding a new row to buildPaymentPermissions()
     * will automatically insert it on the next restart without touching existing rows.
     */
    private void seedPaymentPermissions() {
        seedMissingRows("PAYMENT", buildPaymentPermissions());
    }

    /**
     * Incrementally seeds GUEST permissions — inserts only rows that are absent.
     * Safe to call on every startup; adding a new row to buildGuestPermissions()
     * will automatically insert it on the next restart without touching existing rows.
     */
    private void seedGuestPermissions() {
        seedMissingRows("GUEST", buildGuestPermissions());
    }

    private void seedOrderViewAllPermissions() {
        seedMissingRows("ORDER_VIEW_ALL", buildOrderViewAllPermissions());
    }

    /**
     * Inserts any rows from {@code candidates} that do not yet exist in the DB.
     * Uses a per-row presence check so the method is fully idempotent and
     * future additions to any build*() method self-seed on next restart.
     */
    private void seedMissingRows(String label, List<RolePermission> candidates) {
        int inserted = 0;
        for (RolePermission rp : candidates) {
            boolean exists = rolePermissionRepository
                    .findByRoleAndModuleAndAction(rp.getRole(), rp.getModule(), rp.getAction())
                    .isPresent();
            if (!exists) {
                rolePermissionRepository.save(rp);
                inserted++;
            }
        }
        if (inserted > 0) {
            System.out.println("[DataSeeder] Seeded " + inserted + " new " + label + " permission entries.");
        } else {
            System.out.println("[DataSeeder] " + label + " permissions already complete — nothing to add.");
        }
    }

    /** Builds all GUEST permission rows (shared between fresh and incremental seed). */
    private List<RolePermission> buildGuestPermissions() {
        List<RolePermission> list = new ArrayList<>();

        // Read-only public browsing
        list.add(perm("GUEST", "PRODUCTS",   "VIEW",   true));
        list.add(perm("GUEST", "CATEGORIES", "VIEW",   true));
        list.add(perm("GUEST", "LOCATIONS",  "VIEW",   true));
        // Guest cart and checkout (identity tracked via X-Guest-Id header)
        list.add(perm("GUEST", "CART",       "VIEW",   true));
        list.add(perm("GUEST", "CART",       "CREATE", true));
        list.add(perm("GUEST", "CART",       "EDIT",   true));
        list.add(perm("GUEST", "ORDERS",     "CREATE", true));
        list.add(perm("GUEST", "ORDERS",     "VIEW",   true));
        list.add(perm("GUEST", "PAYMENT",    "CREATE", true));
        list.add(perm("GUEST", "PAYMENT",    "VERIFY", true));

        return list;
    }

    /** Builds the PAYMENT module permission rows (shared between fresh and incremental seed). */
    private List<RolePermission> buildPaymentPermissions() {
        List<RolePermission> list = new ArrayList<>();

        // CUSTOMER: can initiate, verify, and view their own payments
        list.add(perm("CUSTOMER",        "PAYMENT", "CREATE", true));
        list.add(perm("CUSTOMER",        "PAYMENT", "VERIFY", true));
        list.add(perm("CUSTOMER",        "PAYMENT", "VIEW",   true));

        // EMPLOYEE: no payment access — payments are customer-initiated only
        list.add(perm("EMPLOYEE",        "PAYMENT", "CREATE", false));
        list.add(perm("EMPLOYEE",        "PAYMENT", "VERIFY", false));
        list.add(perm("EMPLOYEE",        "PAYMENT", "VIEW",   false));

        // BRANCH_MANAGER: read-only view for payment health; no initiate/verify
        list.add(perm("BRANCH_MANAGER",  "PAYMENT", "CREATE", false));
        list.add(perm("BRANCH_MANAGER",  "PAYMENT", "VERIFY", false));
        list.add(perm("BRANCH_MANAGER",  "PAYMENT", "VIEW",   true));

        // GUEST: can create and verify their own payment but cannot use the admin view endpoints
        list.add(perm("GUEST",           "PAYMENT", "VIEW",   false));

        return list;
    }

    private List<RolePermission> buildOrderViewAllPermissions() {
        List<RolePermission> list = new ArrayList<>();
        list.add(perm("BRANCH_MANAGER",  "ORDERS", "VIEW_ALL", true));
        list.add(perm("EMPLOYEE",        "ORDERS", "VIEW_ALL", true));
        list.add(perm("CUSTOMER",        "ORDERS", "VIEW_ALL", false));
        list.add(perm("GUEST",           "ORDERS", "VIEW_ALL", false));
        return list;
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