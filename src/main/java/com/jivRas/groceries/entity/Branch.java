package com.jivRas.groceries.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "branches")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Display name of the branch — e.g. "Virar Store", "Vasai Store"
    @Column(nullable = false)
    private String name;

    // Full street address of this branch
    private String address;

    // Pincode used for order routing (customer pincode → nearest branch)
    private String pincode;

    // City where this branch is located
    private String city;

    // Username of the Branch Manager assigned to this branch
    // Nullable — a branch can exist without a manager initially
    private String managerUsername;

    // If false, this branch is soft-deleted or temporarily closed
    @Column(nullable = false)
    private boolean active = true;

    // Timestamp when this branch was created — auto set on save
    private LocalDateTime createdAt;
}