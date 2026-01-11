
#!/usr/bin/env bash
set -euo pipefail

# Ensure required env vars are present
: "${AZURE_SQL_SERVER:?set AZURE_SQL_SERVER}"
: "${AZURE_SQL_DATABASE:?set AZURE_SQL_DATABASE}"
: "${AZURE_SQL_USER:?set AZURE_SQL_USER"
: "${AZURE_SQL_PASSWORD:?set AZURE_SQL_PASSWORD}"

mvn -q -DskipTests package
mvn -q exec:java
