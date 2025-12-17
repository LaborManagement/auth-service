# Copilot Instructions for Auth Service

## Project Overview

Spring Boot microservice providing JWT authentication, RBAC authorization, RLS-based multi-tenancy, and comprehensive audit logging for the payment reconciliation platform.

**Stack:** Java 17 | Spring Boot 3.2.5 | PostgreSQL | jOOQ | JWT

**Setup:** Follow `documentation/LBE/guides/local-environment.md`

## Code Organization

```
com.example.userauth/
├── config/       # Spring configuration (Security, JPA, jOOQ)
├── controller/   # REST API endpoints
├── dao/          # jOOQ-based complex queries
├── dto/          # Data Transfer Objects
├── entity/       # JPA entities (User, Role, Policy, UserTenantAcl)
├── repository/   # Spring Data JPA repositories
├── security/     # JWT utilities, filters, authentication
├── service/      # Business logic (AuthService, AuthorizationService, RoleService)
└── util/         # Utility classes
```

## API Documentation Standards (Swagger/OpenAPI)

**All REST endpoints and DTOs must be documented using Swagger/OpenAPI annotations.**

### Controller Guidelines

- Annotate each controller class with `@Tag` to describe the API group.
- Annotate each endpoint method with:
  - `@Operation` (summary, description, tags, security, etc.)
  - `@ApiResponses` and one or more `@ApiResponse` for all possible HTTP responses (success, error, not found, etc.)
  - `@Parameter` for path/query/header parameters if not obvious from method signature.
- Use `@Schema` on DTO fields for field-level documentation, including descriptions and examples.
- Document all request/response bodies, path variables, and query parameters.
- Keep documentation up to date with code changes.

#### Example (Controller)

```java
@RestController
@RequestMapping("/api/example")
@Tag(name = "Example", description = "Example API endpoints")
public class ExampleController {

  @GetMapping("/{id}")
  @Operation(summary = "Get example by ID", description = "Returns an example resource by its ID.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Resource found"),
    @ApiResponse(responseCode = "404", description = "Resource not found")
  })
  public ResponseEntity<ExampleDto> getExample(@PathVariable Long id) {
    // ...
  }
}
```

#### Example (DTO)

```java
import io.swagger.v3.oas.annotations.media.Schema;

public class ExampleDto {
  @Schema(description = "Unique identifier", example = "123")
  private Long id;

  @Schema(description = "Example name", example = "Sample")
  private String name;
  // ...
}
```

### Best Practices

- Always update Swagger annotations when changing endpoints or DTOs.
- Use meaningful summaries and descriptions.
- Include all possible response codes.
- Use `@Schema` for DTO fields, especially for request/response bodies.
- Review generated Swagger UI to ensure clarity and completeness.

**Reference:** See `springdoc-openapi` documentation and existing controllers for patterns.

- Follow Spring Boot conventions and existing patterns
- Use constructor injection for dependencies
- Add JavaDoc for public APIs
- Use meaningful variable names (`tenantId`, `policyId`, `userId`)
- Return DTOs from controllers, not entities
- Document endpoints with OpenAPI annotations

## Database Access Patterns ⭐ CRITICAL

**ALWAYS read `documentation/LBE/guides/data-access-patterns.md` before writing database code.**

### Quick Reference

| Pattern            | Use For                                       | Example                            |
| ------------------ | --------------------------------------------- | ---------------------------------- |
| **JPA Repository** | CRUD operations, writes, simple reads         | `UserRepository`, `RoleRepository` |
| **jOOQ DSL**       | Complex queries, multi-joins, dynamic filters | `UserQueryDao`, `RoleQueryDao`     |
| **jOOQ + SQL**     | Analyst-maintained reports, complex CTEs      | Load from `sql/` templates         |

### Rules for ALL Patterns

🔒 **Security:** Always set RLS context: `SELECT auth.set_user_context(:userId)`  
🔄 **Transactions:** Use `@Transactional` for writes, `@Transactional(readOnly=true)` for reads  
✅ **Testing:** Test with multiple personas, verify RLS isolation

**Details:** See `documentation/LBE/guides/data-access-patterns.md`

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

**Read:** `documentation/LBE/architecture/audit-design.md` | `documentation/LBE/reference/audit-quick-reference.md`

