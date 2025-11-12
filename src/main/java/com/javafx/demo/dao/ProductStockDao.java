package com.javafx.demo.dao;

import com.javafx.demo.db.Database;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductStockDao {

    public record LocationStock(String locationName, int quantity) {}

    public record ProductWithQty(int productId, String name, String unit, int quantity) {}

    public int getQuantity(int productId, int locationId) {
        String sql = "SELECT quantity FROM product_stock WHERE product_id = ? AND location_id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, locationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                return 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("getQuantity failed", e);
        }
    }

    public int getQuantityForUpdate(int productId, int locationId, Connection c) throws SQLException {
        String sql = "SELECT quantity FROM product_stock WHERE product_id = ? AND location_id = ? FOR UPDATE";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, locationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                return 0;
            }
        }
    }

    public void increment(int productId, int locationId, int delta, Connection c) throws SQLException {
        // Insert path: start at max(0, delta). Update path: add the signed delta.
        String upsert = """
            INSERT INTO product_stock(product_id, location_id, quantity)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE quantity = GREATEST(0, quantity + ?)
            """;
        try (PreparedStatement ps = c.prepareStatement(upsert)) {
            ps.setInt(1, productId);
            ps.setInt(2, locationId);
            ps.setInt(3, Math.max(0, delta)); // if creating row on negative delta, clamp to 0
            ps.setInt(4, delta);               // on update, apply signed delta
            ps.executeUpdate();
        }
    }

    public int sumForProduct(int productId, Connection c) throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantity),0) FROM product_stock WHERE product_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                return 0;
            }
        }
    }

    public List<LocationStock> findByProduct(int productId) {
        String sql = """
            SELECT l.name, ps.quantity
            FROM product_stock ps
            JOIN locations l ON l.id = ps.location_id
            WHERE ps.product_id = ? AND ps.quantity > 0
            ORDER BY l.name
            """;
        List<LocationStock> result = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new LocationStock(rs.getString(1), rs.getInt(2)));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByProduct stock failed", e);
        }
        return result;
    }

    public List<ProductWithQty> findByLocation(int locationId) {
        String sql = """
            SELECT p.id, p.name, p.unit, ps.quantity
            FROM product_stock ps
            JOIN products p ON p.id = ps.product_id
            WHERE ps.location_id = ? AND ps.quantity > 0
            ORDER BY p.name
            """;
        List<ProductWithQty> out = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, locationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new ProductWithQty(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getInt(4)
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByLocation failed", e);
        }
        return out;
    }

    public void cleanupZeroRows(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM product_stock WHERE quantity <= 0")) {
            ps.executeUpdate();
        }
    }

    public void backfillFromProductsIfEmpty() {
        String countSql = "SELECT COUNT(*) FROM product_stock";
        try (Connection c = Database.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) > 0) return;
        } catch (SQLException e) {
            throw new RuntimeException("backfill count failed", e);
        }
        // Map location name -> id
        java.util.Map<String, Integer> locMap = new java.util.HashMap<>();
        int warehouseAId = -1;
        try (Connection c = Database.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name FROM locations")) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                locMap.put(name, id);
                if ("Warehouse A".equalsIgnoreCase(name)) warehouseAId = id;
            }
        } catch (SQLException e) {
            throw new RuntimeException("load locations failed", e);
        }
        String selProducts = "SELECT id, quantity, location FROM products";
        try (Connection c = Database.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(selProducts)) {
            while (rs.next()) {
                int pid = rs.getInt("id");
                int qty = rs.getInt("quantity");
                String locName = rs.getString("location");
                int destLocId = (locName != null && locMap.containsKey(locName)) ? locMap.get(locName)
                    : (warehouseAId > 0 ? warehouseAId : locMap.values().stream().findFirst().orElseThrow());
                try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO product_stock(product_id, location_id, quantity) VALUES(?,?,?) " +
                        "ON DUPLICATE KEY UPDATE quantity = VALUES(quantity)")) {
                    ps.setInt(1, pid);
                    ps.setInt(2, destLocId);
                    ps.setInt(3, qty);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("backfill products to product_stock failed", e);
        }
    }

    public void resetAndBackfillFromProducts() {
        try (Connection c = Database.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("DELETE FROM product_stock");
        } catch (SQLException e) {
            throw new RuntimeException("reset product_stock failed", e);
        }
        backfillFromProductsIfEmpty();
    }

    public void ensureMissingFromProducts() {
        List<Integer> missing = new ArrayList<>();
        String sql = """
            SELECT p.id
            FROM products p
            LEFT JOIN product_stock ps ON ps.product_id = p.id
            WHERE ps.product_id IS NULL
            """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                missing.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("ensureMissingFromProducts scan failed", e);
        }
        if (missing.isEmpty()) return;

        LocationDao locationDao = new LocationDao();
        try (Connection c = Database.getConnection()) {
            for (int pid : missing) {
                try (PreparedStatement ps = c.prepareStatement("SELECT quantity, location FROM products WHERE id = ?")) {
                    ps.setInt(1, pid);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int qty = rs.getInt("quantity");
                            String locName = rs.getString("location");
                            var loc = locationDao.findOrCreateByName(locName);
                            try (PreparedStatement ins = c.prepareStatement(
                                "INSERT IGNORE INTO product_stock(product_id, location_id, quantity) VALUES(?,?,?)")) {
                                ins.setInt(1, pid);
                                ins.setInt(2, loc.id());
                                ins.setInt(3, qty);
                                ins.executeUpdate();
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("ensureMissingFromProducts failed", e);
        }
    }
}


