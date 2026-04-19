package com.jivRas.groceries.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jivRas.groceries.annotation.ModuleAction;
import com.jivRas.groceries.config.ModuleActionScanner;
import com.jivRas.groceries.config.ModuleActionScanner.ModuleActionEntry;
import com.jivRas.groceries.dto.RolePermissionRequest;
import com.jivRas.groceries.entity.RolePermission;
import com.jivRas.groceries.repository.RolePermissionRepository;
import com.jivRas.groceries.service.DynamicAuthorizationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Admin API for managing the role+module+action permission matrix.
 *
 * <p><strong>Security:</strong> These endpoints are bootstrapped — they CANNOT
 * use the DB-driven {@link DynamicAuthorizationService} to protect themselves
 * (that would be circular). Instead they perform a direct JWT role check
 * ({@code ROLE_ADMIN}) from the Spring Security {@link Authentication} object,
 * which is always populated by {@code JwtAuthFilter} before this controller runs.
 */
@RestController
@RequestMapping("/api/role-permissions")
@RequiredArgsConstructor
public class RolePermissionController {

    private final RolePermissionRepository rolePermissionRepository;
    private final DynamicAuthorizationService dynamicAuthorizationService;
    private final ModuleActionScanner moduleActionScanner;
    private final com.jivRas.groceries.repository.EmployeeUserRepository employeeUserRepository;

    // ── Admin guard ──────────────────────────────────────────────────────────

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())
                            || "ADMIN".equals(a.getAuthority()));
    }

    // ── GET /api/role-permissions/matrix?role=EMPLOYEE ───────────────────────

    /**
     * Returns all module+action permission rows for the requested role.
     * ADMIN only.
     */
    @ModuleAction(module = "ROLE_MANAGEMENT", action = "VIEW")
    @GetMapping("/matrix")
    public ResponseEntity<?> getMatrix(
            @RequestParam String role,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body("Access denied: ADMIN only");
        }

        List<RolePermission> permissions = rolePermissionRepository.findByRole(role.toUpperCase());
        return ResponseEntity.ok(permissions);
    }

    // ── PUT /api/role-permissions/matrix ─────────────────────────────────────

    /**
     * Upsert a single role+module+action permission row.
     * If the row already exists it is updated; otherwise a new one is inserted.
     * ADMIN only.
     */
    @ModuleAction(module = "ROLE_MANAGEMENT", action = "EDIT")
    @PutMapping("/matrix")
    public ResponseEntity<?> upsertMatrix(
            @Valid @RequestBody RolePermissionRequest request,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body("Access denied: ADMIN only");
        }

        String role   = request.getRole().toUpperCase();
        String module = request.getModule().toUpperCase();
        String action = request.getAction().toUpperCase();

        RolePermission rp = rolePermissionRepository
                .findByRoleAndModuleAndAction(role, module, action)
                .orElseGet(RolePermission::new);

        rp.setRole(role);
        rp.setModule(module);
        rp.setAction(action);
        rp.setAllowed(request.isAllowed());

        RolePermission saved = rolePermissionRepository.save(rp);
        dynamicAuthorizationService.evictPermissionCache();
        return ResponseEntity.ok(saved);
    }

    // ── GET /api/role-permissions/modules ────────────────────────────────────

    /**
     * Returns the distinct list of all modules discovered from
     * {@link ModuleActionScanner}'s registry (i.e. from @ModuleAction annotations).
     * Useful for building the admin permission matrix UI.
     * ADMIN only.
     */
    @ModuleAction(module = "ROLE_MANAGEMENT", action = "VIEW")
    @GetMapping("/modules")
    public ResponseEntity<?> getModules(Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body("Access denied: ADMIN only");
        }

        return ResponseEntity.ok(moduleActionScanner.getRegistry());
    }

    // ── GET /api/role-permissions/roles ──────────────────────────────────────

    /**
     * Returns distinct roles that have at least one permission entry in the DB.
     * ADMIN only.
     */
    @ModuleAction(module = "ROLE_MANAGEMENT", action = "VIEW")
    @GetMapping("/roles")
    public ResponseEntity<?> getRoles(Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body("Access denied: ADMIN only");
        }

        List<String> roles = rolePermissionRepository.findDistinctRoles();
        return ResponseEntity.ok(roles);
    }

    // ── POST /api/role-permissions/roles ──────────────────────────────────────

    @ModuleAction(module = "ROLE_MANAGEMENT", action = "CREATE")
    @org.springframework.web.bind.annotation.PostMapping("/roles")
    public ResponseEntity<?> createRole(
            @Valid @RequestBody com.jivRas.groceries.dto.RoleCreateRequest request,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body("Access denied: ADMIN only");
        }

        String roleName = request.getRoleName().trim().toUpperCase();

        if (rolePermissionRepository.existsByRole(roleName)) {
            return ResponseEntity.status(409).body("Role already exists");
        }

        List<RolePermission> toSave = new java.util.ArrayList<>();
        for (com.jivRas.groceries.dto.RoleCreateRequest.PermissionEntry entry : request.getPermissions()) {
            RolePermission rp = new RolePermission();
            rp.setRole(roleName);
            rp.setModule(entry.getModule().toUpperCase());
            rp.setAction(entry.getAction().toUpperCase());
            rp.setAllowed(entry.isAllowed());
            toSave.add(rp);
        }

        List<RolePermission> savedList = rolePermissionRepository.saveAll(toSave);
        dynamicAuthorizationService.evictPermissionCache();

        return ResponseEntity.status(201).body(java.util.Map.of(
                "role", roleName,
                "permissionsCount", savedList.size()
        ));
    }

    // ── DELETE /api/role-permissions/roles/{roleName} ─────────────────────────

    @ModuleAction(module = "ROLE_MANAGEMENT", action = "DELETE")
    @org.springframework.web.bind.annotation.DeleteMapping("/roles/{roleName}")
    public ResponseEntity<?> deleteRole(
            @org.springframework.web.bind.annotation.PathVariable String roleName,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body("Access denied: ADMIN only");
        }

        String role = roleName.trim().toUpperCase();

        if (!rolePermissionRepository.existsByRole(role)) {
            return ResponseEntity.status(404).body("Role not found");
        }

        if (employeeUserRepository.existsByRole(role)) {
            return ResponseEntity.status(409).body("Cannot delete role while employees are assigned to it");
        }

        rolePermissionRepository.deleteAllByRole(role);
        dynamicAuthorizationService.evictPermissionCache();

        return ResponseEntity.ok(java.util.Map.of("message", "Role deleted"));
    }
}
