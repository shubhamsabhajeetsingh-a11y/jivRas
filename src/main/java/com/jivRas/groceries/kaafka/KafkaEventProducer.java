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

	    public void sendLoginEvent(String username) {
	        try {
	            Map<String, String> event = new HashMap<>();
	            event.put("username", username);

	            String json = objectMapper.writeValueAsString(event);
	            kafkaTemplate.send("logging", json);

	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }

}
