package com.jivRas.groceries.config;

import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.jivRas.groceries.entity.Customer;
import com.jivRas.groceries.entity.EmployeeUser;
import com.jivRas.groceries.repository.CustomerRepository;
import com.jivRas.groceries.repository.EmployeeUserRepository;

@Service
public class UserInfoDetailService implements UserDetailsService {

    private final CustomerRepository customerRepository;
    private final EmployeeUserRepository employeeUserRepository;

    public UserInfoDetailService(CustomerRepository customerRepository, EmployeeUserRepository employeeUserRepository) {
        this.customerRepository = customerRepository;
        this.employeeUserRepository = employeeUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // 1. Check if it's a registered customer
        Optional<Customer> customerInfo = customerRepository.findByUsernameAndAccountCreatedTrue(username);
        if (customerInfo.isPresent()) {
            Customer customer = customerInfo.get();
            return org.springframework.security.core.userdetails.User.builder()
                    .username(customer.getUsername())
                    .password(customer.getPassword())
                    .roles(customer.getRole() != null ? customer.getRole() : "CUSTOMER")
                    .build();
        }

        // 2. Check if it's an employee
        Optional<EmployeeUser> employeeInfo = employeeUserRepository.findByUsername(username);
        if (employeeInfo.isPresent()) {
            EmployeeUser employee = employeeInfo.get();
            return org.springframework.security.core.userdetails.User.builder()
                    .username(employee.getUsername())
                    .password(employee.getPassword())
                    .roles(employee.getRole())
                    .build();
        }

        throw new UsernameNotFoundException("User not found");
    }
}