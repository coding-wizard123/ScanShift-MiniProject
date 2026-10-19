package com.scanshift.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found. Ensure mysql-connector-j is in pom.xml and Maven dependencies have been downloaded.", e);
        }

        String url = DatabaseConfig.getJdbcUrl();
        String user = DatabaseConfig.getUsername();
        String password = DatabaseConfig.getPassword();
        if (password == null || password.isBlank()) {
            throw new SQLException("SCANSHIFT_DB_PASSWORD environment variable is not set.");
        }

        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new SQLException(
                    "Unable to connect to MySQL. Check that MySQL is running, the database exists, and the configured credentials are correct.",
                    e
            );
        }
    }
}
