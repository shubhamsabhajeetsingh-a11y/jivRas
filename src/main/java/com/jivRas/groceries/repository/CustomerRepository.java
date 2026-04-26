package com.jivRas.groceries.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.jivRas.groceries.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUsernameAndAccountCreatedTrue(String username);

    long countByAccountCreatedTrueAndCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
