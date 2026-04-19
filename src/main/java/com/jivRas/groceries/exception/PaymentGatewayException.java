package com.jivRas.groceries.exception;

/**
 * Thrown when communication with the Razorpay payment gateway fails —
 * e.g. network error, invalid API response, or unexpected SDK exception.
 * Wraps the original RazorpayException so the full stack trace is preserved.
 */
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
