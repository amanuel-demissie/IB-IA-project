package com.javafx.demo;

import com.javafx.demo.app.Session;
import com.javafx.demo.model.Alert;
import com.javafx.demo.model.Product;
import com.javafx.demo.model.User;
import com.javafx.demo.service.AlertService;
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
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public class AlertsController {

    @FXML
    private Label userLabel;
    @FXML
    private Label alertsCountLabel;
    @FXML
    private RadioButton unresolvedRadio;
    @FXML
    private RadioButton allAlertsRadio;
    @FXML
    private TableView<AlertTableRow> alertsTable;
    @FXML
    private TableColumn<AlertTableRow, String> dateColumn;
    @FXML
    private TableColumn<AlertTableRow, String> productColumn;
    @FXML
    private TableColumn<AlertTableRow, String> typeColumn;
    @FXML
    private TableColumn<AlertTableRow, String> messageColumn;
    @FXML
    private TableColumn<AlertTableRow, String> statusColumn;
    @FXML
    private Label messageLabel;
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
    private javafx.scene.control.ComboBox<com.javafx.demo.model.Product> filterProductCombo;
    @FXML
    private javafx.scene.control.DatePicker filterFromDate;
    @FXML
    private javafx.scene.control.DatePicker filterToDate;
    @FXML
    private javafx.scene.control.Pagination pagination;

    private final AlertService alertService = new AlertService();
    private final ProductService productService = new ProductService();
    // Avoid explicit DateTimeFormatter to prevent runtime resolution issues
    private static final int PAGE_SIZE = 20;

    @FXML
    private void initialize() {
        // Require Admin or Security
        if (!AuthGuard.isLoggedIn()) {
            navigateToLoginInternal();
            return;
        }
        if (!AuthGuard.hasAnyRole("ADMIN", "SECURITY")) {
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

            // Product Log visible to Admin and Staff
            productLogButton.setVisible(isAdmin || isStaff);
            // Products visible to Admin
            productsButton.setVisible(isAdmin);
            // Alerts visible to Admin and Security (we're here already)
            alertsButton.setVisible(isAdmin || isSecurity);
            // User Management only for Admin
            userManagementButton.setVisible(isAdmin);
        }

        // Setup table columns
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        productColumn.setCellValueFactory(new PropertyValueFactory<>("product"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        messageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Load alerts
        loadFilterProducts();
        initializeDatePickers();
        setupPagination();

        // Check for overdue items on load
        checkForOverdueItems();
    }

    private void loadAlerts(int pageIndex) {
        try {
            List<Alert> alerts;
            int offset = pageIndex * PAGE_SIZE;
            Integer productId = null;
            var fp = filterProductCombo != null ? filterProductCombo.getSelectionModel().getSelectedItem() : null;
            if (fp != null) productId = fp.id();
            String status = unresolvedRadio.isSelected() ? "UNRESOLVED" : "ALL";
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
                alertsTable.getItems().clear();
                return;
            }

            alerts = alertService.getAlertsFiltered(productId, status, from, to, PAGE_SIZE, offset);
            int total = alertService.countAlertsFiltered(productId, status, from, to);
            int unresolvedCount = alertService.countAlertsFiltered(productId, "UNRESOLVED", from, to);
            if (pagination != null) pagination.setPageCount(Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE)));
            if ("UNRESOLVED".equals(status)) {
                alertsCountLabel.setText(total + " Unresolved Alerts");
            } else {
                alertsCountLabel.setText(unresolvedCount + " Unresolved / " + total + " Total Alerts");
            }

            ObservableList<AlertTableRow> alertRows = FXCollections.observableArrayList();

            for (Alert alert : alerts) {
                String productName = productService.getProductById(alert.productId())
                    .map(Product::name)
                    .orElse("Product ID: " + alert.productId());

                alertRows.add(new AlertTableRow(
                    alert.id(),
                    String.valueOf(alert.createdAt()),
                    productName,
                    alert.alertType(),
                    alert.message(),
                    alert.status()
                ));
            }

            alertsTable.getSelectionModel().clearSelection();
            alertsTable.setItems(alertRows);
            if (alertRows.isEmpty()) {
                messageLabel.setTextFill(javafx.scene.paint.Color.GRAY);
                messageLabel.setText("No results for the selected filters");
            } else {
                messageLabel.setText("");
            }
            messageLabel.setText("");
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Error loading alerts: " + e.getMessage());
        }
    }

    @FXML
    private void onFilterChange(ActionEvent event) {
        if (pagination != null) pagination.setCurrentPageIndex(0);
        loadAlerts(0);
    }

    @FXML
    private void onCheckOverdueClick(ActionEvent event) {
        try {
            // For demo purposes, mark any existing check-outs as overdue immediately
            int alertsCreated = alertService.checkForOverdueCheckouts(0);
            if (alertsCreated > 0) {
                messageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
                messageLabel.setText("Created " + alertsCreated + " new alert(s)");
            } else {
                messageLabel.setTextFill(javafx.scene.paint.Color.GRAY);
                messageLabel.setText("No new overdue items found");
            }
            onFilterChange(null);
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setTextFill(javafx.scene.paint.Color.RED);
            messageLabel.setText("Error checking for overdue items: " + e.getMessage());
        }
    }

    @FXML
    private void onResolveClick(ActionEvent event) {
        AlertTableRow selected = alertsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setTextFill(javafx.scene.paint.Color.RED);
            messageLabel.setText("Please select an alert to resolve");
            return;
        }

        if ("RESOLVED".equals(selected.getStatus())) {
            messageLabel.setTextFill(javafx.scene.paint.Color.ORANGE);
            messageLabel.setText("Alert is already resolved");
            return;
        }

        User currentUser = Session.getInstance().getCurrentUser();
        if (currentUser == null) {
            messageLabel.setTextFill(javafx.scene.paint.Color.RED);
            messageLabel.setText("User not logged in");
            return;
        }

        // Confirm resolution
        javafx.scene.control.Alert confirmDialog = new javafx.scene.control.Alert(AlertType.CONFIRMATION);
        confirmDialog.setTitle("Resolve Alert");
        confirmDialog.setHeaderText("Resolve Alert");
        confirmDialog.setContentText("Are you sure you want to mark this alert as resolved?");
        Optional<ButtonType> result = confirmDialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                alertService.resolveAlert(selected.getAlertId(), currentUser.id());
                messageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
                messageLabel.setText("Alert resolved successfully");
                reloadCurrentPage();
            } catch (Exception e) {
                e.printStackTrace();
                messageLabel.setTextFill(javafx.scene.paint.Color.RED);
                messageLabel.setText("Error resolving alert: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onDashboardClick(ActionEvent event) {
        navigateToDashboard(event);
    }

    @FXML
    private void onProductLogClick(ActionEvent event) {
        navigateToProductLog(event);
    }

    @FXML
    private void onAlertsClick(ActionEvent event) {
        // Already on alerts page
    }

    @FXML
    private void onUserManagementClick(ActionEvent event) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(AlertType.INFORMATION);
        alert.setTitle("Coming Soon");
        alert.setHeaderText("User Management");
        alert.setContentText("User management feature is coming soon!");
        alert.showAndWait();
    }
    @FXML
    private void onProductsClick(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/javafx/demo/product-management-view.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = stage.getScene();
            scene.setRoot(loader.load());
            stage.setTitle("Products");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void navigateToDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/javafx/demo/dashboard-view.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = stage.getScene();
            scene.setRoot(loader.load());
            stage.setTitle("Factory Dashboard");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void navigateToProductLog(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/javafx/demo/product-log-view.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = stage.getScene();
            scene.setRoot(loader.load());
            stage.setTitle("Product Log");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupPagination() {
        if (pagination != null) {
            pagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> {
                loadAlerts(newIndex.intValue());
            });
            pagination.setCurrentPageIndex(0);
            loadAlerts(0);
        } else {
            loadAlerts(0);
        }
    }

    private void reloadCurrentPage() {
        if (pagination != null) {
            loadAlerts(pagination.getCurrentPageIndex());
        } else {
            loadAlerts(0);
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
        loadAlerts(0);
    }

    @FXML
    private void initializeDatePickers() {
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

    private void checkForOverdueItems() {
        try {
            // Seed alerts if any check-outs exist (0 hours threshold for demo usability)
            alertService.checkForOverdueCheckouts(0);
        } catch (Exception e) {
            // Silent check on load
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

    // Table row model for alerts
    public static class AlertTableRow {
        private final int alertId;
        private final String date;
        private final String product;
        private final String type;
        private final String message;
        private final String status;

        public AlertTableRow(int alertId, String date, String product, String type, String message, String status) {
            this.alertId = alertId;
            this.date = date;
            this.product = product;
            this.type = type;
            this.message = message;
            this.status = status;
        }

        public int getAlertId() { return alertId; }
        public String getDate() { return date; }
        public String getProduct() { return product; }
        public String getType() { return type; }
        public String getMessage() { return message; }
        public String getStatus() { return status; }
    }

    private void showAccessDeniedAndGoDashboard() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Access Denied");
        alert.setHeaderText("Insufficient permissions");
        alert.setContentText("You do not have access to Alerts.");
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
}

