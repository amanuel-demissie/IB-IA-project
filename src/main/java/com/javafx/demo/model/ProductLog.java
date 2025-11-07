package com.javafx.demo.model;

import java.time.LocalDateTime;

public record ProductLog(
    int id,
    int productId,
    int userId,
    String actionType, // "CHECK_IN" or "CHECK_OUT"
    int quantity,
    LocalDateTime timestamp,
    String notes
) {
    public ProductLog {
        if (actionType == null || (!actionType.equals("CHECK_IN") && !actionType.equals("CHECK_OUT"))) {
            throw new IllegalArgumentException("Action type must be CHECK_IN or CHECK_OUT");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }
}
