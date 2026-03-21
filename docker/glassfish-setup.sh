#!/bin/bash
# =============================================================================
# GlassFish DataSource Configuration via asadmin
# Creates the JDBC connection pool and resource for LibraryLab
#
# Environment variables (with defaults for docker-compose):
#   DB_HOST      - PostgreSQL hostname (default: postgres)
#   DB_PORT      - PostgreSQL port (default: 5432)
#   DB_NAME      - Database name (default: librarydb)
#   DB_USER      - Database user (default: library_user)
#   DB_PASSWORD  - Database password (default: library_password)
# =============================================================================

ASADMIN="${GLASSFISH_HOME}/bin/asadmin"

DB_HOST="${DB_HOST:-postgres}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-librarydb}"
DB_USER="${DB_USER:-library_user}"
DB_PASSWORD="${DB_PASSWORD:-library_password}"

echo ">>> Creating JDBC Connection Pool: LibraryPool"
${ASADMIN} create-jdbc-connection-pool \
    --datasourceclassname org.postgresql.ds.PGSimpleDataSource \
    --restype javax.sql.DataSource \
    --property "serverName=${DB_HOST}:portNumber=${DB_PORT}:databaseName=${DB_NAME}:user=${DB_USER}:password=${DB_PASSWORD}" \
    --steadypoolsize 8 \
    --maxpoolsize 32 \
    --poolresize 2 \
    --statementtimeout 30 \
    LibraryPool

echo ">>> Creating JDBC Resource: jdbc/libraryDS"
${ASADMIN} create-jdbc-resource \
    --connectionpoolid LibraryPool \
    jdbc/libraryDS

echo ">>> Pinging connection pool..."
${ASADMIN} ping-connection-pool LibraryPool || echo "WARNING: Pool ping failed (DB may not be running yet — this is expected during image build)"

echo ">>> DataSource configuration complete"
