package com.jivRas.groceries.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.jivRas.groceries.entity.EmployeeUser;

public interface EmployeeUserRepository extends JpaRepository<EmployeeUser, Long> {
    Optional<EmployeeUser> findByUsername(String username);
}
