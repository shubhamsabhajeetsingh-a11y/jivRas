package com.jivRas.groceries.service;

import com.jivRas.groceries.dto.dashboard.LowStockAlertDTO;
import com.jivRas.groceries.dto.dashboard.MorningSummaryDTO;
import com.jivRas.groceries.entity.BranchInventory;
import com.jivRas.groceries.entity.Order;
import com.jivRas.groceries.repository.BranchInventoryRepository;
import com.jivRas.groceries.repository.CustomerRepository;
import com.jivRas.groceries.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final BranchInventoryRepository branchInventoryRepository;

    public DashboardService(OrderRepository orderRepository,
                            CustomerRepository customerRepository,
                            BranchInventoryRepository branchInventoryRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.branchInventoryRepository = branchInventoryRepository;
    }

    public MorningSummaryDTO getMorningSummary() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd   = todayStart.plusDays(1);

        List<Order> todayOrderList = orderRepository.findWithItemsByDateRange(todayStart, todayEnd);

        long todayOrders = todayOrderList.size();

        double todayRevenue = 0.0;
        for (Order order : todayOrderList) {
            todayRevenue += order.getTotalAmount();
        }

        List<Order> allOrders = orderRepository.findAllByOrderByOrderDateDesc();
        long pendingOrders = 0;
        for (Order order : allOrders) {
            String status = order.getOrderStatus();
            if ("PENDING".equals(status) || "PROCESSING".equals(status)) {
                pendingOrders++;
            }
        }

        long todayNewCustomers = customerRepository
                .countByAccountCreatedTrueAndCreatedAtBetween(todayStart, todayEnd);

        List<BranchInventory> lowStockItems = branchInventoryRepository.findAllLowStock();
        List<LowStockAlertDTO> lowStockAlerts = new ArrayList<>();
        for (BranchInventory bi : lowStockItems) {
            LowStockAlertDTO alert = LowStockAlertDTO.builder()
                    .branchName(bi.getBranch().getName())
                    .productName(bi.getProduct().getName())
                    .currentStock((int) Math.floor(bi.getAvailableStockKg()))
                    .reorderThreshold((int) Math.floor(bi.getLowStockThreshold()))
                    .build();
            lowStockAlerts.add(alert);
        }

        return MorningSummaryDTO.builder()
                .todayOrders(todayOrders)
                .todayRevenue(todayRevenue)
                .pendingOrders(pendingOrders)
                .todayNewCustomers(todayNewCustomers)
                .lowStockAlerts(lowStockAlerts)
                .build();
    }
}
