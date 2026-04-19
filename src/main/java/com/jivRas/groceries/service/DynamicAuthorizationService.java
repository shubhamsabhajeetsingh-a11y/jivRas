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

        if (role == null || endpoint == null || httpMethod == null) {
            return false;
        }

        String cleanRole = role.startsWith("ROLE_") ? role.substring(5) : role;

        if ("ADMIN".equals(cleanRole) || "SUPER_ADMIN".equals(cleanRole)) {
            return true;
        }

        String[] resolved = moduleActionScanner.resolveModuleAction(httpMethod, endpoint);
        if (resolved == null) {
            return false;
        }

        return queryPermission(cleanRole, resolved[0], resolved[1]);
    }

    /**
     * Checks permission directly by module and action — used by the AOP aspect
     * ({@link com.jivRas.groceries.aspect.ModuleActionAspect}) which already
     * knows the module/action from the {@code @ModuleAction} annotation.
     */
    @Cacheable(value = "permissions", key = "#role + ':' + #module + ':' + #action")
    public boolean isAllowedByModuleAction(String role, String module, String action) {

        if (role == null || module == null || action == null) {
            return false;
        }

        String cleanRole = role.startsWith("ROLE_") ? role.substring(5) : role;

        if ("ADMIN".equals(cleanRole) || "SUPER_ADMIN".equals(cleanRole)) {
            return true;
        }

        return queryPermission(cleanRole, module, action);
    }

    private boolean queryPermission(String cleanRole, String module, String action) {
        Optional<RolePermission> rpOpt =
                rolePermissionRepository.findByRoleAndModuleAndAction(cleanRole, module, action);
        return rpOpt.isPresent() && rpOpt.get().isAllowed();
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
