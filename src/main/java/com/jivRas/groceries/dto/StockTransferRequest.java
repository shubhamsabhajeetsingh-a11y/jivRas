package com.jivRas.groceries.dto;
 
import lombok.Data;
 
@Data
public class StockTransferRequest {
 
    // Branch sending the stock (source)
    private Long fromBranchId;
 
    // Branch receiving the stock (destination)
    private Long toBranchId;
 
    // Which product to transfer
    private Long productId;
 
    // How many kg to transfer
    private Double quantityKg;
 
    // Optional reason — shown in audit trail
    private String reason;
}