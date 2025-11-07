package com.javafx.demo.model;

import java.time.LocalDateTime;

public record Product(
    int id,
    String name,
    String description,
    int quantity,
    String location,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public Product {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be null or blank");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
    }
}
