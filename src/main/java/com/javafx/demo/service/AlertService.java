package com.javafx.demo.service;

import com.javafx.demo.dao.AlertDao;
import com.javafx.demo.dao.LogDao;
import com.javafx.demo.dao.ProductDao;
import com.javafx.demo.model.Alert;
import com.javafx.demo.model.Product;
import com.javafx.demo.model.ProductLog;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class AlertService {
    private final AlertDao alertDao = new AlertDao();
    private final LogDao logDao = new LogDao();
    private final ProductDao productDao = new ProductDao();
    
    // Default threshold: 2 hours (as per PRD)
    private static final int DEFAULT_OVERDUE_HOURS = 2;

    /**
     * Check for overdue checkouts and create alerts
     * This should be called periodically (e.g., every minute or when dashboard loads)
     * @param overdueHours Threshold in hours (defaults to 2)
     * @return Number of new alerts created
     */
    public int checkForOverdueCheckouts(int overdueHours) {
        List<ProductLog> overdueLogs = logDao.findCheckOutsOlderThan(overdueHours);
        int alertsCreated = 0;

        for (ProductLog log : overdueLogs) {
            // Check if alert already exists for this log
            List<Alert> existingAlerts = alertDao.findByProductId(log.productId());
            boolean alertExists = existingAlerts.stream()
                .anyMatch(alert -> 
                    !alert.isResolved() && 
                    alert.logId() != null && 
                    alert.logId() == log.id()
                );

            if (!alertExists) {
                Optional<Product> productOpt = productDao.findById(log.productId());
                String productName = productOpt.map(Product::name).orElse("Unknown Product");
                
                // Calculate how long overdue
                Duration duration = Duration.between(log.timestamp(), LocalDateTime.now());
                long hoursOverdue = duration.toHours();
                
                String message = String.format(
                    "Product '%s' (ID: %d) has been checked out for %d hours. " +
                    "Quantity: %d. Checked out by user ID: %d",
                    productName, log.productId(), hoursOverdue, log.quantity(), log.userId()
                );

                alertDao.create(
                    log.productId(),
                    log.id(),
                    "OVERDUE_CHECKOUT",
                    message
                );
                alertsCreated++;
            }
        }

        return alertsCreated;
    }

    /**
     * Check for overdue checkouts using default threshold (2 hours)
     */
    public int checkForOverdueCheckouts() {
        return checkForOverdueCheckouts(DEFAULT_OVERDUE_HOURS);
    }

    /**
     * Get all unresolved alerts
     */
    public List<Alert> getUnresolvedAlerts() {
        return alertDao.findUnresolved();
    }

    /**
     * Get all alerts
     */
    public List<Alert> getAllAlerts() {
        return alertDao.findAll();
    }

    /**
     * Mark an alert as resolved
     * @param alertId The alert ID
     * @param resolvedByUserId The user ID resolving the alert
     */
    public void resolveAlert(int alertId, int resolvedByUserId) {
        alertDao.markResolved(alertId, resolvedByUserId);
    }

    /**
     * Get alerts for a specific product
     */
    public List<Alert> getProductAlerts(int productId) {
        return alertDao.findByProductId(productId);
    }

    /**
     * Check if a specific checkout log is overdue
     */
    public boolean isCheckoutOverdue(ProductLog log, int overdueHours) {
        if (!"CHECK_OUT".equals(log.actionType())) {
            return false;
        }

        Duration duration = Duration.between(log.timestamp(), LocalDateTime.now());
        return duration.toHours() >= overdueHours;
    }

    /**
     * Get count of unresolved alerts
     */
    public int getUnresolvedAlertCount() {
        return alertDao.findUnresolved().size();
    }
}

