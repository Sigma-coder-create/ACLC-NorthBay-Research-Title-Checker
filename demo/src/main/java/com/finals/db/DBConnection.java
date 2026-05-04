package com.finals.db;


import java.sql.*;
import java.util.Properties;
import java.io.InputStream;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class DBConnection {

    private static Properties props = new Properties();
    private static Connection mysqlConnection = null;

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
    }

    public static boolean isInternetAvailable() {
        try {
            java.net.URL url = new java.net.URL("https://www.google.com");
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

    /** now always returns MySQL – no SQLite fallback */
    public static Connection getConnection() {
        Connection conn = getMySQLConnection();
        if (conn != null) {
            System.out.println("[DB] Using MySQL");
            return conn;
        }
        System.err.println("[DB] ❌ No MySQL connection available!");
        return null;
    }

    public static Connection getMySQLConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://" + props.getProperty("mysql.host") + ":" +
                    props.getProperty("mysql.port") + "/" + props.getProperty("mysql.db") +
                    "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&connectTimeout=3000";
            Connection conn = DriverManager.getConnection(url,
                    props.getProperty("mysql.user"),
                    props.getProperty("mysql.password"));
            System.out.println("[MySQL] ✅ Connected successfully.");
            ensureAdminTableExists(conn);  // auto‑create admin table if missing
            return conn;
        } catch (Exception e) {
            System.err.println("[MySQL] ❌ Connection failed: " + e.getMessage());
            return null;
        }
    }

    /** Creates admin table if it doesn’t exist */
    public static void ensureAdminTableExists(Connection mysql) {
        String sql = "CREATE TABLE IF NOT EXISTS admin (" +
                     "id INT AUTO_INCREMENT PRIMARY KEY, " +
                     "full_name VARCHAR(100) NOT NULL, " +
                     "email VARCHAR(100) NOT NULL UNIQUE, " +
                     "password VARCHAR(255) NOT NULL" +
                     ")";
        try (Statement stmt = mysql.createStatement()) {
            stmt.execute(sql);
            System.out.println("[MySQL] admin table verified/created.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void closeConnections() {
        try {
            if (mysqlConnection != null && !mysqlConnection.isClosed())
                mysqlConnection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
