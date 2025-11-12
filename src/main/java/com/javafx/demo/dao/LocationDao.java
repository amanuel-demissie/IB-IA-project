package com.javafx.demo.dao;

import com.javafx.demo.db.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LocationDao {

    public record Location(int id, String name) {}

    public List<Location> findAll() {
        String sql = "SELECT id, name FROM locations ORDER BY name";
        List<Location> list = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Location(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findAll locations failed", e);
        }
        return list;
    }

    public Optional<Location> findById(int id) {
        String sql = "SELECT id, name FROM locations WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Location(rs.getInt("id"), rs.getString("name")));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById location failed", e);
        }
    }

    public Optional<Location> findByName(String name) {
        String sql = "SELECT id, name FROM locations WHERE name = ? LIMIT 1";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Location(rs.getInt("id"), rs.getString("name")));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByName location failed", e);
        }
    }

    public Location findOrCreateByName(String name) {
        if (name == null || name.isBlank()) {
            return findByName("Warehouse A").orElseGet(() -> create("Warehouse A"));
        }
        return findByName(name).orElseGet(() -> create(name));
    }

    public Location create(String name) {
        String sql = "INSERT INTO locations(name) VALUES(?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return new Location(rs.getInt(1), name);
                }
            }
            throw new RuntimeException("Failed to create location");
        } catch (SQLException e) {
            throw new RuntimeException("create location failed", e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM locations WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("delete location failed", e);
        }
    }

    public void ensureDefaults() {
        List<String> defaults = List.of("Warehouse A", "Warehouse B", "Storage C");
        String sql = "INSERT IGNORE INTO locations(name) VALUES(?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (String d : defaults) {
                ps.setString(1, d);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("ensureDefaults locations failed", e);
        }
    }
}


