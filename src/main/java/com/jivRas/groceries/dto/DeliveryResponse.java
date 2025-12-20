package com.jivRas.groceries.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@AllArgsConstructor
public class DeliveryResponse {

    private String trackingId;
    private String status;          // CREATED, PICKED, IN_TRANSIT
    private int estimatedDays;
    private String deliveryPartner; // Shiprocket, Delhivery, etc.
}
