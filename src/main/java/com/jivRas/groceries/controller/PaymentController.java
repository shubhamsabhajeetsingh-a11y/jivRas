package com.jivRas.groceries.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jivRas.groceries.annotation.ModuleAction;
import com.jivRas.groceries.dto.CreatePaymentOrderRequest;
import com.jivRas.groceries.dto.CreatePaymentOrderResponse;
import com.jivRas.groceries.dto.VerifyPaymentRequest;
import com.jivRas.groceries.dto.VerifyPaymentResponse;
import com.jivRas.groceries.service.PaymentService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for payment lifecycle endpoints.
 * Delegates entirely to {@link PaymentService} — no business logic here.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/payments/create-order
     *
     * Creates a Razorpay order for the given JivRas order.
     * The frontend calls this before showing the Razorpay checkout modal.
     * Returns the Razorpay order ID, public key, amount, and currency needed
     * to initialise checkout.js.
     */
    @PostMapping("/create-order")
    @ModuleAction(module = "PAYMENT", action = "CREATE")
    public ResponseEntity<CreatePaymentOrderResponse> createOrder(
            @RequestBody CreatePaymentOrderRequest req) {

        CreatePaymentOrderResponse response = paymentService.createRazorpayOrder(req);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/payments/verify
     *
     * Verifies the Razorpay payment signature after the user completes payment.
     * The frontend sends the three Razorpay identifiers from the checkout.js
     * success callback. Returns whether verification passed and the updated order status.
     */
    @PostMapping("/verify")
    @ModuleAction(module = "PAYMENT", action = "VERIFY")
    public ResponseEntity<VerifyPaymentResponse> verify(
            @RequestBody VerifyPaymentRequest req) {

        VerifyPaymentResponse response = paymentService.verifyAndCapture(req);
        return ResponseEntity.ok(response);
    }
}
