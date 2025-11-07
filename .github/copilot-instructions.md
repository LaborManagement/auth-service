# Copilot Instructions for Auth Service

## Project Overview

This is a Spring Boot microservice that provides authentication and authorization for a payment reconciliation platform. The service implements:

- **JWT-based authentication** with Spring Security
- **Role-Based Access Control (RBAC)** with policies, capabilities, and endpoint bindings
- **Row-Level Security (RLS)** using PostgreSQL Virtual Private Database (VPD) features
- **Multi-tenant data isolation** through tenant ACL tables
- **Comprehensive audit logging** for compliance and debugging

## Technology Stack

- **Java 17** (OpenJDK)
- **Spring Boot 3.2.5** with Spring Security, Spring Data JPA, Spring Web, jOOQ
- **Maven** for build and dependency management
- **PostgreSQL** as the primary database (with RLS policies)
- **JWT tokens** (io.jsonwebtoken:jjwt 0.12.3)
- **jOOQ** for type-safe SQL queries
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
4. **Set up the database** following `documentation/LBE/guides/local-environment.md`:
   - Run PostgreSQL via Docker or connect to a PostgreSQL instance
   - Execute SQL scripts in `documentation/LBE/onboarding/setup/` in sequence
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
├── config/           # Spring configuration classes (Security, JPA, jOOQ, etc.)
├── controller/       # REST API endpoints
├── dao/              # Data Access Objects for complex queries (jOOQ-based)
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
- **UserQueryDao** / **RoleQueryDao** - jOOQ-based DAOs for complex authorization lookups

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

## Database Access Patterns ⭐ CRITICAL

**ALWAYS consult `documentation/LBE/guides/data-access-patterns.md` before writing any database code.**

### Pattern Selection Decision Tree

```
┌─────────────────────────────────────────┐
│ What type of operation are you doing?  │
└─────────────┬───────────────────────────┘
              │
    ┌─────────┴─────────┐
    │                   │
    ▼                   ▼
┌───────┐         ┌──────────┐
│ WRITE │         │   READ   │
└───┬───┘         └────┬─────┘
    │                  │
    │            ┌─────┴─────────────────────┐
    │            │                           │
    │            ▼                           ▼
    │    ┌──────────────┐          ┌────────────────┐
    │    │ Simple       │          │ Complex        │
    │    │ Lookup       │          │ Multi-join     │
    │    └──────┬───────┘          └────────┬───────┘
    │           │                           │
    │           │                    ┌──────┴──────┐
    │           │                    │             │
    │           │                    ▼             ▼
    │           │          ┌────────────┐  ┌──────────────┐
    │           │          │ Maintained │  │  Developer   │
    │           │          │ by Analyst │  │  Maintained  │
    │           │          └──────┬─────┘  └──────┬───────┘
    │           │                 │                │
    ▼           ▼                 ▼                ▼
┌────────┐ ┌────────┐   ┌────────────┐   ┌────────────┐
│  JPA   │ │  JPA   │   │    jOOQ    │   │    jOOQ    │
│Repository││Repository│ │ +SQL File  │   │    DSL     │
└────────┘ └────────┘   └────────────┘   └────────────┘
```

### 1. Spring Data JPA - Use for:

✅ **When to use:**

- Simple CRUD operations on entities (`findByUsername`, `save`, `delete`, etc.)
- Write-heavy flows requiring entity lifecycle events (auditing, cascade rules)
- Operations needing entity-level auditing and changeset tracking
- Mutations on JPA entities or areas relying on entity lifecycle callbacks

📁 **Examples in this service:**

- `UserRepository` - Basic user persistence
- `RoleRepository` - Role CRUD operations
- `PolicyRepository` - Policy management

💡 **Rules:**

- Keep repository interfaces focused on persistence concerns
- Push business logic up into services
- Use method-name queries or small JPQL snippets
- Avoid returning entities from external APIs; map to DTOs
- Add `@DataJpaTest` integration tests for new JPQL queries

### 2. jOOQ DSL - Use for:

