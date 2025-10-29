package com.javafx.demo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class HelloJavaFX extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        Label label = new Label("Hello, JavaFX from Cursor!");
        Button button = new Button("Click me!");
        
        button.setOnAction(e -> label.setText("Button clicked!"));
        
        VBox root = new VBox(10);
        root.getChildren().addAll(label, button);
        
        Scene scene = new Scene(root, 300, 200);
        
        primaryStage.setTitle("JavaFX Demo in Cursor");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
