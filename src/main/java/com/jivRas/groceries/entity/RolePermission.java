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
 * Stores which module+action each role is allowed to perform.
 *
 * <p>The logical module/action pair is resolved from an incoming HTTP request by
 * {@link com.jivRas.groceries.config.ModuleActionScanner} using the
 * {@link com.jivRas.groceries.annotation.ModuleAction} annotations placed on
 * controller methods — no raw endpoints or HTTP methods are stored here.
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

    /** Role name stored WITHOUT "ROLE_" prefix, e.g. "EMPLOYEE", "BRANCH_MANAGER". */
    @Column(nullable = false)
    private String role;

    /** Logical module, e.g. "INVENTORY", "ORDERS", "PRODUCTS". */
    @Column(nullable = false)
    private String module;

    /** Logical action, e.g. "VIEW", "CREATE", "EDIT", "DELETE". */
    @Column(nullable = false)
    private String action;

    /** Whether this role is permitted to perform this module+action combination. */
    @Column(nullable = false)
    private boolean isAllowed;
}
