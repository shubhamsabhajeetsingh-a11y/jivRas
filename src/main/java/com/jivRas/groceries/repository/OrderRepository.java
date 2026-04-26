package com.jivRas.groceries.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jivRas.groceries.entity.Order;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long>  {

    List<Order> findAllByOrderByOrderDateDesc();

    /**
     * Registered user's full order history, newest first.
     * Used on the "My Orders" page after the customer logs in.
     */
    List<Order> findByUserIdOrderByOrderDateDesc(String userId);

    /**
     * Admin-side phone number lookup — returns orders for both registered and guest customers
     * sharing the same phone number, newest first.
     */
    List<Order> findByCustomerPhoneOrderByOrderDateDesc(String customerPhone);

    /**
     * Fetch guest-only orders for a phone number (userId is null = no linked account).
     * Used by the guest-to-registered linking service during signup to migrate past orders.
     */
    List<Order> findByCustomerPhoneAndUserIdIsNull(String customerPhone);

    /**
     * Phase 5: Guest-to-registered linking.
     * Returns all orders for a phone number where is_guest = true, regardless of userId value.
     * Preferred over findByCustomerPhoneAndUserIdIsNull because guest orders may carry a
     * "guest_<uuid>" userId (non-null) — the isGuest flag is the canonical guest marker.
     */
    List<Order> findByCustomerPhoneAndIsGuestTrue(String customerPhone);

    /**
     * Phase 6: Agent dashboard — orders assigned to this agent that are still in transit.
     * Status is always OUT_FOR_DELIVERY for the active delivery view.
     */
    List<Order> findByDeliveryAgentIdAndOrderStatus(Long deliveryAgentId, String orderStatus);

    /**
     * Fetch orders within a date range, eagerly loading items, products, and categories.
     * Used by ReportService for all aggregation (done in Java streams, not SQL).
     */
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE o.orderDate >= :from AND o.orderDate < :to " +
           "ORDER BY o.orderDate")
    List<Order> findWithItemsByDateRange(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );
}
