# PostgreSQL Testing Guide

## Test 1: Database Connection

```bash
# Test connection from command line
psql -U app_auth -d auth_service_db -c "SELECT 1 as test;"
# Expected: 
# test
# ------
# 1
```

## Test 2: Schema Verification

```sql
-- Connect to database
psql -U app_auth -d auth_service_db

-- List all tables
\dt
-- Expected: All required tables listed

-- Check specific table
\d users
-- Expected: All columns shown with correct types

-- Count tables
SELECT COUNT(*) FROM information_schema.tables 
WHERE table_schema = 'public';
-- Expected: > 0

-- List sequences
\ds
-- Expected: All sequences listed
```

## Test 3: JSONB Support

```sql
-- Create test table
CREATE TABLE test_jsonb (
    id SERIAL PRIMARY KEY,
    data JSONB
);

-- Insert JSON data
INSERT INTO test_jsonb (data) VALUES ('{"roles": ["ADMIN", "USER"]}');

-- Query JSON
SELECT data->>'roles' FROM test_jsonb;
-- Expected: ["ADMIN", "USER"]

-- Search in JSON
SELECT * FROM test_jsonb WHERE CAST(data AS varchar) LIKE '%ADMIN%';
-- Expected: 1 row

-- Cleanup
DROP TABLE test_jsonb;
```

## Test 4: Java Application Integration

```bash
# Build application
mvn clean install

# Run with PostgreSQL
mvn spring-boot:run -Dspring.profiles.active=dev

# Check logs
tail -f logs/application.log
# Look for:
# - "Connected to PostgreSQL"
# - "Hibernate: Dialect: PostgreSQLDialect"
# - No connection errors
```

## Test 5: JSONB Binding Test

```java
@SpringBootTest
public class PostgresJDBCTest {
    
    @Test
    public void testJsonbBinding() {
        String jsonString = "{\"roles\": [\"ADMIN\"]}";
        MapSqlParameterSource params = new MapSqlParameterSource();
        
        // CRITICAL: Must use Types.OTHER for JSONB
        params.addValue("expression", jsonString, Types.OTHER);
        
        // This should not throw exception
        assertNotNull(params);
    }
}
```

## Test 6: Sequence Functionality

```sql
-- Create test sequence
CREATE SEQUENCE test_seq START 1;

-- Check current value
SELECT nextval('test_seq');
-- Expected: 1

-- Check next value
SELECT nextval('test_seq');
-- Expected: 2

-- Reset sequence
SELECT setval('test_seq', 1);

-- Verify
SELECT nextval('test_seq');
-- Expected: 2

-- Cleanup
DROP SEQUENCE test_seq;
```

## Test 7: Case Sensitivity

```sql
-- PostgreSQL is case-sensitive
CREATE TABLE test_case (
    id SERIAL PRIMARY KEY,
    username varchar(100),
    CreatedAt timestamp
);

-- This works (exact match)
SELECT * FROM test_case WHERE username = 'admin';

-- This does NOT work (case-sensitive)
SELECT * FROM test_case WHERE Username = 'admin';
-- ERROR: column "Username" does not exist

-- This works (case-insensitive)
SELECT * FROM test_case WHERE username ILIKE 'ADMIN';

-- Cleanup
DROP TABLE test_case;
```

## Test 8: Performance Test

```sql
-- Enable timing
\timing

-- Run a query
SELECT COUNT(*) FROM users;
-- Expected: < 100ms for small tables

-- Check if indexes are used
EXPLAIN ANALYZE
SELECT * FROM users WHERE username = 'admin';
-- Look for: "Index Scan" (good) or "Seq Scan" (needs index)

-- Disable timing
\timing
```

## Test 9: Backup and Restore

```bash
# Backup
pg_dump -U app_auth auth_service_db > test_backup.sql

# Verify backup file
ls -lh test_backup.sql
# Expected: File size > 1KB

# Test restore (on copy of database)
createdb -U postgres auth_service_db_test
psql -U postgres auth_service_db_test < test_backup.sql

# Verify data restored
psql -U app_auth auth_service_db_test -c "SELECT COUNT(*) FROM users;"

# Cleanup
dropdb -U postgres auth_service_db_test
rm test_backup.sql
```

## Test 10: Migration Test (MySQL to PostgreSQL)

```bash
# 1. Export from MySQL
mysqldump -u root -p user_auth_db > mysql_export.sql

# 2. Convert to PostgreSQL (manual or tool)
# Update: AUTO_INCREMENT → sequences
# Update: MySQL functions → PostgreSQL equivalents

# 3. Import to PostgreSQL
psql -U app_auth auth_service_db < converted.sql

# 4. Verify data
psql -U app_auth auth_service_db -c "SELECT COUNT(*) FROM users;"
# Expected: Same count as MySQL

# 5. Check sequences
psql -U postgres auth_service_db -f scripts/postgres/reset_all_sequences.sql

# 6. Test inserts
psql -U app_auth auth_service_db -c "INSERT INTO users (username, email, password) VALUES ('test', 'test@test.com', 'hash');"
# Expected: No "duplicate key" error
```

## Test Checklist

- [ ] Connection works from command line (psql)
- [ ] Schema created with all tables
- [ ] JSONB data type works
- [ ] JSONB queries return correct results
- [ ] Application connects to PostgreSQL
- [ ] JSONB Java bindings work
- [ ] Sequences work correctly
- [ ] Case sensitivity understood
- [ ] Indexes exist and are used
- [ ] Backup/restore works
- [ ] Migration from MySQL complete (if applicable)
- [ ] Unit tests pass

## Performance Metrics

```bash
# Test typical query performance
time psql -U app_auth auth_service_db -c \
  "SELECT * FROM users WHERE username = 'admin';"

# Expected: < 50ms

# Test auth endpoint
time curl http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "password"}'

# Expected: < 100ms
```

