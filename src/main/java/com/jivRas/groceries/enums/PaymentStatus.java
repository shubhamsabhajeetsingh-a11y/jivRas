package com.jivRas.groceries.enums;

/**
 * Lifecycle states for a Razorpay payment attempt.
 */
public enum PaymentStatus {

    /** Razorpay order created and sent to the frontend; customer has not yet paid. */
    CREATED,

    /** Payment completed by the customer and HMAC-SHA256 signature verified by backend. */
    PAID,

    /** Payment attempt failed, was declined, or the Razorpay order expired. */
    FAILED,

    /** Payment was successfully refunded to the customer via Razorpay. */
    REFUNDED
}
