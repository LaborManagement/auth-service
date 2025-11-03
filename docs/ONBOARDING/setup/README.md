# SQL Initialization Scripts - README

**Last Updated:** November 3, 2025  
**Status:** Complete RBAC setup with endpoint registration (11 scripts)

---

## Overview

This directory contains all SQL scripts for initializing the complete RBAC (Role-Based Access Control) and UI system. Execute scripts **00-11 in order** for a complete setup.

The system implements a **dual-relationship model** for authorization:
- **Backend Authorization**: User → Role → Policy → Capability ↔ Endpoint
- **Frontend UI Binding**: PageAction → Endpoint (direct FK for API calls)

---

## Quick Start

```bash
# From repository root, execute in order:
for i in 00 01 02 03 04 05 06 07 08 09 10 11; do
  PGPASSWORD=root psql -U root -d labormanagement -h localhost \
    -f docs/onboarding/setup/${i}_*.sql || exit 1
done
```

---

## Execution Order (12 Scripts)

| # | Script | Purpose |
|---|--------|---------|
| 00 | `00_register_endpoints.sql` | Register 70 endpoints (auth: 14, admin: 51, internal: 5) |
| 01 | `01_create_roles.sql` | Create 3 bootstrap roles (BASIC_USER, BUSINESS_ADMIN, TECHNICAL_BOOTSTRAP) |
| 02 | `02_create_ui_pages.sql` | Create 10 UI pages with hierarchy |
| 03 | `03_create_capabilities.sql` | Create 91 atomic capabilities |
| 04 | `04_create_policies.sql` | Create 17 granular policies |
| 05 | `05_link_policies_to_capabilities.sql` | Link 56 policy-capability pairs |
| 06 | `06_link_endpoints_to_policies.sql` | Link 70 endpoint-policy pairs |
| 07 | `07_create_seed_users.sql` | Create 2 bootstrap users (business.admin, tech.bootstrap) |
| 08 | `08_assign_users_to_roles.sql` | Assign 4 user-role relationships |
| 09 | `09_create_page_actions.sql` | Create 16 page actions (UI → capability + endpoint) |
| 10 | `10_fix_seed_user_passwords.sql` | Set seed user passwords (bcrypt) |
| 11 | `11_add_missing_user_endpoints.sql` | Add PUT/DELETE user endpoints |

---

## Key Points

- **Idempotent:** Most scripts use ON CONFLICT or safe insertions
- **Order Matters:** Execute 00→11 sequentially
- **Total Time:** ~10 seconds for complete setup
- **Schema:** All scripts use `auth` schema
- **Dual Relationships:**
  - **Authorization**: Capability ↔ Endpoint (via policy_capabilities + endpoint_policies)
  - **UI Binding**: PageAction → Endpoint (direct foreign key)

---

## Bootstrap Roles & Users

### Roles
| Role | Purpose | Policies | Capabilities |
|------|---------|----------|--------------|
| BASIC_USER | Minimal access | 1 | 2 (auth APIs only) |
| BUSINESS_ADMIN | User/role/policy management | 3 | 8 (user CRUD) |
| TECHNICAL_BOOTSTRAP | Full system access | 16 | 54 (all modules) |

### Users
| Username | Password | Roles |
|----------|----------|-------|
| business.admin | tech123 | BASIC_USER + BUSINESS_ADMIN |
| tech.bootstrap | tech123 | BASIC_USER + TECHNICAL_BOOTSTRAP |

**⚠️ Change passwords immediately in production!**

---

## Authorization Flow

### Backend (Endpoint Security)
```
Request → JWT → User → Roles → Policies → Capabilities ↔ Endpoints
                                                          ↓
                                                    Allow/Deny
```

### Frontend (UI Visibility)
```
Page Load → User Capabilities → PageActions → Endpoints → API Calls
                                      ↓
                            Show/Hide Buttons
```

### Key Tables
- `endpoints`: 72 API endpoints (70 original + 2 added in script 11)
- `policies`: 17 permission groups
- `capabilities`: 91 atomic permissions
- `policy_capabilities`: Capability → Policy links
- `endpoint_policies`: Endpoint → Policy links
- `page_actions`: UI actions with capability_id + endpoint_id
- `user_roles`: User → Role assignments

---

## Post-Setup Verification

```sql
-- Check all components created
SELECT 'Roles' as item, COUNT(*) as count FROM auth.roles
UNION ALL
SELECT 'Users', COUNT(*) FROM auth.users
UNION ALL
SELECT 'Capabilities', COUNT(*) FROM auth.capabilities
UNION ALL
SELECT 'Policies', COUNT(*) FROM auth.policies
UNION ALL
SELECT 'Endpoints', COUNT(*) FROM auth.endpoints
UNION ALL
SELECT 'UI Pages', COUNT(*) FROM auth.ui_pages
UNION ALL
SELECT 'Page Actions', COUNT(*) FROM auth.page_actions;

-- Expected output:
-- Roles        | 3
-- Users        | 2
-- Capabilities | 91
-- Policies     | 17
-- Endpoints    | 72
-- UI Pages     | 10
-- Page Actions | 16
```

### Verify Complete Linkage
```sql
-- Check page actions have both capability and endpoint
SELECT 
    pa.id,
    pa.label,
    CASE WHEN pa.capability_id IS NOT NULL THEN '✓' ELSE '✗' END as has_capability,
    CASE WHEN pa.endpoint_id IS NOT NULL THEN '✓' ELSE '✗' END as has_endpoint
FROM auth.page_actions pa
ORDER BY pa.id;

-- All should show ✓ for both columns
```

### Test Authorization Chain
```sql
-- Verify User Management page actions
SELECT 
    pa.id,
    pa.label,
    c.name as capability,
    e.method,
    e.path,
    STRING_AGG(DISTINCT p.name, ', ') as policies
FROM auth.page_actions pa
LEFT JOIN auth.capabilities c ON pa.capability_id = c.id
LEFT JOIN auth.endpoints e ON pa.endpoint_id = e.id
LEFT JOIN auth.policy_capabilities pc ON c.id = pc.capability_id
LEFT JOIN auth.policies p ON pc.policy_id = p.id
WHERE pa.page_id = 2  -- User Management page
GROUP BY pa.id, pa.label, c.name, e.method, e.path
ORDER BY pa.id;
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Connection refused | Verify PostgreSQL running: `docker ps \| grep postgres` |
| User not found | Run phases 01-11 first |
| Pages empty | Run phase 08 (page actions) |
| No permissions | Run phase 13 (BASIC_POLICY) |

---

## Notes

- Seed users: Change default passwords immediately
- PLATFORM_BOOTSTRAP: Disable after setup
- Database credentials: `app_auth` / `root` for `labormanagement` database
- Default hostname: `localhost:5432`

