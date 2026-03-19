package com.jivRas.groceries.exception;

/**
 * Thrown when a requested resource (product, cart, order) is not found in the database.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
