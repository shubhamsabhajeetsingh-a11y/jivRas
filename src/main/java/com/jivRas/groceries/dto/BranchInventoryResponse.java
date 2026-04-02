package com.jivRas.groceries.dto;
 
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchInventoryResponse {
 
    private Long id;
 
    // Branch info
    private Long branchId;
    private String branchName;
 
    // Product info
    private Long productId;
    private String productName;
    private Double pricePerKg;
    private String imageUrl;
 
    // Stock info
    private Double availableStockKg;
    private Double lowStockThreshold;
 
    // Computed flag — true if stock is at or below threshold
    // Used by dashboard to show red/yellow warning badge
    private boolean lowStock;
}