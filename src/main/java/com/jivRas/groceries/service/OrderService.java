package com.jivRas.groceries.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.jivRas.groceries.dto.OrderItemRequest;
import com.jivRas.groceries.dto.OrderRequest;
import com.jivRas.groceries.entity.Customer;
import com.jivRas.groceries.entity.Order;
import com.jivRas.groceries.entity.Product;
import com.jivRas.groceries.repository.OrderRepository;
import com.jivRas.groceries.repository.ProductRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final CustomerService customerService;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final DeliveryService deliveryService;
    @Transactional
    public Order placeOrder(OrderRequest request) {

        Customer customer = customerService.createCustomer(request);

        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderDate(LocalDateTime.now());

        double total = 0;

        for (OrderItemRequest itemReq : request.getItems()) {

            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            if (product.getAvailableStockKg() < itemReq.getQuantityKg()) {
                throw new RuntimeException("Insufficient stock for " + product.getName());
            }

            // 🔥 stock deduction
            product.setAvailableStockKg(
                    product.getAvailableStockKg() - itemReq.getQuantityKg()
            );

            productRepository.save(product);

            total += itemReq.getQuantityKg() * product.getPricePerKg();
        }

        order.setTotalAmount(total);
        order.setEstimatedDeliveryDays(
                deliveryService.calculateETA(customer.getPincode())
        );

        return orderRepository.save(order);
    }
}
