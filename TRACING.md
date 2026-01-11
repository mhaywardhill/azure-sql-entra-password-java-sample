# JDBC Driver Tracing Guide

This document explains how to use the JDBC driver tracing functionality added to this sample.

## Quick Start

Enable tracing by setting the environment variable:

```bash
export JDBC_TRACE="true"
./scripts/run.sh
```

## What Gets Logged

With tracing enabled, you'll see detailed information about:
- Connection establishment and authentication
- SQL statement preparation and execution
- Result set operations
- Driver internal operations
- Error details and stack traces

## Logging Frameworks

This sample supports two logging frameworks:

### 1. SLF4J with Logback (Primary)

The Microsoft SQL Server JDBC driver uses SLF4J, and this project uses Logback as the implementation.

**Configuration file:** `src/main/resources/logback.xml`

**Adjust log level:**
```xml
<!-- Current configuration -->
<logger name="com.microsoft.sqlserver.jdbc" level="INFO"/>

<!-- Change to one of these for more detail: -->
<logger name="com.microsoft.sqlserver.jdbc" level="DEBUG"/>
<logger name="com.microsoft.sqlserver.jdbc" level="TRACE"/>
```

**Log Levels:**
- `TRACE` - Most detailed: packet-level operations, method entry/exit, internal state
- `DEBUG` - SQL statements, connection events, driver operations  
- `INFO` - Basic driver information (default)
- `WARN` - Warnings and errors only

### 2. Java Util Logging (Alternative)

The driver also supports standard java.util.logging.

**Configuration file:** `src/main/resources/logging.properties`

This is automatically loaded when `JDBC_TRACE=true` is set.

**Adjust log level in logging.properties:**
```properties
# Change FINE to FINER or FINEST for more detail
com.microsoft.sqlserver.jdbc.level=FINE
```

**Log Levels:**
- `FINEST` - Most detailed tracing including packet-level details
- `FINER` - Detailed method entry/exit and SQL statements
- `FINE` - Connection events, statement execution
- `INFO` - Basic driver information
- `WARNING` - Warnings only

## Controlling Output

### Reducing Verbosity

If the logs are too verbose, adjust the level in `logback.xml`:

```xml
<!-- Less verbose - only important events -->
<logger name="com.microsoft.sqlserver.jdbc" level="INFO"/>

<!-- Quiet - only warnings and errors -->
<logger name="com.microsoft.sqlserver.jdbc" level="WARN"/>
```

### Filtering Specific Components

You can control logging for specific driver components:

```xml
<!-- Log only connection-related operations -->
<logger name="com.microsoft.sqlserver.jdbc.SQLServerConnection" level="DEBUG"/>

<!-- Log only statement execution -->
<logger name="com.microsoft.sqlserver.jdbc.SQLServerStatement" level="DEBUG"/>

<!-- Log authentication details -->
<logger name="com.microsoft.sqlserver.jdbc.Authentication" level="TRACE"/>
```

## Common Use Cases

### Debugging Connection Issues

Set to `DEBUG` or `TRACE` to see:
- Connection URL parsing
- Authentication flow
- TLS/SSL handshake details
- Network errors

```xml
<logger name="com.microsoft.sqlserver.jdbc" level="DEBUG"/>
```

### Analyzing SQL Performance

Set to `DEBUG` to see:
- SQL statement text
- Parameter values
- Execution timing
- Result set sizes

```xml
<logger name="com.microsoft.sqlserver.jdbc.SQLServerStatement" level="DEBUG"/>
```

### Troubleshooting Authentication

For Entra ID (Azure AD) authentication issues, also enable Azure/MSAL logging:

```xml
<!-- Enable Azure SDK logging -->
<logger name="com.microsoft" level="DEBUG"/>
<logger name="com.azure" level="DEBUG"/>

<!-- And JDBC driver authentication -->
<logger name="com.microsoft.sqlserver.jdbc" level="DEBUG"/>
```

## Log Output Example

With `JDBC_TRACE=true` and `level="DEBUG"`, you'll see output like:

```
JDBC driver tracing enabled
Connecting to Azure SQL with Entra Password auth...
14:23:45.123 [main] DEBUG com.microsoft.sqlserver.jdbc.SQLServerConnection - Connecting with URL: jdbc:sqlserver://...
14:23:45.234 [main] DEBUG com.microsoft.sqlserver.jdbc.Authentication - Using ActiveDirectoryPassword authentication
14:23:46.456 [main] DEBUG com.microsoft.sqlserver.jdbc.SQLServerConnection - Connection established
Connected.
14:23:46.567 [main] DEBUG com.microsoft.sqlserver.jdbc.SQLServerStatement - Executing SQL: IF OBJECT_ID...
Inserted rows: 1
Rows in dbo.Events: 1
```

## Performance Impact

⚠️ **Important:** Driver tracing can impact performance:
- `TRACE` level: Significant overhead, use only for debugging
- `DEBUG` level: Moderate overhead, acceptable for development
- `INFO` level: Minimal overhead, safe for production

**Recommendation:** Only enable tracing in development/debugging scenarios.

## References

- [Microsoft SQL Server JDBC Driver - Tracing Driver Operations](https://learn.microsoft.com/en-us/sql/connect/jdbc/tracing-driver-operation)
- [Logback Documentation](https://logback.qos.ch/manual/)
- [SLF4J Documentation](https://www.slf4j.org/manual.html)
