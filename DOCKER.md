# Docker Setup — LibraryLab Monolith

## File Placement

```
library-monolith/
├── Dockerfile
├── .dockerignore
├── docker-compose.yml
├── docker/
│   ├── glassfish-setup.sh
│   └── init-db.sql
├── pom.xml
├── library-domain/
├── library-application/
├── library-infrastructure/
└── library-web/
```

## How It Works

The Docker setup consists of four key files that work together to build, configure, and run the application in containers.

### Dockerfile (Multi-Stage Build)

The Dockerfile uses a **multi-stage build** to keep the final image lean and secure.

**Stage 1 — Builder** (`maven:3.9.9-eclipse-temurin-21`): copies POM files first to leverage Docker's layer caching for dependency downloads, then copies source code and runs `mvn package` to produce `library.war`. This ordering means that code-only changes skip the dependency download step entirely during rebuilds.

**Stage 2 — Runtime** (`ghcr.io/eclipse-ee4j/glassfish:8.0.0`): downloads the PostgreSQL JDBC driver into the GlassFish domain `lib/` directory, runs `asadmin` commands via `glassfish-setup.sh` to create the JDBC connection pool (`LibraryPool`) and resource (`jdbc/libraryDS`), copies the WAR into `autodeploy/`, and removes `glassfish-resources.xml` from the WAR to prevent datasource conflicts. The final image contains only GlassFish + the JDBC driver + the WAR — no Maven, no source code, no build artifacts.

### docker/glassfish-setup.sh (DataSource Configuration)

This script uses GlassFish's `asadmin` CLI to create the JDBC connection pool and resource at the **domain level**, which is the equivalent of configuring the datasource through the GlassFish Admin Console (port 4848). The pool points to the `postgres` hostname (the Docker Compose service name), with configurable defaults for host, port, database name, and credentials.

### docker/init-db.sql (Schema Initialization)

PostgreSQL automatically executes any `.sql` files found in `/docker-entrypoint-initdb.d/` during its first startup (when the data volume is empty). This script creates all four tables (`categories`, `books`, `members`, `loans`) with proper column types, constraints, foreign keys, and indexes. This approach is more reliable than relying on EclipseLink's auto-generated DDL in a Docker environment (see Lessons Learned below).

### docker-compose.yml (Service Orchestration)

Defines two services: `postgres` (PostgreSQL 17 Alpine) and `librarylab` (the GlassFish image built from the Dockerfile). PostgreSQL includes a healthcheck (`pg_isready`), and GlassFish uses `depends_on` with `condition: service_healthy` to ensure the database is ready before the application server starts. Both services share a bridge network (`librarylab-net`), allowing them to communicate using service names as hostnames.

## Quick Start

```bash
# Build and start everything
docker compose up -d --build

# Watch the GlassFish logs (wait for "Successfully autodeployed")
docker compose logs -f librarylab

# Once deployed, access the application:
#   JSF UI:       http://localhost:8080/library
#   REST API:     http://localhost:8080/library/api/v1/books
#   GlassFish UI: http://localhost:4848
```

## Common Operations

```bash
# Rebuild after code changes
docker compose up -d --build

# Stop everything (keep database data)
docker compose down

# Stop and wipe the database volume (forces schema re-creation on next start)
docker compose down -v

# Shell into the GlassFish container
docker exec -it librarylab-app bash

# Run asadmin commands inside the container
docker exec -it librarylab-app asadmin list-jdbc-connection-pools
docker exec -it librarylab-app asadmin list-jdbc-resources
docker exec -it librarylab-app asadmin ping-connection-pool LibraryPool

# Check database from host (requires psql installed locally)
psql -h localhost -U library_user -d librarydb
# Password: library_password

# View container resource usage
docker stats
```

## Environment Variables

The JDBC pool is configured during image build with these defaults. Override them in `docker-compose.yml` if needed:

| Variable      | Default           | Description               |
|---------------|-------------------|---------------------------|
| `DB_HOST`     | `postgres`        | Database hostname         |
| `DB_PORT`     | `5432`            | Database port             |
| `DB_NAME`     | `librarydb`       | Database name             |
| `DB_USER`     | `library_user`    | Database user             |
| `DB_PASSWORD` | `library_password`| Database password         |

## Lessons Learned

These are the issues encountered during dockerization and how they were resolved. Each one represents a common pattern when containerizing Jakarta EE applications.

### 1. GlassFish 8 Runs as Non-Root User

**Symptom:** `chmod: changing permissions of '/tmp/glassfish-setup.sh': Operation not permitted` during image build. Similarly, `Failed to open jar file: postgresql-42.7.8.jar (Permission denied)` at runtime.

**Root Cause:** The official GlassFish 8 Docker image runs as a non-root user for security. Standard `RUN chmod` and `ADD` instructions create files owned by root, which the GlassFish process cannot read or execute.

