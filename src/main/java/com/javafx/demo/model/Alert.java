package com.javafx.demo.model;

import java.time.LocalDateTime;

public record Alert(
    int id,
    int productId,
    Integer logId, // nullable
    String alertType,
    String message,
    String status, // "UNRESOLVED" or "RESOLVED"
    LocalDateTime createdAt,
    LocalDateTime resolvedAt, // nullable
    Integer resolvedBy // nullable user ID
) {
    public Alert {
        if (status == null || (!status.equals("UNRESOLVED") && !status.equals("RESOLVED"))) {
            throw new IllegalArgumentException("Status must be UNRESOLVED or RESOLVED");
        }
    }
    
    public boolean isResolved() {
        return "RESOLVED".equals(status);
    }
}
