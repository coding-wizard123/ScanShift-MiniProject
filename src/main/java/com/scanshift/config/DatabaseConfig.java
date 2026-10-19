package com.scanshift.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class DatabaseConfig {
    private static final String DEFAULT_PROPERTIES_NAME = "db.properties";
    private static final Properties PROPERTIES = new Properties();

    static {
        loadProperties();
    }

    private DatabaseConfig() {
    }

    public static String getHost() {
        return getProperty("DB_HOST", "localhost");
    }

    public static String getPort() {
        return getProperty("DB_PORT", "3306");
    }

    public static String getDatabaseName() {
        return getProperty("DB_NAME", "scanshift");
    }

    public static String getUsername() {
        return getProperty("DB_USER", "root");
    }

    public static String getPassword() {
        return System.getenv("SCANSHIFT_DB_PASSWORD");
    }

    public static String getJdbcUrl() {
        String configuredUrl = getProperty("DB_URL", "");
        if (!configuredUrl.isBlank()) {
            return configuredUrl;
        }
        return "jdbc:mysql://" + getHost() + ":" + getPort() + "/" + getDatabaseName()
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    private static void loadProperties() {
        try (InputStream resourceStream = DatabaseConfig.class.getClassLoader().getResourceAsStream(DEFAULT_PROPERTIES_NAME)) {
            if (resourceStream != null) {
                PROPERTIES.load(resourceStream);
            }
        } catch (IOException e) {
            System.err.println("Unable to read db.properties from classpath: " + e.getMessage());
        }

        Path workingDirProperties = Path.of(System.getProperty("user.dir"), DEFAULT_PROPERTIES_NAME);
        if (Files.exists(workingDirProperties) && Files.isRegularFile(workingDirProperties)) {
            try (InputStream fileStream = Files.newInputStream(workingDirProperties)) {
                PROPERTIES.load(fileStream);
            } catch (IOException e) {
                System.err.println("Unable to read db.properties from project root: " + e.getMessage());
            }
        }

        String envHost = System.getenv("DB_HOST");
        if (envHost != null && !envHost.isBlank()) {
            PROPERTIES.setProperty("DB_HOST", envHost);
        }
        String envPort = System.getenv("DB_PORT");
        if (envPort != null && !envPort.isBlank()) {
            PROPERTIES.setProperty("DB_PORT", envPort);
        }
        String envName = System.getenv("DB_NAME");
        if (envName != null && !envName.isBlank()) {
            PROPERTIES.setProperty("DB_NAME", envName);
        }
        String envUser = System.getenv("DB_USER");
        if (envUser != null && !envUser.isBlank()) {
            PROPERTIES.setProperty("DB_USER", envUser);
        }
        String envUrl = System.getenv("DB_URL");
        if (envUrl != null && !envUrl.isBlank()) {
            PROPERTIES.setProperty("DB_URL", envUrl);
        }
    }

    private static String getProperty(String key, String defaultValue) {
        return PROPERTIES.getProperty(key, defaultValue).trim();
    }
}
