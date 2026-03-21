# =============================================================================
# LibraryLab - Multi-stage Dockerfile
# Stage 1: Maven build
# Stage 2: GlassFish 8 runtime with PostgreSQL JDBC driver
# =============================================================================

# ---------------------------------------------------------------------------
# Stage 1 — Build the WAR with Maven
# ---------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy POM files first to leverage Docker layer caching for dependencies
COPY pom.xml .
COPY library-domain/pom.xml library-domain/
COPY library-application/pom.xml library-application/
COPY library-infrastructure/pom.xml library-infrastructure/
COPY library-web/pom.xml library-web/

# Download dependencies (cached unless POMs change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY library-domain/src library-domain/src
COPY library-application/src library-application/src
COPY library-infrastructure/src library-infrastructure/src
COPY library-web/src library-web/src

# Build the WAR (skip tests — they run in CI, not in image build)
RUN mvn clean package -pl library-web -am -DskipTests -B

# ---------------------------------------------------------------------------
# Stage 2 — GlassFish 8 runtime
# ---------------------------------------------------------------------------
FROM ghcr.io/eclipse-ee4j/glassfish:8.0.0

# GlassFish paths
ENV GLASSFISH_HOME=/opt/gfinstall
ENV DOMAIN_DIR=${GLASSFISH_HOME}/glassfish/domains/domain1
ENV DEPLOY_DIR=${DOMAIN_DIR}/autodeploy
ENV DOMAIN_LIB=${DOMAIN_DIR}/lib

# PostgreSQL JDBC driver version
ENV PG_JDBC_VERSION=42.7.8

# Download PostgreSQL JDBC driver into domain lib
ADD --chmod=644 https://repo1.maven.org/maven2/org/postgresql/postgresql/${PG_JDBC_VERSION}/postgresql-${PG_JDBC_VERSION}.jar \
    ${DOMAIN_LIB}/postgresql-${PG_JDBC_VERSION}.jar

# Copy the setup script with execute permissions
COPY --chmod=755 docker/glassfish-setup.sh /tmp/glassfish-setup.sh

# Run asadmin commands to create JDBC pool and resource
RUN ${GLASSFISH_HOME}/bin/asadmin start-domain domain1 && \
    /tmp/glassfish-setup.sh && \
    ${GLASSFISH_HOME}/bin/asadmin stop-domain domain1

# Copy the WAR from the builder stage
COPY --from=builder /build/library-web/target/library.war ${DEPLOY_DIR}/library.war

# Remove glassfish-resources.xml from the WAR to avoid
# overriding the domain-level datasource configured via asadmin
RUN zip -d ${DEPLOY_DIR}/library.war WEB-INF/glassfish-resources.xml || true

# Expose HTTP and admin ports
EXPOSE 8080 4848

# Start GlassFish in foreground (verbose mode keeps the container alive)
CMD ["asadmin", "start-domain", "--verbose", "domain1"]
