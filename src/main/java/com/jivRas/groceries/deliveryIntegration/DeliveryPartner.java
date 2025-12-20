package com.jivRas.groceries.deliveryIntegration;

import com.jivRas.groceries.dto.DeliveryResponse;
import com.jivRas.groceries.entity.Order;

public interface DeliveryPartner {

    DeliveryResponse createShipment(Order order);
}
