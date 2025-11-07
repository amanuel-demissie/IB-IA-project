package com.javafx.demo.dao;

import com.javafx.demo.db.Database;
import com.javafx.demo.model.Alert;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AlertDao {

    public Alert create(int productId, Integer logId, String alertType, String message) {
        String sql = """
            INSERT INTO alerts (product_id, log_id, alert_type, message, status)
            VALUES (?, ?, ?, ?, 'UNRESOLVED')
            """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, productId);
            if (logId != null) {
                ps.setInt(2, logId);
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setString(3, alertType);
            ps.setString(4, message);
            ps.executeUpdate();
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return findById(id).orElseThrow();
                }
            }
            throw new RuntimeException("Failed to retrieve created alert");
        } catch (SQLException e) {
            throw new RuntimeException("create alert failed", e);
        }
    }

    public List<Alert> findUnresolved() {
        String sql = """
            SELECT id, product_id, log_id, alert_type, message, status, created_at, resolved_at, resolved_by
            FROM alerts
            WHERE status = 'UNRESOLVED'
            ORDER BY created_at DESC
            """;
        List<Alert> alerts = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                alerts.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findUnresolved failed", e);
        }
        return alerts;
    }

    public List<Alert> findAll() {
        String sql = """
            SELECT id, product_id, log_id, alert_type, message, status, created_at, resolved_at, resolved_by
            FROM alerts
            ORDER BY created_at DESC
            """;
        List<Alert> alerts = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                alerts.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findAll failed", e);
        }
        return alerts;
    }

    public List<Alert> findByProductId(int productId) {
        String sql = """
            SELECT id, product_id, log_id, alert_type, message, status, created_at, resolved_at, resolved_by
            FROM alerts
            WHERE product_id = ?
            ORDER BY created_at DESC
            """;
        List<Alert> alerts = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    alerts.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByProductId failed", e);
        }
        return alerts;
    }

    public void markResolved(int alertId, int resolvedByUserId) {
        String sql = """
            UPDATE alerts
            SET status = 'RESOLVED', resolved_at = NOW(), resolved_by = ?
            WHERE id = ?
            """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, resolvedByUserId);
            ps.setInt(2, alertId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("markResolved failed", e);
        }
    }

    public Optional<Alert> findById(int id) {
        String sql = """
            SELECT id, product_id, log_id, alert_type, message, status, created_at, resolved_at, resolved_by
            FROM alerts
            WHERE id = ?
            """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed", e);
        }
    }

    private Alert mapRow(ResultSet rs) throws SQLException {
        Integer logId = rs.getObject("log_id", Integer.class);
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp resolvedAt = rs.getTimestamp("resolved_at");
        Integer resolvedBy = rs.getObject("resolved_by", Integer.class);
        
        return new Alert(
            rs.getInt("id"),
            rs.getInt("product_id"),
            logId,
            rs.getString("alert_type"),
            rs.getString("message"),
            rs.getString("status"),
            createdAt != null ? createdAt.toLocalDateTime() : LocalDateTime.now(),
            resolvedAt != null ? resolvedAt.toLocalDateTime() : null,
            resolvedBy
        );
    }
}
