package com.javafx.demo.db;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public final class Database {

    private static final Properties props = new Properties();

    static {
        try (InputStream in = Database.class.getResourceAsStream("/db.properties")) {
            if (in == null) throw new IllegalStateException("db.properties not found on classpath");
            props.load(in);
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database configuration", e);
        }
    }

    private Database() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            props.getProperty("db.url"),
            props.getProperty("db.username"),
            props.getProperty("db.password")
        );
    }

    public static void migrateIfNeeded() {
        try (InputStream in = Database.class.getResourceAsStream("/schema.sql")) {
            if (in == null) return;
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            try (Connection c = getConnection(); Statement st = c.createStatement()) {
                for (String stmt : sql.split(";")) {
                    String s = stmt.trim();
                    if (!s.isEmpty()) st.execute(s);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Schema migration failed", e);
        }
    }
}


