package com.jivRas.groceries.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jivRas.groceries.entity.RolePermission;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    /**
     * Fetch all permission rules for a given role + HTTP method combination.
     * Wildcards (method = "*") are intentionally NOT handled here — the service
     * layer resolves them via a separate query below.
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.role = :role AND rp.httpMethod = :method")
    List<RolePermission> findByRoleAndMethod(@Param("role") String role, @Param("method") String method);

    /**
     * Also fetch wildcard-method permissions (httpMethod = "*") for this role.
     * Used together with findByRoleAndMethod to support ADMIN → ALL methods.
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.role = :role AND rp.httpMethod = '*'")
    List<RolePermission> findWildcardByRole(@Param("role") String role);

    /** Count rows so the seeder can skip if data already exists. */
    long countByRole(String role);

    /** Returns all distinct roles that have at least one permission entry. */
    @Query("SELECT DISTINCT rp.role FROM RolePermission rp")
    List<String> findDistinctRoles();

    /** All permissions for a specific role — used by Role Definition permission panel. */
    List<RolePermission> findByRole(String role);
}
