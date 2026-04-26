package com.jivRas.groceries.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MorningSummaryDTO {
    private long todayOrders;
    private double todayRevenue;
    private long pendingOrders;
    private long todayNewCustomers;
    private List<LowStockAlertDTO> lowStockAlerts;
}
