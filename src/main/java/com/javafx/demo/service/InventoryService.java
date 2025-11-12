package com.javafx.demo.service;

import com.javafx.demo.dao.LogDao;
import com.javafx.demo.dao.ProductDao;
import com.javafx.demo.dao.ProductStockDao;
import com.javafx.demo.db.Database;
import com.javafx.demo.model.ProductLog;

import java.sql.Connection;

public class InventoryService {
    private final ProductStockDao stockDao = new ProductStockDao();
    private final LogDao logDao = new LogDao();
    private final ProductDao productDao = new ProductDao();

    public ProductLog checkIn(int productId, int locationId, int userId, int quantity, String notes) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            stockDao.increment(productId, locationId, quantity, c);
            stockDao.cleanupZeroRows(c);
            int total = stockDao.sumForProduct(productId, c);
            productDao.updateQuantity(productId, total, c);
            c.commit();
            return logDao.create(productId, userId, "CHECK_IN", quantity, notes);
        } catch (Exception e) {
            throw new RuntimeException("checkIn failed", e);
        }
    }

    public ProductLog checkOut(int productId, int locationId, int userId, int quantity, String notes) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            int available = stockDao.getQuantityForUpdate(productId, locationId, c);
            if (available < quantity) {
                throw new IllegalArgumentException("Insufficient stock at source location. Available: " + available);
            }
            stockDao.increment(productId, locationId, -quantity, c);
            stockDao.cleanupZeroRows(c);
            int total = stockDao.sumForProduct(productId, c);
            productDao.updateQuantity(productId, total, c);
            c.commit();
            return logDao.create(productId, userId, "CHECK_OUT", quantity, notes);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException("checkOut failed", e);
        }
    }

    public ProductLog transfer(int productId, int fromLocationId, int toLocationId, int userId, int quantity, String notes) {
        if (fromLocationId == toLocationId) throw new IllegalArgumentException("From and To locations must differ");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            int available = stockDao.getQuantityForUpdate(productId, fromLocationId, c);
            if (available < quantity) {
                throw new IllegalArgumentException("Insufficient stock at source location. Available: " + available);
            }
            stockDao.increment(productId, fromLocationId, -quantity, c);
            stockDao.increment(productId, toLocationId, quantity, c);
            stockDao.cleanupZeroRows(c);
            int total = stockDao.sumForProduct(productId, c);
            productDao.updateQuantity(productId, total, c);
            c.commit();
            return logDao.createTransfer(productId, userId, quantity, notes, fromLocationId, toLocationId);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException("transfer failed", e);
        }
    }
}


