package com.jivRas.groceries.service;

import org.springframework.stereotype.Service;

import com.jivRas.groceries.deliveryIntegration.DeliveryPartner;
import com.jivRas.groceries.dto.DeliveryResponse;
import com.jivRas.groceries.entity.Order;

@Service
public class ShiprocketDeliveryPartner implements DeliveryPartner {

	@Override
	public DeliveryResponse createShipment(Order order) {
		// Call Shiprocket API using RestTemplate / WebClient
		return new DeliveryResponse("TRACK123", "CREATED", order.getEstimatedDeliveryDays(), "SHIPROCKET");
	}
}
