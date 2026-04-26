package com.jivRas.groceries.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "Customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Basic profile properties (used by both guests and registered)
    private String name;
    private String mobile;
    private String addressLine;
    private String city;
    private String state;
    private String pincode;
    
    // Login properties
    private String username;
    private String password;
    private String role; // Usually "CUSTOMER"
    
    // Flag to distinguish registered customers from guest checkouts
    @Column(name = "is_account_created")
    private boolean accountCreated;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