### Two Audit Mechanisms

| Mechanism                 | Purpose                                            | Implementation                                                  |
| ------------------------- | -------------------------------------------------- | --------------------------------------------------------------- |
| **API-Level Auditing**    | Log controller actions, endpoints, business events | `@Auditable` annotation on controllers                          |
| **Entity-Level Auditing** | Track data changes with tamper detection           | `@EntityListeners(SharedEntityAuditListener.class)` on entities |

### Configuration (DO NOT CHANGE)

```yaml
shared-lib:
  audit:
    enabled: true
    service-name: auth-service # Enables cross-service queries
    source-schema: auth
  entity-audit:
    enabled: true
    service-name: auth-service
    source-schema: auth
    source-table: users
```

### 1. API-Level Auditing with @Auditable

Use `@Auditable` on controller methods for automatic audit logging:

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @PostMapping("/login")
    @Auditable(
        action = "USER_LOGIN",
        entityType = "USER",
        description = "User login attempt"
    )
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        // Business logic - audit logged automatically
    }

    @PutMapping("/{id}/role")
    @Auditable(
        action = "USER_ROLE_UPDATE",
        entityType = "USER",
        includeRequestBody = false  // Don't log sensitive data
    )
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody RoleUpdate update) {
        // Audit captures: endpoint, user_id, trace_id, status_code
    }
}
```

**What @Auditable captures automatically:**

- API endpoint, HTTP method, status code
- User ID (from security context)
- Trace ID (for request correlation)
- Request/response metadata (configurable)

### 2. Entity-Level Auditing with @EntityListeners

```java
@Entity
@Table(name = "users", schema = "auth")
@EntityListeners(SharedEntityAuditListener.class)
public class User {
    // All INSERT/UPDATE/DELETE tracked with before/after values + hash chain
}
```

### 3. Manual Auditing (When Needed)

```java
@Autowired
private AuditTrailService auditTrailService;

// For non-controller actions or custom audit needs
auditTrailService.logAction(userId, "POLICY_CHANGE", "POLICY",
    policyId, policyName, "Updated policy bindings", metadata);
