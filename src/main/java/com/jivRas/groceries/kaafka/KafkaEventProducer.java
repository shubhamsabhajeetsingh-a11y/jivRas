package com.jivRas.groceries.kaafka;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class KafkaEventProducer {
	
	
	 @Autowired
	    private KafkaTemplate<String, String> kafkaTemplate;

	    @Autowired
	    private ObjectMapper objectMapper;

	    public void sendLoginEvent(String username, String role) {
	        try {
	            Map<String, String> event = new HashMap<>();
	            event.put("type", "LOGIN");
	            event.put("username", username);
	            event.put("role", role);

	            String json = objectMapper.writeValueAsString(event);
	            java.util.concurrent.CompletableFuture.runAsync(() -> {
	                try {
	                    kafkaTemplate.send("logging", json);
	                } catch (Exception e) {
	                    System.out.println("Kafka send error: " + e.getMessage());
	                }
	            });

	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }

	    public void sendLogoutEvent(String username) {
	        try {
	            Map<String, String> event = new HashMap<>();
	            event.put("type", "LOGOUT");
	            event.put("username", username);

	            String json = objectMapper.writeValueAsString(event);
	            java.util.concurrent.CompletableFuture.runAsync(() -> {
	                try {
	                    kafkaTemplate.send("logging", json);
	                } catch (Exception e) {
	                    System.out.println("Kafka send error: " + e.getMessage());
	                }
	            });

	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }

}
