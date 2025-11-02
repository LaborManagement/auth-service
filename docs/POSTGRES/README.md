# PostgreSQL Migration from MySQL

**Status:** ✅ Complete | **Last Updated:** November 2, 2025

## Overview

This service has been migrated from MySQL to PostgreSQL. This guide explains the key differences and how to work with PostgreSQL.

## Why PostgreSQL?

| Feature | MySQL | PostgreSQL |
|---------|-------|-----------|
| JSON Support | Limited (JSON) | Advanced (JSONB) |
| Row-Level Security | None | Built-in (RLS) |
| Sequences | AUTO_INCREMENT | Proper sequences |
| Type System | Basic | Rich type system |
| Performance | Good | Excellent |

## Key Differences

### 1. JSONB Columns

PostgreSQL uses `JSONB` for binary JSON storage. This is **much faster** than MySQL's JSON.

**When working with JSONB in Java:**

```java
// IMPORTANT: Use Types.OTHER for JSONB binding
NamedParameterJdbcTemplate template = ...;
MapSqlParameterSource params = new MapSqlParameterSource();

// ✅ CORRECT
params.addValue("metadata", jsonString, Types.OTHER);

// ❌ WRONG
params.addValue("metadata", jsonString);  // Will fail!
```

**Example:**
```java
String policyJson = "{\"roles\": [\"ADMIN\", \"USER\"]}";
params.addValue("expression", policyJson, Types.OTHER);  // ← Note Types.OTHER
```

### 2. LIKE Queries on JSON

PostgreSQL requires casting JSON to text for LIKE queries.

**MySQL:**
```sql
WHERE policies.expression LIKE '%ADMIN%'
```

**PostgreSQL (must cast):**
```sql
WHERE CAST(policies.expression AS varchar) LIKE '%ADMIN%'
```

**In JPA/Hibernate:**
```java
@Query("SELECT p FROM Policy p WHERE CAST(p.expression AS string) LIKE CONCAT('%', :roleName, '%')")
List<Policy> searchPolicies(@Param("roleName") String roleName);
```

### 3. Sequences

PostgreSQL uses explicit sequences instead of AUTO_INCREMENT.

**MySQL:**
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ...
);
```

**PostgreSQL:**
```sql
CREATE SEQUENCE users_id_seq START 1;
CREATE TABLE users (
    id BIGINT PRIMARY KEY DEFAULT nextval('users_id_seq'),
    ...
);
```

### 4. Column Names

PostgreSQL is case-sensitive for unquoted identifiers.

```sql
-- These are DIFFERENT in PostgreSQL
SELECT CreatedAt FROM table;  -- Column named 'createdAt' won't match
SELECT created_at FROM table; -- ✅ Use snake_case
```

**Fix:** Use `@Column(name = "created_at")` in entities.

### 5. Case Sensitivity

**MySQL (default):** Case-insensitive
**PostgreSQL:** Case-sensitive

```sql
-- MySQL: Both return results
SELECT * FROM users WHERE username = 'Admin';
SELECT * FROM users WHERE username = 'admin';

-- PostgreSQL: Only exact match returns results
SELECT * FROM users WHERE username = 'admin';  -- ✅ Matches 'admin'
SELECT * FROM users WHERE username = 'Admin';  -- ✗ No match

-- PostgreSQL: Use ILIKE for case-insensitive
SELECT * FROM users WHERE username ILIKE 'admin';  -- Matches 'Admin', 'admin', 'ADMIN'
```

## Migration Checklist

- [x] Schema converted to PostgreSQL DDL
- [x] JSONB bindings in Java code
- [x] LIKE queries on JSON fixed with CAST
- [x] Sequences aligned with AUTO_INCREMENT values
- [x] Application properties updated
- [x] Connection pooling configured
- [x] Tests pass with PostgreSQL

## Application Configuration

### application-dev.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/auth_service_db
    username: app_auth
    password: secure_password
    driver-class-name: org.postgresql.Driver
    hikari:
      auto-commit: true           # ← IMPORTANT for RLS
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      
  jpa:
    hibernate:
      ddl-auto: validate          # Don't auto-create schema
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 20
        order_inserts: true
```

### pom.xml

