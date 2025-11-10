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

    public java.util.List<User> findAll() {
        String sql = """
            SELECT u.id, u.username, u.password_hash, r.name AS role_name
            FROM users u JOIN roles r ON r.id = u.role_id
            ORDER BY u.username
            """;
        java.util.ArrayList<User> users = new java.util.ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("role_name")
                ));
            }
            return users;
        } catch (SQLException e) {
            throw new RuntimeException("findAll failed", e);
        }
    }

    public User createUser(String username, String passwordHash, String roleName) {
        String sql = """
            INSERT INTO users(username, password_hash, role_id)
            SELECT ?, ?, r.id FROM roles r WHERE r.name = ?
            """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, roleName);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return findById(id).orElseThrow();
                }
            }
            throw new RuntimeException("Failed to create user");
        } catch (SQLException e) {
            throw new RuntimeException("createUser failed", e);
        }
    }

    public void updateUserPassword(int userId, String newPasswordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("updateUserPassword failed", e);
        }
    }

    public void updateUserRole(int userId, String roleName) {
        String sql = """
            UPDATE users u
            JOIN roles r ON r.name = ?
            SET u.role_id = r.id
            WHERE u.id = ?
            """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, roleName);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("updateUserRole failed", e);
        }
    }

    public void deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("deleteUser failed", e);
        }
    }
}


