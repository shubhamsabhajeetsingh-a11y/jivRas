package com.jivRas.groceries.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "employee_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;

    /**
     * Role values:
     *   "EMPLOYEE"        — Can view and update only their own branch inventory/orders
     *   "BRANCH_MANAGER"  — NEW: Manages a specific branch, can create employees for that branch
     *   "ADMIN"           — Super Admin, can see and manage all branches
     */
    private String role;

    private String firstName;
    private String lastName;
    private String mobile;
    private String address;
    private String email;

    // Username of the employee/admin who created this account
    private String createdBy;

    /**
     * NEW FIELD — branchId
     *
     * Rules:
     *   EMPLOYEE       → must have a branchId (assigned at creation)
     *   BRANCH_MANAGER → must have a branchId (assigned at creation)
     *   ADMIN          → branchId is NULL (they see all branches)
     *
     * This is the KEY field that locks an employee to their branch.
     * Set once at account creation. Cannot be changed by employee themselves.
     */
    private Long branchId;
}