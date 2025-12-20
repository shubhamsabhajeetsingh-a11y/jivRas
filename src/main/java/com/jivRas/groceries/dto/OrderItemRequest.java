package com.jivRas.groceries.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemRequest {
	   private Long productId;
	    private double quantityKg;
}
