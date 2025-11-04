package com.javafx.demo;

import com.javafx.demo.app.Session;
import com.javafx.demo.model.User;
import com.javafx.demo.security.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
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
            Scene scene = new Scene(loader.load(), 1200, 800);
            scene.getStylesheets().add(
                getClass().getResource("/com/javafx/demo/styles.css").toExternalForm()
            );
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Factory Dashboard");
        } catch (Exception e) {
            errorLabel.setText("Failed to load dashboard");
        }
    }
}


