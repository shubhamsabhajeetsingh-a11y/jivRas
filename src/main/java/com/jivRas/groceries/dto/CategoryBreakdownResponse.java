package com.jivRas.groceries.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryBreakdownResponse {
    private String categoryName;
    private double totalRevenue;
    private double totalQuantityKg;
    private long orderCount;
}
