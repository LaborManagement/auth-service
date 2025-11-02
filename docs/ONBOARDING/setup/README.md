# SQL Initialization Scripts - README

**Last Updated:** November 2, 2025  
**Status:** Clean execution order (15 scripts)

---

## Overview

This directory contains all SQL scripts for initializing the complete RBAC (Role-Based Access Control) and UI system. Execute scripts **01-15 in order** for a complete setup.

---

## Quick Start

```bash
# From repository root, execute:
for script in 01 02 03 04 05 06 07 08 09 10 11 12 13 14 15; do
  PGPASSWORD=root psql -U app_auth -d labormanagement -h localhost \
    -f docs/ONBOARDING/setup/${script}_*.sql || exit 1
done
```

---

## Execution Order (15 Phases)

| # | Script | Purpose |
|---|--------|---------|
| 01 | `01_create_roles.sql` | Create 7 core roles |
| 02 | `02_create_ui_pages.sql` | Create 15 UI pages with hierarchy |
| 03 | `03_create_capabilities.sql` | Create 98 atomic capabilities |
| 04 | `04_create_policies.sql` | Create 7 role policies |
| 05 | `05_link_policies_to_capabilities.sql` | Link capabilities to policies (~288 links) |
| 06 | `06_create_seed_users.sql` | Create 7 seed test users |
| 07 | `07_assign_users_to_roles.sql` | Assign users to roles |
| 08 | `08_create_page_actions.sql` | Link UI pages to capabilities (22 actions) |
| 09 | `09_verify_setup.sql` | Verify phases 01-08 completeness |
| 10 | `10_configure_vpd.sql` | Setup Row-Level Security (RLS) |
| 11 | `11_fix_seed_user_passwords.sql` | Set seed user passwords |
| 12 | `12_create_basic_policy.sql` | Create BASIC_POLICY (6 capabilities) |
| 13 | `13_assign_basic_policy_to_users.sql` | Assign BASIC_POLICY to all users |
| 14 | `14_link_critical_endpoints_to_basic_policy.sql` | Link 5 authorization endpoints |
| 15 | `15_link_board_page_actions.sql` | Link dashboard actions for board role |

---

## Key Points

- **Idempotent:** Phases 01-13 are idempotent (safe to rerun)
- **Order Matters:** Execute 01→15 sequentially
- **Total Time:** ~15 seconds for complete setup
- **Schema:** All scripts use `SET search_path TO auth;`

---

## Post-Setup Verification

```sql
-- Check all components created
SELECT 'Roles' as item, COUNT(*) as count FROM auth.roles WHERE is_active = true
UNION ALL
SELECT 'Users', COUNT(*) FROM auth.users WHERE is_active = true
UNION ALL
SELECT 'Capabilities', COUNT(*) FROM auth.capabilities WHERE is_active = true
UNION ALL
SELECT 'Policies', COUNT(*) FROM auth.policies WHERE is_active = true
UNION ALL
SELECT 'UI Pages', COUNT(*) FROM auth.ui_pages WHERE is_active = true
UNION ALL
SELECT 'Page Actions', COUNT(*) FROM auth.page_actions WHERE is_active = true;

-- Expected output:
-- Roles        | 7+
-- Users        | 15+
-- Capabilities | 98
-- Policies     | 8
-- UI Pages     | 15
-- Page Actions | 25+
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

