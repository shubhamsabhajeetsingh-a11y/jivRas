package com.jivRas.groceries.dto;
 
import lombok.Data;
 
@Data
public class CreateEmployeeRequest {
 
    private String firstName;
    private String lastName;
    private String mobile;
    private String address;
    private String email;
    private String username;
    private String password;
 
    // Role to assign — "EMPLOYEE" or "BRANCH_MANAGER"
    // ADMIN cannot be created via this endpoint
    private String role;
 
    /**
     * NEW FIELD — branchId
     *
     * Required for EMPLOYEE and BRANCH_MANAGER roles.
     * ADMIN accounts do not need a branchId (leave null).
     *
     * The Super Admin selects which branch this employee belongs to
     * at the time of account creation. Employee cannot change this.
     */
    private Long branchId;
}