✅ **When to use:**

- Read-heavy queries with multiple joins, filters, or window functions
- Complex queries where schema drift must fail at build time
- Analytical queries requiring advanced SQL features (CTEs, window functions)
- Replacing custom `JdbcTemplate` code while keeping control over SQL shape

📁 **Examples in this service:**

- `UserQueryDao` - Complex user authorization lookups
- `RoleQueryDao` - Role resolution with policy checking
- Multi-table joins for authorization matrix generation

💡 **Rules:**

- Inject `DSLContext` (auto-configured) into DAO classes
- Prefer generated table/field classes (`Tables.USERS`) once codegen is enabled
- Otherwise use `DSL.table(DSL.name("users"))` temporarily
- Keep result mapping close to the query
- Use `fetchInto()` or small record/DTO mappers
- Add integration tests hitting real database (H2/Postgres container)
- Group nested structures: fetch flat → group in Java (`Collectors.groupingBy`) → map to DTOs

### 3. jOOQ + SQL Templates - Use for:

✅ **When to use:**

- Reporting queries curated by analysts or BI teams
- Queries that change frequently or are easier to maintain as text
- Long CTEs, custom sort logic, or documentation-shared SQL
- Contexts where SQL needs to be reviewed/modified outside the application

📁 **File location:**

- Store templates in `src/main/resources/sql/<domain>/` (e.g., `sql/reports/user_activity_summary.sql`)
- Use `.sql` extensions

💡 **Rules:**

- Load templates with `SqlTemplateLoader` (or create similar utility)
- Use positional (`?`) or named parameters supported by jOOQ parser
- Keep column aliases stable—service code maps results by alias (`status`, `count`, etc.)
- Document templates in README so analysts know what they can safely change
- Test by loading template and executing against in-memory database

### Database Access Rules (ALL PATTERNS)

🔒 **Security & RLS:**

- **ALWAYS** set PostgreSQL session context before queries: `SELECT auth.set_user_context(:userId)`
- **NEVER** bypass RLS policies in application code
- Use `RLSContext` or `RLSContextFilter` to manage session variables
- Set both `app.current_user_id` and `app.current_tenant_id`

🔄 **Transactions:**

- Use `@Transactional` appropriately for all write operations
- For reads, consider read-only transactions: `@Transactional(readOnly = true)`

✅ **Testing:**

- Add integration tests for all new queries
- Test with multiple user personas and tenant contexts
- Verify RLS isolation in tests
- Test jOOQ queries against real database (use Testcontainers)

### Migration Between Patterns

**JPA → jOOQ DSL:**

1. Identify the read-heavy repository method
2. Create DAO using `DSLContext`
3. Implement the query
4. Update service to call DAO
5. Keep JPA repository for writes if needed
6. Add tests to verify behavior matches

**jOOQ DSL → jOOQ + SQL Template:**

1. Extract SQL string into `.sql` file
2. Load via `SqlTemplateLoader`
3. Execute with `dsl.resultQuery`
4. Keep method signature the same
5. Update documentation

## Security Guidelines

### Authentication & Authorization

- **Never log sensitive data** (passwords, tokens, API keys)
- Use **BCrypt** for password hashing (already configured)
- Validate all user input with **Bean Validation** annotations
- Check authorization before accessing resources:
  - Use `@PreAuthorize("hasAnyAuthority('POLICY_NAME')")` for endpoint security
  - Use `AuthorizationService` for programmatic checks
- Implement **CORS** configuration properly for production
- Keep JWT tokens **short-lived**

### RLS & Multi-Tenancy

- **Always** use `RLSContext` before querying business data
- **Never** construct SQL with user input directly
- Test multi-tenancy isolation thoroughly
- Always include `tenantId` in audit logs
- Consult `documentation/LBE/foundations/data-guardrails-101.md` for RLS patterns

## Audit Logging Guidelines ⭐ CRITICAL

**ALWAYS consult `documentation/LBE/architecture/audit-design.md` for complete audit system documentation.**

