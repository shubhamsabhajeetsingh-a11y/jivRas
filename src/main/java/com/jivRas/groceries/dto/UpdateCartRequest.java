package com.jivRas.groceries.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Request body for updating an item's quantity in the cart.
 */
@Data
public class UpdateCartRequest {

    @Min(value = 1, message = "Quantity must be at least 0.1 kg")
    private double quantity;
}