```

### Best Practices

**DO:**

- ✅ Use `@Auditable` on all controller endpoints
- ✅ Use `@EntityListeners` on sensitive entities (User, Role, Policy, UserTenantAcl)
- ✅ Include trace_id for distributed tracing
- ✅ Use specific action names (USER_LOGIN, ROLE_ASSIGNED)

**DON'T:**

- ❌ Log passwords, tokens, API keys
- ❌ Use generic names ("UPDATE", "CREATE")
- ❌ Skip audit for failed operations
- ❌ Modify audit tables directly

### Auth Service Audit Checklist

- [ ] All authentication endpoints have `@Auditable`
- [ ] Authorization failures are logged
- [ ] User/Role/Policy entities have `@EntityListeners`
- [ ] UserTenantAcl changes are tracked
- [ ] Password resets are audited (but not passwords themselves)

**Troubleshooting:** Check `shared-lib.audit.enabled=true` | Verify DB grants | See audit-design.md

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

### Adding a New API Endpoint

1. **Consult Documentation**: `documentation/LBE/guides/extend-access.md` | `documentation/LBE/reference/policy-matrix.md`
2. **Choose Data Access Pattern**: JPA (simple) | jOOQ DSL (complex) | jOOQ+SQL (reports) — See `data-access-patterns.md`
3. **Implement**: DTO → DAO/Repository → Service → Controller with `@PreAuthorize`
4. **Register**: Migration script → `auth.endpoints` + `auth.endpoint_policies`
5. **Update Docs**: `PHASE5_ENDPOINT_POLICY_MAPPINGS.md` | `policy-matrix.md`
6. **Test**: Authorization + RLS isolation

### Adding/Modifying Roles or Policies

1. SQL migration with changes
2. Update `role-catalog.md` and `policy-matrix.md`
3. Test with different personas
4. Document in `recent-updates.md`

### Debugging Authorization Issues

1. Check JWT token contents and claims
2. Verify role assignments in `auth.user_roles`
3. Review policy bindings: `auth.role_policies` + `auth.policy_capabilities`
4. Check RLS context: `SELECT current_setting('app.current_user_id')`
5. Consult `documentation/LBE/playbooks/troubleshoot-auth.md`

## Important Considerations

- **RLS:** Always use `RLSContext` for tenant isolation. Test multi-tenancy thoroughly.
- **Performance:** Use pagination, caching, proper indexes. Profile jOOQ queries.
- **Migrations:** SQL scripts only. Test on production copies. Document in `TABLE_NAMES_REFERENCE.md`.

---

# Auth Service — Documentation Reference 📚

**Source of Truth:** `documentation/LBE/` - Always consult before coding

## Essential Reading 🎯

**Start Here:**

- `documentation/LBE/README.md` – Guided journey through auth system
- `documentation/LBE/architecture/overview.md` – System topology and flows
- `documentation/LBE/architecture/data-map.md` – Table relationships
- `documentation/LBE/architecture/audit-design.md` – Audit system ⭐

**Foundations:**

- `documentation/LBE/foundations/access-control-101.md` – RBAC fundamentals
- `documentation/LBE/foundations/data-guardrails-101.md` – RLS primer
- `documentation/LBE/foundations/postgres-for-auth.md` – PostgreSQL features

## Implementation Guides 💻

**Data Access (CRITICAL):**

- `documentation/LBE/guides/data-access-patterns.md` ⭐ – **Read before ANY database code**

**Workflows:**

- `documentation/LBE/guides/login-to-data.md` – Login → JWT → RLS flow
- `documentation/LBE/guides/setup/rbac.md` – RBAC setup
- `documentation/LBE/guides/setup/vpd.md` – RLS/VPD setup
- `documentation/LBE/guides/extend-access.md` – Adding policies
- `documentation/LBE/guides/verify-permissions.md` – Testing

## Quick Reference 📖

- `documentation/LBE/reference/role-catalog.md` – All roles
- `documentation/LBE/reference/policy-matrix.md` – Policy mappings
- `documentation/LBE/reference/audit-quick-reference.md` – Audit guide
- `documentation/LBE/reference/TABLE_NAMES_REFERENCE.md` – Schema reference
- `documentation/LBE/reference/recent-updates.md` – Latest changes

## Troubleshooting 🔧

- `documentation/LBE/playbooks/troubleshoot-auth.md` – Auth issues
- `documentation/LBE/reference/postgres-operations.md` – Database ops

## Maintenance Checklist ✅

**Adding Endpoint:**

1. Choose data pattern (`data-access-patterns.md`)
2. Implement: DTO → DAO → Service → Controller
3. Register: `auth.endpoints` + `auth.endpoint_policies`
4. Update: `PHASE5_ENDPOINT_POLICY_MAPPINGS.md` + `policy-matrix.md`
5. Test: Authorization + RLS

**Modifying Roles/Policies:**

1. SQL migration
2. Update: `policy-matrix.md` + `role-catalog.md`
3. Test with personas
4. Document in `recent-updates.md`

**Schema Changes:**

1. Migration script
2. Update: `data-map.md` + `TABLE_NAMES_REFERENCE.md`
3. Test RLS
4. Document in `recent-updates.md`

**Audit Changes:**

1. Match `audit-quick-reference.md`
2. Update `audit-design.md` (Auth section)
3. Ensure compliance

## Key Principles 🎯

- 🔒 **Security:** Never bypass RLS | Always validate JWT | Set session context | Check authorization | No sensitive logging
- 📝 **Documentation:** Read docs first | Update with code | Keep in sync
- 🧪 **Testing:** Multiple personas | Tenant isolation | RBAC | Error scenarios

## Quick Links 🔗

| Task               | Documentation                           |
| ------------------ | --------------------------------------- |
| Local setup        | `guides/local-environment.md`           |
| Architecture       | `architecture/overview.md`              |
| **Data access**    | **`guides/data-access-patterns.md`** ⭐ |
| Add endpoint       | `guides/extend-access.md`               |
| Create role/policy | `guides/setup/rbac.md`                  |
| Debug auth         | `playbooks/troubleshoot-auth.md`        |
| RLS                | `foundations/data-guardrails-101.md`    |
| PostgreSQL ops     | `reference/postgres-operations.md`      |
| Recent changes     | `reference/recent-updates.md`           |

---

**Remember:** `documentation/LBE/` is the single source of truth. Consult before changing, update with changes.
