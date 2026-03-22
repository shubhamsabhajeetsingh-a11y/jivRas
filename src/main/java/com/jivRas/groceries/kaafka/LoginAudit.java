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
    private String role;

    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    
    public LoginAudit(){}

    public LoginAudit(String username, String role, LocalDateTime loginTime) {
        this.username = username;
        this.role = role;
        this.loginTime = loginTime;
    }

    
}
