# Copilot Instructions for User Auth Service

## Project Overview

This is a Spring Boot microservice that provides authentication and authorization for a payment reconciliation platform. The service implements:

- **JWT-based authentication** with Spring Security
- **Role-Based Access Control (RBAC)** with policies, capabilities, and endpoint bindings
- **Row-Level Security (RLS)** using PostgreSQL Virtual Private Database (VPD) features
- **Multi-tenant data isolation** through tenant ACL tables
- **Comprehensive audit logging** for compliance and debugging

## Technology Stack

- **Java 17** (OpenJDK)
- **Spring Boot 3.2.5** with Spring Security, Spring Data JPA, Spring Web
- **Maven** for build and dependency management
- **PostgreSQL** as the primary database (with RLS policies)
- **JWT tokens** (io.jsonwebtoken:jjwt 0.12.3)
- **Docker** for containerization
- **OpenAPI/Swagger** for API documentation

## Development Environment Setup

### Prerequisites

- Java 17 or later
- Maven 3.8+
- Docker Desktop (for PostgreSQL container)
- PostgreSQL client (psql) for database setup
- IDE with Java support (IntelliJ IDEA, Eclipse, or VS Code with Java extensions)

### Initial Setup

1. **Clone the repository** and create a feature branch
2. **Install dependencies**:
   ```bash
   mvn dependency:go-offline
   ```
3. **Build the project**:
   ```bash
   mvn clean package
   ```
4. **Set up the database** following `docs/guides/local-environment.md`:
   - Run PostgreSQL via Docker or connect to a PostgreSQL instance
   - Execute SQL scripts in `docs/ONBOARDING/setup/` in sequence
   - These create roles, policies, capabilities, endpoints, and seed users

### Environment Configuration

- Configuration files are in `src/main/resources/`
- Use `application-dev.yml` for local development
- Never commit secrets; use environment variables:
  - `APP_JWT_SECRET` - JWT signing secret
  - `INTERNAL_API_KEY` - Internal service authentication key
  - Database credentials via `SPRING_DATASOURCE_*` variables

### Running the Service

```bash
# Run locally with dev profile
mvn spring-boot:run

# Or specify a profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Access health endpoint
curl http://localhost:8080/actuator/health

# Access API documentation
http://localhost:8080/swagger-ui.html
```

### Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn verify

# Run specific test class
mvn test -Dtest=AuthServiceTest
```

## Code Organization

### Package Structure

```
com.example.userauth/
├── config/           # Spring configuration classes (Security, JPA, etc.)
├── controller/       # REST API endpoints
├── dao/              # Data Access Objects for complex queries
├── dto/              # Data Transfer Objects (requests, responses)
├── entity/           # JPA entities (User, Role, Policy, etc.)
├── repository/       # Spring Data JPA repositories
├── security/         # Security filters, JWT utilities, authentication
├── service/          # Business logic layer
└── util/             # Utility classes and helpers
```

### Key Components

- **AuthService** - Handles user registration, login, token generation
- **AuthorizationService** - Resolves user permissions based on roles and policies
- **TokenIntrospectionService** - Validates and introspects JWT tokens for other services
- **RoleService** - Manages role assignments and policy bindings
- **UserDetailsServiceImpl** - Spring Security integration for user authentication

## Coding Standards

### Java Code Style

- **Follow Spring Boot conventions** and existing code patterns in the repository
- Use **constructor injection** for dependencies (prefer over field injection)
- Add **JavaDoc comments** for public APIs and complex business logic
- Use **meaningful variable names** that reflect domain concepts (e.g., `tenantId`, `policyId`)
- Keep methods **focused and small** (single responsibility principle)
- Use **Optional** for potentially null return values
- Handle exceptions appropriately with **custom exception classes**

### REST API Design

- Follow REST principles with proper HTTP methods (GET, POST, PUT, DELETE)
- Use appropriate HTTP status codes (200, 201, 400, 401, 403, 404, 500)
- Return consistent response structures using DTOs
- Document all endpoints with OpenAPI annotations (@Operation, @ApiResponse)
- Version APIs if making breaking changes

### Database Access

- Use **Spring Data JPA repositories** for simple CRUD operations
- Use **DAOs with native queries** for complex queries involving RLS context
- Always set PostgreSQL session context (`app.current_user_id`, `app.current_tenant_id`) before querying business data
- Never bypass RLS policies in application code
- Use **transactions** appropriately (@Transactional)

### Security Guidelines

- **Never log sensitive data** (passwords, tokens, API keys)
- Use **BCrypt** for password hashing (already configured in Spring Security)
- Validate all user input with **Bean Validation** annotations
- Check authorization before accessing resources (use @PreAuthorize or AuthorizationService)
- Implement **CORS** configuration properly for production
- Keep JWT tokens **short-lived** and implement refresh token mechanism if needed

## Building and Testing

### Build Commands

```bash
# Clean build
mvn clean install

