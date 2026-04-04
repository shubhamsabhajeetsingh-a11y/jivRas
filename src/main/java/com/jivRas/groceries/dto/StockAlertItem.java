package com.jivRas.groceries.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAlertItem {
    private Long branchId;
    private String branchName;
    private Long productId;
    private String productName;
    private String categoryName;
    private double availableStockKg;
    private double lowStockThreshold;
    /** "CRITICAL" when stock < 3 kg, "WARNING" otherwise */
    private String severity;
    /** Estimated days until stockout based on 30-day avg daily consumption */
    private Double projectedDaysLeft;
}
