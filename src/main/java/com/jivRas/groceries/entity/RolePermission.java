package com.jivRas.groceries.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stores which HTTP method + endpoint path each role is allowed to access.
 * Endpoints may use /** wildcards (e.g. /api/inventory/**).
 */
@Entity
@Table(name = "role_permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Role name stored WITHOUT "ROLE_" prefix, e.g. "ADMIN", "EMPLOYEE". */
    @Column(nullable = false)
    private String role;

    /** Endpoint pattern, supports /** wildcard, e.g. "/api/inventory/**". */
    @Column(nullable = false)
    private String endpoint;

    /** HTTP method: GET, POST, PUT, DELETE, PATCH, or * for all. */
    @Column(nullable = false)
    private String httpMethod;

    /** Whether this role is permitted to call this endpoint+method combination. */
    @Column(nullable = false)
    private boolean isAllowed;
}
