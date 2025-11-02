# Virtual Private Database (VPD) / Row-Level Security (RLS)

**Status:** ✅ Production Ready | **Last Updated:** November 2, 2025

## 📌 Quick Start

VPD/RLS automatically filters database rows based on user permissions at the **database layer**. Users cannot bypass it.

### How It Works

```mermaid
flowchart TD
    A[User authenticates] --> B[Spring Security validates JWT]
    B --> C[RLSContextFilter<br/>sets user context]
    C --> D[Application executes query]
    D --> E[PostgreSQL RLS policies evaluate row access]
    E --> F[Only authorised rows returned]
```

### Architecture Diagram

```mermaid
sequenceDiagram
    participant User
    participant API as Spring Security
    participant Filter as RLSContextFilter
    participant DB as PostgreSQL

    User->>API: Request + JWT
    API->>API: Validate token & extract user_id
    API->>Filter: Pass authenticated principal
    Filter->>DB: SELECT auth.set_user_context(user_id)
    API->>DB: Business query
    DB-->>API: Rows filtered by RLS policies
    API-->>User: Response with authorised data only
```

## 🎯 Key Concepts

### Tenant Keys

Every data table has two columns that control access:

| Column | Type | Purpose |
|--------|------|---------|
| `board_id` | VARCHAR | Which board owns this data (e.g., 'BOARD_1', 'BOARD_2') |
| `employer_id` | VARCHAR | Which employer owns this data (NULL = accessible to all employers) |

### Permission Matrix

| User ID | Role | Board | Employer | Can Read | Can Write |
| --- | --- | --- | --- | --- | --- |
| 1 | ADMIN | BOARD_1 | NULL | ✓ All | ✓ All |
| 8 | WORKER | BOARD_1 | EMP_2 | ✓ EMP_2 | ✗ |
| 2 | EMPLOYER | BOARD_1 | EMP_1 | ✓ EMP_1 | ✓ EMP_1 |

**Result:** User 8 with `BOARD_1 + EMP_2` sees only rows with that combination.

## 📂 Documentation Structure

- **[Setup Guide](setup.md)** - How to implement VPD/RLS from scratch
- **[Testing Guide](testing.md)** - How to verify RLS is working correctly
- **[Troubleshooting](troubleshoot.md)** - Common issues and fixes
- **SQL Test Scripts** - In `testing/` folder

## 🧪 Testing (Most Common Question)

### Why does superuser see ALL data?

**Root cause:** Superuser bypasses RLS by default. This is PostgreSQL's expected behavior.

**Solution:** Always test with the application role, not superuser:

```sql
-- ✅ CORRECT: This respects RLS
SET ROLE app_payment_flow;
SELECT auth.set_user_context('8');
SELECT * FROM payment_flow.worker_uploaded_data;

-- ❌ WRONG: Superuser bypasses RLS
SELECT auth.set_user_context('8');
SELECT * FROM payment_flow.worker_uploaded_data;
```

## ⚙️ Implementation Checklist

- [ ] Phase 1: Database roles created (`app_auth`, `app_payment_flow`, etc.)
- [ ] Phase 2: Tenant columns added to all data tables
- [ ] Phase 2: Columns backfilled with actual board/employer values
- [ ] Phase 3: ACL table (`auth.user_tenant_acl`) populated
- [ ] Phase 4: RLS functions created (`set_user_context`, `can_read_row`, etc.)
- [ ] Phase 5: RLS policies enabled on all data tables
- [ ] Phase 6: `RLSContextFilter` registered in Spring Security
- [ ] Verification: Different users see different data

## 🔍 Common Troubleshooting

| Issue | Cause | Fix |
|-------|-------|-----|
| Superuser sees BRD1, User 8 sees only BRD2 | Superuser bypasses RLS | Use `SET ROLE app_payment_flow` when testing |
| No rows returned when they should exist | ACL table is empty | Populate `auth.user_tenant_acl` with user permissions |
| RLS policies not filtering | Policies not enabled | Run Phase 5 migration script |
| User sees rows they shouldn't | Wrong ACL entries | Verify `user_tenant_acl` has correct board/employer combos |

## 📖 Next Steps

1. **New to VPD?** → Read [Setup Guide](setup.md)
2. **Want to test?** → See [Testing Guide](testing.md)
3. **Something broken?** → Check [Troubleshooting](troubleshoot.md)
