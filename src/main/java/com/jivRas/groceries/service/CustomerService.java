package com.jivRas.groceries.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.jivRas.groceries.dto.OrderRequest;
import com.jivRas.groceries.entity.Customer;
import com.jivRas.groceries.entity.Order;
import com.jivRas.groceries.repository.CustomerRepository;
import com.jivRas.groceries.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    // Phase 5: needed to locate and claim guest orders during customer registration
    private final OrderRepository orderRepository;

    /** Creates a guest-checkout Customer record (used by the checkout flow, not registration). */
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

    /**
     * Phase 5: Guest-to-registered order linking.
     *
     * After a new customer successfully registers, finds all guest orders placed with the
     * same phone number and claims them: sets userId to the new customer's numeric ID and
     * clears the isGuest flag so the orders appear under "My Orders" and are excluded from
     * the admin guest filter.
     *
     * @param phone      the mobile number provided at registration (matched against customer_phone on orders)
     * @param customerId the auto-generated DB ID of the newly saved Customer record
     * @return the number of orders linked (0 if the customer had no prior guest orders)
     */
    public int linkGuestOrders(String phone, Long customerId) {

        // Find all orders where customer_phone matches this number and is_guest = true
        List<Order> guestOrders = orderRepository.findByCustomerPhoneAndIsGuestTrue(phone);

        if (guestOrders.isEmpty()) {
            // No past guest orders for this phone number — nothing to migrate
            return 0;
        }

        for (Order order : guestOrders) {
            // userId is stored as a String; the convention for registered users is
            // String.valueOf(customerId) — this is what OrderService.getMyOrders() looks up
            order.setUserId(String.valueOf(customerId));
            // Clear the guest flag so admin guest-filter and isGuest badge no longer apply
            order.setIsGuest(false);
        }

        // Persist all updates in a single batch — avoids N individual UPDATE statements
        orderRepository.saveAll(guestOrders);

        // TODO: emit an event or notify customer that guestOrders.size() past orders are now visible in My Orders

        return guestOrders.size();
    }
}

