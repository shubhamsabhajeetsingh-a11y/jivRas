package com.jivRas.groceries.dto.order;

import java.time.LocalDateTime;
import java.util.List;

import com.jivRas.groceries.dto.OrderItemResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Slim view of an order shown on the delivery agent's dashboard.
 * Contains everything the agent needs to locate and complete the delivery —
 * customer contact, full address, items, and the ETA set by the admin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOrderDto {

    private Long orderId;
    private String orderStatus;
    private LocalDateTime orderDate;
    private double totalAmount;

    // ── Delivery contact & address ──
    private String customerName;
    private String mobile;
    private String addressLine;
    private String city;
    private String state;
    private String pincode;

    /** ETA assigned by admin; null if not set. */
    private LocalDateTime estimatedDeliveryTime;

    // ── Items summary — first item name + overflow count ──
    private String firstItemName;
    private int additionalItemCount;
    private List<OrderItemResponse> items;
}
