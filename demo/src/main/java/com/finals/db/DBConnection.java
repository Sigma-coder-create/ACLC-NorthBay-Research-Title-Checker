package com.finals.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.Properties;
import java.io.InputStream;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class DBConnection {

    private static Properties props = new Properties();
    private static HikariDataSource hikariPool = null;

    static {
        boolean loaded = false;
        // try encrypted
        try (InputStream encIn = DBConnection.class.getResourceAsStream("/raven/modal/demo/data/db.properties.enc")) {
            if (encIn != null) {
                System.out.println("[DB] Found encrypted properties file. Attempting decryption...");
                byte[] encrypted = encIn.readAllBytes();
                SecretKeySpec keySpec = new SecretKeySpec("16ByteSecretKey!".getBytes("UTF-8"), "AES");
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.DECRYPT_MODE, keySpec);
                byte[] decrypted = cipher.doFinal(encrypted);
                props.load(new java.io.ByteArrayInputStream(decrypted));
                loaded = true;
                System.out.println("[DB] ✅ Loaded encrypted configuration.");
            }
        } catch (Exception e) {
            System.err.println("[DB] Encrypted config failed: " + e.getMessage());
        }
        // fallback to plain
        if (!loaded) {
            try (InputStream in = DBConnection.class.getResourceAsStream("/raven/modal/demo/data/db.properties")) {
                if (in == null) throw new RuntimeException("Neither db.properties.enc nor db.properties found!");
                props.load(in);
                System.out.println("[DB] ✅ Loaded plain configuration.");
            } catch (Exception e) {
                throw new RuntimeException("Failed to load any configuration: " + e.getMessage(), e);
            }
        }
        // Resolve environment variable placeholders
        resolveEnvPlaceholders();

        // Initialize HikariCP pool
        try {
            HikariConfig config = new HikariConfig();
            String url = "jdbc:mysql://" + props.getProperty("mysql.host") + ":" +
                        props.getProperty("mysql.port") + "/" + props.getProperty("mysql.db") +
                        "?sslMode=REQUIRED&serverTimezone=UTC&allowPublicKeyRetrieval=true&connectTimeout=3000";
            config.setJdbcUrl(url);
            config.setUsername(props.getProperty("mysql.user"));
            config.setPassword(props.getProperty("mysql.password"));
            config.setMaximumPoolSize(10);               // adjust based on load
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");

            hikariPool = new HikariDataSource(config);
            System.out.println("[DB] HikariCP pool initialized.");
        } catch (Exception e) {
            System.err.println("[DB] ❌ HikariCP pool creation failed: " + e.getMessage());
            // If pool fails, fall back to direct connections? For robustness, we leave null.
        }
    }

    private static void resolveEnvPlaceholders() {
        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            if (value != null && value.contains("${")) {
                StringBuilder sb = new StringBuilder();
                int i = 0;
                while (i < value.length()) {
                    int start = value.indexOf("${", i);
                    if (start == -1) {
                        sb.append(value.substring(i));
                        break;
                    }
                    int end = value.indexOf("}", start);
                    if (end == -1) {
                        sb.append(value.substring(i));
                        break;
                    }
                    String envName = value.substring(start + 2, end);
                    String envValue = System.getenv(envName);
                    sb.append(value, i, start);
                    sb.append(envValue != null ? envValue : ("${" + envName + "}"));
                    i = end + 1;
                }
                props.setProperty(key, sb.toString());
            }
        }
    }

    public static boolean isInternetAvailable() {
        try {
            java.net.URI uri = new java.net.URI("https://www.google.com");
            java.net.URL url = uri.toURL();
            java.net.URLConnection connection = url.openConnection();
            connection.setConnectTimeout(2000);
            connection.connect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getProperty(String key) {
        return props.getProperty(key);
    }

    public static Connection getConnection() {
        // Returns a pooled MySQL connection (compatible with existing code)
        return getMySQLConnection();
    }

    public static Connection getMySQLConnection() {
        if (hikariPool == null) {
            System.err.println("[DB] ❌ Connection pool not available");
            return null;
        }
        try {
            return hikariPool.getConnection();
        } catch (SQLException e) {
            System.err.println("[DB] ❌ Failed to get connection from pool: " + e.getMessage());
            return null;
        }
    }

    public static void ensureUsersTableExists(Connection mysql) {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                     "id INT AUTO_INCREMENT PRIMARY KEY, " +
                     "username VARCHAR(100) NOT NULL UNIQUE, " +
                     "email VARCHAR(100) NOT NULL UNIQUE, " +
                     "password_hash VARCHAR(255) NOT NULL, " +
                     "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                     "role ENUM('teacher', 'student') NOT NULL DEFAULT 'student'" +
                     ")";
        try (Statement stmt = mysql.createStatement()) {
            stmt.execute(sql);
            System.out.println("[MySQL] users table verified/created.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void closeConnections() {
        if (hikariPool != null && !hikariPool.isClosed()) {
            hikariPool.close();
            System.out.println("[DB] HikariCP pool shut down.");
        }
    }
}