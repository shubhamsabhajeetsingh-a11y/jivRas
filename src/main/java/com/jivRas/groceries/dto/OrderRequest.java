package com.jivRas.groceries.dto;

import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class OrderRequest {

    // customer details
    private String customerName;
    private String mobile;
    private String addressLine;
    private String city;
    private String state;
    private String pincode;

    // products
    private List<OrderItemRequest> items;
}
