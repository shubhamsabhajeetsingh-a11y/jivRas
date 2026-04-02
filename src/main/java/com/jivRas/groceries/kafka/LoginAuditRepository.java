package com.jivRas.groceries.kafka;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {
    Optional<LoginAudit> findTopByUsernameOrderByLoginTimeDesc(String username);
}
