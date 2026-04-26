package com.jivRas.groceries.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LowStockAlertDTO {
    private String branchName;
    private String productName;
    private int currentStock;
    private int reorderThreshold;
}
