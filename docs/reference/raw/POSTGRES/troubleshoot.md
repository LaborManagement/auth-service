# PostgreSQL Troubleshooting Guide

## Connection Issues

### Issue 1: "Connection refused"

**Cause:** PostgreSQL not running or wrong port

#### Fix:
```bash
# Check if PostgreSQL is running
ps aux | grep postgres

# Start PostgreSQL
brew services start postgresql@15    # macOS
sudo systemctl start postgresql      # Linux

# Test connection
psql -U postgres -h localhost -p 5432
```

---

### Issue 2: "FATAL: role does not exist"

**Cause:** User not created or wrong username in connection

```bash
# Check roles
psql -U postgres -c "\du"

# Create role if missing
psql -U postgres -c "CREATE ROLE app_auth LOGIN PASSWORD 'password';"

# Update application.yml with correct username
```

---

### Issue 3: "FATAL: database does not exist"

**Cause:** Database not created

```bash
# Check databases
psql -U postgres -c "\l"

# Create database
psql -U postgres -c "CREATE DATABASE auth_service_db OWNER app_auth;"

# Grant permissions
psql -U postgres -d auth_service_db -c "GRANT ALL ON ALL TABLES IN SCHEMA public TO app_auth;"
```

---

## Data Issues

### Issue 4: "Column does not exist"

**Cause:** PostgreSQL is case-sensitive, column name wrong case

```sql
-- ❌ WRONG (looking for literal "UserId")
SELECT UserId FROM users;

-- ✅ CORRECT
SELECT user_id FROM users;
```

**Fix:**
- Update queries to use correct case
- Use `@Column(name = "user_id")` in JPA entities

---

### Issue 5: "Duplicate key value violates unique constraint"

**Cause:** Sequence out of sync after data import

```bash
# Check sequence vs actual max ID
psql -U app_auth -d auth_service_db -c \
  "SELECT MAX(id) FROM users; SELECT last_value FROM users_id_seq;"

# If different, align sequences
psql -U postgres -d auth_service_db -f scripts/postgres/reset_all_sequences.sql
```

---

### Issue 6: "No rows returned" when data exists

**Cause:** Case sensitivity or data type mismatch

```sql
-- Check data actually exists
SELECT * FROM users LIMIT 1;

-- Check for case mismatches
SELECT username FROM users WHERE LOWER(username) = 'admin';

-- Check data type
SELECT username, pg_typeof(username) FROM users LIMIT 1;
```

---

## JSONB Issues

### Issue 7: JSONB binding fails in Java

**Error:** "No binary output handler for type"

```java
// ❌ WRONG
params.addValue("expression", jsonString);

// ✅ CORRECT - Must use Types.OTHER
params.addValue("expression", jsonString, Types.OTHER);
```

**Search and fix all occurrences:**
```bash
grep -r "addValue.*json" src/
# Update all to add ", Types.OTHER" parameter
```

---

### Issue 8: JSONB LIKE queries return no results

**Cause:** JSON not cast to text

```sql
-- ❌ WRONG
SELECT * FROM policies WHERE expression LIKE '%ADMIN%';

-- ✅ CORRECT
SELECT * FROM policies WHERE CAST(expression AS varchar) LIKE '%ADMIN%';
```

**In JPA:**
```java
@Query("SELECT p FROM Policy p WHERE CAST(p.expression AS string) LIKE CONCAT('%', :text, '%')")
List<Policy> search(@Param("text") String text);
```

---

### Issue 9: JSONB queries return NULL

**Cause:** Wrong JSON path operator

```sql
-- Get full JSON object
SELECT data FROM test_jsonb;
-- Returns: {"roles": ["ADMIN"]}

-- Get text value (use ->>)
SELECT data->>'roles' FROM test_jsonb;
-- Returns: ["ADMIN"]

-- Get JSON value (use ->)
SELECT data->'roles' FROM test_jsonb;
-- Returns: ["ADMIN"]  (as JSON)
```

---

## Performance Issues

### Issue 10: Slow queries

**Cause:** Missing indexes

```sql
-- Analyze slow query
EXPLAIN ANALYZE
SELECT * FROM users WHERE username = 'admin';

-- If shows "Seq Scan", create index
CREATE INDEX idx_users_username ON users(username);

-- Re-analyze
ANALYZE;

-- Test again
EXPLAIN ANALYZE SELECT * FROM users WHERE username = 'admin';
-- Should now show "Index Scan"
```

