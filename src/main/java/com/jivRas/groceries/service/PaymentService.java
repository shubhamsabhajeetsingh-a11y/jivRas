package com.jivRas.groceries.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.jivRas.groceries.dto.CreatePaymentOrderRequest;
import com.jivRas.groceries.dto.CreatePaymentOrderResponse;
import com.jivRas.groceries.dto.VerifyPaymentRequest;
import com.jivRas.groceries.dto.VerifyPaymentResponse;
import com.jivRas.groceries.entity.Order;
import com.jivRas.groceries.entity.Payment;
import com.jivRas.groceries.enums.PaymentStatus;
import com.jivRas.groceries.exception.PaymentGatewayException;
import com.jivRas.groceries.exception.ResourceNotFoundException;
import com.jivRas.groceries.repository.OrderRepository;
import com.jivRas.groceries.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;

import jakarta.transaction.Transactional;

/**
 * Handles all payment lifecycle operations:
 *   1. Creating a Razorpay order (before the user sees the payment modal)
 *   2. Verifying the Razorpay signature (after the user pays)
 *
 * Both methods are @Transactional — the Payment row and Order status
 * must update together or not at all.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RazorpayClient razorpayClient;

    /** Razorpay secret — injected from the named bean in RazorpayConfig, never sent to the client. */
    private final String razorpayKeySecret;

    /** Razorpay public key ID — sent to the frontend so checkout.js can initialise. */
    private final String razorpayKeyId;

    /** ISO 4217 currency code read from application.properties (default "INR"). */
    @Value("${razorpay.currency}")
    private String currency;

    public PaymentService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            RazorpayClient razorpayClient,
            @Qualifier("razorpayKeySecret") String razorpayKeySecret,
            @Qualifier("razorpayKeyId")     String razorpayKeyId) {
        this.orderRepository   = orderRepository;
        this.paymentRepository = paymentRepository;
        this.razorpayClient    = razorpayClient;
        this.razorpayKeySecret = razorpayKeySecret;
        this.razorpayKeyId     = razorpayKeyId;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Method A — Create Razorpay Order
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a Razorpay order for the given JivRas order and persists a
     * Payment record with status CREATED.
     *
     * Flow:
     *   1. Load and validate the JivRas order
     *   2. Convert rupee amount → paise
     *   3. Call Razorpay API to create an order
     *   4. Persist Payment row
     *   5. Return response for checkout.js initialisation
     *
     * @param req contains the JivRas order ID
     * @return Razorpay order details needed by the frontend checkout widget
     */
    @Transactional
    public CreatePaymentOrderResponse createRazorpayOrder(CreatePaymentOrderRequest req) {

        // 1. Load the JivRas order — fail early if it doesn't exist
        Order order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with ID: " + req.getOrderId()));

        // 2. Guard: only CREATED orders are payable. Any other status means payment
        //    was already attempted, confirmed, or the order was cancelled.
        if (!"CREATED".equals(order.getOrderStatus())) {
            throw new IllegalStateException(
                    "Payment already initiated or order not payable. Current status: "
                    + order.getOrderStatus());
        }

        // 3. Convert order amount to paise (Razorpay expects the smallest currency subunit).
        //    Using BigDecimal.valueOf avoids floating-point precision loss from double arithmetic.
        long amountInPaise = BigDecimal.valueOf(order.getTotalAmount())
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        // 4. Build the Razorpay order request payload
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount",   amountInPaise);   // must be in paise
        orderRequest.put("currency", currency);        // "INR"
        // Receipt is a short label visible in the Razorpay dashboard; max 40 chars
        orderRequest.put("receipt",  "jivras_rcpt_" + order.getId());

        // Notes are stored on the Razorpay order and visible in their dashboard — helpful for debugging
        JSONObject notes = new JSONObject();
        notes.put("jivras_order_id",  order.getId().toString());
        notes.put("customer_phone",   order.getCustomerPhone() != null ? order.getCustomerPhone() : "");
        orderRequest.put("notes", notes);

        // 5. Call Razorpay API — SDK returns com.razorpay.Order (different from our entity)
        com.razorpay.Order razorpayOrder;
        try {
            razorpayOrder = razorpayClient.orders.create(orderRequest);
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed for JivRas order {}: {}", order.getId(), e.getMessage());
            throw new PaymentGatewayException(
                    "Failed to create Razorpay order for JivRas order ID " + order.getId(), e);
        }

        // 6. Extract Razorpay's generated order ID from the response
        String razorpayOrderId = razorpayOrder.get("id");

        // 7. Persist a Payment row to track this attempt
        //    @PrePersist in Payment sets: status=CREATED, attempts=0, currency="INR", createdAt=now()
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setRazorpayOrderId(razorpayOrderId);
        payment.setAmount(amountInPaise);
        // razorpayPaymentId and razorpaySignature remain null until the user pays
        paymentRepository.save(payment);

        log.info("Created Razorpay order {} for JivRas order {}", razorpayOrderId, order.getId());

        // 8. Return all fields the frontend needs to initialise checkout.js
        return new CreatePaymentOrderResponse(
                razorpayOrderId,
                razorpayKeyId,    // public key — safe to send to the client
                amountInPaise,
                currency,
                order.getId()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Method B — Verify and Capture Payment
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies the Razorpay payment signature and, if valid, marks the payment
     * as PAID and the JivRas order as CONFIRMED.
     *
     * Flow:
     *   1. Load the Payment row by Razorpay order ID
     *   2. Idempotency guard — return success immediately if already PAID
     *   3. Build the signature verification payload
     *   4. Call Razorpay SDK to verify HMAC-SHA256 signature
     *   5a. Failure → mark payment FAILED, return failure response
     *   5b. Success → mark payment PAID, update order to CONFIRMED
     *
     * @param req contains the three Razorpay identifiers from the frontend callback
     * @return outcome indicating success/failure and the current order status
     */
    @Transactional
    public VerifyPaymentResponse verifyAndCapture(VerifyPaymentRequest req) {

        // 1. Load the Payment row using the Razorpay order ID
        Payment payment = paymentRepository.findByRazorpayOrderId(req.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No payment record found for Razorpay order ID: " + req.getRazorpayOrderId()));

        Order order = payment.getOrder();

        // 2. Idempotency guard — if this payment was already verified successfully,
        //    return success immediately without re-verifying. This handles duplicate
        //    webhook deliveries or frontend retries.
        if (PaymentStatus.PAID == payment.getStatus()) {
            log.info("Payment {} already verified — returning idempotent success", payment.getId());
            return new VerifyPaymentResponse(true, "Payment already verified", order.getOrderStatus());
        }

        // 3. Build the attributes object expected by the Razorpay SDK signature verifier.
        //    The SDK computes HMAC-SHA256 over "razorpay_order_id|razorpay_payment_id"
        //    and compares it to razorpay_signature using the API secret.
        JSONObject options = new JSONObject();
        options.put("razorpay_order_id",  req.getRazorpayOrderId());
        options.put("razorpay_payment_id", req.getRazorpayPaymentId());
        options.put("razorpay_signature",  req.getRazorpaySignature());

        // 4. Attempt signature verification
        boolean signatureValid;
        try {
            signatureValid = Utils.verifyPaymentSignature(options, razorpayKeySecret);
        } catch (RazorpayException e) {
            // SDK throws on malformed input (e.g. null fields) — treat as a failed verification
            log.warn("Signature verification threw exception for Razorpay order {}: {}",
                     req.getRazorpayOrderId(), e.getMessage());
            signatureValid = false;
        }

        // 5a. Verification failed — record the attempt and reject the payment
        if (!signatureValid) {
            payment.setAttempts(payment.getAttempts() + 1);
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);

            log.warn("Signature mismatch for Razorpay order {} — payment rejected", req.getRazorpayOrderId());
            return new VerifyPaymentResponse(
                    false,
                    "Signature mismatch — payment rejected",
                    order.getOrderStatus()   // order status stays unchanged on failure
            );
        }

        // 5b. Verification succeeded — update Payment and Order atomically
        payment.setRazorpayPaymentId(req.getRazorpayPaymentId());
        payment.setRazorpaySignature(req.getRazorpaySignature());
        payment.setStatus(PaymentStatus.PAID);
        payment.setVerifiedAt(LocalDateTime.now());
        payment.setAttempts(payment.getAttempts() + 1);
        paymentRepository.save(payment);

        // Transition the JivRas order from CREATED → CONFIRMED
        order.setOrderStatus("CONFIRMED");
        orderRepository.save(order);

        // TODO: decrement BranchInventory on confirmation once the stock-reservation
        //       service is wired into the payment flow (Phase 3 or later).

        log.info("Payment {} verified — order {} confirmed", payment.getId(), order.getId());

        return new VerifyPaymentResponse(true, "Payment verified and order confirmed", "CONFIRMED");
    }
}
