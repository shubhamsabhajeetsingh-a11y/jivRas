package com.jivRas.groceries.config;

import com.jivRas.groceries.entity.RolePermission;
import com.jivRas.groceries.repository.RolePermissionRepository;
import com.jivRas.groceries.service.DynamicAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Idempotent permission migration — runs after Spring fully starts.
 *
 * <p>Unlike DataSeeder (which only seeds when the table is empty), this class
 * ensures specific permissions always exist, making it safe to run on every
 * startup even after the table has been populated. It is a no-op when the
 * entry already exists.
 *
 * <p>Add new entries here whenever a new endpoint needs RBAC coverage and
 * the DataSeeder has already run in production.
 */
@Component
@RequiredArgsConstructor
public class PermissionMigration implements ApplicationRunner {

    private final RolePermissionRepository rolePermissionRepository;
    private final DynamicAuthorizationService dynamicAuthorizationService;

    @Override
    public void run(ApplicationArguments args) {
        boolean changed = false;

        // ── Day 3 + Day 4: GET /api/orders/** ──────────────────────────────────
        // Covers: /{id}/timeline, /{id}/invoice, /{id} — any GET sub-path.
        // ADMIN already has wildcard /** + * so it is intentionally excluded.
        changed |= ensurePermission("EMPLOYEE",       "/api/orders/**", "GET", true);
        changed |= ensurePermission("BRANCH_MANAGER", "/api/orders/**", "GET", true);

        if (changed) {
            // Flush the Spring Cache so stale isAllowed() results are not served.
            dynamicAuthorizationService.evictPermissionCache();
            System.out.println("[PermissionMigration] New permissions added — cache evicted.");
        } else {
            System.out.println("[PermissionMigration] All required permissions already present.");
        }
    }

    /**
     * Inserts the permission only if no row with the same role + endpoint + method
     * already exists. Returns {@code true} if a new row was inserted.
     */
    private boolean ensurePermission(String role, String endpoint, String method, boolean allowed) {
        List<RolePermission> existing = rolePermissionRepository.findByRole(role);
        boolean alreadyExists = existing.stream()
                .anyMatch(rp -> rp.getEndpoint().equals(endpoint)
                        && rp.getHttpMethod().equals(method));

        if (!alreadyExists) {
            RolePermission rp = new RolePermission();
            rp.setRole(role);
            rp.setEndpoint(endpoint);
            rp.setHttpMethod(method);
            rp.setAllowed(allowed);
            rolePermissionRepository.save(rp);
            System.out.printf("[PermissionMigration] Inserted: %-16s %-8s %s%n", role, method, endpoint);
            return true;
        }
        return false;
    }
}
