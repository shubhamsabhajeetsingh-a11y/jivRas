package com.jivRas.groceries.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jivRas.groceries.entity.Payment;

/**
 * Persistence interface for Payment records.
 * All queries are Spring Data derived queries — no hand-written JPQL needed.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find a payment record by Razorpay's order identifier.
     * Used during the Razorpay webhook/callback to locate the pending payment record
     * before verifying the signature.
     */
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    /**
     * Retrieve all payment attempts for a given order, newest first.
     * Used to display payment history and determine whether a retry is allowed.
     */
    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
