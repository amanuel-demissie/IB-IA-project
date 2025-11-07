package com.javafx.demo;

import com.javafx.demo.app.Session;
import com.javafx.demo.model.Alert;
import com.javafx.demo.model.Product;
import com.javafx.demo.model.User;
import com.javafx.demo.service.AlertService;
import com.javafx.demo.service.ProductService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

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

    private final AlertService alertService = new AlertService();
    private final ProductService productService = new ProductService();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    private void initialize() {
        // Set user info
        User u = Session.getInstance().getCurrentUser();
        if (u != null) {
            userLabel.setText("Logged in as: " + u.username() + " (" + u.roleName() + ")");
        }

        // Setup table columns
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        productColumn.setCellValueFactory(new PropertyValueFactory<>("product"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        messageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Load alerts
        loadAlerts();

        // Check for overdue items on load
        checkForOverdueItems();
    }

    private void loadAlerts() {
        try {
            List<Alert> alerts;
            if (unresolvedRadio.isSelected()) {
                alerts = alertService.getUnresolvedAlerts();
                alertsCountLabel.setText(alerts.size() + " Unresolved Alerts");
            } else {
                alerts = alertService.getAllAlerts();
                long unresolvedCount = alerts.stream().filter(a -> !a.isResolved()).count();
                alertsCountLabel.setText(unresolvedCount + " Unresolved / " + alerts.size() + " Total Alerts");
            }

            ObservableList<AlertTableRow> alertRows = FXCollections.observableArrayList();

            for (Alert alert : alerts) {
                String productName = productService.getProductById(alert.productId())
                    .map(Product::name)
                    .orElse("Product ID: " + alert.productId());

                alertRows.add(new AlertTableRow(
                    alert.id(),
                    alert.createdAt().format(DATE_FORMATTER),
                    productName,
                    alert.alertType(),
                    alert.message(),
                    alert.status()
                ));
            }

            alertsTable.setItems(alertRows);
            messageLabel.setText("");
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Error loading alerts: " + e.getMessage());
        }
    }

    @FXML
    private void onFilterChange(ActionEvent event) {
        loadAlerts();
    }

    @FXML
    private void onCheckOverdueClick(ActionEvent event) {
        try {
            int alertsCreated = alertService.checkForOverdueCheckouts();
            if (alertsCreated > 0) {
                messageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
                messageLabel.setText("Created " + alertsCreated + " new alert(s)");
            } else {
                messageLabel.setTextFill(javafx.scene.paint.Color.GRAY);
                messageLabel.setText("No new overdue items found");
            }
            loadAlerts();
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
                loadAlerts();
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

    private void navigateToProductLog(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/javafx/demo/product-log-view.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);
            scene.getStylesheets().add(
                getClass().getResource("/com/javafx/demo/styles.css").toExternalForm()
            );
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Product Log");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkForOverdueItems() {
        try {
            alertService.checkForOverdueCheckouts();
        } catch (Exception e) {
            // Silent check on load
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
}

