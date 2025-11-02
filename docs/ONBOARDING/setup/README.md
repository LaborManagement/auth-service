# SQL Initialization Scripts - README

**Last Updated:** November 2, 2025  
**Version:** Phase 8 Complete

---

## Overview

This directory contains all SQL scripts for initializing the RBAC (Role-Based Access Control) system. These scripts are referenced by the main `SETUP_GUIDE.md` and should be executed in the specified order to properly configure the system.

---

## Script Execution Order

Execute the scripts in this exact order:

### Phase 1-6: Core System Setup (Required)

```bash
# Phase 1: Create 7 roles
psql -U postgres -d auth_service_db -f docs/ONBOARDING/setup/01_create_roles.sql

# Phase 2: Create 98 capabilities
psql -U postgres -d auth_service_db -f docs/ONBOARDING/setup/02_create_capabilities.sql

# Phase 3: Create 7 policies
psql -U postgres -d auth_service_db -f docs/ONBOARDING/setup/03_create_policies.sql

# Phase 4: Link policies to capabilities (288 links)
psql -U postgres -d auth_service_db -f docs/ONBOARDING/setup/04_link_policies_to_capabilities.sql

# Phase 5: Create 7 seed users
psql -U postgres -d auth_service_db -f docs/ONBOARDING/setup/05_create_seed_users.sql

# Phase 6: Assign users to roles
psql -U postgres -d auth_service_db -f docs/ONBOARDING/setup/06_assign_users_to_roles.sql
```

### Phase 7: VPD Configuration

```bash
# Configure Virtual Private Data (Row-Level Security)
psql -U postgres -d auth_service_db -f docs/ONBOARDING/setup/08_configure_vpd.sql
```

### Phase 8: Verification

```bash
# Verify setup completeness
psql -U postgres -d auth_service_db -f docs/ONBOARDING/setup/07_verify_setup.sql
```

---

## Script Details

### 01_create_roles.sql

**Purpose:** Create the 7 core roles  
**Creates:**
- PLATFORM_BOOTSTRAP (56% of capabilities - system initialization)
- ADMIN_TECH (52% of capabilities - technical administration)
- ADMIN_OPS (43% of capabilities - operations management)
- BOARD (17% of capabilities - board financial approvals)
- EMPLOYER (19% of capabilities - employer request management)
- WORKER (14% of capabilities - worker payment operations)
- TEST_USER (51% of capabilities - QA testing)

**Duration:** < 1 second  
**Idempotent:** ✅ Yes

---

### 02_create_capabilities.sql

**Purpose:** Create all 98 atomic capabilities  
**Organized by 13 modules:**
- User Management (5)
- Payment File Management (8)
- Payment Request Management (9)
- Worker Operations (6)
- Employer Operations (5)
- Board Operations (7)
- RBAC Role Management (6)
- RBAC Policy Management (7)
- RBAC Capability Management (6)
- API Endpoint Management (7)
- UI Page Management (8)
- UI Action Management (7)
- System & Reporting (8)

**Naming:** `<domain>.<subject>.<action>` format  
**Examples:** `user.account.create`, `payment.file.upload`, `rbac.policy.link-capability`

**Duration:** < 1 second  
**Idempotent:** ✅ Yes

---

### 03_create_policies.sql

**Purpose:** Create 7 policies (one per role)  
**Creates:**
- PLATFORM_BOOTSTRAP_POLICY (system initialization)
- ADMIN_TECH_POLICY (technical administration)
- ADMIN_OPS_POLICY (operations management)
- BOARD_POLICY (board financial approvals)
- EMPLOYER_POLICY (employer request validation)
- WORKER_POLICY (worker payment operations)
- TEST_USER_POLICY (QA testing access)

**Duration:** < 1 second  
**Idempotent:** ✅ Yes

---

### 04_link_policies_to_capabilities.sql

**Purpose:** Link policies to capabilities (Layer 1 Authorization)  
**Total Links:** 288 (55+51+42+17+19+14+50)

**Distribution:**
- PLATFORM_BOOTSTRAP_POLICY → 55 capabilities
- ADMIN_TECH_POLICY → 51 capabilities
- ADMIN_OPS_POLICY → 42 capabilities
- BOARD_POLICY → 17 capabilities
- EMPLOYER_POLICY → 19 capabilities
- WORKER_POLICY → 14 capabilities
- TEST_USER_POLICY → 50 capabilities

**Duration:** 1-2 seconds  
**Idempotent:** ✅ Yes

---

### 05_create_seed_users.sql

