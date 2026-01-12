
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
        
        // The Microsoft JDBC Driver uses java.util.logging
        // Configure java.util.logging with console handler
        try {
            java.util.logging.Level julLevel;
            switch (level) {
                case "FINEST":
                    julLevel = java.util.logging.Level.FINEST;
                    break;
                case "FINER":
                    julLevel = java.util.logging.Level.FINER;
                    break;
                case "FINE":
                    julLevel = java.util.logging.Level.FINE;
                    break;
                case "WARNING":
                    julLevel = java.util.logging.Level.WARNING;
                    break;
                case "SEVERE":
                    julLevel = java.util.logging.Level.SEVERE;
                    break;
                case "OFF":
                    julLevel = java.util.logging.Level.OFF;
                    break;
                case "ALL":
                    julLevel = java.util.logging.Level.ALL;
                    break;
                case "INFO":
                default:
                    julLevel = java.util.logging.Level.INFO;
                    break;
            }
            
            // Configure the JDBC driver logger
            java.util.logging.Logger jdbcLogger = java.util.logging.Logger.getLogger("com.microsoft.sqlserver.jdbc");
            jdbcLogger.setLevel(julLevel);
            
            // Configure console handler to show the output
            java.util.logging.ConsoleHandler consoleHandler = new java.util.logging.ConsoleHandler();
            consoleHandler.setLevel(julLevel);
            consoleHandler.setFormatter(new java.util.logging.SimpleFormatter());
            
            // Remove existing handlers and add our console handler
            jdbcLogger.setUseParentHandlers(false);
            for (java.util.logging.Handler handler : jdbcLogger.getHandlers()) {
                jdbcLogger.removeHandler(handler);
            }
            jdbcLogger.addHandler(consoleHandler);
            
            System.out.println("Java util logging configured for JDBC driver");
        } catch (Exception e) {
            System.err.println("Could not configure java.util.logging: " + e.getMessage());
            e.printStackTrace();
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
