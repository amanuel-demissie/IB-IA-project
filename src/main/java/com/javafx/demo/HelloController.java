package com.javafx.demo;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HelloController {

    @FXML
    private Label label;

    @FXML
    private void onButtonClick() {
        label.setText("Button clicked!");
    }
}

