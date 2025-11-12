package com.javafx.demo.service;

import com.javafx.demo.dao.LogDao;
import com.javafx.demo.dao.ProductDao;
import com.javafx.demo.model.Product;
import com.javafx.demo.model.ProductLog;

import java.util.List;
import java.util.Optional;

public class ProductService {
    private final ProductDao productDao = new ProductDao();
    private final LogDao logDao = new LogDao();

    /**
     * Check out a product (decrease inventory)
     * @param productId The product to check out
     * @param userId The user performing the action
     * @param quantity The quantity to check out
     * @param notes Optional notes
     * @return The created log entry
     * @throws IllegalArgumentException if product doesn't exist or insufficient quantity
     */
    public ProductLog checkOut(int productId, int userId, int quantity, String notes) {
        // Validate product exists
        Optional<Product> productOpt = productDao.findById(productId);
        if (productOpt.isEmpty()) {
            throw new IllegalArgumentException("Product not found with ID: " + productId);
        }

        Product product = productOpt.get();

        // Validate sufficient quantity
        if (product.quantity() < quantity) {
            throw new IllegalArgumentException(
                String.format("Insufficient quantity. Available: %d, Requested: %d", 
                    product.quantity(), quantity)
            );
        }

        // Update product quantity
        int newQuantity = product.quantity() - quantity;
        productDao.updateQuantity(productId, newQuantity);

        // Create log entry
        return logDao.create(productId, userId, "CHECK_OUT", quantity, notes);
    }

    /**
     * Check in a product (increase inventory)
     * @param productId The product to check in
     * @param userId The user performing the action
     * @param quantity The quantity to check in
     * @param notes Optional notes
     * @return The created log entry
     * @throws IllegalArgumentException if product doesn't exist
     */
    public ProductLog checkIn(int productId, int userId, int quantity, String notes) {
        // Validate product exists
        Optional<Product> productOpt = productDao.findById(productId);
        if (productOpt.isEmpty()) {
            throw new IllegalArgumentException("Product not found with ID: " + productId);
        }

        Product product = productOpt.get();

        // Update product quantity
        int newQuantity = product.quantity() + quantity;
        productDao.updateQuantity(productId, newQuantity);

        // Create log entry
        return logDao.create(productId, userId, "CHECK_IN", quantity, notes);
    }

    /**
     * Get all products
     */
    public List<Product> getAllProducts() {
        return productDao.findAll();
    }

    /**
     * Get product by ID
     */
    public Optional<Product> getProductById(int id) {
        return productDao.findById(id);
    }

    /**
     * Create a new product
     */
    public Product createProduct(String name, String description, int quantity, String location, String unit) {
        Product p = productDao.create(name, description, quantity, location, unit);
        // Ensure product_stock has an initial row matching product's location and quantity
        try {
            com.javafx.demo.dao.LocationDao locationDao = new com.javafx.demo.dao.LocationDao();
            var loc = locationDao.findOrCreateByName(location);
            try (java.sql.Connection c = com.javafx.demo.db.Database.getConnection()) {
                c.setAutoCommit(false);
                // insert only if missing
                try (java.sql.PreparedStatement ps = c.prepareStatement(
                        "INSERT IGNORE INTO product_stock(product_id, location_id, quantity) VALUES(?,?,?)")) {
                    ps.setInt(1, p.id());
                    ps.setInt(2, loc.id());
                    ps.setInt(3, quantity);
                    ps.executeUpdate();
                }
                c.commit();
            }
        } catch (Exception ignored) {
            // best-effort; user can always adjust via transfers
        }
        return p;
    }

    /**
     * Update an existing product
     */
    public void updateProduct(Product product) {
        productDao.update(product);
    }

    /**
     * Get recent logs for a product
     */
    public List<ProductLog> getProductLogs(int productId) {
        return logDao.findByProductId(productId);
    }

    /**
     * Get recent logs across all products
     */
    public List<ProductLog> getRecentLogs(int limit) {
        return logDao.findRecent(limit);
    }

    /**
     * Get logs for a specific user
     */
    public List<ProductLog> getUserLogs(int userId) {
        return logDao.findByUserId(userId);
    }

    public List<ProductLog> getLogsFiltered(Integer productId, Integer userId, String actionType,
                                            java.time.LocalDate fromDate, java.time.LocalDate toDate,
                                            int limit, int offset) {
        return logDao.findFiltered(productId, userId, actionType, fromDate, toDate, limit, offset);
    }

    public int countLogsFiltered(Integer productId, Integer userId, String actionType,
                                 java.time.LocalDate fromDate, java.time.LocalDate toDate) {
        return logDao.countFiltered(productId, userId, actionType, fromDate, toDate);
    }

    /**
     * Seed a few sample products to make the demo usable out of the box.
     * This only runs if the products table is empty.
     * @return number of products created
     */
    public int seedSampleProductsIfEmpty() {
        var existing = productDao.findAll();
        if (!existing.isEmpty()) {
            return 0;
        }

        createProduct("Widget A", "Sample widget for testing", 100, "Warehouse A", "pcs");
        createProduct("Gadget B", "Demo gadget", 60, "Warehouse B", "pcs");
        createProduct("Component C", "Replacement component", 35, "Storage C", "pcs");
        return 3;
    }
}