# Build without tests (use sparingly)
mvn clean install -DskipTests

# Build Docker image
docker build -t user-auth-service:latest .

# Package for deployment
mvn clean package spring-boot:repackage
```

### Running Tests

- All tests use **H2 in-memory database** (configured in `application-test.yml`)
- Mock external dependencies using **Mockito**
- Write tests for:
  - Service layer business logic
  - Authorization rules and policy evaluation
  - JWT token validation
  - API endpoints (use MockMvc)

## Documentation

### Internal Documentation

The `docs/` directory contains comprehensive documentation:

- **Start here**: `docs/README.md` - Guided documentation path
- **Architecture**: `docs/architecture/overview.md` - System design and components
- **Setup guides**: `docs/guides/local-environment.md` - Environment setup
- **Reference**: `docs/reference/` - Role catalogs, policy matrices, VPD checklists

### When Making Changes

- Update relevant documentation in `docs/` when adding features
- Keep API documentation in sync with code (OpenAPI annotations)
- Update README or guides if changing setup procedures
- Document new environment variables in configuration files

## Common Tasks

### Adding a New API Endpoint

1. Create DTO classes in `dto/` package
2. Add controller method with OpenAPI annotations
3. Implement business logic in service layer
4. Add authorization checks (role/policy requirements)
5. Write unit and integration tests
6. Update API documentation

### Adding a New Role or Policy

1. Create SQL migration script with role/policy/capability definitions
2. Update `docs/reference/role-catalog.md` with the new role
3. Update `docs/reference/policy-matrix.md` if needed
4. Test with different user personas
5. Document in relevant guides

### Debugging Authorization Issues

- Check JWT token contents and claims
- Verify role assignments in `user_roles` table
- Review policy bindings in `role_policies` and `policy_capabilities`
- Check RLS context variables (`SELECT current_setting('app.current_user_id')`)
- Consult `docs/playbooks/troubleshoot-auth.md` for common issues

## Important Considerations

### PostgreSQL Row-Level Security (RLS)

- This service relies heavily on **PostgreSQL RLS policies** for data isolation
- Always use `RLSContext` or similar mechanism to set session variables before queries
- RLS policies are defined in database setup scripts
- Test multi-tenancy isolation thoroughly when making changes

### Multi-tenancy

- Tenant isolation is enforced at the database level via RLS
- Tenant ACL tables define which users can access which tenants
- Never bypass tenant checks in application code
- Always include `tenantId` in audit logs

### Performance

- Use pagination for list endpoints
- Consider caching for frequently accessed authorization data
- Monitor database connection pool usage
- Use database indexes appropriately

### Migrations and Schema Changes

- PostgreSQL is the primary database (MySQL support is legacy/commented)
- Schema changes should be done via SQL migration scripts
- Test migrations on a copy of production data
- Keep `ddl-auto: update` for development, but use explicit migrations for production

## Additional Resources

- Spring Boot Documentation: https://docs.spring.io/spring-boot/docs/3.2.5/reference/html/
- Spring Security: https://docs.spring.io/spring-security/reference/
- JWT Best Practices: https://tools.ietf.org/html/rfc8725
- PostgreSQL RLS: https://www.postgresql.org/docs/current/ddl-rowsecurity.html

## Getting Help

- Review existing code patterns in the repository
- Check `docs/` directory for architecture and design decisions
- Consult `docs/playbooks/troubleshoot-auth.md` for debugging guidance
- Review commit history for context on recent changes
