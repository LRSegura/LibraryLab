# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build all modules
mvn clean install

# Build specific module
mvn clean install -pl library-domain
mvn clean install -pl library-application
mvn clean install -pl library-infrastructure
mvn clean install -pl library-web

# Build WAR for deployment (produces library-web/target/library.war)
mvn clean package -pl library-web -am

# Skip tests during build
mvn clean install -DskipTests
```

## Technology Stack

- **Java 21** with Jakarta EE 11
- **JSF** (Jakarta Faces) with **PrimeFaces 15** for UI
- **JPA** with EclipseLink (GlassFish default provider)
- **CDI** for dependency injection
- **PostgreSQL** database
- **GlassFish** application server
- **Lombok** for boilerplate reduction
- **Maven** multi-module build

## Architecture

This is a library management system following **Hexagonal Architecture** (Ports & Adapters) organized as a multi-module Maven project with three bounded contexts: **Catalog**, **Lending**, and **Membership**.

### Module Dependency Flow

```
library-domain → library-application → library-infrastructure → library-web
```

### Module Responsibilities

**library-domain** (JAR)
- Domain entities extending `BaseEntity` (auto-managed `id`, `version`, `createdAt`, `updatedAt`)
- Repository port interfaces extending `BaseRepository<T>`
- Business rule validation in entity methods (throw `BusinessRuleException`)
- No framework dependencies except JPA annotations

**library-application** (JAR)
- Use case services extending `BaseService<T>` with `@ApplicationScoped`
- DTOs with `fromEntity()` and `toEntity()`/`updateEntity()` mapping methods
- Services work with DTOs at boundaries, entities internally
- `@Transactional` on mutating methods
- Throws `EntityNotFoundException` for missing entities

**library-infrastructure** (JAR)
- JPA repository implementations extending `BaseRepositoryJpa<T>` with `@ApplicationScoped`
- Implements domain port interfaces

**library-web** (WAR)
- JSF managed beans extending `BasicBean` with `@Named` and `@ViewScoped`
- PrimeFaces XHTML views in `webapp/` organized by bounded context
- Uses `executeOperation(Runnable, String)` for exception handling in beans

### Bounded Context Package Structure

Each bounded context follows this pattern:
```
{context}/
  model/        # Domain entities and enums (library-domain)
  port/         # Repository interfaces (library-domain)
  adapter/      # JPA implementations (library-infrastructure)
  usecase/      # Application services (library-application)
  dto/          # Data transfer objects (library-application)
```

### Key Base Classes

- `BaseEntity`: Provides `id`, `version`, audit timestamps, equals/hashCode by id
- `BaseRepository<T>`: Standard CRUD interface (`findById`, `findAll`, `save`, `delete`, `update`)
- `BaseRepositoryJpa<T>`: Generic JPA implementation with `EntityManager`
- `BaseService<T>`: Provides `validateFieldsConstraint()` using Bean Validation
- `BasicBean`: JSF helper methods (`executeOperation`, `addInfoMessage`, `addErrorMessage`)

### Exception Hierarchy

```
ApplicationException (unchecked)
├── BusinessRuleException  # Domain rule violations (in domain layer)
└── EntityNotFoundException  # Missing entities (in application layer)
```

## Database Configuration

- Persistence unit: `libraryPU`
- JNDI datasource: `jdbc/libraryDS`
- Default database: `librarydb` on localhost:5432
- Credentials defined in `library-web/src/main/webapp/WEB-INF/glassfish-resources.xml`

## Conventions

- Constructor injection for services and beans (with no-arg constructor for CDI proxy)
- Repository methods: `findByX()` returns `Optional<T>` or `List<T>`, `existsByX()` returns `boolean`
- Services return DTOs, never expose domain entities to web layer
- Business rules enforced in domain entities, validation constraints on both entities and DTOs
