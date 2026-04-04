package com.jivRas.groceries.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchStockComparison {
    private Long branchId;
    private String branchName;
    private int totalProducts;
    private int lowStockCount;
    private double totalStockKg;
    private double lowStockPercentage;  // (lowStockCount / totalProducts) * 100
}
