package com.jivRas.groceries.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jivRas.groceries.entity.Payment;
import com.jivRas.groceries.enums.PaymentStatus;

/**
 * Persistence interface for Payment records.
 * All queries are Spring Data derived queries — no hand-written JPQL needed
 * except the aggregate SUM which derived queries can't express.
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

    /**
     * Retrieve all payment attempts for a given order, oldest first.
     * Used to build the timeline DTO where the most recent attempt is the last element.
     */
    List<Payment> findByOrderIdOrderByCreatedAtAsc(Long orderId);

    /** Total count of payments in a given status — for overall health dashboard. */
    long countByStatus(PaymentStatus status);

    /**
     * All payments in a given status, newest first.
     * Used by the admin list endpoint when the caller filters by status only.
     */
    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);

    /**
     * All payments within a date window, newest first.
     * Used by the admin list endpoint when the caller filters by date range only.
     */
    List<Payment> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);

    /**
     * Payments matching both a status and a date window, newest first.
     * Used by analytics to stream failed payments within a reporting period
     * so the service can average the {@code attempts} field.
     */
    List<Payment> findByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
            PaymentStatus status, LocalDateTime from, LocalDateTime to);

    /**
     * Count of payments in a given status within a date window.
     * Used by analytics to compute paidCount, failedCount, pendingCount.
     */
    long countByStatusAndCreatedAtBetween(PaymentStatus status, LocalDateTime from, LocalDateTime to);

    /**
     * Monetary sum of all payments in a given status within a date window.
     * Spring Data derived queries can't express SUM, so JPQL is used here.
     * COALESCE ensures 0 is returned rather than null when no rows match.
     * Returns paise (1 INR = 100 paise).
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.status = :status AND p.createdAt BETWEEN :from AND :to")
    Long sumAmountByStatusBetween(
            @Param("status") PaymentStatus status,
            @Param("from")   LocalDateTime from,
            @Param("to")     LocalDateTime to);
}
