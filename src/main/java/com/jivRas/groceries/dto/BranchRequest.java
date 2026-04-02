package com.jivRas.groceries.dto;
 
import lombok.Data;
 
@Data
public class BranchRequest {
 
    // Name of the branch — e.g. "Virar Store"
    private String name;
 
    // Street address of this branch
    private String address;
 
    // Pincode — used to route customer orders to nearest branch
    private String pincode;
 
    // City — e.g. "Virar", "Vasai"
    private String city;
 
    // Optional: assign a branch manager at creation time
    // This should be the username of an existing BRANCH_MANAGER employee
    private String managerUsername;
}
 