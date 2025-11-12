package com.javafx.demo.dao;

import com.javafx.demo.db.Database;
import com.javafx.demo.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductDao {

    public Product create(String name, String description, int quantity, String location, String unit) {
        String sql = """
            INSERT INTO products (name, description, quantity, location, unit)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setInt(3, quantity);
            ps.setString(4, location);
            ps.setString(5, unit);
            ps.executeUpdate();
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return findById(id).orElseThrow();
                }
            }
            throw new RuntimeException("Failed to retrieve created product");
        } catch (SQLException e) {
            throw new RuntimeException("create product failed", e);
        }
    }

    public Optional<Product> findById(int id) {
        String sql = """
            SELECT id, name, description, quantity, location, unit, created_at, updated_at
            FROM products
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

    public List<Product> findAll() {
        String sql = """
            SELECT id, name, description, quantity, location, unit, created_at, updated_at
            FROM products
            ORDER BY name
            """;
        List<Product> products = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                products.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findAll failed", e);
        }
        return products;
    }

    public void update(Product product) {
        String sql = """
            UPDATE products
            SET name = ?, description = ?, quantity = ?, location = ?, unit = ?
            WHERE id = ?
            """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, product.name());
            ps.setString(2, product.description());
            ps.setInt(3, product.quantity());
            ps.setString(4, product.location());
            ps.setString(5, product.unit());
            ps.setInt(6, product.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update product failed", e);
        }
    }

    public void updateQuantity(int productId, int newQuantity) {
        String sql = "UPDATE products SET quantity = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, newQuantity);
            ps.setInt(2, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("updateQuantity failed", e);
        }
    }

    public void updateQuantity(int productId, int newQuantity, Connection c) {
        String sql = "UPDATE products SET quantity = ? WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, newQuantity);
            ps.setInt(2, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("updateQuantity failed", e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("delete product failed", e);
        }
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        return new Product(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getInt("quantity"),
            rs.getString("location"),
            rs.getString("unit"),
            createdAt != null ? createdAt.toLocalDateTime() : null,
            updatedAt != null ? updatedAt.toLocalDateTime() : null
        );
    }
}