```xml
<!-- PostgreSQL Driver -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.1</version>
</dependency>
```

## Working with PostgreSQL

### Connect to Database

```bash
# Command line
psql -U app_auth -d auth_service_db -h localhost

# In application
mvn spring-boot:run
```

### Run SQL Files

```bash
# From command line
psql -U postgres -d auth_service_db -f script.sql

# From psql prompt
\i /path/to/script.sql
```

### View Sequences

```sql
-- List all sequences
SELECT * FROM information_schema.sequences;

-- Check current sequence value
SELECT last_value FROM users_id_seq;
```

### Align Sequences After Data Import

After bulk-loading data from MySQL, sequences may be out of sync.

```bash
# Run sequence alignment script
psql -U postgres -d auth_service_db -f scripts/postgres/reset_all_sequences.sql
```

**Script content:**
```sql
DO $$ 
DECLARE
    rec RECORD;
BEGIN
    FOR rec IN 
        SELECT table_schema, table_name, column_name
        FROM information_schema.columns
        WHERE column_name = 'id'
    LOOP
        EXECUTE format(
            'SELECT setval(%L, (SELECT max(%I) FROM %I.%I))',
            rec.table_schema || '.' || rec.table_name || '_id_seq',
            rec.column_name,
            rec.table_schema,
            rec.table_name
        );
    END LOOP;
END $$;
```

## Common Issues

### Issue 1: "Column does not exist"

**Cause:** PostgreSQL is case-sensitive

```sql
-- ❌ WRONG
SELECT UserId FROM users;  -- PostgreSQL looks for "UserId"

-- ✅ CORRECT
SELECT user_id FROM users;  -- Use snake_case
```

**Fix:** Ensure all column names in queries use correct case.

### Issue 2: JSONB binding fails

**Cause:** Not using Types.OTHER for JSONB

```java
// ❌ WRONG
params.addValue("expression", jsonString);

// ✅ CORRECT
params.addValue("expression", jsonString, Types.OTHER);
```

### Issue 3: LIKE queries return no results

**Cause:** Not casting JSON to text

```sql
-- ❌ WRONG
SELECT * FROM policies WHERE expression LIKE '%ADMIN%';

-- ✅ CORRECT
SELECT * FROM policies WHERE CAST(expression AS varchar) LIKE '%ADMIN%';
```

### Issue 4: Inserts fail with "duplicate key"

**Cause:** Sequence out of sync after bulk import

```bash
# Fix: Run sequence alignment
psql -U postgres -d auth_service_db -f scripts/postgres/reset_all_sequences.sql
```

## Testing

### Run Tests
```bash
mvn test
```

### Verify Key Features
```bash
# 1. Check JSONB storage works
mvn test -Dtest=PolicyTest

# 2. Check audit events persist
mvn test -Dtest=AuditTest

# 3. Check authorization endpoints
curl http://localhost:8080/api/me/authorizations \
  -H "Authorization: Bearer <JWT>"
# Expected: 200 OK with role permissions
```

## Rollback (If Needed)

If you need to rollback to MySQL:

1. **Revert application.yml:**
   ```yaml
   datasource:
     url: jdbc:mysql://localhost:3306/user_auth_db
     username: root
     driver-class-name: com.mysql.cj.jdbc.Driver
   ```

2. **Restore from backup:**
   ```bash
   mysql -u root -p user_auth_db < backup.sql
   ```

3. **Revert code changes:**
   ```bash
   git checkout application-dev.yml pom.xml
   ```

## Performance Tips

### Indexes
```sql
-- Create indexes on frequently queried columns
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_policies_name ON policies(name);
CREATE INDEX idx_policies_expression_gin ON policies USING gin(expression);
```

### Query Optimization
```sql
-- Use EXPLAIN ANALYZE to see query plans
EXPLAIN ANALYZE
SELECT * FROM users WHERE username = 'admin';
```

### Connection Pooling
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # Adjust based on load
      minimum-idle: 5        # Keep some connections open
```

## Next Steps

- **Setup:** See [setup.md](setup.md)
- **Testing:** See [testing.md](testing.md)
- **Troubleshooting:** See [troubleshoot.md](troubleshoot.md)

