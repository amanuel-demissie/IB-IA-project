package com.javafx.demo;

import com.javafx.demo.app.Session;
import com.javafx.demo.model.Product;
import com.javafx.demo.model.ProductLog;
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

import java.time.format.DateTimeFormatter;

public class ProductLogController {

    @FXML
    private Label userLabel;
    @FXML
    private ComboBox<Product> productComboBox;
    @FXML
    private RadioButton checkOutRadio;
    @FXML
    private RadioButton checkInRadio;
    @FXML
    private TextField quantityField;
    @FXML
    private TextArea notesArea;
    @FXML
    private Label messageLabel;
    @FXML
    private TableView<LogTableRow> logsTable;
    @FXML
    private TableColumn<LogTableRow, String> dateColumn;
    @FXML
    private TableColumn<LogTableRow, String> productColumn;
    @FXML
    private TableColumn<LogTableRow, String> actionColumn;
    @FXML
    private TableColumn<LogTableRow, Integer> quantityColumn;
    @FXML
    private TableColumn<LogTableRow, String> userColumn;
    @FXML
    private TableColumn<LogTableRow, String> notesColumn;
    @FXML
    private Button dashboardButton;
    @FXML
    private Button productLogButton;
    @FXML
    private Button alertsButton;
    @FXML
    private Button userManagementButton;

    private final ProductService productService = new ProductService();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    private void initialize() {
        // Set user info
        User u = Session.getInstance().getCurrentUser();
        if (u != null) {
            userLabel.setText("Logged in as: " + u.username() + " (" + u.roleName() + ")");
        }

        // Load products into combo box
        loadProducts();

        // Setup table columns
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        productColumn.setCellValueFactory(new PropertyValueFactory<>("product"));
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        userColumn.setCellValueFactory(new PropertyValueFactory<>("user"));
        notesColumn.setCellValueFactory(new PropertyValueFactory<>("notes"));

        // Load recent logs
        loadRecentLogs();

        // Clear message label
        messageLabel.setText("");
    }

    private void loadProducts() {
        try {
            var products = productService.getAllProducts();
            ObservableList<Product> productList = FXCollections.observableArrayList(products);
            
            // Create a custom string converter for ComboBox
            productComboBox.setItems(productList);
            productComboBox.setCellFactory(listView -> new ListCell<Product>() {
                @Override
                protected void updateItem(Product product, boolean empty) {
                    super.updateItem(product, empty);
                    if (empty || product == null) {
                        setText(null);
                    } else {
                        setText(product.name() + " (Qty: " + product.quantity() + ")");
                    }
                }
            });
            productComboBox.setButtonCell(new ListCell<Product>() {
                @Override
                protected void updateItem(Product product, boolean empty) {
                    super.updateItem(product, empty);
                    if (empty || product == null) {
                        setText("Select a product");
                    } else {
                        setText(product.name() + " (Qty: " + product.quantity() + ")");
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Error loading products: " + e.getMessage());
        }
    }

    private void loadRecentLogs() {
        try {
            ObservableList<LogTableRow> logRows = FXCollections.observableArrayList();
            var recentLogs = productService.getRecentLogs(20);

            User currentUser = Session.getInstance().getCurrentUser();
            int currentUserId = currentUser != null ? currentUser.id() : 0;

            for (ProductLog log : recentLogs) {
                String productName = productService.getProductById(log.productId())
                    .map(Product::name)
                    .orElse("Product ID: " + log.productId());

                String userName = currentUserId == log.userId() 
                    ? (currentUser != null ? currentUser.username() : "User " + log.userId())
                    : "User " + log.userId();

                logRows.add(new LogTableRow(
                    log.timestamp().format(DATE_FORMATTER),
                    log.actionType(),
                    productName,
                    log.quantity(),
                    userName,
                    log.notes() != null ? log.notes() : ""
                ));
            }

            logsTable.setItems(logRows);
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Error loading logs: " + e.getMessage());
        }
    }

    @FXML
    private void onSubmitClick(ActionEvent event) {
        try {
            // Validate inputs
            Product selectedProduct = productComboBox.getSelectionModel().getSelectedItem();
            if (selectedProduct == null) {
                messageLabel.setText("Please select a product");
                return;
            }

            String quantityText = quantityField.getText().trim();
            if (quantityText.isEmpty()) {
                messageLabel.setText("Please enter a quantity");
                return;
            }

            int quantity;
            try {
                quantity = Integer.parseInt(quantityText);
                if (quantity <= 0) {
                    messageLabel.setText("Quantity must be positive");
                    return;
                }
            } catch (NumberFormatException e) {
                messageLabel.setText("Quantity must be a number");
                return;
            }

            User currentUser = Session.getInstance().getCurrentUser();
            if (currentUser == null) {
                messageLabel.setText("User not logged in");
                return;
            }

            String actionType = checkOutRadio.isSelected() ? "CHECK_OUT" : "CHECK_IN";
            String notes = notesArea.getText().trim();

            // Perform check-in or check-out
            if ("CHECK_OUT".equals(actionType)) {
                productService.checkOut(selectedProduct.id(), currentUser.id(), quantity, notes);
                messageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
                messageLabel.setText("Successfully checked out " + quantity + " " + selectedProduct.name());
            } else {
                productService.checkIn(selectedProduct.id(), currentUser.id(), quantity, notes);
                messageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
                messageLabel.setText("Successfully checked in " + quantity + " " + selectedProduct.name());
            }

            // Clear form
            onClearClick(null);

            // Reload products and logs
            loadProducts();
            loadRecentLogs();

        } catch (IllegalArgumentException e) {
            messageLabel.setTextFill(javafx.scene.paint.Color.RED);
            messageLabel.setText("Error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setTextFill(javafx.scene.paint.Color.RED);
            messageLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onClearClick(ActionEvent event) {
        productComboBox.getSelectionModel().clearSelection();
        quantityField.clear();
        notesArea.clear();
        messageLabel.setText("");
        checkOutRadio.setSelected(true);
    }

    @FXML
    private void onDashboardClick(ActionEvent event) {
        navigateToDashboard(event);
    }

    @FXML
    private void onProductLogClick(ActionEvent event) {
        // Already on product log page
    }

    @FXML
    private void onAlertsClick(ActionEvent event) {
        navigateToAlerts(event);
    }

    @FXML
    private void onUserManagementClick(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Coming Soon");
        alert.setHeaderText("User Management");
        alert.setContentText("User management feature is coming soon!");
        alert.showAndWait();
    }

    private void navigateToDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/javafx/demo/dashboard-view.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);
            scene.getStylesheets().add(
                getClass().getResource("/com/javafx/demo/styles.css").toExternalForm()
            );
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Factory Dashboard");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void navigateToAlerts(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/javafx/demo/alerts-view.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);
            scene.getStylesheets().add(
                getClass().getResource("/com/javafx/demo/styles.css").toExternalForm()
            );
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Alerts");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Reuse LogTableRow from DashboardController
    public static class LogTableRow {
        private final String date;
        private final String action;
        private final String product;
        private final Integer quantity;
        private final String user;
        private final String notes;

        public LogTableRow(String date, String action, String product, Integer quantity, String user, String notes) {
            this.date = date;
            this.action = action;
            this.product = product;
            this.quantity = quantity;
            this.user = user;
            this.notes = notes;
        }

        public String getDate() { return date; }
        public String getAction() { return action; }
        public String getProduct() { return product; }
        public Integer getQuantity() { return quantity; }
        public String getUser() { return user; }
        public String getNotes() { return notes; }
    }
}

