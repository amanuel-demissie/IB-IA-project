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

    public void ensureAdminSeeded(String username, String passwordHash) {
        String roleUpsert = "INSERT IGNORE INTO roles(name) VALUES('ADMIN')";
        String userInsert = """
            INSERT INTO users(username, password_hash, role_id)
            SELECT ?, ?, r.id FROM roles r WHERE r.name='ADMIN'
            ON DUPLICATE KEY UPDATE username=username
            """;
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try (Statement st = c.createStatement()) {
                st.executeUpdate(roleUpsert);
            }
            try (PreparedStatement ps = c.prepareStatement(userInsert)) {
                ps.setString(1, username);
                ps.setString(2, passwordHash);
                ps.executeUpdate();
            }
            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException("ensureAdminSeeded failed", e);
        }
    }
}


