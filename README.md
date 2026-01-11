
# Azure SQL Java sample — Entra Password authentication (Codespaces-ready)

This is a minimal Java 17 + Maven console app that connects to **Azure SQL Database** using **Microsoft Entra (Azure AD) password authentication** via the Microsoft JDBC Driver for SQL Server. It works in **GitHub Codespaces** and on any **Linux VM** with Java and Maven installed.

## What it does
- Builds a JDBC connection using `authentication=ActiveDirectoryPassword`.
- Ensures a table `dbo.Events` exists, inserts one row, and prints the total row count.

## Prerequisites
1. **Azure SQL Database** and **server** (e.g., `yourserver.database.windows.net`).
2. A **Microsoft Entra user** (UPN, e.g., `user@contoso.com`) that:
   - Has a **password** set (not only MFA/SSPR enforced at sign-in).
   - Is **created as a contained database user** and has permissions in your database.
3. Network access to the Azure SQL server on **TCP 1433** from your environment (Codespaces or your Linux VM).

> ⚠️ If Conditional Access **requires MFA**, `ActiveDirectoryPassword` will **fail**. For MFA scenarios, use **access tokens** (MSI/service principal) instead of password auth.

## Set up the database user (once per database)
Connect as a user with permissions (e.g., Azure AD Admin) and run:

```sql
-- In the target database
CREATE USER [your-entra-email@domain.com] FROM EXTERNAL PROVIDER;  -- if not already present
ALTER ROLE db_datareader ADD MEMBER [your-entra-email@domain.com];
ALTER ROLE db_datawriter ADD MEMBER [your-entra-email@domain.com];
-- or: ALTER ROLE db_owner ADD MEMBER [your-entra-email@domain.com];
```

Ensure your Azure SQL server firewall allows your client (or enable **Allow Azure services** if appropriate for testing).

## Running in GitHub Codespaces
1. Create a new repo and push these files, or upload the provided ZIP as the initial commit.
2. In GitHub, click **Code → Codespaces → Create codespace on `main`**.
   - Java 17 and Maven will be automatically installed via the `.devcontainer/devcontainer.json` configuration.
3. Open a terminal and set environment variables (replace values):

   ```bash
   export AZURE_SQL_SERVER="yourserver.database.windows.net"
   export AZURE_SQL_DATABASE="yourdb"
   export AZURE_SQL_USER="your-entra-email@domain.com"
   export AZURE_SQL_PASSWORD="<password>"
   export EVENT_NAME="FromCodespaces"
   ```

   Optional: Enable JDBC driver tracing for debugging connection issues:
   ```bash
   export JDBC_TRACE="true"
   ```

4. Run:
   ```bash
   ./scripts/run.sh
   ```

You should see output like:

```text
Connecting to Azure SQL with Entra Password auth...
Connected.
Inserted rows: 1
Rows in dbo.Events: 1
```

## Running on a Linux VM
1. Install Java 17 and Maven (example for Ubuntu/Debian):
   ```bash
   sudo apt-get update && sudo apt-get install -y openjdk-17-jdk maven
   java -version
   mvn -version
   ```
2. Clone your repo or copy these files to the VM.
3. Set environment variables (as above) and run `./scripts/run.sh`.

## How it connects
The app uses the Microsoft JDBC Driver for SQL Server with the URL:

```text
jdbc:sqlserver://<server>:1433;database=<db>;encrypt=true;hostNameInCertificate=*.database.windows.net;loginTimeout=30;authentication=ActiveDirectoryPassword
```

Credentials are supplied via the standard `user` and `password` JDBC properties (the `user` must be a UPN). TLS is enforced with `encrypt=true`.

## JDBC Driver Tracing

This sample includes support for SQL Server JDBC driver tracing to help debug connection issues and observe driver operations. The driver uses **SLF4J** for logging, with **Logback** as the implementation.

**📖 For detailed tracing configuration and usage, see [TRACING.md](TRACING.md)**

### Enabling tracing

Set the `JDBC_TRACE` environment variable to `true`:

```bash
export JDBC_TRACE="true"
./scripts/run.sh
```

Optionally, set the trace level (defaults to INFO):

```bash
export JDBC_TRACE="true"
export JDBC_TRACE_LEVEL="DEBUG"  # or TRACE, INFO, WARN
./scripts/run.sh
```

### Log levels

You can adjust the logging detail via the `JDBC_TRACE_LEVEL` environment variable or by editing [src/main/resources/logback.xml](src/main/resources/logback.xml):

- **TRACE**: Most detailed - packet-level operations, method entry/exit
- **DEBUG**: SQL statements, connection events, driver operations
- **INFO**: Basic driver information (default)
- **WARN**: Warnings and errors only

Example configuration in logback.xml:

```xml
<!-- Set to TRACE for detailed driver operations -->
<logger name="com.microsoft.sqlserver.jdbc" level="TRACE"/>
```

### Alternative: Java Util Logging

The driver also supports java.util.logging. A configuration file is provided at [src/main/resources/logging.properties](src/main/resources/logging.properties). This is automatically used when `JDBC_TRACE=true` is set.

For more information, see [Microsoft's documentation on tracing driver operations](https://learn.microsoft.com/en-us/sql/connect/jdbc/tracing-driver-operation).

## Troubleshooting
- **MFA required / Conditional Access**: `ActiveDirectoryPassword` does **not** support MFA. Use a non-MFA test user, exclude the user from the MFA policy, or switch to **Managed Identity / Service Principal + access token** authentication.
- **Login failed for user**: Ensure the Entra user exists **inside the database** (contained user) and has permissions.
- **Firewall**: Open outbound **TCP 1433** and allow traffic to `*.database.windows.net`.
- **Certificate/SSL**: Linux distros generally trust the DigiCert root used by Azure SQL. We set `hostNameInCertificate=*.database.windows.net` for stricter hostname validation.
- **Time skew**: Large system time drift can break Entra sign-ins. Sync time via NTP.

## Clean up
Remove the test row(s) or drop `dbo.Events` if no longer needed:

```sql
DROP TABLE IF EXISTS dbo.Events;
```

---
**Note:** For production or when MFA/CA are in play, prefer **token-based auth** with **Managed Identity** (Azure VMs, Azure Functions) or **service principals** using MSAL; then pass the access token to the JDBC driver instead of a password.
