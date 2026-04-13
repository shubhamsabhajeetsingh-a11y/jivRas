package com.jivRas.groceries.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.jivRas.groceries.entity.RolePermission;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    /** All permissions for a specific role — used by the matrix view endpoint. */
    List<RolePermission> findByRole(String role);

    /**
     * Look up a single role+module+action entry.
     * Used by {@link com.jivRas.groceries.service.DynamicAuthorizationService}
     * to decide whether a request is allowed.
     */
    Optional<RolePermission> findByRoleAndModuleAndAction(String role, String module, String action);

    /** Distinct modules granted to a specific role. */
    @Query("SELECT DISTINCT rp.module FROM RolePermission rp WHERE rp.role = :role")
    List<String> findDistinctModulesByRole(String role);

    /** Returns all distinct roles that have at least one permission entry. */
    @Query("SELECT DISTINCT rp.role FROM RolePermission rp")
    List<String> findDistinctRoles();

    /** Count rows so the seeder can skip if data already exists. */
    long countByRole(String role);
    
    /** Checks if any permission rows exist for a role */
    boolean existsByRole(String role);
    
    /** Deletes all permission rows for a specific role */
    void deleteAllByRole(String role);
}
