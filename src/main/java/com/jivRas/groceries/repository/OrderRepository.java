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
