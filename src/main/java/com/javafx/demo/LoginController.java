package com.javafx.demo;

import com.javafx.demo.app.Session;
import com.javafx.demo.model.User;
import com.javafx.demo.security.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void onLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        User user = authService.login(username, password);
        if (user == null) {
            errorLabel.setText("Invalid credentials");
            return;
        }

        Session.getInstance().setCurrentUser(user);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/javafx/demo/dashboard-view.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = stage.getScene();
            scene.setRoot(loader.load());
            stage.setTitle("Factory Dashboard");
        } catch (Exception e) {
            e.printStackTrace();
            Throwable root = e;
            while (root.getCause() != null) root = root.getCause();
            String msg = root.getClass().getSimpleName() + (root.getMessage() != null ? (": " + root.getMessage()) : "");
            errorLabel.setText("Failed to load dashboard: " + msg);
        }
    }

    @FXML
    private void onForgotPassword(ActionEvent event) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Password Reset");
        info.setHeaderText("Forgot your password?");
        info.setContentText("Please contact an Admin to reset your password. Admins can reset it from User Management.");
        info.showAndWait();
    }
}


