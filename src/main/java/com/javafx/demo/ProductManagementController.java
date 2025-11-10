package com.javafx.demo;

import com.javafx.demo.app.Session;
import com.javafx.demo.dao.ProductDao;
import com.javafx.demo.model.Product;
import com.javafx.demo.model.User;
import com.javafx.demo.service.ProductService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class ProductManagementController {

    @FXML
    private Label userLabel;
    @FXML
    private Button dashboardButton;
    @FXML
    private Button productLogButton;
    @FXML
    private Button alertsButton;
    @FXML
    private Button userManagementButton;
    @FXML
    private Button productsButton;

    @FXML
    private TableView<ProductRow> productsTable;
    @FXML
    private TableColumn<ProductRow, Integer> idColumn;
    @FXML
    private TableColumn<ProductRow, String> nameColumn;
    @FXML
    private TableColumn<ProductRow, Integer> quantityColumn;
    @FXML
    private TableColumn<ProductRow, String> locationColumn;
    @FXML
    private TableColumn<ProductRow, String> unitColumn;

    @FXML
    private TextField nameField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextField quantityField;
    @FXML
    private TextField locationField;
    @FXML
    private TextField unitField;
    @FXML
    private Label messageLabel;

    private final ProductService productService = new ProductService();
    private final ProductDao productDao = new ProductDao();

    @FXML
    private void initialize() {
        User current = Session.getInstance().getCurrentUser();
        if (current != null) {
            userLabel.setText("Logged in as: " + current.username() + " (" + current.roleName() + ")");
        }

        if (current == null || !"ADMIN".equals(current.roleName())) {
            messageLabel.setText("Access denied: Admins only");
            navigateToDashboardInternal();
            return;
        }

        // Sidebar visibility for admin page
        dashboardButton.setVisible(true);
        productLogButton.setVisible(true);
        alertsButton.setVisible(true);
        userManagementButton.setVisible(true);
        productsButton.setVisible(true);

        // Table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));

        loadProducts();

        // Populate form on selection
        productsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, newRow) -> {
            if (newRow != null) {
                nameField.setText(newRow.getName());
                descriptionArea.setText(newRow.getDescription());
                quantityField.setText(String.valueOf(newRow.getQuantity()));
                locationField.setText(newRow.getLocation());
                unitField.setText(newRow.getUnit());
                messageLabel.setText("");
            } else {
                clearForm();
            }
        });
    }

    private void loadProducts() {
        ObservableList<ProductRow> rows = FXCollections.observableArrayList();
        for (Product p : productService.getAllProducts()) {
            rows.add(new ProductRow(
                p.id(),
                p.name(),
                p.description(),
                p.quantity(),
                p.location(),
                p.unit()
            ));
        }
        productsTable.setItems(rows);
        messageLabel.setText("");
    }

    @FXML
    private void onCreateProduct(ActionEvent event) {
        try {
            String name = nameField.getText().trim();
            String description = descriptionArea.getText().trim();
            String quantityText = quantityField.getText().trim();
            String location = locationField.getText().trim();
            String unit = unitField.getText() == null || unitField.getText().trim().isEmpty() ? "pcs" : unitField.getText().trim();

            if (name.isEmpty() || quantityText.isEmpty()) {
                messageLabel.setText("Name and Quantity are required");
                return;
            }
            int quantity = Integer.parseInt(quantityText);
            if (quantity < 0) {
                messageLabel.setText("Quantity cannot be negative");
                return;
            }
            productService.createProduct(name, description, quantity, location, unit);
            messageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
            messageLabel.setText("Product created");
            clearForm();
            loadProducts();
        } catch (NumberFormatException e) {
            messageLabel.setTextFill(javafx.scene.paint.Color.RED);
            messageLabel.setText("Quantity must be a number");
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setTextFill(javafx.scene.paint.Color.RED);
            messageLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onUpdateProduct(ActionEvent event) {
        ProductRow selected = productsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("Select a product to update");
            return;
        }
        try {
            String name = nameField.getText().trim();
            String description = descriptionArea.getText().trim();
            String quantityText = quantityField.getText().trim();
            String location = locationField.getText().trim();
            String unit = unitField.getText() == null || unitField.getText().trim().isEmpty() ? "pcs" : unitField.getText().trim();
            if (name.isEmpty() || quantityText.isEmpty()) {
                messageLabel.setText("Name and Quantity are required");
                return;
            }
            int quantity = Integer.parseInt(quantityText);
            if (quantity < 0) {
                messageLabel.setText("Quantity cannot be negative");
                return;
            }
            var existing = productService.getProductById(selected.getId()).orElseThrow();
            Product updated = new Product(
                existing.id(),
                name,
                description,
                quantity,
                location,
                unit,
                existing.createdAt(),
                existing.updatedAt()
            );
            productService.updateProduct(updated);
            messageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
            messageLabel.setText("Product updated");
            loadProducts();
        } catch (NumberFormatException e) {
            messageLabel.setTextFill(javafx.scene.paint.Color.RED);
            messageLabel.setText("Quantity must be a number");
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setTextFill(javafx.scene.paint.Color.RED);
            messageLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteProduct(ActionEvent event) {
        ProductRow selected = productsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("Select a product to delete");
            return;
        }
        var confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete product " + selected.getName() + "?", ButtonType.OK, ButtonType.CANCEL);
        var res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                productDao.delete(selected.getId());
                messageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
                messageLabel.setText("Product deleted");
                loadProducts();
                clearForm();
            } catch (Exception e) {
                e.printStackTrace();
                messageLabel.setTextFill(javafx.scene.paint.Color.RED);
                messageLabel.setText("Error: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onClearForm(ActionEvent event) {
        clearForm();
        messageLabel.setText("");
    }

    private void clearForm() {
        nameField.clear();
        descriptionArea.clear();
        quantityField.clear();
        locationField.clear();
        unitField.clear();
        productsTable.getSelectionModel().clearSelection();
    }

    // Navigation
    @FXML
    private void onDashboardClick(ActionEvent event) { navigateTo("/com/javafx/demo/dashboard-view.fxml", event, "Factory Dashboard"); }
    @FXML
    private void onProductLogClick(ActionEvent event) { navigateTo("/com/javafx/demo/product-log-view.fxml", event, "Product Log"); }
    @FXML
    private void onAlertsClick(ActionEvent event) { navigateTo("/com/javafx/demo/alerts-view.fxml", event, "Alerts"); }
    @FXML
    private void onUserManagementClick(ActionEvent event) { navigateTo("/com/javafx/demo/user-management-view.fxml", event, "User Management"); }
    @FXML
    private void onProductsClick(ActionEvent event) { /* already here */ }

    @FXML
    private void onLogoutClick(ActionEvent event) {
        Session.getInstance().setCurrentUser(null);
        navigateTo("/com/javafx/demo/login-view.fxml", event, "Factory Inventory Login");
    }

    private void navigateTo(String fxml, ActionEvent event, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Scene scene = new Scene(loader.load(), 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/com/javafx/demo/styles.css").toExternalForm());
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void navigateToDashboardInternal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/javafx/demo/dashboard-view.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/com/javafx/demo/styles.css").toExternalForm());
            Stage stage = (Stage) userLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Factory Dashboard");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class ProductRow {
        private final Integer id;
        private final String name;
        private final String description;
        private final Integer quantity;
        private final String location;
        private final String unit;

        public ProductRow(Integer id, String name, String description, Integer quantity, String location, String unit) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.quantity = quantity;
            this.location = location;
            this.unit = unit;
        }

        public Integer getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Integer getQuantity() { return quantity; }
        public String getLocation() { return location; }
        public String getUnit() { return unit; }
    }
}



