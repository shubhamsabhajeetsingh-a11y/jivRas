package com.jivRas.groceries.dto;

import lombok.Data;

@Data
public class CreateCustomerRequest {

    private String firstName;
    private String lastName;
    private String mobile;
    private String address;
    private String email;     // optional for customers
    private String username;
    private String password;
}
