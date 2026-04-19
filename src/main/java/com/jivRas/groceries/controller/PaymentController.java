package com.jivRas.groceries.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jivRas.groceries.annotation.ModuleAction;
import com.jivRas.groceries.dto.CreatePaymentOrderRequest;
import com.jivRas.groceries.dto.CreatePaymentOrderResponse;
import com.jivRas.groceries.dto.VerifyPaymentRequest;
import com.jivRas.groceries.dto.VerifyPaymentResponse;
import com.jivRas.groceries.dto.payment.OrderPaymentTimelineDto;
import com.jivRas.groceries.dto.payment.PaymentAnalyticsDto;
import com.jivRas.groceries.dto.payment.PaymentListItemDto;
import com.jivRas.groceries.enums.PaymentStatus;
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

    /**
     * GET /api/payments/by-order/{orderId}
     *
     * Returns the full payment attempt timeline for one order, oldest-attempt-first.
     * effectiveStatus is NOT_APPLICABLE when the order has no payment records.
     * Accessible by BRANCH_MANAGER and CUSTOMER (CUSTOMER ownership check deferred to Phase 5).
     */
    @GetMapping("/by-order/{orderId}")
    @ModuleAction(module = "PAYMENT", action = "VIEW")
    public ResponseEntity<OrderPaymentTimelineDto> getTimeline(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getTimelineForOrder(orderId));
    }

    /**
     * GET /api/payments/list?status=PAID&from=...&to=...
     *
     * Returns a filtered list of payments for the admin Payments tab.
     * Filters are mutually exclusive: status takes priority over date range.
     * Capped at 500 rows; real pagination comes in a future iteration.
     */
    @GetMapping("/list")
    @ModuleAction(module = "PAYMENT", action = "VIEW")
    public ResponseEntity<List<PaymentListItemDto>> list(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(paymentService.getPaymentsList(status, from, to));
    }

    /**
     * GET /api/payments/analytics?from=2026-01-01&to=2026-04-19
     *
     * Returns aggregate analytics for the given inclusive date window.
     * Both {@code from} and {@code to} are required (ISO date format: yyyy-MM-dd).
     */
    @GetMapping("/analytics")
    @ModuleAction(module = "PAYMENT", action = "VIEW")
    public ResponseEntity<PaymentAnalyticsDto> analytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(paymentService.getAnalytics(from, to));
    }
}
