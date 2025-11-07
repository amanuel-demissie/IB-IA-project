package com.javafx.demo.dao;

import com.javafx.demo.db.Database;
import com.javafx.demo.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UserDao {

    public User findByUsername(String username) {
        String sql = """
            SELECT u.id, u.username, u.password_hash, r.name AS role_name
            FROM users u JOIN roles r ON r.id = u.role_id
            WHERE u.username = ?
            """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role_name")
                    );
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByUsername failed", e);
        }
    }

    public java.util.Optional<User> findById(int id) {
        String sql = """
            SELECT u.id, u.username, u.password_hash, r.name AS role_name
            FROM users u JOIN roles r ON r.id = u.role_id
            WHERE u.id = ?
            """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return java.util.Optional.of(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role_name")
                    ));
                }
                return java.util.Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed", e);
        }
    }

    public void ensureAdminSeeded(String username, String passwordHash) {
        seedRoles();
        String userInsert = """
            INSERT INTO users(username, password_hash, role_id)
            SELECT ?, ?, r.id FROM roles r WHERE r.name='ADMIN'
            ON DUPLICATE KEY UPDATE username=username
            """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(userInsert)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("ensureAdminSeeded failed", e);
        }
    }

    public void seedRoles() {
        String sql = """
            INSERT IGNORE INTO roles(name) VALUES
            ('ADMIN'),
            ('SECURITY'),
            ('STAFF')
            """;
        try (Connection c = Database.getConnection();
             Statement st = c.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("seedRoles failed", e);
        }
    }
}


