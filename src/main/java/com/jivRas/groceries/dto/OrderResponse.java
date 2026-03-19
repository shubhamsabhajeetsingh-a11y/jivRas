package com.jivRas.groceries.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a placed order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long orderId;
    private String userId;
    private double totalAmount;
    private String orderStatus;
    private LocalDateTime orderDate;
    private int estimatedDeliveryDays;

    // Delivery info
    private String customerName;
    private String mobile;
    private String addressLine;
    private String city;
    private String state;
    private String pincode;

    // Items
    private List<OrderItemResponse> items;
}
