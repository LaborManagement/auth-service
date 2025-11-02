# Virtual Private Database (VPD) / Row-Level Security (RLS) - Master Guide

**Date:** November 2, 2025 | **Status:** ✅ Production Ready | **Version:** 1.0

---

## 📖 QUICK NAVIGATION

- **5-minute overview?** → Jump to [How It Works](#how-it-works)
- **Want to deploy?** → Go to [Implementation (6 Phases)](#-phase-by-phase-implementation)
- **Troubleshooting?** → See [Common Issues & Fixes](#-common-issues--fixes)
- **Need test examples?** → Check [Testing Section](#-testing-validation)

---

## How It Works

### The Big Picture

When a user makes a request, PostgreSQL **automatically filters database rows** based on their permissions. This happens at the database layer, not the application layer.

```
┌─────────────────────────────────────────────────────────────┐
│                 REQUEST FLOW WITH RLS                       │
└─────────────────────────────────────────────────────────────┘

1. User logs in
   └─> JWT created with user_id

2. User makes request (GET /api/payments)
   └─> Authorization header: Bearer <JWT>

3. Spring Security verifies JWT
   └─> Extracts user_id from claims

4. RLSContextFilter (automatic)
   └─> Calls: SELECT auth.set_user_context(user_id)
   └─> Context stored in PostgreSQL transaction variable

5. Application queries database
   └─> SELECT * FROM <data_schema>.<data_table>

6. PostgreSQL RLS Policy intercepts query
   └─> For each row, checks: auth.can_read_row(board_id, employer_id)
   └─> If user has permission → row included
   └─> If user lacks permission → row excluded

7. Only allowed rows returned to user
   └─> Filtering happened at database layer
   └─> User cannot bypass it

8. Transaction ends
   └─> Context automatically cleared
```

### What Gets Filtered?

Every data table has two **tenant key** columns:

| Column | Example | Purpose |
|--------|---------|---------|
| `board_id` | '<BOARD_1>', '<BOARD_2>' | Which board owns this data |
| `employer_id` | '<EMPLOYER_1>', '<EMPLOYER_2>', NULL | Which employer owns this data (NULL = all employers) |

When querying, PostgreSQL checks: *"Is this user allowed to see rows with board_id=X and employer_id=Y?"*

### Who Can See What?

```
```
┌──────────────────────────────────────────────────────────────┐
│                    Permission Matrix                         │
├──────────────────────────────────────────────────────────────┤
│ User | Role | Board | Employer | can_read | can_write       │
├──────────────────────────────────────────────────────────────┤
│ 1    | ADMIN    | <BOARD_1> | NULL     | ✓ | ✓ (sees ALL data)   │
│ 2    | EMPLOYER | <BOARD_1> | <EMPLOYER_1>   | ✓ | ✓ (sees <EMPLOYER_1> only)│
│ 3    | EMPLOYER | <BOARD_1> | <EMPLOYER_2>   | ✓ | ✓ (sees <EMPLOYER_2> only)│
│ 4    | WORKER   | <BOARD_1> | NULL     | ✓ | ✗ (read-only) │
```
```

---

## Initial Setup & Prerequisites

### What You Need

- ✅ PostgreSQL 14+ running
- ✅ superuser (postgres) credentials
- ✅ All 3 services deployed (auth-service, payment-flow-service, reconciliation-service)
- ✅ Existing users in `auth.users` table

### What Gets Created

| Item | Location | Purpose |
|------|----------|---------|
| **4 Database Roles** | PostgreSQL | `app_auth`, `app_payment_flow`, `app_reconciliation`, `data_ops` |
| **Tenant Columns** | 21 data tables | `board_id`, `employer_id` on each row |
| **Performance Indexes** | 21 data tables | Composite index on `(board_id, employer_id)` |
| **ACL Table** | `auth.user_tenant_acl` | Maps users to their allowed (board, employer) combinations |
| **RLS Functions** | `auth` schema | 5 helper functions for context & permission checking |
| **RLS Policies** | 20+ data tables | Automatic row filtering on SELECT/INSERT/UPDATE/DELETE |
| **Java Filter** | Spring Security | `RLSContextFilter` that sets user context on each request |

### Tables That Get Modified

**Data Schemas (~19 tables):**
- Multiple tables across business logic schemas will receive `board_id` and `employer_id` columns
- Refer to migration script: `02-add-tenant-keys.sql` for exact table list

**Auth Schema (2 tables modified/created):**
- `auth.users` - Extended with tenant assignment columns
- `auth.user_tenant_acl` - New table created for permission matrix

---

## 🚀 Phase-by-Phase Implementation

### ⏱️ Timeline
- **Phase 1**: 2-3 minutes
- **Phase 2**: 3-5 minutes + manual backfill (10-20 minutes)
- **Phase 3**: 1-2 minutes + manual ACL population (10-15 minutes)
- **Phase 4**: 1-2 minutes
- **Phase 5**: 2-3 minutes
- **Phase 6**: 10-15 minutes (code changes)
- **Total**: ~1 hour including backfills

---

### Phase 1: Create PostgreSQL Roles

**File**: `infra/db-migration/01-postgres-roles-setup.sql`

**What It Does:**
Creates 4 database roles that applications will use. Each role has `NOBYPASSRLS` flag, meaning they CANNOT bypass RLS policies.

**Execute:**
```bash
# Backup first
pg_dump -U postgres -d <database-name> > backup_phase1.sql

# Run as superuser
psql -U postgres -d <database-name> -f infra/db-migration/01-postgres-roles-setup.sql

# Verify
psql -U postgres -d <database-name> -c "\du"
```

**Expected Output:**
```
            Role name       |         Attributes         
───────────────────────────┼────────────────────────────
app_auth                   | 
app_payment_flow           | 
app_reconciliation         | 
data_ops                   | Superuser, Bypass RLS
```

**Update Application Configs:**

Update service configuration files with new database role credentials:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/<database-name>
    username: <app-specific-role>     # ← Use appropriate role for this service
    password: <secure-password>
    hikari:
      auto-commit: true               # ← CRITICAL for RLS
      maximum-pool-size: 20
```

**Roles to use per service:**
- `auth-service` → `app_auth`
- `payment-flow-service` → `app_payment_flow`
- `reconciliation-service` → `app_reconciliation`

**Test Phase 1:**
```bash
# Restart services - they should connect with new roles
# Check logs for successful connections
# Verify services respond to requests (no DB errors)
```

✅ **Phase 1 Complete** - All services connect successfully with new roles

---

### Phase 2: Add Tenant Key Columns

**File**: `infra/db-migration/02-add-tenant-keys.sql`

**What It Does:**
Adds `board_id` and `employer_id` columns to all data tables. Creates composite indexes for performance.

**Execute:**
```bash
pg_dump -U postgres -d <database-name> > backup_phase2_pre.sql

psql -U postgres -d <database-name> -f infra/db-migration/02-add-tenant-keys.sql

# Verify columns added
psql -U postgres -d <database-name> -c \
  "SELECT COUNT(*) FROM information_schema.columns 
   WHERE column_name IN ('board_id', 'employer_id');"

# Verify indexes created  
psql -U postgres -d <database-name> -c \
  "SELECT COUNT(*) FROM pg_indexes WHERE indexname LIKE '%board_employer%';"
```

**⚠️ CRITICAL MANUAL STEP: Backfill Tenant Keys**

After Phase 2, all rows have NULL values. You MUST populate them with appropriate values:

```sql
-- Example: Set all rows to default board value
UPDATE <schema>.<table> SET board_id = '<BOARD_VALUE>' WHERE board_id IS NULL;

-- Repeat for all affected tables
-- Refer to migration script for complete list of tables

-- Verify backfill
SELECT COUNT(*) FROM <schema>.<table> WHERE board_id IS NULL;
-- Should return: 0
```

**Note:** `employer_id` can be NULL (means accessible to all employers for that board)

✅ **Phase 2 Complete** - All columns populated with actual board values

---

### Phase 3: Create ACL (Access Control List)

**File**: `infra/db-migration/03-create-acl-projection.sql`

**What It Does:**
Creates the `auth.user_tenant_acl` table - the permission matrix that controls which users can see/edit which data.

**Execute:**
```bash
pg_dump -U postgres -d <database-name> > backup_phase3_pre.sql

psql -U postgres -d <database-name> -f infra/db-migration/03-create-acl-projection.sql

# Verify table created
psql -U postgres -d <database-name> -c "SELECT * FROM auth.user_tenant_acl LIMIT 5;"

# Check indexes
psql -U postgres -d <database-name> -c \
  "SELECT indexname FROM pg_indexes WHERE tablename = 'user_tenant_acl';"
```

**⚠️ CRITICAL MANUAL STEP: Populate ACL Table**

Create ACL entries based on your users and their roles. The script provides a template - adjust based on your actual user role mappings:

```sql
-- Insert ACL entries for all enabled users
-- Adjust the role-to-permission mapping as per your business logic
INSERT INTO auth.user_tenant_acl (user_id, board_id, employer_id, can_read, can_write)
SELECT 
  u.id,
  u.board_id,
  u.employer_id,
  CASE 
    WHEN u.role IN (<list-of-read-enabled-roles>) THEN true
    ELSE false
  END as can_read,
  CASE 
    WHEN u.role IN (<list-of-write-enabled-roles>) THEN true
    ELSE false
  END as can_write
FROM auth.users u
WHERE u.is_enabled = true
ON CONFLICT (user_id, board_id, employer_id) DO NOTHING;

-- Verify
SELECT COUNT(*) FROM auth.user_tenant_acl;
```

**Permission Logic (customize per requirements):**
- **Admin roles**: can_read=true, can_write=true (full access)
- **Data management roles**: can_read=true, can_write=true (data access)
- **View-only roles**: can_read=true, can_write=false (read-only)
- **Disabled/revoked**: Remove from ACL (no access)

✅ **Phase 3 Complete** - ACL populated with user permissions

---

### Phase 4: Create RLS Helper Functions

**File**: `infra/db-migration/05-create-sec-schema.sql`

**What It Does:**
Creates PostgreSQL functions in the `auth` schema that handle user context and permission checking.

**Execute:**
```bash
psql -U postgres -d <database-name> -f infra/db-migration/05-create-sec-schema.sql

# Verify functions exist
psql -U postgres -d <database-name> -c \
  "SELECT proname FROM pg_proc 
   WHERE pronamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'auth') 
   AND proname IN ('set_user_context', 'get_user_context', 'can_read_row', 'can_write_row');"

# Test functions
psql -U postgres -d <database-name> -c \
  "SELECT auth.set_user_context('<test-user-id>');
   SELECT auth.get_user_context();"
```

**Functions Created:**

| Function | Purpose |
|----------|---------|
| `auth.set_user_context(user_id)` | Store current user in transaction variable |
| `auth.get_user_context()` | Retrieve current user from transaction |
| `auth.can_read_row(board_id, employer_id)` | Check if user can read rows |
| `auth.can_write_row(board_id, employer_id)` | Check if user can write rows |
| `auth.user_accessible_tenants()` | Debug: list user's allowed tenants |

✅ **Phase 4 Complete** - RLS functions created and tested

---

### Phase 5: Enable RLS Policies

**File**: `infra/db-migration/06-generic-policy-applier.sql`

**What It Does:**
Automatically discovers all tables with tenant keys, enables RLS, and creates filtering policies.

**Execute:**
```bash
pg_dump -U postgres -d <database-name> > backup_phase5_pre.sql

psql -U postgres -d <database-name> -f infra/db-migration/06-generic-policy-applier.sql

# Verify RLS enabled on tables
psql -U postgres -d <database-name> -c \
  "SELECT COUNT(*) FROM pg_tables 
   WHERE rowsecurity = true;"

# Verify policies created
psql -U postgres -d <database-name> -c \
  "SELECT COUNT(*) FROM pg_policies;"
```

**Test RLS is Working:**

```bash
# Set user context
psql -U postgres -d <database-name> -c \
  "SELECT auth.set_user_context('<test-user-id>');
   SELECT COUNT(*) FROM <schema>.<table>;"

# Switch user context
psql -U postgres -d <database-name> -c \
  "SELECT auth.set_user_context('<different-user-id>');
   SELECT COUNT(*) FROM <schema>.<table>;"

# Different counts = RLS is working!
```

✅ **Phase 5 Complete** - RLS policies enabled and filtering works

---

### Phase 6: Java Integration (Spring Security)

**Location**: Shared security library - `RLSContextManager.java` and `RLSContextFilter.java`

Already created:
- `RLSContextManager.java` - manages database context
- `RLSContextFilter.java` - Spring Security filter

**Register Filter in Each Service:**

Update the Spring Security configuration in each service to register the RLSContextFilter:

```java
import com.lbe.shared.security.RLSContextFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            YourJwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        
        http
            .csrf().disable()
            .cors().and()
            .authorizeRequests()
                .antMatchers("/auth/login", "/auth/register").permitAll()
                .anyRequest().authenticated()
            .and()
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new RLSContextFilter(), YourJwtAuthenticationFilter.class);  // ← Add this
        
        return http.build();
    }
}
```

**Apply to all services:**
- Repeat the same pattern for each microservice
- Ensure filter is registered after JWT authentication but before business logic

**Test Integration:**

```bash
# Start services
./mvn spring-boot:run

