package com.javafx.demo;

import com.javafx.demo.app.Session;
import com.javafx.demo.model.Product;
import com.javafx.demo.model.ProductLog;
import com.javafx.demo.model.User;
import com.javafx.demo.service.ProductService;
import com.javafx.demo.security.AuthGuard;
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

import java.time.LocalDate;

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
    @FXML
    private Button productsButton;
    // Filters
    @FXML
    private ComboBox<Product> filterProductCombo;
    @FXML
    private ComboBox<String> filterActionCombo;
    @FXML
    private javafx.scene.control.DatePicker filterFromDate;
    @FXML
    private javafx.scene.control.DatePicker filterToDate;
    @FXML
    private javafx.scene.control.Pagination pagination;

    private final ProductService productService = new ProductService();
    // Avoid explicit DateTimeFormatter to prevent runtime resolution issues
    private static final int PAGE_SIZE = 20;

    @FXML
    private void initialize() {
        // Require Admin or Staff
        if (!AuthGuard.isLoggedIn()) {
            navigateToLoginInternal();
            return;
        }
        if (!AuthGuard.hasAnyRole("ADMIN", "STAFF")) {
            showAccessDeniedAndGoDashboard();
            return;
        }
        User u = AuthGuard.currentUser();
        if (u != null) {
            userLabel.setText("Logged in as: " + u.username() + " (" + u.roleName() + ")");
        }

        // Role-based visibility for navigation
        if (u != null) {
            String role = u.roleName();
            boolean isAdmin = "ADMIN".equals(role);
            boolean isSecurity = "SECURITY".equals(role);
            boolean isStaff = "STAFF".equals(role);

            // Product Log visible to Admin and Staff (this page)
            productLogButton.setVisible(isAdmin || isStaff);
            // Products visible to Admin
            productsButton.setVisible(isAdmin);
            // Alerts visible to Admin and Security
            alertsButton.setVisible(isAdmin || isSecurity);
            // User Management only for Admin
            userManagementButton.setVisible(isAdmin);
        }

        // Load products into combo box
        loadProducts();
        // Load products into filter combo
        loadFilterProducts();
        // Setup action filter
        filterActionCombo.setItems(FXCollections.observableArrayList("ALL", "CHECK_IN", "CHECK_OUT"));
        filterActionCombo.getSelectionModel().select("ALL");

        // Setup table columns
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        productColumn.setCellValueFactory(new PropertyValueFactory<>("product"));
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        userColumn.setCellValueFactory(new PropertyValueFactory<>("user"));
        notesColumn.setCellValueFactory(new PropertyValueFactory<>("notes"));

        // Load logs (first page)
        setupPagination();

        // Clear message label
        messageLabel.setText("");

        // Disable future dates in filters
        setupDatePickers();
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
                        setText(product.name() + " (Qty: " + product.quantity() + " " + product.unit() + ")");
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
                        setText(product.name() + " (Qty: " + product.quantity() + " " + product.unit() + ")");
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Error loading products: " + e.getMessage());
        }
    }

    private void loadRecentLogs(int pageIndex) {
        try {
            ObservableList<LogTableRow> logRows = FXCollections.observableArrayList();
            int offset = pageIndex * PAGE_SIZE;
            Integer productId = null;
            Product fp = filterProductCombo != null ? filterProductCombo.getSelectionModel().getSelectedItem() : null;
            if (fp != null) productId = fp.id();
            String action = filterActionCombo != null ? filterActionCombo.getSelectionModel().getSelectedItem() : "ALL";
            var from = filterFromDate != null ? filterFromDate.getValue() : null;
            var to = filterToDate != null ? filterToDate.getValue() : null;

            // Validate date range
            if (to != null && to.isAfter(LocalDate.now())) {
                to = LocalDate.now();
                filterToDate.setValue(to);
            }
            if (from != null && to != null && from.isAfter(to)) {
                messageLabel.setTextFill(javafx.scene.paint.Color.RED);
                messageLabel.setText("From date must be before To date");
                logsTable.getItems().clear();
                return;
            }

            var filteredLogs = productService.getLogsFiltered(productId, null, action, from, to, PAGE_SIZE, offset);
            int total = productService.countLogsFiltered(productId, null, action, from, to);
            int pageCount = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
            if (pagination != null) pagination.setPageCount(pageCount);

            User currentUser = Session.getInstance().getCurrentUser();
            int currentUserId = currentUser != null ? currentUser.id() : 0;

            for (ProductLog log : filteredLogs) {
                String productName = productService.getProductById(log.productId())
                    .map(Product::name)
                    .orElse("Product ID: " + log.productId());

                String userName = currentUserId == log.userId() 
                    ? (currentUser != null ? currentUser.username() : "User " + log.userId())
                    : "User " + log.userId();

                logRows.add(new LogTableRow(
                    String.valueOf(log.timestamp()),
                    log.actionType(),
                    productName,
                    log.quantity(),
                    userName,
                    log.notes() != null ? log.notes() : ""
                ));
            }

            logsTable.getSelectionModel().clearSelection();
            logsTable.setItems(logRows);
            if (logRows.isEmpty()) {
                messageLabel.setTextFill(javafx.scene.paint.Color.GRAY);
                messageLabel.setText("No results for the selected filters");
            } else {
                messageLabel.setText("");
            }
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Error loading logs: " + e.getMessage());
        }
    }

    private void setupPagination() {
        if (pagination != null) {
            pagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> {
                loadRecentLogs(newIndex.intValue());
            });
            pagination.setCurrentPageIndex(0);
            loadRecentLogs(0);
        } else {
            loadRecentLogs(0);
        }
    }

    private void loadFilterProducts() {
        try {
            var products = productService.getAllProducts();
            ObservableList<Product> productList = FXCollections.observableArrayList(products);
            filterProductCombo.setItems(productList);
            filterProductCombo.setCellFactory(listView -> new ListCell<Product>() {
                @Override
                protected void updateItem(Product product, boolean empty) {
                    super.updateItem(product, empty);
                    setText(empty || product == null ? null : product.name());
                }
            });
            filterProductCombo.setButtonCell(new ListCell<Product>() {
                @Override
                protected void updateItem(Product product, boolean empty) {
                    super.updateItem(product, empty);
                    setText(empty || product == null ? "All products" : product.name());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onApplyFilters(ActionEvent event) {
        if (pagination != null) pagination.setCurrentPageIndex(0);
        loadRecentLogs(0);
    }

    private void setupDatePickers() {
        final LocalDate today = LocalDate.now();
        if (filterFromDate != null) {
            filterFromDate.setDayCellFactory(dp -> new DateCell() {
                @Override
                public void updateItem(LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    setDisable(empty || item.isAfter(today));
                }
            });
        }
        if (filterToDate != null) {
            filterToDate.setDayCellFactory(dp -> new DateCell() {
                @Override
                public void updateItem(LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    setDisable(empty || item.isAfter(today));
                }
            });
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
            onApplyFilters(null);

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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/javafx/demo/user-management-view.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);
            scene.getStylesheets().add(
                getClass().getResource("/com/javafx/demo/styles.css").toExternalForm()
            );
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("User Management");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void onProductsClick(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/javafx/demo/product-management-view.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);
            scene.getStylesheets().add(
                getClass().getResource("/com/javafx/demo/styles.css").toExternalForm()
            );
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Products");
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    @FXML
    private void onLogoutClick(ActionEvent event) {
        Session.getInstance().setCurrentUser(null);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/javafx/demo/login-view.fxml"));
            Scene scene = new Scene(loader.load(), 960, 640);
            scene.getStylesheets().add(
                getClass().getResource("/com/javafx/demo/styles.css").toExternalForm()
            );
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Factory Inventory Login");
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

    private void showAccessDeniedAndGoDashboard() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Access Denied");
        alert.setHeaderText("Insufficient permissions");
        alert.setContentText("You do not have access to Product Log.");
        alert.showAndWait();
        navigateToDashboard(null);
    }

    private void navigateToLoginInternal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/javafx/demo/login-view.fxml"));
            Scene scene = new Scene(loader.load(), 960, 640);
            scene.getStylesheets().add(getClass().getResource("/com/javafx/demo/styles.css").toExternalForm());
            Stage stage = (Stage) userLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Factory Inventory Login");
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

