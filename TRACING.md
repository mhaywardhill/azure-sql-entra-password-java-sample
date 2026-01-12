# JDBC Driver Tracing Guide

This document explains how to use the JDBC driver tracing functionality added to this sample.

## Quick Start

Enable tracing by setting the environment variable:

```bash
export JDBC_TRACE="true"
./scripts/run.sh
```

Set a specific trace level:

```bash
export JDBC_TRACE="true"
export JDBC_TRACE_LEVEL="FINE"  # Options: INFO, FINE, FINER, FINEST, WARNING, SEVERE
./scripts/run.sh
```

## What Gets Logged

With tracing enabled, you'll see detailed information about:
- Connection establishment and authentication
- SQL statement preparation and execution
- Result set operations
- Driver internal operations
- Error details and stack traces

## Configuration Methods

You can configure JDBC logging in two ways:

### Method 1: Environment Variables (Recommended)

Set the trace level dynamically without editing configuration files:

```bash
# Enable tracing at FINE level
export JDBC_TRACE="true"
export JDBC_TRACE_LEVEL="FINE"
./scripts/run.sh

# Enable tracing at FINEST level for maximum detail
export JDBC_TRACE="true"
export JDBC_TRACE_LEVEL="FINEST"
./scripts/run.sh
```

**Valid values for JDBC_TRACE_LEVEL:**
- `FINEST` - Most detailed tracing including packet-level details (lowest level)
- `FINER` - Detailed method entry/exit and all public method parameters  
- `FINE` - Basic object creation/destruction, exceptions from public methods
- `INFO` - Informational messages (default)
- `WARNING` - Indicates potential problems
- `SEVERE` - Serious failures, errors and exceptions (highest level)
- `OFF` - Turns off logging
- `ALL` - Enables logging of all messages

## Logging Framework

This sample uses **java.util.logging**, which is the native logging framework supported by the Microsoft SQL Server JDBC driver for tracing operations.

### Configuration Method 1: Environment Variables (Recommended)

The application programmatically configures java.util.logging when `JDBC_TRACE=true` based on the `JDBC_TRACE_LEVEL` environment variable.

### Configuration Method 2: Configuration File

**Configuration file:** `src/main/resources/logging.properties`

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

If the logs are too verbose, adjust the level via environment variable:

```bash
# Less verbose - only important events
export JDBC_TRACE_LEVEL="INFO"

# Quiet - only warnings and errors  
export JDBC_TRACE_LEVEL="WARNING"

# Errors only
export JDBC_TRACE_LEVEL="SEVERE"
```

Or by editing `logging.properties`:

```properties
# Less verbose
com.microsoft.sqlserver.jdbc.level=INFO

# Quiet  
com.microsoft.sqlserver.jdbc.level=WARNING
```

## Common Use Cases

### Debugging Connection Issues

Set to `FINE` or `FINER` to see:
- Connection URL parsing
- Authentication flow
- TLS/SSL handshake details
- Network errors

```bash
export JDBC_TRACE="true"
export JDBC_TRACE_LEVEL="FINE"
./scripts/run.sh
```

### Analyzing SQL Performance

Set to `FINE` to see:
- SQL statement text
- Parameter values
- Execution timing
- Result set sizes

```bash
export JDBC_TRACE="true"
export JDBC_TRACE_LEVEL="FINE"
./scripts/run.sh
```

### Troubleshooting Authentication

For Entra ID (Azure AD) authentication issues, use detailed tracing:

```bash
export JDBC_TRACE="true"
export JDBC_TRACE_LEVEL="FINER"  # or FINEST for maximum detail
./scripts/run.sh
```

**Note:** The JDBC driver logs authentication details through its `com.microsoft.sqlserver.jdbc` logger.

## Log Output Example

With `JDBC_TRACE=true` and `JDBC_TRACE_LEVEL="FINE"`, you'll see output like:

```
JDBC driver tracing enabled at level: FINE
Connecting to Azure SQL with Entra Password auth...
Jan 11, 2026 2:23:45 PM com.microsoft.sqlserver.jdbc.SQLServerConnection <init>
FINE: Connecting with URL: jdbc:sqlserver://...
Jan 11, 2026 2:23:45 PM com.microsoft.sqlserver.jdbc.SQLServerConnection connect
FINE: Using ActiveDirectoryPassword authentication
Jan 11, 2026 2:23:46 PM com.microsoft.sqlserver.jdbc.SQLServerConnection open
FINE: Connection established
Connected.
Inserted rows: 1
Rows in dbo.Events: 1
```

## Performance Impact

⚠️ **Important:** Driver tracing can impact performance:
- `FINEST` level: Significant overhead, use only for debugging
- `FINER` level: Moderate overhead, use only for debugging
- `FINE` level: Some overhead, acceptable for development
- `INFO` level: Minimal overhead, safe for production

**Recommendation:** Only enable tracing in development/debugging scenarios.

## References

- [Microsoft SQL Server JDBC Driver - Tracing Driver Operations](https://learn.microsoft.com/en-us/sql/connect/jdbc/tracing-driver-operation)
- [Java Logging Overview](https://docs.oracle.com/en/java/javase/17/core/java-logging-overview.html)
