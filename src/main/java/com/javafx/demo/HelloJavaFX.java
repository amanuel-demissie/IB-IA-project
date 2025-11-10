package com.javafx.demo;

import com.javafx.demo.db.Database;
import com.javafx.demo.security.AuthService;
import com.javafx.demo.service.ProductService;
import com.javafx.demo.service.AlertService;
import com.javafx.demo.service.ReportService;
import com.javafx.demo.dao.SettingsDao;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class HelloJavaFX extends Application {
    
    private ScheduledExecutorService scheduler;
    private int schedulerIntervalMinutes = 1;
    private final SettingsDao settingsDao = new SettingsDao();
    private ScheduledExecutorService reportScheduler;

    @Override
    public void start(Stage stage) throws Exception {
        // Bootstrap database and seed admin account
        Database.migrateIfNeeded();
        new AuthService().seedAdminIfMissing();
        new ProductService().seedSampleProductsIfEmpty();

        // Read settings
        schedulerIntervalMinutes = settingsDao.getInt("scheduler_interval_minutes", 1);

        // Start background scheduler for overdue checks
        startAlertScheduler();
        // Start daily report scheduler
        startReportScheduler();

        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/com/javafx/demo/login-view.fxml")
        );
        Scene scene = new Scene(loader.load(), 960, 640);
        scene.getStylesheets().add(
            getClass().getResource("/com/javafx/demo/styles.css").toExternalForm()
        );
        
        stage.setTitle("Factory Inventory Login");
        stage.setScene(scene);
        stage.setOnCloseRequest(evt -> shutdownScheduler());
        stage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }

    private void startAlertScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "alert-scheduler");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Read overdue hours dynamically each run; default from PRD is 2
                int overdueHours = settingsDao.getInt("overdue_hours", 2);
                new AlertService().checkForOverdueCheckouts(overdueHours);
            } catch (Exception ignored) {
                // Best-effort background task
            }
        }, 0, schedulerIntervalMinutes, TimeUnit.MINUTES);
    }

    private void shutdownScheduler() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (reportScheduler != null) {
            reportScheduler.shutdownNow();
        }
    }

    private void startReportScheduler() {
        reportScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "report-scheduler");
            t.setDaemon(true);
            return t;
        });
        String timeStr = settingsDao.get("report_time");
        if (timeStr == null || !timeStr.matches("^\\d{2}:\\d{2}$")) {
            timeStr = "23:55";
        }
        String[] parts = timeStr.split(":");
        LocalTime reportTime = LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        Runnable task = () -> {
            try {
                new ReportService().generateCsvReportForDate(LocalDate.now());
            } catch (Exception ignored) {}
        };
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = LocalDateTime.of(now.toLocalDate(), reportTime);
        if (now.isAfter(nextRun)) {
            nextRun = nextRun.plusDays(1);
        }
        long initialDelaySeconds = Duration.between(now, nextRun).getSeconds();
        long periodSeconds = TimeUnit.DAYS.toSeconds(1);
        reportScheduler.scheduleAtFixedRate(task, initialDelaySeconds, periodSeconds, TimeUnit.SECONDS);
    }
}
