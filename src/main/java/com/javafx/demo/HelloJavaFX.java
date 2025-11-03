package com.javafx.demo;

import com.javafx.demo.db.Database;
import com.javafx.demo.security.AuthService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloJavaFX extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
        // Bootstrap database and seed admin account
        Database.migrateIfNeeded();
        new AuthService().seedAdminIfMissing();

        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/com/javafx/demo/login-view.fxml")
        );
        Scene scene = new Scene(loader.load(), 360, 220);
        
        stage.setTitle("Factory Inventory Login");
        stage.setScene(scene);
        stage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
