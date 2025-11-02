# System Setup & Initialization Guide

**Last Updated:** November 2, 2025  
**Version:** Phase 4-5 Complete (7 Roles, 98 Capabilities, 100+ Endpoints)

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Quick Start](#quick-start)
4. [Detailed Setup Steps](#detailed-setup-steps)
5. [Verification Checklist](#verification-checklist)
6. [Troubleshooting](#troubleshooting)
7. [Post-Setup Configuration](#post-setup-configuration)

---

## Overview

This guide walks through complete RBAC (Role-Based Access Control) and RLS (Row-Level Security) system initialization for the payment reconciliation platform.

### What Gets Created

```
┌─────────────────────────────────────────────────────┐
│ RBAC System Initialization                          │
├─────────────────────────────────────────────────────┤
│ • 7 Roles (PLATFORM_BOOTSTRAP, ADMIN_TECH,          │
│           ADMIN_OPS, BOARD, EMPLOYER, WORKER,       │
│           TEST_USER)                                │
│ • 98 Atomic Capabilities (organized by 13 modules)  │
│ • 7 Policies (one per role)                         │
│ • 100+ API Endpoints (registered & linked)          │
│ • 36 UI Pages (for frontend navigation)             │
│ • 7 Seed Users (one per role)                       │
│ • VPD Security Policies (for data isolation)        │
└─────────────────────────────────────────────────────┘
```

### Architecture Flow

```
┌──────────────┐
│   User       │
└──────┬───────┘
       │
       ▼
┌──────────────────────────────────────────┐
│  User → Role Assignment                  │
└──────┬───────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────┐
│  Role → Policy Assignment                │
│  (PLATFORM_BOOTSTRAP, ADMIN_TECH, etc.)  │
└──────┬───────────────────────────────────┘
       │
       ▼ (Layer 1: Capability-Policy)
┌──────────────────────────────────────────┐
│  Policy → Capabilities                   │
│  (98 atomic permissions)                 │
└──────┬───────────────────────────────────┘
       │
       ├─→ (Layer 2: Endpoint-Policy)
       │   Policy → Endpoints
       │   (100+ API access control)
       │
       └─→ (Layer 3: UI Page-Policy)
           Policy → UI Pages
           (36 frontend pages)
           
       ▼
┌──────────────────────────────────────────┐
│  Database Row-Level Security (RLS)       │
│  (VPD: WORKER, EMPLOYER see own data)    │
└──────────────────────────────────────────┘
```

---

## Prerequisites

### Required
- PostgreSQL 13+ (or version configured in environment)
- Database created and accessible
- `psql` command-line client installed
- Administrator database access

### Environment Setup
```bash
# Verify PostgreSQL connection
psql -U postgres -d postgres -c "SELECT version();"

# Create database if not exists
createdb -U postgres auth_service_db

# Verify database exists
psql -U postgres -d auth_service_db -c "SELECT 'Database ready' as status;"
```

---

## Quick Start

### One-Command Setup (Recommended for First Time)

```bash
# Complete initialization in single command
./scripts/bootstrap.sh init

# Or manually with psql:
psql -U postgres -d auth_service_db -f docs/ONBOARDING/setup/00_complete_bootstrap.sql

# Verify setup
psql -U postgres -d auth_service_db -f docs/ONBOARDING/setup/verify_setup.sql
```

### Expected Output
```
✅ 7 roles created
✅ 98 capabilities defined
✅ 7 policies created
✅ Policy-capability links created
✅ 7 seed users created
✅ User-role assignments created
✅ 100+ endpoints registered
✅ 36 UI pages registered
✅ VPD policies configured
✅ Setup complete!
```

---

## Detailed Setup Steps

### Phase 1: Create Roles (7 total)

**Purpose:** Define the 7 core roles with descriptions and capability allocations.

**What Happens:**
- Creates `PLATFORM_BOOTSTRAP` role (56% of capabilities)
- Creates `ADMIN_TECH` role (52% of capabilities)
- Creates `ADMIN_OPS` role (43% of capabilities)
- Creates `BOARD` role (17% of capabilities)
- Creates `EMPLOYER` role (19% of capabilities)
- Creates `WORKER` role (14% of capabilities)
- Creates `TEST_USER` role (51% of capabilities)

**SQL File:** `docs/ONBOARDING/setup/01_create_roles.sql`

**Verification:**
```bash
psql -U postgres -d auth_service_db -c "SELECT name, is_active FROM roles ORDER BY name;"
```

Expected: 7 rows (all roles active)

---

### Phase 2: Create Capabilities (98 total)

**Purpose:** Define all 98 atomic capabilities using the `<domain>.<subject>.<action>` naming convention.

**Capabilities Organized By Domain:**
- User Management (5): user.account.create, user.account.read, etc.
- Payment File Management (8): payment.file.upload, payment.file.read, etc.
- Payment Request Management (9): reconciliation.request.create, etc.
- Worker Operations (6): worker.data.upload, worker.request.create, etc.
- Employer Operations (5): employer.request.read, employer.payment.approve, etc.
- Board Operations (7): board.request.read, board.payment.reconcile, etc.
- RBAC - Role Management (6): rbac.role.create, rbac.role.read, etc.
- RBAC - Policy Management (7): rbac.policy.create, rbac.policy.read, etc.
- RBAC - Capability Management (6): rbac.capability.create, rbac.capability.read, etc.
- API Endpoint Management (7): rbac.endpoint.create, rbac.endpoint.read, etc.
- UI Page Management (8): ui.page.create, ui.page.read, etc.
- Page Action Management (7): ui.action.create, ui.action.read, etc.
- System & Reporting (8): system.ingestion.trigger-mt940, system.audit.read, etc.

**SQL File:** `docs/ONBOARDING/setup/02_create_capabilities.sql`

**Verification:**
```bash
psql -U postgres -d auth_service_db -c "SELECT COUNT(*) as total_capabilities FROM capabilities WHERE is_active = true;"
```

Expected: 98 rows

---

### Phase 3: Create Policies (7 total)

**Purpose:** Create 7 policies, one for each role, as the link between roles and capabilities.

**Policies Created:**
1. `PLATFORM_BOOTSTRAP_POLICY` → 55 capabilities (full system access)
2. `ADMIN_TECH_POLICY` → 51 capabilities (system config only)
3. `ADMIN_OPS_POLICY` → 42 capabilities (operations + ingestion)
4. `BOARD_POLICY` → 17 capabilities (board approvals)
5. `EMPLOYER_POLICY` → 19 capabilities (request management)
6. `WORKER_POLICY` → 14 capabilities (personal payments, VPD-protected)
7. `TEST_USER_POLICY` → 50 capabilities (testing access)

**SQL File:** `docs/ONBOARDING/setup/03_create_policies.sql`

**Verification:**
```bash
psql -U postgres -d auth_service_db -c "SELECT name, is_active FROM policies ORDER BY name;"
```

Expected: 7 rows (all policies active)

---

### Phase 4: Link Policies to Capabilities

**Purpose:** Create the policy-capability relationships that grant capabilities to roles.

**What Happens:**
- Policy-Capability Link (Layer 1)
  - PLATFORM_BOOTSTRAP_POLICY linked to 55 capabilities
  - ADMIN_TECH_POLICY linked to 51 capabilities
  - ADMIN_OPS_POLICY linked to 42 capabilities
  - BOARD_POLICY linked to 17 capabilities
  - EMPLOYER_POLICY linked to 19 capabilities
  - WORKER_POLICY linked to 14 capabilities
  - TEST_USER_POLICY linked to 50 capabilities

**SQL File:** `docs/ONBOARDING/setup/04_link_policies_to_capabilities.sql`

**Verification:**
```bash
psql -U postgres -d auth_service_db -c "
  SELECT p.name, COUNT(pc.capability_id) as capability_count
  FROM policies p
  LEFT JOIN policy_capabilities pc ON p.id = pc.policy_id
  GROUP BY p.id, p.name
  ORDER BY p.name;
"
```

Expected capability counts: BOOTSTRAP=55, ADMIN_TECH=51, ADMIN_OPS=42, BOARD=17, EMPLOYER=19, WORKER=14, TEST_USER=50

---

### Phase 5: Register Endpoints (100+ total)

**Purpose:** Register all API endpoints and link them to policies.

**Endpoints Organized By Category:**
- Authentication (4): /api/auth/login, /api/auth/logout, /api/auth/register, /api/auth/ui-config
- Authorization & Metadata (4): /api/me/authorizations, /api/meta/service-catalog, etc.
- User Management (6): /api/auth/users, /api/auth/users/{id}, etc.
- Role Management (9): /api/admin/roles, /api/admin/roles/{id}, etc.
- Policy Management (9): /api/admin/policies, /api/admin/policies/{id}, etc.
- Capability Management (6): /api/admin/capabilities, etc.
- Endpoint Management (9): /api/admin/endpoints, etc.
- UI Page Management (9): /api/admin/ui-pages, etc.
- Page Action Management (8): /api/admin/page-actions, etc.
- Audit Logs (3): /api/admin/audit-logs, etc.
- System Settings (2): /api/admin/system/settings, etc.
- File Ingestion (3): /api/mt940/ingest, /api/van/ingest, /api/system/ingestion-status
- Worker Payments (7): /api/worker/uploaded-data/*, /api/v1/worker-payments/*
- Worker Records (3): /api/v1/worker-payments/*, etc.
- Worker Receipts (1): /api/worker/receipts/*/send-to-employer
- Employer Payments (2): /api/employer/receipts/*, etc.
- Board Receipts (3): /api/v1/board-receipts/*, etc.

**SQL File:** `docs/ONBOARDING/setup/05_register_endpoints.sql`

**Verification:**
```bash
psql -U postgres -d auth_service_db -c "SELECT COUNT(*) as total_endpoints FROM endpoints WHERE is_active = true;"
```

Expected: 98+ rows

---

### Phase 6: Link Policies to Endpoints (Endpoint-Policy Layer)

**Purpose:** Create the endpoint-policy relationships for Layer 2 authorization.

**What Happens:**
- Endpoint-Policy Link (Layer 2)
  - Each policy linked to appropriate endpoints
  - ADMIN_TECH_POLICY → 51 admin endpoints
  - ADMIN_OPS_POLICY → 42 operational endpoints
  - WORKER_POLICY → 14 worker endpoints
  - EMPLOYER_POLICY → 19 employer endpoints
  - BOARD_POLICY → 17 board endpoints
  - TEST_USER_POLICY → 50 test endpoints

**SQL File:** `docs/ONBOARDING/setup/06_link_policies_to_endpoints.sql`

**Verification:**
```bash
psql -U postgres -d auth_service_db -c "
  SELECT p.name, COUNT(DISTINCT ep.endpoint_id) as endpoint_count
  FROM policies p
  LEFT JOIN endpoint_policies ep ON p.id = ep.policy_id
  GROUP BY p.id, p.name
  ORDER BY p.name;
"
```

---

### Phase 7: Create & Register UI Pages (36 total)

**Purpose:** Create UI page structure and link to policies.

**Pages Organized By Category:**
- Dashboard (3): Worker Dashboard, Employer Dashboard, Board Dashboard
- Payment Management (5): File Upload, Payment Records, Payment Details, etc.
- Request Management (5): Request Creation, Request List, Request Details, Request Approval, Request Status
- Approvals & Reconciliation (5): Board Approvals, Reconciliation Matrix, Board Receipts, etc.
- User Management (5): User Accounts, User Roles, User Details, etc.
- RBAC Configuration (6): Roles Management, Policies Management, Capabilities Matrix, etc.
- UI Configuration (3): UI Pages, Page Actions, Elements Configuration
- System Configuration (4): System Settings, API Endpoints, Audit Logs, Ingestion Status

**SQL File:** `docs/ONBOARDING/setup/07_create_ui_pages.sql`

**Verification:**
```bash
psql -U postgres -d auth_service_db -c "SELECT COUNT(*) as total_pages FROM ui_pages WHERE is_active = true;"
```

Expected: 36 rows

---

### Phase 8: Link Policies to UI Pages (UI Page-Policy Layer)

**Purpose:** Create the UI page-policy relationships for Layer 3 authorization.

**What Happens:**
- UI Page-Policy Link (Layer 3)
  - ADMIN_TECH_POLICY → 14 pages (admin config pages)
  - ADMIN_OPS_POLICY → 9 pages (operational pages)
  - WORKER_POLICY → 5 pages (worker dashboard, payments, requests)
  - EMPLOYER_POLICY → 8 pages (employer dashboard, payments, requests)
  - BOARD_POLICY → 6 pages (board dashboard, receipts, approvals)
  - TEST_USER_POLICY → 35 pages (almost all, except hidden pages)

**SQL File:** `docs/ONBOARDING/setup/08_link_policies_to_ui_pages.sql`

**Verification:**
```bash
psql -U postgres -d auth_service_db -c "
  SELECT p.name, COUNT(DISTINCT up.ui_page_id) as page_count
  FROM policies p
  LEFT JOIN ui_page_policies up ON p.id = up.policy_id
  GROUP BY p.id, p.name
  ORDER BY p.name;
"
```

---

### Phase 9: Create Seed Users (7 total)

**Purpose:** Create one test user for each role for initial testing and demonstration.

**Users Created:**
1. `platform.bootstrap` → PLATFORM_BOOTSTRAP role (disabled after setup)
2. `admin.tech` → ADMIN_TECH role (technical administrator)
3. `admin.ops` → ADMIN_OPS role (operations administrator)
4. `board1` → BOARD role (board member)
5. `employer1` → EMPLOYER role (employer staff)
6. `worker1` → WORKER role (worker/employee)
7. `test.user` → TEST_USER role (QA testing)

**SQL File:** `docs/ONBOARDING/setup/09_create_seed_users.sql`

**Verification:**
```bash
psql -U postgres -d auth_service_db -c "SELECT username, email, is_active FROM users ORDER BY username;"
```

Expected: 7 rows

---

### Phase 10: Assign Users to Roles (Linking Step)

**Purpose:** Create user-role assignments so users get their role's capabilities.

**Assignments:**
- platform.bootstrap → PLATFORM_BOOTSTRAP role
- admin.tech → ADMIN_TECH role
- admin.ops → ADMIN_OPS role
- board1 → BOARD role
- employer1 → EMPLOYER role
- worker1 → WORKER role
- test.user → TEST_USER role

**SQL File:** `docs/ONBOARDING/setup/10_assign_users_to_roles.sql`

**Verification:**
```bash
psql -U postgres -d auth_service_db -c "
  SELECT u.username, r.name as role, r.is_active
  FROM user_role_assignments ura
  JOIN users u ON ura.user_id = u.id
  JOIN roles r ON ura.role_id = r.id
  ORDER BY u.username;
"
```

Expected: 7 rows (one per user)

---

### Phase 11: Configure VPD Security Policies

**Purpose:** Set up Row-Level Security (RLS) and Virtual Private Data (VPD) for WORKER and EMPLOYER roles.

**What Gets Configured:**
- WORKER VPD: User sees only their own payment records (user-scoped via user_id)
- EMPLOYER VPD: User sees only their organization's requests (tenant-scoped via organization_id)
- BOARD VPD: No filtering (board-level access to all data - NO_FILTER type)
- ADMIN roles: No filtering (administrative access - NO_FILTER type)

**VPD Types:**
- `USER_SCOPED`: User sees only their own data (WORKER role, tenant_id = user_id)
- `TENANT_SCOPED`: User sees organization's data (EMPLOYER role, tenant_id = organization_id)
- `NO_FILTER`: Full access without restrictions (BOARD, ADMIN roles)

**Implementation Layer:**
- Application-level VPD via `UserTenantAcl` table queries
- Optional database-level RLS policies for additional security

**SQL File:** `docs/ONBOARDING/setup/08_configure_vpd.sql`

**Verification:**
```bash
psql -U postgres -d auth_service_db << EOF
-- View all VPD assignments
SELECT u.username, r.name as role, uta.acl_type, uta.tenant_id
FROM user_tenant_acl uta
JOIN users u ON uta.user_id = u.id
JOIN user_role_assignments ura ON u.id = ura.user_id
JOIN roles r ON ura.role_id = r.id
ORDER BY r.name, u.username;

-- Verify WORKER VPD (user-scoped)
SELECT COUNT(*) as worker_vpd FROM user_tenant_acl uta
JOIN users u ON uta.user_id = u.id
WHERE u.username = 'worker1' AND uta.acl_type = 'USER_SCOPED';

-- Verify EMPLOYER VPD (tenant-scoped)
SELECT COUNT(*) as employer_vpd FROM user_tenant_acl uta
JOIN users u ON uta.user_id = u.id
WHERE u.username = 'employer1' AND uta.acl_type = 'TENANT_SCOPED';
EOF
```

Expected:
- WORKER: user_scoped entries (tenant_id = user's own id)
- EMPLOYER: tenant_scoped entries (tenant_id = 1)
- BOARD/ADMIN: no_filter entries

---

## Verification Checklist

After running all setup phases, verify completeness:

### ✅ Roles Verification
```bash
psql -U postgres -d auth_service_db << EOF
SELECT 'ROLES' as check_type, COUNT(*) as count FROM roles WHERE is_active = true;
EOF
```
Expected: 7

### ✅ Capabilities Verification
```bash
psql -U postgres -d auth_service_db << EOF
SELECT 'CAPABILITIES' as check_type, COUNT(*) as count FROM capabilities WHERE is_active = true;
EOF
```
Expected: 98

### ✅ Policies Verification
```bash
psql -U postgres -d auth_service_db << EOF
SELECT 'POLICIES' as check_type, COUNT(*) as count FROM policies WHERE is_active = true;
EOF
```
Expected: 7

### ✅ Policy-Capability Links
```bash
psql -U postgres -d auth_service_db << EOF
SELECT 'POLICY-CAPABILITY LINKS' as check_type, COUNT(*) as count FROM policy_capabilities;
EOF
```
Expected: 288 (55+51+42+17+19+14+50)

### ✅ Endpoints Verification
```bash
psql -U postgres -d auth_service_db << EOF
SELECT 'ENDPOINTS' as check_type, COUNT(*) as count FROM endpoints WHERE is_active = true;
EOF
```
Expected: 98+

### ✅ Endpoint-Policy Links
```bash
psql -U postgres -d auth_service_db << EOF
SELECT 'ENDPOINT-POLICY LINKS' as check_type, COUNT(*) as count FROM endpoint_policies;
EOF
```
Expected: 200+

### ✅ UI Pages Verification
```bash
psql -U postgres -d auth_service_db << EOF
SELECT 'UI PAGES' as check_type, COUNT(*) as count FROM ui_pages WHERE is_active = true;
EOF
```
Expected: 36

### ✅ UI Page-Policy Links
```bash
psql -U postgres -d auth_service_db << EOF
SELECT 'UI-PAGE-POLICY LINKS' as check_type, COUNT(*) as count FROM ui_page_policies;
EOF
```
Expected: 80+

### ✅ Users Verification
```bash
psql -U postgres -d auth_service_db << EOF
SELECT 'USERS' as check_type, COUNT(*) as count FROM users WHERE is_active = true;
EOF
```
Expected: 7

### ✅ User-Role Assignments
```bash
psql -U postgres -d auth_service_db << EOF
SELECT 'USER-ROLE ASSIGNMENTS' as check_type, COUNT(*) as count FROM user_role_assignments;
EOF
```
Expected: 7

---

## Troubleshooting

### Issue: "Role already exists" error

**Cause:** Running setup twice without cleanup

**Solution:**
```bash
# Option 1: Reset entire system
psql -U postgres -d auth_service_db -f docs/ONBOARDING/setup/reset_all.sql

# Option 2: Continue anyway (IF clauses handle duplicates)
# Just run the setup again, it will skip existing items
```

### Issue: "Foreign key constraint violation" error

**Cause:** Tables don't exist or were dropped

**Solution:**
```bash
# Ensure schema is created
psql -U postgres -d auth_service_db -f docs/schema/init.sql

# Then run setup again
psql -U postgres -d auth_service_db -f docs/ONBOARDING/setup/00_complete_bootstrap.sql
```

### Issue: User login fails with "Unauthorized"

**Cause:** User not assigned to role

**Solution:**
```bash
# Check user has role assigned
psql -U postgres -d auth_service_db -c "
  SELECT u.username, r.name FROM users u
  LEFT JOIN user_role_assignments ura ON u.id = ura.user_id
  LEFT JOIN roles r ON ura.role_id = r.id
  WHERE u.username = 'worker1';
"

# If null, assign role
psql -U postgres -d auth_service_db << EOF
INSERT INTO user_role_assignments (user_id, role_id, assigned_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.username = 'worker1' AND r.name = 'WORKER'
ON CONFLICT DO NOTHING;
EOF
```

### Issue: Endpoint shows as "not accessible"

**Cause:** Endpoint not linked to policy

**Solution:**
```bash
# Check policy-endpoint link
psql -U postgres -d auth_service_db -c "
  SELECT p.name, COUNT(ep.endpoint_id) as link_count
  FROM policies p
  LEFT JOIN endpoint_policies ep ON p.id = ep.policy_id
  GROUP BY p.id, p.name;
"

# If count is low, run endpoint linking phase again
psql -U postgres -d auth_service_db -f docs/ONBOARDING/setup/06_link_policies_to_endpoints.sql
```

---

## Post-Setup Configuration

### 1. Disable PLATFORM_BOOTSTRAP Account (IMPORTANT!)

```bash
psql -U postgres -d auth_service_db << EOF
UPDATE users
SET is_active = false,
    updated_at = NOW()
WHERE username = 'platform.bootstrap';

-- Verify disabled
SELECT username, is_active FROM users WHERE username = 'platform.bootstrap';
EOF
```

### 2. Change Default Passwords

The seed users have placeholder passwords. Change them immediately:

```bash
# Change admin.tech password (via application login or direct database update)
# Change admin.ops password
# Change board1, employer1, worker1, test.user passwords
```

### 3. Configure External File Ingestion (ADMIN_OPS)

Set up MT940/VAN file ingestion permissions:

```bash
# Verify ADMIN_OPS has:
# ✅ system.ingestion.trigger-mt940 capability
# ✅ system.ingestion.trigger-van capability
# ✅ Access to /api/mt940/ingest endpoint
# ✅ Access to /api/van/ingest endpoint
```

### 4. Enable Audit Logging

Verify @Auditable annotation is working:

```bash
# Make a test API call
curl -X GET http://localhost:8080/api/me/authorizations \
  -H "Authorization: Bearer <jwt_token>"

# Check audit log
psql -U postgres -d auth_service_db << EOF
SELECT action, entity_type, user_id, created_at
FROM audit_logs
ORDER BY created_at DESC
LIMIT 10;
EOF
```

### 5. Test Each Role's Access

```bash
# 1. Login as worker1
#    ✅ Should see: Worker Dashboard, File Upload, My Requests
#    ❌ Should NOT see: Board operations, System Configuration

# 2. Login as employer1
#    ✅ Should see: Employer Dashboard, Request Management, Payment Records
#    ❌ Should NOT see: Worker data, Board operations, System Configuration

# 3. Login as board1
#    ✅ Should see: Board Dashboard, Reconciliation, Board Receipts
#    ❌ Should NOT see: File uploads, Request creation, System Configuration

# 4. Login as admin.ops
#    ✅ Should see: Operations Dashboard, Audit Logs, System Settings
#    ❌ Should NOT see: User Management, RBAC Configuration

# 5. Login as admin.tech
#    ✅ Should see: RBAC Configuration, User Management, Endpoint Management
#    ❌ Should NOT see: Business operations, File ingestion triggers
```

---

## Related Documentation

- **[ROLES.md](ROLES.md)** - Detailed role descriptions and capabilities
- **[RBAC/README.md](../RBAC/README.md)** - RBAC system architecture
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - Overall system architecture with RBAC design
- **[Phase 1: Endpoints](../../PHASE1_ENDPOINTS_EXTRACTION.md)** - All 100+ endpoints
- **[Phase 3: Capabilities](../../PHASE3_CAPABILITIES_DEFINITION.md)** - All 98 capabilities
- **[Phase 4: Policies](../../PHASE4_POLICY_CAPABILITY_MAPPINGS.md)** - Policy-capability mappings
- **[Phase 5: Endpoints](../../PHASE5_ENDPOINT_POLICY_MAPPINGS.md)** - Endpoint-policy mappings

---

**Last Updated:** November 2, 2025  
**Status:** Ready for SQL Script Generation (Phase 8)  
**Next Step:** Execute `docs/ONBOARDING/setup/*.sql` files to initialize system