# Check logs for RLS context initialization
tail -f logs/application.log | grep -i "rls"

# Make authenticated request and verify different users see different data
```

✅ **Phase 6 Complete** - Java filter registered and context being set

---

## Important Considerations

### 1. HikariCP Configuration (CRITICAL)

```yaml
spring:
  datasource:
    hikari:
      auto-commit: true              # ← MUST BE TRUE
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 600000
      max-lifetime: 1800000
      connection-timeout: 30000
      leak-detection-threshold: 60000
```

**Why?** With `auto-commit: true`, each query is its own transaction. The filter must set context on each connection before the query runs.

### 2. Context Lifecycle

```
Request arrives
  ↓
RLSContextFilter: SELECT auth.set_user_context(user_id)
  ↓
Business logic runs (all queries use context)
  ↓
Transaction commits
  ↓
PostgreSQL clears context automatically
  ↓
Connection returns to pool
  ↓
Next request: new context set (if different user)
```

**Important:** Context is **transaction-scoped**. No manual cleanup needed.

### 3. Null Values in Tenant Keys

```
employer_id = NULL → Row accessible to ALL employers for this board
employer_id = '<EMPLOYER_ID>' → Row accessible ONLY to users with that employer access

Policy logic:
WHERE user_id = current_user
  AND board_id = row.board_id
  AND (employer_id IS NULL OR employer_id = row.employer_id)  ← NULL matches all
  AND can_read = true
