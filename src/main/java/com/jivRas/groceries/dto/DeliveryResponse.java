package com.jivRas.groceries.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeliveryResponse {

    private String trackingId;
    private String status;          // CREATED, PICKED, IN_TRANSIT
    private int estimatedDays;
    private String deliveryPartner; // Shiprocket, Delhivery, etc.
    
   
}
