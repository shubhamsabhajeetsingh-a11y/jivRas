package com.jivRas.groceries.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

/**
 * Wires up the Razorpay SDK client as a Spring-managed singleton.
 *
 * The app fails fast at startup if the configured credentials are syntactically
 * invalid — better than a NullPointerException at the first payment attempt.
 */
@Configuration
public class RazorpayConfig {

    /** Razorpay public key ID — safe to expose in the create-order response for checkout.js. */
    @Value("${razorpay.key.id}")
    private String keyId;

    /** Razorpay secret key — never sent to the frontend; used only for server-side calls. */
    @Value("${razorpay.key.secret}")
    private String keySecret;

    /**
     * Singleton RazorpayClient used for all SDK calls (order creation, etc.).
     * Throws at startup rather than deferring failure to the first request.
     */
    @Bean
    public RazorpayClient razorpayClient() {
        try {
            return new RazorpayClient(keyId, keySecret);
        } catch (RazorpayException e) {
            // Rethrow so Spring fails the application context — misconfigured keys
            // should never silently produce a broken app.
            throw new RuntimeException(
                "Failed to initialise RazorpayClient — check razorpay.key.id and razorpay.key.secret in application.properties",
                e
            );
        }
    }

    /**
     * Exposes the secret as a named bean so services can inject it without
     * re-reading the property. RazorpayClient does not expose its secret field
     * publicly, so we need this separate bean for signature verification.
     */
    @Bean(name = "razorpayKeySecret")
    public String razorpayKeySecret() {
        return keySecret;
    }

    /**
     * Exposes the public key ID as a named bean so PaymentService can include
     * it in the create-order response without importing the config class.
     */
    @Bean(name = "razorpayKeyId")
    public String razorpayKeyId() {
        return keyId;
    }
}