```

### 4. Performance

- Composite indexes already created: `(board_id, employer_id)`
- RLS policies use these indexes automatically
- Typical query: <10ms with proper indexes
- Monitor with: `EXPLAIN ANALYZE SELECT ...`

### 5. Security Best Practices

✅ **Do:**
- Use `NOBYPASSRLS` on app roles (cannot bypass)
- Test with read-only users to verify restrictions
- Regularly audit `auth.user_tenant_acl`
- Use parameterized queries (automatic with ORM)

❌ **Never:**
- Connect as superuser in app code
- Disable RLS to bypass policies
- Hard-code user_ids in queries
- Trust app-level filtering alone (RLS is the boundary)

### 6. Multi-User Scenario

```sql
-- Example: 3 users with different permission levels

-- User 1 (admin) - sees all data
INSERT INTO auth.user_tenant_acl VALUES 
  (1, '<BOARD_1>', NULL, true, true);

-- User 2 (employer_1) - sees only <EMPLOYER_1> data
INSERT INTO auth.user_tenant_acl VALUES 
  (2, '<BOARD_1>', '<EMPLOYER_1>', true, true);

-- User 3 (employer_2) - sees only <EMPLOYER_2> data
INSERT INTO auth.user_tenant_acl VALUES 
  (3, '<BOARD_1>', '<EMPLOYER_2>', true, true);