### Centralized Audit Schema

This service writes to a **centralized audit schema** shared across all services:

- **audit.audit_event** - General action logging (API calls, user actions, system events)
- **audit.entity_audit_event** - Entity-level change tracking with hash chains

### Configuration

Audit configuration is in `application.yml`:

```yaml
shared-lib:
  audit:
    enabled: true
    table-name: audit.audit_event
    service-name: auth-service # DO NOT CHANGE
    source-schema: auth # DO NOT CHANGE
  entity-audit:
    enabled: true
    table-name: audit.entity_audit_event
    service-name: auth-service # DO NOT CHANGE
    source-schema: auth # DO NOT CHANGE
    source-table: users # Primary table for this service
```

**CRITICAL:** Never modify `service-name` or `source-schema` values—these enable cross-service audit queries.

### When to Use Each Audit Table

#### Use audit.audit_event for:

- ✅ API endpoint calls (login, logout, token refresh)
- ✅ User actions (registration, password change, role assignment)
- ✅ Authorization decisions (403/401 responses)
- ✅ Security events (failed login attempts, suspicious activity)
- ✅ Policy/role/capability management actions
- ✅ System events (cache invalidation, batch operations)

#### Use audit.entity_audit_event for:

- ✅ User entity changes (create, update, delete)
- ✅ Role assignment changes
- ✅ Policy binding changes
- ✅ Tenant ACL modifications
- ✅ Any sensitive data modification requiring tamper detection

### Manual Audit Logging

```java
@Autowired
private AuditTrailService auditTrailService;

public void updateUserRole(Long userId, String newRole) {
    // Business logic
    User user = userRepository.findById(userId).orElseThrow();
    String oldRole = user.getRole();
    user.setRole(newRole);
    userRepository.save(user);

    // Log audit event
    auditTrailService.logAction(
        userId,                                    // user_id
        "USER_ROLE_UPDATE",                        // action
        "USER",                                    // entity_type
        String.valueOf(userId),                    // entity_id
        user.getUsername(),                        // entity_name
        String.format("Changed role from %s to %s", oldRole, newRole), // description
        Map.of(                                    // metadata
            "old_role", oldRole,
            "new_role", newRole,
            "ip_address", requestMetadata.getIp()
        )
    );
}
```

### Automatic Entity Audit

Enable automatic audit for JPA entities:

```java
@Entity
@Table(name = "users", schema = "auth")
@EntityListeners(SharedEntityAuditListener.class)  // Enable automatic auditing
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String email;

    // All changes to this entity are automatically audited with:
    // - before/after values
    // - cryptographic hash chain for tamper detection
    // - operation type (INSERT, UPDATE, DELETE)
}
```

### Audit Best Practices for Auth Service

#### DO:

✅ Log all authentication events (login, logout, token refresh)  
✅ Log authorization failures (403 responses) with policy/capability context  
✅ Include trace_id in all audit events for request correlation  
✅ Log role/policy/capability changes with before/after values  
✅ Use entity audit for User table changes (automatically tracked)  
✅ Log password reset requests and completions  
✅ Include user_id whenever available  
✅ Tag events with meaningful action names (USER_LOGIN, ROLE_ASSIGNED, etc.)

#### DON'T:

❌ Log passwords, tokens, or API keys in audit metadata  
❌ Skip audit logging for failed operations—log failures too  
❌ Modify audit tables directly—they're append-only  
❌ Delete audit records (archive instead)  
❌ Use generic action names like "UPDATE" (be specific: "USER_ROLE_UPDATE")

### Common Audit Patterns

#### Login Event

```java
auditTrailService.logAction(
    user.getId(),
    "USER_LOGIN",
    "USER",
    String.valueOf(user.getId()),
    user.getUsername(),
    "User logged in successfully",
    Map.of(
        "ip_address", ipAddress,
        "user_agent", userAgent,
        "trace_id", traceId
    )
);
```

#### Failed Authorization

