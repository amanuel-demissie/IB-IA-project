package com.javafx.demo.dao;

import com.javafx.demo.db.Database;
import com.javafx.demo.model.ProductLog;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LogDao {

    public ProductLog create(int productId, int userId, String actionType, int quantity, String notes) {
        String sql = """
            INSERT INTO logs (product_id, user_id, action_type, quantity, notes)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, productId);
            ps.setInt(2, userId);
            ps.setString(3, actionType);
            ps.setInt(4, quantity);
            ps.setString(5, notes);
            ps.executeUpdate();
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    // Fetch the created log to get the timestamp
                    return findById(id).orElseThrow();
                }
            }
            throw new RuntimeException("Failed to retrieve created log");
        } catch (SQLException e) {
            throw new RuntimeException("create log failed", e);
        }
    }

    public List<ProductLog> findFiltered(Integer productId, Integer userId, String actionType,
                                         java.time.LocalDate fromDate, java.time.LocalDate toDate,
                                         int limit, int offset) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            SELECT id, product_id, user_id, action_type, quantity, timestamp, notes
            FROM logs
            WHERE 1=1
            """);
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (productId != null) {
            sb.append(" AND product_id = ?");
            params.add(productId);
        }
        if (userId != null) {
            sb.append(" AND user_id = ?");
            params.add(userId);
        }
        if (actionType != null && !actionType.isBlank() && !"ALL".equalsIgnoreCase(actionType)) {
            sb.append(" AND action_type = ?");
            params.add(actionType);
        }
        if (fromDate != null) {
            sb.append(" AND DATE(timestamp) >= ?");
            params.add(java.sql.Date.valueOf(fromDate));
        }
        if (toDate != null) {
            sb.append(" AND DATE(timestamp) <= ?");
            params.add(java.sql.Date.valueOf(toDate));
        }
        sb.append(" ORDER BY timestamp DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        List<ProductLog> logs = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findFiltered failed", e);
        }
        return logs;
    }

    public int countFiltered(Integer productId, Integer userId, String actionType,
                             java.time.LocalDate fromDate, java.time.LocalDate toDate) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT COUNT(*) AS cnt FROM logs WHERE 1=1");
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (productId != null) {
            sb.append(" AND product_id = ?");
            params.add(productId);
        }
        if (userId != null) {
            sb.append(" AND user_id = ?");
            params.add(userId);
        }
        if (actionType != null && !actionType.isBlank() && !"ALL".equalsIgnoreCase(actionType)) {
            sb.append(" AND action_type = ?");
            params.add(actionType);
        }
        if (fromDate != null) {
            sb.append(" AND DATE(timestamp) >= ?");
            params.add(java.sql.Date.valueOf(fromDate));
        }
        if (toDate != null) {
            sb.append(" AND DATE(timestamp) <= ?");
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

    public List<ProductLog> findRecent(int limit) {
        String sql = """
            SELECT id, product_id, user_id, action_type, quantity, timestamp, notes
            FROM logs
            ORDER BY timestamp DESC
            LIMIT ?
            """;
        List<ProductLog> logs = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findRecent failed", e);
        }
        return logs;
    }

    public List<ProductLog> findByProductId(int productId) {
        String sql = """
            SELECT id, product_id, user_id, action_type, quantity, timestamp, notes
            FROM logs
            WHERE product_id = ?
            ORDER BY timestamp DESC
            """;
        List<ProductLog> logs = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByProductId failed", e);
        }
        return logs;
    }

    public List<ProductLog> findByUserId(int userId) {
        String sql = """
            SELECT id, product_id, user_id, action_type, quantity, timestamp, notes
            FROM logs
            WHERE user_id = ?
            ORDER BY timestamp DESC
            """;
        List<ProductLog> logs = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByUserId failed", e);
        }
        return logs;
    }

    public List<ProductLog> findCheckOutsOlderThan(int hours) {
        String sql = """
            SELECT id, product_id, user_id, action_type, quantity, timestamp, notes
            FROM logs
            WHERE action_type = 'CHECK_OUT'
            AND timestamp < DATE_SUB(NOW(), INTERVAL ? HOUR)
            AND NOT EXISTS (
                SELECT 1 FROM logs l2
                WHERE l2.product_id = logs.product_id
                AND l2.action_type = 'CHECK_IN'
                AND l2.timestamp > logs.timestamp
            )
            ORDER BY timestamp DESC
            """;
        List<ProductLog> logs = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, hours);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findCheckOutsOlderThan failed", e);
        }
        return logs;
    }

    public java.util.Optional<ProductLog> findById(int id) {
        String sql = """
            SELECT id, product_id, user_id, action_type, quantity, timestamp, notes
            FROM logs
            WHERE id = ?
            """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return java.util.Optional.of(mapRow(rs));
                }
                return java.util.Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed", e);
        }
    }

    public int countTodayCheckIns() {
        String sql = """
            SELECT COUNT(*) as count
            FROM logs
            WHERE action_type = 'CHECK_IN'
            AND DATE(timestamp) = CURDATE()
            """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("count");
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("countTodayCheckIns failed", e);
        }
    }

    public int countTodayCheckOuts() {
        String sql = """
            SELECT COUNT(*) as count
            FROM logs
            WHERE action_type = 'CHECK_OUT'
            AND DATE(timestamp) = CURDATE()
            """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("count");
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("countTodayCheckOuts failed", e);
        }
    }

    private ProductLog mapRow(ResultSet rs) throws SQLException {
        Timestamp timestamp = rs.getTimestamp("timestamp");
        return new ProductLog(
            rs.getInt("id"),
            rs.getInt("product_id"),
            rs.getInt("user_id"),
            rs.getString("action_type"),
            rs.getInt("quantity"),
            timestamp != null ? timestamp.toLocalDateTime() : LocalDateTime.now(),
            rs.getString("notes")
        );
    }
}
