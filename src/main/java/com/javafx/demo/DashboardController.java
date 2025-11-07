package com.javafx.demo;

import com.javafx.demo.app.Session;
import com.javafx.demo.dao.LogDao;
import com.javafx.demo.dao.ProductDao;
import com.javafx.demo.dao.UserDao;
import com.javafx.demo.model.Product;
import com.javafx.demo.model.ProductLog;
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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

public class DashboardController {

    @FXML
    private Label userLabel;
    @FXML
    private Label totalProductsLabel;
    @FXML
    private Label incomingProductsLabel;
    @FXML
    private Label outgoingProductsLabel;
    @FXML
    private Label alertsLabel;
    @FXML
    private TableView<LogTableRow> logsTable;
    @FXML
    private TableColumn<LogTableRow, String> dateColumn;
    @FXML
    private TableColumn<LogTableRow, String> actionColumn;
    @FXML
    private TableColumn<LogTableRow, String> productColumn;
    @FXML
    private TableColumn<LogTableRow, Integer> quantityColumn;
    @FXML
    private TableColumn<LogTableRow, String> userColumn;
    @FXML
    private Button dashboardButton;
    @FXML
    private Button productLogButton;
    @FXML
    private Button alertsButton;
    @FXML
    private Button userManagementButton;

    private final ProductService productService = new ProductService();
    private final AlertService alertService = new AlertService();
    private final LogDao logDao = new LogDao();
    private final ProductDao productDao = new ProductDao();
    private final UserDao userDao = new UserDao();

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
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        productColumn.setCellValueFactory(new PropertyValueFactory<>("product"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        userColumn.setCellValueFactory(new PropertyValueFactory<>("user"));

        // Load dashboard data
        loadDashboardData();

        // Check for overdue checkouts and create alerts
        alertService.checkForOverdueCheckouts();
    }

    private void loadDashboardData() {
        try {
            // Load statistics
            int totalProducts = productDao.findAll().size();
            totalProductsLabel.setText(String.valueOf(totalProducts));

            int todayCheckIns = logDao.countTodayCheckIns();
            incomingProductsLabel.setText(String.valueOf(todayCheckIns));

            int todayCheckOuts = logDao.countTodayCheckOuts();
            outgoingProductsLabel.setText(String.valueOf(todayCheckOuts));

            int unresolvedAlerts = alertService.getUnresolvedAlertCount();
            alertsLabel.setText(String.valueOf(unresolvedAlerts));

            // Load recent logs
            ObservableList<LogTableRow> logRows = FXCollections.observableArrayList();
            var recentLogs = productService.getRecentLogs(50);
            
            for (ProductLog log : recentLogs) {
                // Get product name
                String productName = productDao.findById(log.productId())
                    .map(Product::name)
                    .orElse("Product ID: " + log.productId());

                // Get user name
                String userName = getUsernameById(log.userId());

                logRows.add(new LogTableRow(
                    log.timestamp().format(DATE_FORMATTER),
                    log.actionType(),
                    productName,
                    log.quantity(),
                    userName
                ));
            }

            logsTable.setItems(logRows);
        } catch (Exception e) {
            e.printStackTrace();
            // Show error message
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load dashboard data");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private String getUsernameById(int userId) {
        return userDao.findById(userId)
            .map(User::username)
            .orElse("User ID: " + userId);
    }

    @FXML
    private void onDashboardClick(ActionEvent event) {
        // Already on dashboard
        updateNavigationStyle(dashboardButton);
    }

    @FXML
    private void onProductLogClick(ActionEvent event) {
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
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load Product Log");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void onAlertsClick(ActionEvent event) {
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
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load Alerts");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void onUserManagementClick(ActionEvent event) {
        // TODO: Implement user management view
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Coming Soon");
        alert.setHeaderText("User Management");
        alert.setContentText("User management feature is coming soon!");
        alert.showAndWait();
    }

    private void updateNavigationStyle(Button selectedButton) {
        dashboardButton.getStyleClass().remove("selected");
        productLogButton.getStyleClass().remove("selected");
        alertsButton.getStyleClass().remove("selected");
        userManagementButton.getStyleClass().remove("selected");
        selectedButton.getStyleClass().add("selected");
    }

    // Inner class for table rows
    public static class LogTableRow {
        private final String date;
        private final String action;
        private final String product;
        private final Integer quantity;
        private final String user;

        public LogTableRow(String date, String action, String product, Integer quantity, String user) {
            this.date = date;
            this.action = action;
            this.product = product;
            this.quantity = quantity;
            this.user = user;
        }

        public String getDate() { return date; }
        public String getAction() { return action; }
        public String getProduct() { return product; }
        public Integer getQuantity() { return quantity; }
        public String getUser() { return user; }
    }
}