```java
auditTrailService.logAction(
    userId,
    "AUTHORIZATION_DENIED",
    "ENDPOINT",
    endpoint,
    endpoint,
    String.format("User lacks required policy: %s", requiredPolicy),
    Map.of(
        "required_policy", requiredPolicy,
        "user_policies", userPolicies,
        "http_method", httpMethod,
        "status_code", 403
    )
);
```

#### Role Assignment

```java
auditTrailService.logAction(
    currentUserId,
    "ROLE_ASSIGNED",
    "USER",
    String.valueOf(targetUserId),
    targetUser.getUsername(),
    String.format("Assigned role %s to user", roleName),
    Map.of(
        "role_name", roleName,
        "assigned_by", currentUser.getUsername()
    )
);
```

### Querying Audit Logs

```java
// Use AuditEventRepository for programmatic queries
@Autowired
private AuditEventRepository auditEventRepository;

// Find all actions by a user
List<AuditEvent> userActivity = auditEventRepository
    .findByUserIdOrderByOccurredAtDesc(userId);

// Find all auth-service events today
List<AuditEvent> todayEvents = auditEventRepository
    .findByServiceNameAndOccurredAtAfter(
        "auth-service",
        LocalDateTime.now().minusDays(1)
    );
```

### Troubleshooting Audit Issues

| Issue                      | Check                                                                               |
| -------------------------- | ----------------------------------------------------------------------------------- |
| Audit events not appearing | Verify `shared-lib.audit.enabled=true` in config                                    |
| Entity audit not working   | Ensure `@EntityListeners(SharedEntityAuditListener.class)` on entity                |
| Permission denied errors   | Check database grants: `GRANT INSERT, SELECT ON audit.audit_event TO auth_app_role` |
| Hash chain broken          | Check logs for concurrent modifications; contact security team                      |

### Testing Audit Logging

```java
@Test
public void testUserLoginAudit() {
    // Perform action
    authService.login(loginRequest);

    // Verify audit event created
    List<AuditEvent> events = auditEventRepository
        .findByActionAndEntityId("USER_LOGIN", userId.toString());

    assertThat(events).hasSize(1);
    assertThat(events.get(0).getServiceName()).isEqualTo("auth-service");
    assertThat(events.get(0).getSourceSchema()).isEqualTo("auth");
}
```

### References

- **Full Documentation:** `documentation/LBE/architecture/audit-design.md`
- **Quick Reference:** `documentation/LBE/reference/audit-quick-reference.md`
- **Configuration:** Review shared-lib audit properties in `application.yml`

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

