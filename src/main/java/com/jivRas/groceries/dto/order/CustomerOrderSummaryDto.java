package com.jivRas.groceries.dto.order;

import java.time.LocalDateTime;

import com.jivRas.groceries.enums.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Slim DTO for customer order history list — only the fields a customer needs to see.
 * Full details fetched via existing GET /api/orders/:id when they click into a specific order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerOrderSummaryDto {
    private Long orderId;
    private LocalDateTime orderDate;
    private String status;
    private PaymentStatus paymentStatus;
    private Long totalAmount;
    private Integer itemCount;
    private String firstItemName;
    private Integer additionalItemCount;
    private Boolean canCancel;
}
