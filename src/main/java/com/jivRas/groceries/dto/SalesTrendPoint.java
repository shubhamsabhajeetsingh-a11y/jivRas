package com.jivRas.groceries.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesTrendPoint {
    private String date;        // "2026-04-04"
    private double revenue;
    private long orderCount;
}
