package com.javafx.demo;

import com.javafx.demo.app.Session;
import com.javafx.demo.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController {

    @FXML
    private Label userLabel;

    @FXML
    private void initialize() {
        User u = Session.getInstance().getCurrentUser();
        if (u != null) {
            userLabel.setText("Logged in as: " + u.username() + " (" + u.roleName() + ")");
        }
    }
}


