package com.jivRas.groceries.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for adding a product to the cart.
 */
@Data
public class AddToCartRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @Min(value = 1, message = "Quantity must be at least 0.1 kg")
    private double quantity;
}
