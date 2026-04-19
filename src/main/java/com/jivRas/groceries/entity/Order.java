package com.jivRas.groceries.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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

    /** Username of logged-in user, or guest UUID. Intentionally nullable — guest orders have no user account. */
    @Column(nullable = true)
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

    /**
     * Customer's phone number — required for both registered and guest orders.
     * Used for admin-side phone lookup and guest-to-registered linking.
     * Column default '' ensures existing rows are not broken during schema update.
     */
    @Column(name = "customer_phone", nullable = false, length = 15,
            columnDefinition = "VARCHAR(15) NOT NULL DEFAULT ''")
    private String customerPhone;

    /**
     * Customer's email — optional. Registered users already have this in their User record;
     * captured here for guest orders to enable order confirmation emails.
     */
    @Column(name = "customer_email", length = 100)
    private String customerEmail;

    /**
     * True when the order was placed without a user account (guest checkout).
     * Allows the linking service to migrate guest orders on signup.
     * Column default false keeps existing rows valid during schema update.
     */
    @Column(name = "is_guest", nullable = false,
            columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private Boolean isGuest = false;

    // ──────────── Order Items ────────────

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
}
