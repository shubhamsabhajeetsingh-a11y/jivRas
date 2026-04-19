package com.jivRas.groceries.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body for POST /api/payments/create-order.
 * The frontend passes these values directly to the Razorpay checkout.js widget.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentOrderResponse {

    /** Razorpay's order identifier — passed to checkout.js as `order_id`. Format: "order_XXXXXXXX". */
    private String razorpayOrderId;

    /** Razorpay public key — passed to checkout.js as `key`. Safe to expose to the client. */
    private String razorpayKeyId;

    /** Payment amount in paise (1 INR = 100 paise) — passed to checkout.js as `amount`. */
    private Long amount;

    /** ISO 4217 currency code, e.g. "INR" — passed to checkout.js as `currency`. */
    private String currency;

    /** JivRas internal order ID — the frontend sends this back in the verify request. */
    private Long jivrasOrderId;
}