---

### Issue 11: High memory usage

**Cause:** Connection pool too large

```yaml
# Reduce pool size
spring:
  datasource:
    hikari:
      maximum-pool-size: 10  # Reduce from 20
      minimum-idle: 2        # Reduce from 5
```

---

## Migration Issues

### Issue 12: Data types incompatible after MySQL migration

**Cause:** MySQL and PostgreSQL type differences

| MySQL | PostgreSQL | Fix |
|-------|-----------|-----|
| TINYINT | SMALLINT | N/A (compatible) |
| INT | INTEGER | N/A (compatible) |
| AUTO_INCREMENT | SEQUENCE | Create sequence |
| DATETIME | TIMESTAMP | N/A (compatible) |
| JSON | JSONB | Convert in import |

**Fix:**
```bash
# Before importing, convert:
# - AUTO_INCREMENT PRIMARY KEY → SERIAL
# - JSON → JSONB
# - MySQL functions → PostgreSQL equivalents
```

---

### Issue 13: Sequences don't reset after truncate

**Cause:** TRUNCATE doesn't reset sequences

```sql
-- ❌ WRONG
TRUNCATE users;
INSERT INTO users ...;  -- Fails with "duplicate key"

-- ✅ CORRECT
TRUNCATE users RESTART IDENTITY;
```

### Issue 14: "Duplicate key value violates unique constraint" after data import

**Cause:** Sequences out of sync after MySQL import

```bash
# Use the provided script to align sequences
psql -U postgres -d auth_service_db -f ../../scripts/postgres/reset_all_sequences.sql

# Or use the shell wrapper
bash ../../scripts/reset-sequences.sh
```

**Verify:**
```sql
SELECT MAX(id) FROM users;
SELECT last_value FROM users_id_seq;
-- Should show the same value
```

---

## Permission Issues

### Issue 14: "Permission denied" errors

**Cause:** Insufficient role permissions

```sql
-- Check role permissions
\du app_auth

-- Grant permissions
GRANT CONNECT ON DATABASE auth_service_db TO app_auth;
GRANT USAGE ON SCHEMA public TO app_auth;
GRANT ALL ON ALL TABLES IN SCHEMA public TO app_auth;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO app_auth;

-- For RLS setup
GRANT USAGE ON SCHEMA auth TO app_auth;
GRANT ALL ON ALL TABLES IN SCHEMA auth TO app_auth;
```

---

## Application Issues

### Issue 15: "Dialect: PostgreSQLDialect not found"

**Cause:** Correct PostgreSQL driver

```xml
<!-- Ensure correct dependency -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.1</version>  <!-- Use latest stable version -->
</dependency>
```

---

### Issue 16: Tests fail on PostgreSQL but worked on MySQL

**Common causes:**

1. **Case sensitivity**
   ```java
   // ❌ WRONG
   repository.findByUserName("Admin");
   
   // ✅ CORRECT
   repository.findByUserName("admin");
   ```

2. **JSONB binding**
   ```java
   // Add Types.OTHER to all JSONB bindings
   params.addValue("data", json, Types.OTHER);
   ```

3. **Sequences not reset**
   ```bash
   mvn test -DskipDbSetup=false  # Reset sequences before tests
   ```

---

## Diagnostic Queries

```sql
-- Check database size
SELECT pg_size_pretty(pg_database_size('auth_service_db'));

-- Check table sizes
SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename))
FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Check index usage
SELECT schemaname, tablename, indexname, idx_scan
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;

-- Find missing indexes
SELECT schemaname, tablename, attname
FROM pg_stats
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
AND inherited = false
ORDER BY null_frac DESC;
```

---

## Recovery Steps

If everything is broken:

1. **Check PostgreSQL is running**
   ```bash
   psql -U postgres -c "SELECT 1;"
   ```

2. **Check database exists**
   ```bash
   psql -U postgres -c "\l auth_service_db"
   ```

3. **Check role permissions**
   ```bash
   psql -U postgres -c "\du app_auth"
   ```

4. **Check tables exist**
   ```bash
   psql -U app_auth -d auth_service_db -c "\dt"
   ```

5. **Restore from backup**
   ```bash
   psql -U app_auth auth_service_db < backup.sql
   ```

