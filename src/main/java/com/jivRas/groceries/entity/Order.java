package com.jivRas.groceries.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Order entity — created when a customer checks out their cart.
 * Delivery details are embedded directly on the order for simplicity.
 * userId is nullable to support guest checkout.
 */
@Entity
@Table(name = "orders")
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Username of logged-in user, or guest UUID. Nullable for legacy data. */
    private String userId;

    private double totalAmount;

    /** CREATED, CONFIRMED, DISPATCHED, DELIVERED, CANCELLED */
    private String orderStatus;

    private LocalDateTime orderDate;

    private int estimatedDeliveryDays;

    // ──────────── Delivery Details (embedded) ────────────

    private String customerName;
    private String mobile;
    private String addressLine;
    private String city;
    private String state;
    private String pincode;

    // ──────────── Order Items ────────────

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
}
