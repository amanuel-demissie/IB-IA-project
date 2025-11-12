package com.javafx.demo.model;

import java.time.LocalDateTime;

public record ProductLog(
    int id,
    int productId,
    int userId,
    String actionType, // "CHECK_IN" or "CHECK_OUT" or "TRANSFER"
    int quantity,
    LocalDateTime timestamp,
    String notes,
    Integer fromLocationId,
    Integer toLocationId
) {
    public ProductLog {
        if (actionType == null || (!actionType.equals("CHECK_IN") && !actionType.equals("CHECK_OUT") && !actionType.equals("TRANSFER"))) {
            throw new IllegalArgumentException("Action type must be CHECK_IN, CHECK_OUT or TRANSFER");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }
}
