package com.jivRas.groceries.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.jivRas.groceries.entity.EmployeeUser;

public interface EmployeeUserRepository extends JpaRepository<EmployeeUser, Long> {
    Optional<EmployeeUser> findByUsername(String username);

    @Query("SELECT DISTINCT e.role FROM EmployeeUser e WHERE e.role IS NOT NULL AND e.role <> 'ADMIN'")
    List<String> findDistinctRolesExcludingAdmin();

    /** All employees excluding ADMIN — used by Role Definition tab */
    List<EmployeeUser> findAllByRoleNot(String role);
    
    /** Check if any users are assigned to this role */
    boolean existsByRole(String role);
}
