package com.jivRas.groceries.dto;
 
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchResponse {
 
    private Long id;
    private String name;
    private String address;
    private String pincode;
    private String city;
    private String managerUsername;
    private boolean active;
}