**Purpose:** Create 7 test users for system testing  
**Users Created:**
- platform.bootstrap (PLATFORM_BOOTSTRAP role)
- admin.tech (ADMIN_TECH role)
- admin.ops (ADMIN_OPS role)
- board1 (BOARD role)
- employer1 (EMPLOYER role)
- worker1 (WORKER role)
- test.user (TEST_USER role)

**⚠️ IMPORTANT:** Change all passwords immediately!

**Duration:** < 1 second  
**Idempotent:** ✅ Yes

---

### 06_assign_users_to_roles.sql

**Purpose:** Link users to roles (inherit capabilities)  
**Assignments:**
- platform.bootstrap → PLATFORM_BOOTSTRAP
- admin.tech → ADMIN_TECH
- admin.ops → ADMIN_OPS
- board1 → BOARD
- employer1 → EMPLOYER
- worker1 → WORKER
- test.user → TEST_USER

**Duration:** < 1 second  
**Idempotent:** ✅ Yes

---

### 07_verify_setup.sql

**Purpose:** Comprehensive verification of all phases  
**Verifies:**
- ✅ All 7 roles created
- ✅ All 98 capabilities defined
- ✅ All 7 policies created
- ✅ All 288 policy-capability links
- ✅ All 7 seed users created
- ✅ All 7 user-role assignments
- ✅ No orphaned data
- ✅ Summary report

**Duration:** < 2 seconds

---

### 08_configure_vpd.sql

**Purpose:** Configure Virtual Private Data (VPD) and Row-Level Security

**VPD Types Configured:**
- **USER_SCOPED (WORKER):** User sees only their own data (tenant_id = user_id)
- **TENANT_SCOPED (EMPLOYER):** User sees organization's data (tenant_id = organization_id)
- **NO_FILTER (BOARD/ADMIN):** Full access without restrictions

**What Gets Configured:**
- WORKER VPD: worker1 sees only own payment records
- EMPLOYER VPD: employer1 sees only organization 1's data
- BOARD VPD: board1 has full access (NO_FILTER)
- ADMIN VPD: admin.tech and admin.ops have full access (NO_FILTER)

**Implementation:**
- Application-level VPD via UserTenantAcl table
- Optional database-level RLS policies (for enhanced security)

**Verification Queries Included:**
- Check all VPD assignments
- Verify WORKER user-scoped configuration
- Verify EMPLOYER tenant-scoped configuration

**Duration:** < 1 second  
**Idempotent:** ✅ Yes

## One-Command Bootstrap

```bash
for script in 01_create_roles.sql 02_create_capabilities.sql 03_create_policies.sql \
              04_link_policies_to_capabilities.sql 05_create_seed_users.sql \
              06_assign_users_to_roles.sql 08_configure_vpd.sql; do
  echo "Executing $script..."
  psql -U postgres -d auth_service_db -f docs/ONBOARDING/setup/$script || exit 1
done

# Verify
psql -U postgres -d auth_service_db -f docs/ONBOARDING/setup/07_verify_setup.sql
```

---

## Important Notes

### ⚠️ PLATFORM_BOOTSTRAP Account

Disable after setup:

```bash
UPDATE users SET is_active = false WHERE username = 'platform.bootstrap';
```

### ⚠️ Default Passwords

Change all seed user passwords immediately in production.

### ⚠️ VPD Configuration

WORKER and EMPLOYER roles require UserTenantAcl setup for data isolation.

---

## Related Documentation

- **[SETUP_GUIDE.md](../SETUP_GUIDE.md)** - Main setup guide with phase descriptions
- **[ROLES.md](../ROLES.md)** - Comprehensive role documentation
- **[ARCHITECTURE.md](../ARCHITECTURE.md)** - System architecture and RBAC design
- **[PHASE3_CAPABILITIES_DEFINITION.md](../../PHASE3_CAPABILITIES_DEFINITION.md)** - All 98 capabilities
- **[PHASE4_POLICY_CAPABILITY_MAPPINGS.md](../../PHASE4_POLICY_CAPABILITY_MAPPINGS.md)** - Capability mappings
- **[PHASE5_ENDPOINT_POLICY_MAPPINGS.md](../../PHASE5_ENDPOINT_POLICY_MAPPINGS.md)** - Endpoint access matrix

---

**Last Updated:** November 2, 2025  
**Status:** ✅ Production Ready  
**Phase:** 8 Complete