-- Result: Each user sees different filtered view automatically
```

---

## 🐛 Common Issues & Fixes

### Issue: "Role does not exist"

```
Error: role "app_*" does not exist
```

**Fix:**
```bash
# Run Phase 1
psql -U postgres -d <database-name> -f infra/db-migration/01-postgres-roles-setup.sql
```

### Issue: "Column board_id does not exist"

```
Error: column "board_id" does not exist
```

**Fix:**
```bash
# Run Phase 2
psql -U postgres -d <database-name> -f infra/db-migration/02-add-tenant-keys.sql
# Then backfill values (see Phase 2)
```

### Issue: "No rows returned when rows exist"

**Cause:** ACL table is empty or has no matching entries

**Fix:**
```sql
-- Check ACL has entries
SELECT COUNT(*) FROM auth.user_tenant_acl;

-- If 0, populate it (Phase 3)
-- Use appropriate role-to-permission mapping for your system
INSERT INTO auth.user_tenant_acl (user_id, board_id, employer_id, can_read, can_write)
VALUES (<user-id>, '<board-value>', '<employer-value>', true, true);

-- Verify
SELECT auth.set_user_context('<user-id>');
SELECT COUNT(*) FROM <schema>.<table>;
-- Should now return rows
```

### Issue: "RLS policy not filtering"

**Check:**
```sql
-- 1. Is RLS enabled?
SELECT rowsecurity FROM pg_tables 
WHERE schemaname='<schema>' AND tablename='<table>';
-- Should be: true

