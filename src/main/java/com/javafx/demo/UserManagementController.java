package com.javafx.demo;

import com.javafx.demo.app.Session;
import com.javafx.demo.dao.UserDao;
import com.javafx.demo.dao.SettingsDao;
import com.javafx.demo.model.User;
import com.javafx.demo.security.PasswordHasher;
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

public class UserManagementController {

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
    private TextField overdueHoursField;
    @FXML
    private TextField schedulerIntervalField;
    @FXML
    private TextField reportTimeField;
    @FXML
    private Label settingsMessageLabel;
    @FXML
    private TextField dashboardRefreshSecondsField;

    @FXML
    private TableView<UserRow> usersTable;
    @FXML
    private TableColumn<UserRow, Integer> idColumn;
    @FXML
    private TableColumn<UserRow, String> usernameColumn;
    @FXML
    private TableColumn<UserRow, String> roleColumn;

    @FXML
    private TextField newUsernameField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private ComboBox<String> newRoleCombo;
    @FXML
    private Label messageLabel;

    @FXML
    private ComboBox<String> editRoleCombo;

    private final UserDao userDao = new UserDao();
    private final SettingsDao settingsDao = new SettingsDao();

    @FXML
    private void initialize() {
        User current = Session.getInstance().getCurrentUser();
        if (current != null) {
            userLabel.setText("Logged in as: " + current.username() + " (" + current.roleName() + ")");
        }

        // Only admins can access
        if (current == null || !"ADMIN".equals(current.roleName())) {
            messageLabel.setText("Access denied: Admins only");
            navigateToDashboardInternal();
            return;
        }

        // Setup sidebar visibility
        dashboardButton.setVisible(true);
        productLogButton.setVisible(true);
        alertsButton.setVisible(true);
        userManagementButton.setVisible(true);

        // Table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));

        // Role combos
        ObservableList<String> roles = FXCollections.observableArrayList("ADMIN", "SECURITY", "STAFF");
        newRoleCombo.setItems(roles);
        editRoleCombo.setItems(roles);

        loadUsers();

        // When a user row is selected, reflect the current role in the combo
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, newRow) -> {
            if (newRow != null) {
                editRoleCombo.getSelectionModel().select(newRow.getRole());
            } else {
                editRoleCombo.getSelectionModel().clearSelection();
            }
        });

        // Load settings defaults
        overdueHoursField.setText(String.valueOf(settingsDao.getInt("overdue_hours", 2)));
        schedulerIntervalField.setText(String.valueOf(settingsDao.getInt("scheduler_interval_minutes", 1)));
        String reportTime = settingsDao.get("report_time");
        if (reportTime == null || reportTime.isBlank()) reportTime = "23:55";
        reportTimeField.setText(reportTime);
        dashboardRefreshSecondsField.setText(String.valueOf(settingsDao.getInt("dashboard_refresh_seconds", 15)));
    }

    private void loadUsers() {
        ObservableList<UserRow> rows = FXCollections.observableArrayList();
        for (User u : userDao.findAll()) {
            rows.add(new UserRow(u.id(), u.username(), u.roleName()));
        }
        usersTable.setItems(rows);
        messageLabel.setText("");
    }

    @FXML
    private void onCreateUser(ActionEvent event) {
        try {
            String username = newUsernameField.getText().trim();
            String password = newPasswordField.getText();
            String role = newRoleCombo.getSelectionModel().getSelectedItem();
            if (username.isEmpty() || password.isEmpty() || role == null) {
                messageLabel.setText("Please fill username, password, and role");
                return;
            }
            userDao.createUser(username, PasswordHasher.hash(password), role);
            newUsernameField.clear();
            newPasswordField.clear();
            newRoleCombo.getSelectionModel().clearSelection();
            messageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
            messageLabel.setText("User created");
            loadUsers();
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setTextFill(javafx.scene.paint.Color.RED);
            messageLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onResetPassword(ActionEvent event) {
        UserRow selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("Select a user first");
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reset Password");
        dialog.setHeaderText("Reset password for " + selected.getUsername());
        dialog.setContentText("New password:");
        var result = dialog.showAndWait();
        result.ifPresent(pw -> {
            if (pw.isBlank()) {
                messageLabel.setText("Password cannot be blank");
                return;
            }
            userDao.updateUserPassword(selected.getId(), PasswordHasher.hash(pw));
            messageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
            messageLabel.setText("Password updated");
        });
    }

    @FXML
    private void onChangeRole(ActionEvent event) {
        UserRow selected = usersTable.getSelectionModel().getSelectedItem();
        String role = editRoleCombo.getSelectionModel().getSelectedItem();
        if (selected == null || role == null) {
            messageLabel.setText("Select a user and role");
            return;
        }
        userDao.updateUserRole(selected.getId(), role);
        messageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
        messageLabel.setText("Role updated");
        loadUsers();
        // keep the combo reflecting the new role
        editRoleCombo.getSelectionModel().select(role);
    }

    @FXML
    private void onDeleteUser(ActionEvent event) {
        UserRow selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("Select a user to delete");
            return;
        }
        if (selected.getUsername().equalsIgnoreCase("admin")) {
            messageLabel.setText("Cannot delete default admin");
            return;
        }
        var confirm = new javafx.scene.control.Alert(Alert.AlertType.CONFIRMATION, "Delete user " + selected.getUsername() + "?", ButtonType.OK, ButtonType.CANCEL);
        var res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            userDao.deleteUser(selected.getId());
            loadUsers();
            messageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
            messageLabel.setText("User deleted");
        }
    }

    @FXML
    private void onDashboardClick(ActionEvent event) { navigateTo("/com/javafx/demo/dashboard-view.fxml", event, "Factory Dashboard"); }
    @FXML
    private void onProductLogClick(ActionEvent event) { navigateTo("/com/javafx/demo/product-log-view.fxml", event, "Product Log"); }
    @FXML
    private void onAlertsClick(ActionEvent event) { navigateTo("/com/javafx/demo/alerts-view.fxml", event, "Alerts"); }
    @FXML
    private void onProductsClick(ActionEvent event) { navigateTo("/com/javafx/demo/product-management-view.fxml", event, "Products"); }
    @FXML
    private void onUserManagementClick(ActionEvent event) { /* already here */ }

    @FXML
    private void onLogoutClick(ActionEvent event) {
        Session.getInstance().setCurrentUser(null);
        navigateTo("/com/javafx/demo/login-view.fxml", event, "Factory Inventory Login");
    }

    @FXML
    private void onSaveSettings(ActionEvent event) {
        try {
            int overdue = Integer.parseInt(overdueHoursField.getText().trim());
            int interval = Integer.parseInt(schedulerIntervalField.getText().trim());
            String time = reportTimeField.getText().trim();
            int dashRefresh = Integer.parseInt(dashboardRefreshSecondsField.getText().trim());
            if (overdue < 0 || interval <= 0) {
                settingsMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
                settingsMessageLabel.setText("Overdue must be >= 0 and interval > 0");
                return;
            }
            if (dashRefresh < 0) {
                settingsMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
                settingsMessageLabel.setText("Dashboard refresh must be >= 0");
                return;
            }
            // naive HH:mm validation
            if (!time.matches("^\\d{2}:\\d{2}$")) {
                settingsMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
                settingsMessageLabel.setText("Report time must be HH:mm");
                return;
            }
            settingsDao.set("overdue_hours", String.valueOf(overdue));
            settingsDao.set("scheduler_interval_minutes", String.valueOf(interval));
            settingsDao.set("report_time", time);
            settingsDao.set("dashboard_refresh_seconds", String.valueOf(dashRefresh));
            settingsMessageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
            settingsMessageLabel.setText("Settings saved. Scheduler interval applies on next app start.");
        } catch (NumberFormatException e) {
            settingsMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
            settingsMessageLabel.setText("Overdue, interval and refresh must be numbers");
        } catch (Exception e) {
            e.printStackTrace();
            settingsMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
            settingsMessageLabel.setText("Error: " + e.getMessage());
        }
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

    public static class UserRow {
        private final Integer id;
        private final String username;
        private final String role;

        public UserRow(Integer id, String username, String role) {
            this.id = id;
            this.username = username;
            this.role = role;
        }

        public Integer getId() { return id; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
    }
}


