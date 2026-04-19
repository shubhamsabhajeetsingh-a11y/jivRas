package com.jivRas.groceries.dto.payment;

import java.time.LocalDateTime;

import com.jivRas.groceries.enums.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One row in the admin payments list — flattened from Payment + linked Order. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentListItemDto {

    private Long paymentId;
    private Long orderId;
    /** From the linked Order entity. */
    private String customerName;
    /** From the linked Order entity. */
    private String customerPhone;
    /** Amount in paise (1 INR = 100 paise). */
    private Long amount;
    private PaymentStatus status;
    private Integer attempts;
    private String razorpayOrderId;
    /** Null until the customer completes payment. */
    private String razorpayPaymentId;
    private LocalDateTime createdAt;
    /** True when the linked order was placed without a user account. */
    private Boolean isGuestOrder;
}