-- 2. Do policies exist?
SELECT policyname FROM pg_policies 
WHERE schemaname='<schema>' AND tablename='<table>';
-- Should show: read and write policies

-- 3. Is context being set?
SELECT auth.set_user_context('<user-id>');
SELECT auth.get_user_context();
-- Should return: <user-id>
```

### Issue: "Function auth.set_user_context not found"

**Fix:**
```bash
# Run Phase 4
psql -U postgres -d <database-name> -f infra/db-migration/05-create-sec-schema.sql

# Test
psql -U postgres -d <database-name> -c "SELECT auth.set_user_context('<test-id>');"
```

### Issue: "RLSContextFilter not setting context in logs"

**Check:**
```bash
# 1. Is filter registered in SecurityConfig?
grep -r "RLSContextFilter" <service>/src/main/java/

# 2. Check logs for context initialization
grep -i "rls" <service>/logs/application.log

# 3. If not found, add to SecurityConfig
http.addFilterAfter(new RLSContextFilter(), JwtAuthenticationFilter.class);
```

---

## ✅ Testing & Validation

### Quick Test (5 minutes)

```bash
# 1. Set user context
psql -U postgres -d <database-name> -c \
  "SELECT auth.set_user_context('<user-id>');"

# 2. Query data
psql -U postgres -d <database-name> -c \
  "SELECT COUNT(*) FROM <schema>.<table>;"

# 3. Check with different user
psql -U postgres -d <database-name> -c \
  "SELECT auth.set_user_context('<different-user-id>');
   SELECT COUNT(*) FROM <schema>.<table>;"

# Different counts = RLS working!
```

### Full Validation Checklist

- [ ] Phase 1: All application roles created (verify with `\du`)
- [ ] Phase 2: All tables have board_id/employer_id columns
- [ ] Phase 2: All columns populated (no NULLs)
- [ ] Phase 3: ACL table exists and has entries
- [ ] Phase 4: RLS functions created and executable
- [ ] Phase 5: RLS enabled on tables (verify with pg_tables)
- [ ] Phase 5: Policies created for SELECT/INSERT/UPDATE/DELETE
- [ ] Phase 5: Filtering works (different row counts per user)
- [ ] Phase 6: RLSContextFilter registered in SecurityConfig
- [ ] Phase 6: Logs show context initialization
- [ ] Phase 6: Different users see different data via API

### Performance Test

```bash
# Query with EXPLAIN to verify index usage
psql -U postgres -d <database-name> -c \
  "SET role <app-role>;
   SELECT auth.set_user_context('<user-id>');
   EXPLAIN (ANALYZE) SELECT * FROM <schema>.<table> LIMIT 10;"

# Look for "Index Scan" or "Index Only Scan" (good)
# Look for "Seq Scan" with Filter (acceptable)
```

---

## Quick Commands Reference

### Verify Setup

```bash
# Check roles
psql -U postgres -d <database-name> -c "\du"

