package com.jivRas.groceries.kafka;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class KafkaEventConsumer {
	
	 @Autowired
	    private LoginAuditRepository loginAuditRepository;

	    @Autowired
	    private ObjectMapper objectMapper;

	    @KafkaListener(topics = "logging", groupId = "auth-group")
	    public void consume(String message) {
	        try {
	            Map<String, String> event = objectMapper.readValue(message, Map.class);
	            String type = event.get("type");
	            String username = event.get("username");

	            if ("LOGIN".equals(type)) {
	                String role = event.get("role");
	                LoginAudit audit = new LoginAudit(
	                        username,
	                        role,
	                        LocalDateTime.now()
	                );
	                loginAuditRepository.save(audit);
	                System.out.println("Saved login audit for: " + username);

	            } else if ("LOGOUT".equals(type)) {
	                loginAuditRepository.findTopByUsernameOrderByLoginTimeDesc(username)
	                    .ifPresent(audit -> {
	                        audit.setLogoutTime(LocalDateTime.now());
	                        loginAuditRepository.save(audit);
	                        System.out.println("Updated logout time for: " + username);
	                    });
	            }

	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }

}
