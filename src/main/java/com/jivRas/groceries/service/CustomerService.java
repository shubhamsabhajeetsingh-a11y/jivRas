package com.jivRas.groceries.service;

import org.springframework.stereotype.Service;

import com.jivRas.groceries.dto.OrderRequest;
import com.jivRas.groceries.entity.Customer;
import com.jivRas.groceries.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public Customer createCustomer(OrderRequest request) {

        Customer customer = new Customer();
        customer.setName(request.getCustomerName());
        customer.setMobile(request.getMobile());
        customer.setAddressLine(request.getAddressLine());
        customer.setCity(request.getCity());
        customer.setState(request.getState());
        customer.setPincode(request.getPincode());

        return customerRepository.save(customer);
    }
}

