
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

        String url = String.format(
            "jdbc:sqlserver://%s:1433;database=%s;encrypt=true;hostNameInCertificate=*.database.windows.net;loginTimeout=30;authentication=ActiveDirectoryPassword",
            server, database
        );

        Properties props = new Properties();
        props.setProperty("user", username);
        props.setProperty("password", password);
        
        // Configure driver logging level programmatically
        if ("true".equalsIgnoreCase(enableTrace)) {
            System.out.println("JDBC driver tracing enabled");
            // The driver uses SLF4J, so logging is controlled via logback.xml
            // You can also use the java.util.logging properties for alternative configuration
            try {
                java.io.InputStream logConfig = EntraPasswordSqlSample.class
                    .getClassLoader()
                    .getResourceAsStream("logging.properties");
                if (logConfig != null) {
                    java.util.logging.LogManager.getLogManager().readConfiguration(logConfig);
                }
            } catch (Exception e) {
                System.err.println("Could not configure java.util.logging: " + e.getMessage());
            }
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

    private static String getenvOrFail(String key) {
        String v = System.getenv(key);
        if (v == null || v.isEmpty()) {
            System.err.println("Missing required environment variable: " + key);
            System.exit(2);
        }
        return v;
    }
}
