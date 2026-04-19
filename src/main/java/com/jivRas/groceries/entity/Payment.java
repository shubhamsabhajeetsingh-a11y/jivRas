package com.jivRas.groceries.entity;

import java.time.LocalDateTime;

import com.jivRas.groceries.enums.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a single Razorpay payment attempt for an order.
 * One order can have multiple Payment rows (e.g., failed attempt then retry).
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment {

    /** Auto-incremented surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The order this payment attempt belongs to.
     * Many payment attempts can be linked to a single order (retries, refunds).
     */
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Razorpay's identifier for the payment order — returned by their Create Order API.
     * Format: "order_XXXXXXXXXXXXXXXXXX". Used to initialise the Razorpay checkout widget.
     */
    @Column(name = "razorpay_order_id", nullable = false, length = 50)
    private String razorpayOrderId;

    /**
     * Razorpay's identifier for the completed payment — sent by the frontend after the user pays.
     * Null until the customer finishes the checkout flow.
     */
    @Column(name = "razorpay_payment_id", length = 50)
    private String razorpayPaymentId;

    /**
     * HMAC-SHA256 signature provided by Razorpay after payment.
     * Null until backend verification is performed; set to the verified value on success.
     */
    @Column(name = "razorpay_signature", length = 255)
    private String razorpaySignature;

    /**
     * Payment amount in paise (smallest INR unit: 1 INR = 100 paise).
     * Stored as Long to avoid floating-point precision issues with monetary values.
     */
    @Column(nullable = false)
    private Long amount;

    /** ISO 4217 currency code (e.g., "INR"). Defaults to "INR" in @PrePersist. */
    @Column(nullable = false, length = 3)
    private String currency;

    /** Current lifecycle state of this payment attempt. Stored as a string for readability in DB. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    /**
     * Number of times payment was attempted for this record.
     * Starts at 0 (set by @PrePersist); incremented by the service layer on each retry.
     */
    @Column(nullable = false)
    private Integer attempts;

    /** When this payment record was first saved. Set automatically by @PrePersist; never updated. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * When Razorpay signature verification succeeded.
     * Null until status transitions to PAID.
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /**
     * Runs before the first INSERT.
     * Sets safe defaults so callers only need to provide order, razorpayOrderId, and amount.
     */
    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = PaymentStatus.CREATED;
        }
        if (attempts == null) {
            attempts = 0;
        }
        if (currency == null) {
            currency = "INR";
        }
    }
}
