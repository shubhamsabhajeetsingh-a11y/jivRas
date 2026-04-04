package com.jivRas.groceries.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryResponse {
    private double totalRevenueToday;
    private long totalOrdersToday;
    private String topSellingItem;       // product name with most kg sold today
    private String topCategory;          // category with most revenue today
    private int lowStockAlertsCount;     // system-wide low stock count
}