**Solution:** Use the `--chmod` flag directly on `COPY` and `ADD` instructions. `COPY --chmod=755` for executable scripts and `ADD --chmod=644` for JAR files. This sets permissions at copy time without requiring a separate `RUN chmod` step. An alternative for scripts is to invoke them via `bash /path/to/script.sh` instead of making them executable, since bash reads the file directly without checking the execute bit.

### 2. Conflicting DataSource Definitions (glassfish-resources.xml vs asadmin)

**Symptom:** EclipseLink connects to PostgreSQL but tables do not exist, even though `persistence.xml` has `schema-generation.database.action=create`.

**Root Cause:** The WAR includes `glassfish-resources.xml` which defines `jdbc/libraryDS` pointing to `localhost:5432`. Inside Docker, PostgreSQL runs in a separate container reachable at hostname `postgres`, not `localhost`. When GlassFish deploys the WAR, the application-level resource from `glassfish-resources.xml` creates a second pool (`java:app/LibraryPool`) alongside the domain-level pool configured via `asadmin`. EclipseLink picks up the application-level one (which points to `localhost` where no database exists), causing schema generation to fail silently.

**Solution:** Remove `glassfish-resources.xml` from the WAR during the Docker build using `RUN zip -d ${DEPLOY_DIR}/library.war WEB-INF/glassfish-resources.xml || true`. This leaves the domain-level datasource (configured by `asadmin` pointing to `postgres:5432`) as the sole provider. The `glassfish-resources.xml` file remains in the source code for local development without Docker.

### 3. EclipseLink DDL Generation Bug with IDENTITY Strategy on PostgreSQL

**Symptom:** `PSQLException: ERROR: syntax error at or near "SERIAL"` during table creation.

**Root Cause:** EclipseLink 5.0.0-B13 (the version bundled with GlassFish 8.0.0) generates invalid DDL for `GenerationType.IDENTITY` with `Long` primary keys on PostgreSQL. It produces `BIGINT SERIAL` instead of `BIGSERIAL` or `BIGINT GENERATED BY DEFAULT AS IDENTITY`. The `SERIAL` pseudo-type in PostgreSQL is a shorthand that implies its own type, so combining it with an explicit `BIGINT` type results in a syntax error.

**Solution:** Manage the database schema explicitly with an init script (`docker/init-db.sql`) mounted into PostgreSQL's `/docker-entrypoint-initdb.d/` directory. This script uses `BIGINT GENERATED BY DEFAULT AS IDENTITY` for all primary keys, which is the standard SQL:2003 syntax and the correct equivalent for what EclipseLink intended. PostgreSQL only executes init scripts on first startup (when the data volume is empty), so running `docker compose down -v` is necessary to trigger re-initialization.

The `persistence.xml` setting `schema-generation.database.action=create` can remain in place. EclipseLink will attempt its DDL, PostgreSQL will reject the invalid statements, but since the tables already exist from the init script, EclipseLink logs warnings and continues normally. This is a non-fatal path by design.

### 4. Hostname Resolution Warning (Cosmetic)

**Symptom:** `Bad OS network configuration. DNS can not resolve the hostname: java.net.UnknownHostException: <container_id>` appears in GlassFish startup logs.

**Root Cause:** Docker assigns the container ID as the hostname by default. GlassFish tries to resolve this hostname via DNS during startup, which fails because container IDs are not DNS-resolvable names.

**Solution:** This is a cosmetic warning and does not affect functionality. To suppress it, add `hostname: librarylab` to the `librarylab` service in `docker-compose.yml`. GlassFish will then be able to resolve its own hostname and the warning disappears.

### 5. Port Conflicts with Local Services

**Symptom:** Container starts but the `PORTS` column in `docker ps` is empty, or Docker reports port binding errors.

**Root Cause:** A local PostgreSQL instance (or another Docker container) is already using port 5432 or 8080 on the host machine.

**Solution:** Either stop the conflicting service before running `docker compose up`, or remap ports in `docker-compose.yml`. For example, change `"5432:5432"` to `"5433:5432"` to expose PostgreSQL on port 5433 instead. The left side is the host port (what you connect to from your machine), the right side is the container port (unchanged). Note that remapping the PostgreSQL host port does not affect the connection between GlassFish and PostgreSQL inside Docker — they communicate over the internal bridge network using port 5432 regardless of host port mapping.

## Next Steps

Once the monolith is running in Docker, the natural progression on the roadmap is:

1. Add containers for the **microservices** (Payara Micro + Redis, Open Liberty + MongoDB, WildFly + PostgreSQL) to `docker-compose.yml`
2. Create a **Kubernetes** setup with Helm charts for orchestration beyond local development
3. Wire up **Jenkins** CI/CD pipelines to build and push images automatically