# Run jOOQ codegen (if configured)
mvn clean generate-sources
```

### Running Tests

- Tests should use Testcontainers or H2 for database interactions
- Mock external dependencies using **Mockito**
- Write tests for:
  - Service layer business logic
  - Authorization rules and policy evaluation
  - JWT token validation
  - API endpoints (use MockMvc)
  - jOOQ queries (integration tests)
  - RLS isolation

## Common Tasks

### Adding a New API Endpoint (e.g., GET /api/employees)

**Step 1: Consult Documentation**

- Read `documentation/LBE/guides/extend-access.md`
- Check `documentation/LBE/reference/policy-matrix.md` for required policies
- Review `documentation/LBE/architecture/permission-patterns.md` for patterns

**Step 2: Determine Data Access Pattern**

1. Is this a simple lookup? → Use JPA Repository
2. Multi-join with complex filters? → Use jOOQ DSL
3. Analyst-maintained report? → Use jOOQ + SQL Template

**Step 3: Implement**

1. Create DTO classes in `dto/` package
2. Create appropriate DAO/Repository
3. Implement service layer business logic
4. Add controller method with OpenAPI annotations
5. Add authorization: `@PreAuthorize("hasAnyAuthority('employee.read')")`
6. Ensure RLS context is set

**Step 4: Register in Auth Catalog**

1. Create migration script to register endpoint in `auth.endpoints`
2. Link to policies via `auth.endpoint_policies`
3. Update `documentation/LBE/reference/raw/RBAC/MAPPINGS/PHASE5_ENDPOINT_POLICY_MAPPINGS.md`

**Step 5: Test & Document**

1. Write unit tests for business logic
2. Write integration tests for database queries
3. Test with different roles and tenant contexts
4. Update `documentation/LBE/reference/recent-updates.md`
5. Update OpenAPI documentation

### Adding a New Role or Policy

1. Create SQL migration script with role/policy/capability definitions
2. Update `documentation/LBE/reference/role-catalog.md`
3. Update `documentation/LBE/reference/policy-matrix.md`
4. Test with different user personas
5. Document in `documentation/LBE/reference/recent-updates.md`

### Debugging Authorization Issues

1. Check JWT token contents and claims
2. Verify role assignments in `auth.user_roles` table
3. Review policy bindings in `auth.role_policies` and `auth.policy_capabilities`
4. Check RLS context: `SELECT current_setting('app.current_user_id')`
5. Consult `documentation/LBE/playbooks/troubleshoot-auth.md`

## Important Considerations

### PostgreSQL Row-Level Security (RLS)

- Service relies heavily on PostgreSQL RLS policies for data isolation
- Always use `RLSContext` or `RLSContextFilter` to set session variables
- RLS policies are defined in database setup scripts
- Test multi-tenancy isolation thoroughly
- See `documentation/LBE/foundations/data-guardrails-101.md`

### Performance

- Use pagination for list endpoints
- Consider caching for frequently accessed authorization data
- Monitor database connection pool usage
- Use database indexes appropriately
- Profile jOOQ queries for optimization opportunities

### Migrations and Schema Changes

- PostgreSQL is the primary database
- Schema changes via SQL migration scripts
- Test migrations on copy of production data
- Keep `ddl-auto: update` for development only
- Document in `documentation/LBE/reference/TABLE_NAMES_REFERENCE.md`

## Additional Resources

- Spring Boot: https://docs.spring.io/spring-boot/docs/3.2.5/reference/html/
- Spring Security: https://docs.spring.io/spring-security/reference/
- jOOQ: https://www.jooq.org/doc/latest/manual/
- JWT Best Practices: https://tools.ietf.org/html/rfc8725
- PostgreSQL RLS: https://www.postgresql.org/docs/current/ddl-rowsecurity.html

---

# Auth Service — Documentation Reference 📚

The source of truth for architecture, onboarding SQL, and RBAC flows lives in the shared documentation repo (`documentation/LBE`). **Always consult this documentation before implementing new features or making changes**.

## Essential Reading (Start Here) 🎯

### Getting Started Journey

1. **`documentation/LBE/README.md`** – Step-by-step Auth journey with numbered guides

   - Architecture → Data Map → Login Journey → RBAC/VPD Setup → References
   - Follow the "Next" links when onboarding new contributors

2. **`documentation/LBE/start/welcome.md`** – Platform introduction and context
3. **`documentation/LBE/start/platform-tour.md`** – System overview and components
4. **`documentation/LBE/start/role-stories.md`** – User persona narratives

### Architecture & Design (Read Before Coding) 🏗️

#### Core Architecture Documents

- **`documentation/LBE/architecture/overview.md`** – System topology, authentication flow, authorization components
- **`documentation/LBE/architecture/data-map.md`** – Table relationships (users → roles → policies → capabilities → endpoints → tenant ACL)
- **`documentation/LBE/architecture/request-lifecycle.md`** – How requests flow through the system (with sequence diagrams)
- **`documentation/LBE/architecture/policy-binding.md`** – How permissions interconnect and bind together
- **`documentation/LBE/architecture/permission-patterns.md`** – Real-world setup examples and common patterns
- **`documentation/LBE/architecture/audit-design.md`** – Centralized audit logging and compliance tracking

#### Foundations & Concepts

- **`documentation/LBE/foundations/access-control-101.md`** – RBAC fundamentals and concepts
- **`documentation/LBE/foundations/data-guardrails-101.md`** – Row-Level Security (RLS) primer
- **`documentation/LBE/foundations/postgres-for-auth.md`** – PostgreSQL features (JSONB, RLS, contexts) used by this service

## Implementation Guides (Use While Coding) 💻

### Data Access Patterns ⭐ CRITICAL ⭐

- **`documentation/LBE/guides/data-access-patterns.md`** – **Read this before writing ANY database code**
  - When to use Spring Data JPA vs jOOQ DSL vs jOOQ + SQL templates
  - Decision flowchart and migration guidance
  - Examples from auth-service, payment-flow-service, reconciliation-service

### Step-by-Step Workflows

- **`documentation/LBE/guides/login-to-data.md`** – Worker/employer/board personas: login → JWT → authorization → RLS
- **`documentation/LBE/guides/setup/rbac.md`** – RBAC Setup Playbook: Create roles, policies, capabilities, endpoints
- **`documentation/LBE/guides/setup/vpd.md`** – VPD Setup Playbook: Configure RLS, load tenant ACL, test users
- **`documentation/LBE/guides/extend-access.md`** – Procedures for adding new policies and capabilities
- **`documentation/LBE/guides/integrate-your-service.md`** – How to connect other services to auth
- **`documentation/LBE/guides/verify-permissions.md`** – Testing RBAC and RLS configurations
- **`documentation/LBE/guides/local-environment.md`** – Local development setup instructions
- **`documentation/LBE/guides/user-management-crud-completion.md`** – User CRUD operations implementation

## Quick Reference (Use During Development) 📖

### Reference Sheets

- **`documentation/LBE/reference/role-catalog.md`** – All roles and their descriptions
- **`documentation/LBE/reference/capability-catalog.md`** – Complete capability list
- **`documentation/LBE/reference/policy-matrix.md`** – Policy → Capability → Role mappings
- **`documentation/LBE/reference/vpd-checklist.md`** – VPD/RLS implementation checklist
- **`documentation/LBE/reference/postgres-operations.md`** – PostgreSQL operational checklist
- **`documentation/LBE/reference/audit-quick-reference.md`** – Audit logging requirements
- **`documentation/LBE/reference/TABLE_NAMES_REFERENCE.md`** – Canonical schema and table list
- **`documentation/LBE/reference/recent-updates.md`** – Latest changes (check before each sprint!)

### Raw/Detailed References

- **`documentation/LBE/reference/raw/README.md`** – Index to exhaustive documentation
- **`documentation/LBE/reference/raw/RBAC/MAPPINGS/PHASE5_ENDPOINT_POLICY_MAPPINGS.md`** – Authoritative endpoint → policy → capability mappings
- **`documentation/LBE/reference/raw/RBAC/MAPPINGS/PHASE1_ENDPOINTS_EXTRACTION.md`** – Endpoint categorization
- **`documentation/LBE/reference/raw/RBAC/MAPPINGS/PHASE4_POLICY_CAPABILITY_MAPPINGS.md`** – Policy → Capability mappings per role
- **`documentation/LBE/reference/raw/RBAC/DEFINITIONS/PHASE2_UI_PAGES_ACTIONS.md`** – UI pages and actions
- **`documentation/LBE/reference/raw/ONBOARDING_ROLES.md`** – Detailed role narratives
- **`documentation/LBE/reference/raw/RBAC/ROLES.md`** – Complete role definitions

## Troubleshooting & Operations 🔧

### Problem Resolution

- **`documentation/LBE/playbooks/troubleshoot-auth.md`** – Issue-led decision tree for RBAC/RLS problems
  - JWT validation issues
  - Authorization failures
  - RLS context problems
  - Common error scenarios

### Operational Playbooks

- **`documentation/LBE/reference/postgres-operations.md`** – Routine tasks, migration workflow, performance checks
- **`documentation/LBE/foundations/postgres-for-auth.md`** – Database role management and operational tasks

## Maintenance Checklist ✅

### When Adding New Endpoints/APIs

1. ✅ Define endpoint in controller with OpenAPI annotations
2. ✅ Choose data access pattern from `documentation/LBE/guides/data-access-patterns.md`
3. ✅ Implement with appropriate pattern (JPA/jOOQ DSL/jOOQ+SQL)
4. ✅ Register endpoint in `auth.endpoints` table (via migration)
5. ✅ Create/update policies and capabilities as needed
6. ✅ Link endpoint to policies via `auth.endpoint_policies`
7. ✅ Update `documentation/LBE/reference/raw/RBAC/MAPPINGS/PHASE5_ENDPOINT_POLICY_MAPPINGS.md`
8. ✅ Update `documentation/LBE/reference/policy-matrix.md`
9. ✅ Update `documentation/LBE/reference/role-catalog.md` if new roles added
10. ✅ Add integration tests verifying authorization and RLS
11. ✅ Document in `documentation/LBE/reference/recent-updates.md`

### When Modifying Policies/Roles

1. ✅ Create SQL migration with changes
2. ✅ Update `documentation/LBE/reference/policy-matrix.md`
3. ✅ Update `documentation/LBE/reference/role-catalog.md`
4. ✅ Test with different user personas
5. ✅ Document in `documentation/LBE/reference/recent-updates.md`
6. ✅ Update affected guides in `documentation/LBE/guides/`

### When Changing Database Schema

1. ✅ Write migration script following PostgreSQL best practices
2. ✅ Update `documentation/LBE/architecture/data-map.md`
3. ✅ Update `documentation/LBE/reference/TABLE_NAMES_REFERENCE.md`
4. ✅ Refresh `documentation/LBE/foundations/postgres-for-auth.md` if roles/permissions change
5. ✅ Update onboarding SQL in `documentation/LBE/onboarding/setup/`
6. ✅ Test RLS policies if data access patterns change
7. ✅ Document in `documentation/LBE/reference/recent-updates.md`

### When Modifying Audit/Logging

1. ✅ Confirm changes match `documentation/LBE/reference/audit-quick-reference.md`
2. ✅ Update `documentation/LBE/architecture/audit-design.md` (Auth Service section)
3. ✅ Ensure compliance requirements still met
4. ✅ Update audit-related guides

## Key Principles 🎯

### Security First 🔒

- ✅ Never bypass RLS policies
- ✅ Always validate JWT tokens
- ✅ Set PostgreSQL session context before queries
- ✅ Check authorization before resource access
- ✅ Never log sensitive data
- ✅ Follow `documentation/LBE/foundations/data-guardrails-101.md`

### Documentation Driven 📝

- ✅ Read relevant docs BEFORE coding
- ✅ Update docs WITH your code changes
- ✅ Link to documentation in code comments
- ✅ Keep documentation and code in sync

### Test Comprehensively 🧪

- ✅ Test with multiple user personas
- ✅ Test tenant isolation (RLS)
- ✅ Test authorization (RBAC)
- ✅ Test error scenarios
- ✅ Follow `documentation/LBE/guides/verify-permissions.md`

## Quick Links by Task 🔗

| Task                             | Primary Documentation                                     |
| -------------------------------- | --------------------------------------------------------- |
| Setting up local environment     | `documentation/LBE/guides/local-environment.md`           |
| Understanding architecture       | `documentation/LBE/architecture/overview.md`              |
| **Choosing data access pattern** | **`documentation/LBE/guides/data-access-patterns.md`** ⭐ |
| Adding new endpoint              | `documentation/LBE/guides/extend-access.md`               |
| Creating new role/policy         | `documentation/LBE/guides/setup/rbac.md`                  |
| Debugging authorization          | `documentation/LBE/playbooks/troubleshoot-auth.md`        |
| Understanding RLS                | `documentation/LBE/foundations/data-guardrails-101.md`    |
| PostgreSQL operations            | `documentation/LBE/reference/postgres-operations.md`      |
| Checking recent changes          | `documentation/LBE/reference/recent-updates.md`           |

---

**Remember**: The documentation in `documentation/LBE/` is the single source of truth. Always consult it before making changes, and update it along with your code changes.