# Check columns added
psql -U postgres -d <database-name> -c \
  "SELECT COUNT(*) FROM information_schema.columns 
   WHERE column_name IN ('board_id', 'employer_id');"

# Check ACL entries
psql -U postgres -d <database-name> -c \
  "SELECT COUNT(*) FROM auth.user_tenant_acl;"

# Check RLS enabled
psql -U postgres -d <database-name> -c \
  "SELECT COUNT(*) FROM pg_tables 
   WHERE rowsecurity = true;"

# Check policies
psql -U postgres -d <database-name> -c \
  "SELECT COUNT(*) FROM pg_policies;"
```

### Debug User Permissions

```bash
# Set user context
psql -U postgres -d <database-name> -c \
  "SELECT auth.set_user_context('<user-id>');"

# See accessible tenants
psql -U postgres -d <database-name> -c \
  "SELECT * FROM auth.user_accessible_tenants();"

# Test read permission
psql -U postgres -d <database-name> -c \
  "SELECT auth.can_read_row('<board-value>', '<employer-value>');"

# Test write permission
psql -U postgres -d <database-name> -c \
  "SELECT auth.can_write_row('<board-value>', '<employer-value>');"
```

### Backup Commands

```bash
# Before Phase 1
pg_dump -U postgres -d <database-name> > backup_phase1_pre.sql

# Before Phase 2
pg_dump -U postgres -d <database-name> > backup_phase2_pre.sql

# Before Phase 5 (RLS policies - critical)
pg_dump -U postgres -d <database-name> > backup_phase5_pre.sql

# Full backup for safety
pg_dump -U postgres -d <database-name> --format=custom -f backup_full.bak
```

### Rollback (if needed)

```bash
# Drop policies on a specific table
DROP POLICY IF EXISTS <table>_std_read ON <schema>.<table>;
DROP POLICY IF EXISTS <table>_std_write ON <schema>.<table>;

# Disable RLS on a specific table
ALTER TABLE <schema>.<table> DISABLE ROW LEVEL SECURITY;

# Drop functions
DROP FUNCTION IF EXISTS auth.set_user_context(TEXT);
DROP FUNCTION IF EXISTS auth.can_read_row(VARCHAR, VARCHAR);

# Drop ACL table
DROP TABLE IF EXISTS auth.user_tenant_acl;

# Restore from backup
pg_restore -U postgres -d <database-name> backup_full.bak
```

---

## Success Checklist

When everything is working:

- ✅ Different users see different rows
- ✅ Read-only users cannot modify data
- ✅ Queries use indexes (good performance)
- ✅ Logs show context being set
- ✅ RLS policies appear in EXPLAIN plans
- ✅ No way to bypass RLS (database enforces it)

---

## Summary Table

| Phase | Duration | File | Key Action |
|-------|----------|------|-----------|
| 1 | 2-3 min | `01-postgres-roles-setup.sql` | Create roles |
| 2 | 3-5 min + backfill | `02-add-tenant-keys.sql` | Add columns, backfill values |
| 3 | 1-2 min + populate | `03-create-acl-projection.sql` | Create ACL table, populate permissions |
| 4 | 1-2 min | `05-create-sec-schema.sql` | Create RLS functions |
| 5 | 2-3 min | `06-generic-policy-applier.sql` | Enable RLS, create policies |
| 6 | 10-15 min | `SecurityConfig.java` (3 services) | Register RLSContextFilter |
| **Total** | **~45 min** | | **Production Ready** |

---

## Need Help?

| Question | Answer |
|----------|--------|
| How does filtering work? | See [How It Works](#how-it-works) section |
| How do I deploy? | Follow [Phase-by-Phase Implementation](#-phase-by-phase-implementation) |
| Something broke! | See [Common Issues & Fixes](#-common-issues--fixes) |
| Is it working? | See [Testing & Validation](#-testing--validation) |
| What's the command? | See [Quick Commands Reference](#quick-commands-reference) |

---

**Last Updated:** November 2, 2025 | **Status:** Production Ready | **Questions?** Check the section above.
