package com.jivRas.groceries.dto;

import java.util.List;
import lombok.Data;

@Data
public class BulkStockUpdateRequest {
    private List<StockUpdateItem> updates;

    @Data
    public static class StockUpdateItem {
        private Long inventoryId;
        private Double newQuantity;
    }
}
