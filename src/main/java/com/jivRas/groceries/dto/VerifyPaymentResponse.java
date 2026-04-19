package com.jivRas.groceries.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body for POST /api/payments/verify.
 * Tells the frontend whether the payment was successfully verified and
 * what the JivRas order status is now.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPaymentResponse {

    /** True if Razorpay signature verification passed and order is confirmed. */
    private boolean success;

    /** Human-readable outcome message suitable for displaying in the UI. */
    private String message;

    /** JivRas order status after this verification — e.g. "CONFIRMED" or unchanged on failure. */
    private String orderStatus;
}
