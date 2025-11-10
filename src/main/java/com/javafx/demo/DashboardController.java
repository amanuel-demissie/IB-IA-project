package com.javafx.demo;

import com.javafx.demo.app.Session;
import com.javafx.demo.dao.LogDao;
import com.javafx.demo.dao.ProductDao;
import com.javafx.demo.dao.UserDao;
import com.javafx.demo.model.Product;
import com.javafx.demo.model.ProductLog;
import com.javafx.demo.model.User;
import com.javafx.demo.service.AlertService;
import com.javafx.demo.service.ReportService;
import com.javafx.demo.service.ProductService;
import com.javafx.demo.dao.SettingsDao;
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
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;
import javafx.print.PrinterJob;

// removed explicit DateTimeFormatter usage to avoid runtime resolution issues
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    @FXML
    private Button productsButton;
    @FXML
    private Button generateReportButton;
    @FXML
    private Button printReportButton;

    private final ProductService productService = new ProductService();
    private final AlertService alertService = new AlertService();
    private final ReportService reportService = new ReportService();
    private final SettingsDao settingsDao = new SettingsDao();
    private final LogDao logDao = new LogDao();
    private final ProductDao productDao = new ProductDao();
    private final UserDao userDao = new UserDao();

    private ScheduledExecutorService refreshScheduler;

    @FXML
    private void initialize() {
        // Require login for all dashboard views
        if (!AuthGuard.isLoggedIn()) {
            navigateToLoginInternal();
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
            // Alerts visible to Admin and Security
            alertsButton.setVisible(isAdmin || isSecurity);
            // User Management only for Admin
            userManagementButton.setVisible(isAdmin);
            // Generate report button only for Admin
            if (generateReportButton != null) {
                generateReportButton.setVisible(isAdmin);
            }
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

        // Start auto-refresh if enabled
        int refreshSeconds = settingsDao.getInt("dashboard_refresh_seconds", 15);
        if (refreshSeconds > 0) {
            startAutoRefresh(refreshSeconds);
        }
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
                    String.valueOf(log.timestamp()),
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

    private void navigateToLoginInternal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/javafx/demo/login-view.fxml"));
            Scene scene = new Scene(loader.load(), 960, 640);
            scene.getStylesheets().add(
                getClass().getResource("/com/javafx/demo/styles.css").toExternalForm()
            );
            Stage stage = (Stage) userLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Factory Inventory Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onGenerateReportClick(ActionEvent event) {
        try {
            var path = reportService.generateTodayCsvReport();
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Report Generated");
            success.setHeaderText("Today's report has been generated");
            success.setContentText("Saved to: " + path.toString());
            success.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Report Error");
            error.setHeaderText("Failed to generate report");
            error.setContentText(e.getMessage());
            error.showAndWait();
        }
    }

    @FXML
    private void onPrintReportClick(ActionEvent event) {
        try {
            String html = reportService.buildHtmlForDate(java.time.LocalDate.now());
            WebView webView = new WebView();
            webView.setContextMenuEnabled(false);
            WebEngine engine = webView.getEngine();
            engine.loadContent(html, "text/html");
            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    try {
                        Object h = engine.executeScript("document.body.scrollHeight");
                        double height = 800;
                        if (h instanceof Number n) {
                            height = n.doubleValue();
                        }
                        webView.setPrefWidth(800);
                        webView.setPrefHeight(height + 20);
                        PrinterJob job = PrinterJob.createPrinterJob();
                        if (job != null) {
                            boolean accepted = job.showPrintDialog(printReportButton.getScene().getWindow());
                            if (accepted) {
                                boolean printed = job.printPage(webView);
                                if (printed) job.endJob();
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Print Error");
            error.setHeaderText("Failed to print report");
            error.setContentText(e.getMessage());
            error.showAndWait();
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
            shutdownAutoRefresh();
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
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load Products");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void onAlertsClick(ActionEvent event) {
        try {
            shutdownAutoRefresh();
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
        try {
            shutdownAutoRefresh();
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
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load User Management");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void onLogoutClick(ActionEvent event) {
        Session.getInstance().setCurrentUser(null);
        try {
            shutdownAutoRefresh();
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

    private void updateNavigationStyle(Button selectedButton) {
        dashboardButton.getStyleClass().remove("selected");
        productLogButton.getStyleClass().remove("selected");
        alertsButton.getStyleClass().remove("selected");
        userManagementButton.getStyleClass().remove("selected");
        productsButton.getStyleClass().remove("selected");
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

    private void startAutoRefresh(int intervalSeconds) {
        refreshScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "dashboard-refresh");
            t.setDaemon(true);
            return t;
        });
        refreshScheduler.scheduleAtFixedRate(() -> {
            try {
                javafx.application.Platform.runLater(this::loadDashboardData);
            } catch (Exception ignored) {}
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private void shutdownAutoRefresh() {
        if (refreshScheduler != null) {
            refreshScheduler.shutdownNow();
            refreshScheduler = null;
        }
    }
}
