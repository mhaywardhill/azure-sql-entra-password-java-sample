
package com.example;

import java.sql.*;
import java.util.Properties;

public class EntraPasswordSqlSample {
    public static void main(String[] args) {
        String server = getenvOrFail("AZURE_SQL_SERVER"); 
        String database = getenvOrFail("AZURE_SQL_DATABASE");
        String username = getenvOrFail("AZURE_SQL_USER"); 
        String password = getenvOrFail("AZURE_SQL_PASSWORD");
        String eventName = System.getenv().getOrDefault("EVENT_NAME", "HelloFromJava");
        
        // Enable JDBC driver logging if JDBC_TRACE is set to true
        String enableTrace = System.getenv().getOrDefault("JDBC_TRACE", "false");
        String traceLevel = System.getenv().getOrDefault("JDBC_TRACE_LEVEL", "INFO").toUpperCase();

        String url = String.format(
            "jdbc:sqlserver://%s:1433;database=%s;encrypt=true;hostNameInCertificate=*.database.windows.net;loginTimeout=30;authentication=ActiveDirectoryPassword",
            server, database
        );

        Properties props = new Properties();
        props.setProperty("user", username);
        props.setProperty("password", password);
        
        // Configure driver logging level programmatically
        if ("true".equalsIgnoreCase(enableTrace)) {
            configureJdbcLogging(traceLevel);
        }

        System.out.println("Connecting to Azure SQL with Entra Password auth...");
        try (Connection conn = DriverManager.getConnection(url, props)) {
            System.out.println("Connected.");
            ensureEventsTable(conn);
            insertEvent(conn, eventName);
            int count = countEvents(conn);
            System.out.println("Rows in dbo.Events: " + count);
        } catch (SQLException e) {
            System.err.println("SQL error: " + e.getMessage());
            for (Throwable t : e) {
                t.printStackTrace();
            }
            System.exit(1);
        }
    }

    private static void ensureEventsTable(Connection conn) throws SQLException {
        String ddl = "IF OBJECT_ID(N'dbo.Events', N'U') IS NULL BEGIN " +
                     "CREATE TABLE dbo.Events (" +
                     "Id INT IDENTITY(1,1) PRIMARY KEY, " +
                     "Name NVARCHAR(200) NOT NULL, " +
                     "CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()" +
                     ") END";
        try (Statement st = conn.createStatement()) {
            st.execute(ddl);
        }
    }

    private static void insertEvent(Connection conn, String name) throws SQLException {
        String sql = "INSERT INTO dbo.Events (Name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            int rows = ps.executeUpdate();
            System.out.println("Inserted rows: " + rows);
        }
    }

    private static int countEvents(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.Events";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static void configureJdbcLogging(String level) {
        System.out.println("JDBC driver tracing enabled at level: " + level);
        
        // Configure Logback logger programmatically using reflection to avoid compile-time dependencies
        try {
            Class<?> loggerClass = Class.forName("ch.qos.logback.classic.Logger");
            Class<?> levelClass = Class.forName("ch.qos.logback.classic.Level");
            
            Object jdbcLogger = org.slf4j.LoggerFactory.getLogger("com.microsoft.sqlserver.jdbc");
            
            Object logbackLevel;
            switch (level) {
                case "TRACE":
                    logbackLevel = levelClass.getField("TRACE").get(null);
                    break;
                case "DEBUG":
                    logbackLevel = levelClass.getField("DEBUG").get(null);
                    break;
                case "WARN":
                case "WARNING":
                    logbackLevel = levelClass.getField("WARN").get(null);
                    break;
                case "INFO":
                default:
                    logbackLevel = levelClass.getField("INFO").get(null);
                    break;
            }
            
            loggerClass.getMethod("setLevel", levelClass).invoke(jdbcLogger, logbackLevel);
        } catch (Exception e) {
            System.err.println("Could not configure Logback logging: " + e.getMessage());
        }
        
        // Also configure java.util.logging for alternative logging
        try {
            java.util.logging.Level julLevel;
            switch (level) {
                case "TRACE":
                    julLevel = java.util.logging.Level.FINEST;
                    break;
                case "DEBUG":
                    julLevel = java.util.logging.Level.FINE;
                    break;
                case "WARN":
                case "WARNING":
                    julLevel = java.util.logging.Level.WARNING;
                    break;
                case "INFO":
                default:
                    julLevel = java.util.logging.Level.INFO;
                    break;
            }
            java.util.logging.Logger.getLogger("com.microsoft.sqlserver.jdbc").setLevel(julLevel);
        } catch (Exception e) {
            System.err.println("Could not configure java.util.logging: " + e.getMessage());
        }
    }

    private static String getenvOrFail(String key) {
        String v = System.getenv(key);
        if (v == null || v.isEmpty()) {
            System.err.println("Missing required environment variable: " + key);
            System.exit(2);
        }
        return v;
    }
}