### 7 Roles
- `PLATFORM_BOOTSTRAP` - Full system access
- `BASIC_USER` - Resource access only
- `WORKER` - Worker functionality
- `EMPLOYER` - Employer functionality
- `BOARD` - Board functionality
- `ADMIN_TECH` - All except worker/employer/board
- `ADMIN_OPS` - Worker/employer/board except RBAC

### 7 Users
- `platform.bootstrap` → PLATFORM_BOOTSTRAP
- `basic.user1` → BASIC_USER
- `worker1` → WORKER
- `employer1` → EMPLOYER
- `board1` → BOARD
- `admin.tech` → ADMIN_TECH
- `admin.ops` → ADMIN_OPS

### 26 Capabilities
Organized by module:
- **user:** create, read, update, delete
- **worker:** create, read, update, delete
- **employer:** create, read, update, delete
- **board:** create, read, update, delete, vote
- **rbac:** create, read, update, delete
- **report:** create, read, download
- **system:** view, admin
- **resource:** read, write

### 7 Policies
Each linking a role to its capabilities:
- `bootstrap_all_policy` - All 26 capabilities
- `basic_user_policy` - resource.read, resource.write
- `worker_policy` - worker.* + resource.*
- `employer_policy` - employer.* + resource.*
- `board_policy` - board.* + resource.*
- `admin_tech_policy` - All except worker/employer/board
- `admin_ops_policy` - worker/employer/board + system.view

## Default Passwords

⚠️ **IMPORTANT:** Change all passwords immediately in production!

```
platform.bootstrap: Platform!Bootstrap1
basic.user1:        BasicUser!2025
worker1:            Worker!2025
employer1:          Employer!2025
board1:             Board!2025
admin.tech:         AdminTech!2025
admin.ops:          AdminOps!2025
```

To change a password:

```bash
# Generate new BCrypt hash
mvn exec:java -Dexec.mainClass="org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder" \
              -Dexec.args="YourNewPassword"

# Update in database
psql -U postgres -d auth_service_db << EOF
UPDATE users 
SET password = '\$2a\$12\$<NEW_HASH_HERE>'
WHERE username = 'worker1';
EOF
```

## Verification

After running scripts, verify success:

```bash
# Quick check
psql -U postgres -d auth_service_db -c "
SELECT 
    (SELECT COUNT(*) FROM roles) as roles,
    (SELECT COUNT(*) FROM users) as users,
    (SELECT COUNT(*) FROM capabilities) as capabilities,
    (SELECT COUNT(*) FROM policies) as policies,
    (SELECT COUNT(*) FROM user_role_assignment) as assignments
;"

# Expected: 7 roles, 7 users, 26+ capabilities, 7 policies, 7 assignments
```

## Troubleshooting

### "User already exists" Error
```sql
-- Delete existing user and assignments
DELETE FROM user_role_assignment WHERE user_id = (SELECT id FROM users WHERE username = 'worker1');
DELETE FROM users WHERE username = 'worker1';

-- Then re-run the script
```

### "Role not found" Error
Ensure scripts are run in order (roles before users, capabilities before policies, etc.)

### Transaction Rollback
If a script fails:
```sql
-- Connection will auto-rollback on error
-- Check the error message above the error statement
-- Fix the issue and re-run
```

## Script Structure

Each script follows this pattern:

```sql
-- ============================================================================
-- Descriptive Title
-- ============================================================================
-- What this script does and when to use it

-- Usage instructions

BEGIN;

-- Phase 1: Main operations
-- Phase 2: Verification
-- etc.

COMMIT;

-- Verification output
SELECT ...
```

## Next Steps

1. ✅ Run onboarding scripts
2. ✅ Verify setup with `verify_onboarding.sql`
3. ✅ Test user authentication
4. ✅ Test role-based access control
5. ✅ Set up RLS policies (see [VPD Setup](../VPD/setup.md))
6. ✅ Change default passwords
7. ✅ Configure API endpoints with policies

## Related Files

- **[Setup Guide](../setup.md)** - Detailed phase-by-phase explanation
- **[Troubleshooting](../troubleshoot.md)** - Common issues and solutions
- **[Testing Guide](../testing.md)** - How to test the setup
- **[Role Reference](../ROLES.md)** - Details on each role

## Requirements

- PostgreSQL installed and running
- Auth service database created
- Flyway migrations executed
- Connection to PostgreSQL as postgres user

## Support

For issues:
1. Check [Troubleshooting Guide](../troubleshoot.md)
2. Review [Testing Guide](../testing.md)
3. Verify database connection: `psql -U postgres -d auth_service_db -c "SELECT 1"`
4. Check application logs for errors

---

**Last Updated:** November 2, 2025
