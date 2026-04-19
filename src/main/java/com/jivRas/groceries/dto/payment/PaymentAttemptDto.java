package com.jivRas.groceries.dto.payment;

import java.time.LocalDateTime;

import com.jivRas.groceries.enums.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One row in a payment attempt timeline — maps 1:1 to a Payment entity. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAttemptDto {

    private Long id;
    private String razorpayOrderId;
    /** Null until the customer completes payment in the Razorpay modal. */
    private String razorpayPaymentId;
    /** Amount in paise (1 INR = 100 paise). */
    private Long amount;
    private String currency;
    private PaymentStatus status;
    private Integer attempts;
    private LocalDateTime createdAt;
    /** Null until status transitions to PAID. */
    private LocalDateTime verifiedAt;
    /**
     * Human-readable failure reason — null for now; reserved for future
     * Razorpay webhook data that carries a decline reason.
     */
    private String failureReason;
}
