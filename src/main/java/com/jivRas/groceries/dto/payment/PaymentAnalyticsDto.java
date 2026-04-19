package com.jivRas.groceries.dto.payment;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Aggregate payment analytics for a reporting window — used by the Payments dashboard tab. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAnalyticsDto {

    /** Sum of amounts for PAID payments in the window, in paise. */
    private Long totalPaidAmount;
    /** Sum of amounts for FAILED payments in the window, in paise. */
    private Long totalFailedAmount;
    /** Sum of amounts for REFUNDED payments in the window, in paise. */
    private Long totalRefundedAmount;

    /** Count of PAID payments in the window. */
    private Integer paidCount;
    /** Count of FAILED payments in the window. */
    private Integer failedCount;
    /** Count of CREATED (pending) payments in the window. */
    private Integer pendingCount;

    /**
     * Percentage of non-pending attempts that succeeded: paidCount / (paidCount + failedCount) × 100.
     * Returns 0.0 when there are no completed attempts to avoid division by zero.
     */
    private Double successRate;

    /**
     * Average number of attempts across failed payments in the window.
     * Returns 0.0 when there are no failed payments.
     */
    private Double avgAttemptsPerFailure;

    /** Inclusive start of the reporting window. */
    private LocalDate from;
    /** Inclusive end of the reporting window. */
    private LocalDate to;
}
