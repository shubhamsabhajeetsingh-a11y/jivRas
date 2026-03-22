package com.jivRas.groceries.dto;

import lombok.Data;

@Data
public class CreateEmployeeRequest {

    private String firstName;
    private String lastName;
    private String mobile;
    private String address;
    private String email;     // mandatory for employees
    private String username;
    private String password;
    private String role;      // e.g. EMPLOYEE, ADMIN
}
