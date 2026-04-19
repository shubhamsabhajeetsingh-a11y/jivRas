package com.jivRas.groceries.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderResponse {
    private Long orderId;
    private String orderStatus;
    private LocalDateTime orderDate;
    private double totalAmount;
    private int estimatedDeliveryDays;
    private String customerName;
    private String mobile;
    private String addressLine;
    private String city;
    private String state;
    private String pincode;
    private Boolean isGuest;
    private Long customerId;
    private List<OrderItemResponse> items;
}
