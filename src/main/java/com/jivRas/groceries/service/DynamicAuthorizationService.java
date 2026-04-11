package com.jivRas.groceries.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.jivRas.groceries.config.ModuleActionScanner;
import com.jivRas.groceries.entity.RolePermission;
import com.jivRas.groceries.repository.RolePermissionRepository;

import java.util.Optional;

/**
 * Evaluates whether a given role may call a specific endpoint + HTTP method
 * by mapping the request to a logical module+action pair via
 * {@link ModuleActionScanner}, then querying the {@code role_permissions} table.
 *
 * <p>Resolution steps:
 * <ol>
 *   <li>Null-check inputs → deny.</li>
 *   <li>Strip {@code ROLE_} prefix from Spring Security authority strings.</li>
 *   <li>SUPER_ADMIN bypass — always allowed, no DB query needed.</li>
 *   <li>Resolve {@code (httpMethod, uri)} → {@code (module, action)} via the
 *       in-memory scanner registry.</li>
 *   <li>Unmapped endpoint → deny by default.</li>
 *   <li>Query DB for {@code (role, module, action)} → if absent → deny.</li>
 *   <li>Return {@code rp.isAllowed()}.</li>
 * </ol>
 *
 * <p>Results are cached by {@code (role, endpoint, method)} to avoid repeated
 * DB hits. Call {@link #evictPermissionCache()} whenever permissions are mutated
 * via the admin API so stale entries are refreshed immediately.
 */
@Service
public class DynamicAuthorizationService {

    private final RolePermissionRepository rolePermissionRepository;
    private final ModuleActionScanner moduleActionScanner;

    public DynamicAuthorizationService(
            RolePermissionRepository rolePermissionRepository,
            ModuleActionScanner moduleActionScanner) {
        this.rolePermissionRepository = rolePermissionRepository;
        this.moduleActionScanner = moduleActionScanner;
    }

    /**
     * Returns {@code true} if the given role is allowed to access the endpoint
     * with the specified HTTP method.
     *
     * @param role       e.g. "ROLE_EMPLOYEE" or "EMPLOYEE" — both accepted
     * @param endpoint   the actual request URI, e.g. "/api/inventory/my-branch"
     * @param httpMethod the HTTP verb in uppercase, e.g. "GET"
     */
    @Cacheable(value = "permissions", key = "#role + ':' + #endpoint + ':' + #httpMethod")
    public boolean isAllowed(String role, String endpoint, String httpMethod) {

        System.out.println("RBAC CHECK → role: [" + role + "] endpoint: [" + endpoint + "] method: [" + httpMethod + "]");

        // Step 1: null guard
        if (role == null || endpoint == null || httpMethod == null) {
            return false;
        }

        // Step 2: strip ROLE_ prefix
        String cleanRole = role.startsWith("ROLE_") ? role.substring(5) : role;
        System.out.println("cleanRole: [" + cleanRole + "] equals SUPER_ADMIN? " + "SUPER_ADMIN".equals(cleanRole));

        // Step 3: ADMIN / SUPER_ADMIN bypass — always allowed, no DB query needed
        if ("ADMIN".equals(cleanRole) || "SUPER_ADMIN".equals(cleanRole)) {
            return true;
        }

        // Step 4: resolve incoming request → module + action
        String[] resolved = moduleActionScanner.resolveModuleAction(httpMethod, endpoint);

        // Step 5: unmapped endpoint → denied by default
        if (resolved == null) {
            return false;
        }

        String module = resolved[0];
        String action = resolved[1];

        // Step 6: query DB
        Optional<RolePermission> rpOpt =
                rolePermissionRepository.findByRoleAndModuleAndAction(cleanRole, module, action);

        // Step 7: no row → denied
        if (rpOpt.isEmpty()) {
            return false;
        }

        // Step 8: return DB-stored flag
        return rpOpt.get().isAllowed();
    }

    /**
     * Evict the entire permissions cache.
     * Call this after any CRUD mutation on role_permissions so cached
     * {@code isAllowed()} results are refreshed on the next request.
     */
    @CacheEvict(value = "permissions", allEntries = true)
    public void evictPermissionCache() {
        // Spring handles cache eviction automatically via the annotation
    }
}
