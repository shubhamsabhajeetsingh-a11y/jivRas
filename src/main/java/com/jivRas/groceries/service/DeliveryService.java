package com.jivRas.groceries.service;

import org.springframework.stereotype.Service;

@Service
public class DeliveryService {

    public int calculateETA(String pincode) {

        // Example logic
        if (pincode.startsWith("40")) {
            return 2; // local
        }
        if (pincode.startsWith("56")) {
            return 4;
        }
        return 6; // rest of India
    }
}
