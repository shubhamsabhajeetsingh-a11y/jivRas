package com.jivRas.groceries.dto;
 
import lombok.Data;
 
@Data
public class BranchInventoryRequest {
 
    // Which branch to update stock for
    private Long branchId;
 
    // Which product to update
    private Long productId;
 
    // New stock quantity in kg
    private Double availableStockKg;
 
    // Alert threshold — when to show "Low Stock" warning
    // Optional — defaults to 5.0 if not provided
    private Double lowStockThreshold;
}