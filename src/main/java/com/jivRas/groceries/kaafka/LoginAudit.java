package com.jivRas.groceries.kaafka;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
public class LoginAudit {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private LocalDateTime loginTime;
    
    public LoginAudit(){}

    public LoginAudit(String username, LocalDateTime loginTime) {
        this.username = username;
        this.loginTime = loginTime;
    }

    
}
