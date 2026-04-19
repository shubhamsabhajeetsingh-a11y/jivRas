package com.jivRas.groceries.dto.payment;

import java.util.List;

import com.jivRas.groceries.enums.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Full payment timeline for a single order — all attempts in ascending chronological order. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaymentTimelineDto {

    private Long orderId;
    /** Order total in paise — copied from the Order entity to spare the frontend a join. */
    private Long totalAmount;
    /**
     * Most recent attempt's status, or NOT_APPLICABLE when the order has no payment attempts
     * (e.g. legacy orders created before the payment module was introduced).
     */
    private PaymentStatus effectiveStatus;
    /** All payment attempts sorted oldest-first so the timeline reads top-to-bottom. */
    private List<PaymentAttemptDto> attempts;
}
