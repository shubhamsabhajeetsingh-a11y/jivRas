package com.jivRas.groceries.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.jivRas.groceries.entity.RolePermission;
import com.jivRas.groceries.repository.RolePermissionRepository;

/**
 * Evaluates whether a given role may call a specific endpoint + HTTP method.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>Exact method match (e.g. GET) + exact or wildcard-path permission rows.</li>
 *   <li>Wildcard method ("*") + exact or wildcard-path permission rows.</li>
 * </ol>
 *
 * <p>Endpoint patterns support a trailing {@code /**} wildcard.
 * Example: "/api/inventory/**" matches "/api/inventory/my-branch".
 *
 * <p>Results are cached by (role, endpoint, method) to avoid repeated DB hits.
 * Call {@link #evictPermissionCache()} whenever permissions are mutated via the
 * admin CRUD API so stale entries are cleared immediately.
 */
@Service
public class DynamicAuthorizationService {

    private final RolePermissionRepository rolePermissionRepository;

    public DynamicAuthorizationService(RolePermissionRepository rolePermissionRepository) {
        this.rolePermissionRepository = rolePermissionRepository;
    }

    /**
     * Returns {@code true} if the given role is allowed to access the endpoint
     * with the specified HTTP method.
     *
     * @param role       e.g. "ADMIN", "EMPLOYEE" (WITHOUT "ROLE_" prefix)
     * @param endpoint   the actual request URI, e.g. "/api/inventory/my-branch"
     * @param httpMethod the HTTP verb in uppercase, e.g. "GET"
     */
    @Cacheable(value = "permissions", key = "#role + ':' + #endpoint + ':' + #httpMethod")
    public boolean isAllowed(String role, String endpoint, String httpMethod) {
        if (role == null || endpoint == null || httpMethod == null) {
            return false;
        }

        // Strip ROLE_ prefix if it was passed through from Spring Security
        String cleanRole = role.startsWith("ROLE_") ? role.substring(5) : role;

        // Fetch all rules for this role, then evaluate method in Java so that
        // stored method "*" is treated as a wildcard matching any HTTP method.
        List<RolePermission> allRules = rolePermissionRepository.findByRole(cleanRole);

        boolean matchFound = false;

        for (RolePermission rp : allRules) {
            if (("*".equals(rp.getHttpMethod()) || rp.getHttpMethod().equalsIgnoreCase(httpMethod))
                    && pathMatches(rp.getEndpoint(), endpoint)) {
                if (!rp.isAllowed()) {
                    return false; // Explicit deny takes priority
                }
                matchFound = true;
            }
        }

        return matchFound;
    }

    /**
     * Checks whether a DB-stored endpoint pattern matches the actual request path.
     * Supports a single trailing {@code /**} wildcard.
     *
     * <p>Examples:
     * <ul>
     *   <li>"/api/inventory/**" matches "/api/inventory/my-branch"</li>
     *   <li>"/api/branches/{id}" matches "/api/branches/5" (treated as prefix-less wildcard)</li>
     *   <li>"/api/products" only matches "/api/products" exactly</li>
     * </ul>
     */
    private boolean pathMatches(String pattern, String actualPath) {
        if (pattern == null || actualPath == null) {
            return false;
        }
        // Strip query strings from actual path
        String path = actualPath.split("\\?")[0];

        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }

        // Support {id}-style path variables: convert to prefix match on the parent
        if (pattern.contains("{")) {
            String prefix = pattern.substring(0, pattern.indexOf("{") - 1);
            return path.startsWith(prefix);
        }

        return pattern.equals(path);
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
