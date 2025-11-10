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

    public List<Alert> findFiltered(Integer productId, String status,
                                    java.time.LocalDate fromDate, java.time.LocalDate toDate,
                                    int limit, int offset) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            SELECT id, product_id, log_id, alert_type, message, status, created_at, resolved_at, resolved_by
            FROM alerts
            WHERE 1=1
            """);
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (productId != null) {
            sb.append(" AND product_id = ?");
            params.add(productId);
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            sb.append(" AND status = ?");
            params.add(status);
        }
        if (fromDate != null) {
            sb.append(" AND DATE(created_at) >= ?");
            params.add(java.sql.Date.valueOf(fromDate));
        }
        if (toDate != null) {
            sb.append(" AND DATE(created_at) <= ?");
            params.add(java.sql.Date.valueOf(toDate));
        }
        sb.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        List<Alert> alerts = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    alerts.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findFiltered failed", e);
        }
        return alerts;
    }

    public int countFiltered(Integer productId, String status,
                             java.time.LocalDate fromDate, java.time.LocalDate toDate) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT COUNT(*) AS cnt FROM alerts WHERE 1=1");
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (productId != null) {
            sb.append(" AND product_id = ?");
            params.add(productId);
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            sb.append(" AND status = ?");
            params.add(status);
        }
        if (fromDate != null) {
            sb.append(" AND DATE(created_at) >= ?");
            params.add(java.sql.Date.valueOf(fromDate));
        }
        if (toDate != null) {
            sb.append(" AND DATE(created_at) <= ?");
            params.add(java.sql.Date.valueOf(toDate));
        }
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt");
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("countFiltered failed", e);
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
