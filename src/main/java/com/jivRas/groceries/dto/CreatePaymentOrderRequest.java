package com.jivRas.groceries.dto;

import lombok.Data;

/**
 * Request body for POST /api/payments/create-order.
 * The frontend sends the JivRas order ID after a successful checkout.
 */
@Data
public class CreatePaymentOrderRequest {

    /** The JivRas internal order ID for which a Razorpay payment order should be created. */
    private Long orderId;
}
