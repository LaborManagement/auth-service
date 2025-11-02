# RBAC System Architecture

> Comprehensive documentation of the Role-Based Access Control (RBAC) system architecture with visual representations, data flow diagrams, and implementation patterns.

**Document Status**: Complete  
**Last Updated**: November 2025  
**Scope**: Auth-Service RBAC, VPD Configuration, Three-Layer Authorization Model

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [System Architecture Overview](#system-architecture-overview)
3. [Authentication & Authorization Flow](#authentication--authorization-flow)
4. [Role Hierarchy & Distribution](#role-hierarchy--distribution)
5. [Three-Layer Authorization Model](#three-layer-authorization-model)
6. [Capability Framework](#capability-framework)
7. [Policy & Role Mapping](#policy--role-mapping)
8. [Data Isolation (VPD)](#data-isolation-vpd)
9. [Business Workflow](#business-workflow)
10. [Component Interactions](#component-interactions)
11. [JWT & Token Flow](#jwt--token-flow)
12. [Implementation Patterns](#implementation-patterns)

---

## Executive Summary

The RBAC system implements a **three-layer authorization model** with:

- **7 Roles**: PLATFORM_BOOTSTRAP, ADMIN_TECH, ADMIN_OPS, BOARD, EMPLOYER, WORKER, TEST_USER
- **98 Atomic Capabilities**: Fine-grained permissions using `<domain>.<subject>.<action>` naming
- **100+ Endpoints**: Mapped to role-based policies with hierarchical access control
- **36 UI Pages**: Organized in 8 groups with capability-based access controls
- **VPD Configuration**: Row-level security for user-tenant data isolation
- **Three-Layer Auth**: Capability-Policy, Endpoint-Policy, UI_Page-Policy enforcement

### Key Metrics

```
┌─────────────────────────────────────┐
│         RBAC System Metrics         │
├─────────────────────────────────────┤
│ Total Roles               : 7       │
│ Total Capabilities        : 98      │
│ Total Endpoints           : 100+    │
│ Total UI Pages            : 36      │
│ Policy-Capability Links   : 288     │
│ Authentication Type       : JWT     │
│ Data Isolation Type       : VPD/RLS │
└─────────────────────────────────────┘
```

---

## System Architecture Overview

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          API Gateway / Load Balancer                    │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                    ┌──────────┴──────────┐
                    │                     │
         ┌──────────▼─────────┐  ┌───────▼──────────────┐
         │   Auth Service     │  │   Other Services    │
         │  (This Service)    │  │  (Payment, etc)     │
         └────────┬───────────┘  └─────────────────────┘
                  │
        ┌─────────┴─────────┐
        │                   │
   ┌────▼──────┐       ┌───▼─────────┐
   │ Security  │       │ Data Access │
   │ Layer     │       │ Layer       │
   │           │       │             │
   │ • JWT     │       │ • RLS       │
   │ • RBAC    │       │ • VPD       │
   │ • Roles   │       │ • ACL       │
   │ • Policies│       │             │
   └────┬──────┘       └───┬─────────┘
        │                  │
   ┌────▼──────────────────▼─────┐
   │                              │
   │    PostgreSQL Database       │
   │                              │
   │  ┌──────────────────────┐   │
   │  │ RBAC Tables:         │   │
   │  │ • roles              │   │
   │  │ • capabilities       │   │
   │  │ • policies           │   │
   │  │ • user_role_assign   │   │
   │  │ • policy_capability  │   │
   │  └──────────────────────┘   │
   │                              │
   │  ┌──────────────────────┐   │
   │  │ Security Tables:     │   │
   │  │ • users              │   │
   │  │ • user_tenant_acl    │   │
   │  │ • jwt_tokens         │   │
   │  └──────────────────────┘   │
   │                              │
   └──────────────────────────────┘
```

---

## Authentication & Authorization Flow

### Complete Request Authorization Flow

```
Client Request
    │
    ├─── [1. JWT Extraction & Validation]
    │       │
    │       ├─ Extract JWT token from Authorization header
    │       ├─ Validate token signature (Secret Key)
    │       ├─ Check token expiration
    │       └─ Extract user_id, username, roles from claims
    │
    ├─── [2. Role Resolution]
    │       │
    │       ├─ Retrieve user_id from JWT
    │       ├─ Query user_role_assignments table
    │       │   WHERE user_id = <jwt_user_id>
    │       ├─ Fetch all assigned roles
    │       └─ Cache roles for session
    │
    ├─── [3. Policy Matching]
    │       │
    │       ├─ Identify requested resource/endpoint
    │       ├─ Query endpoint_policies table
    │       │   WHERE endpoint_path = <request_path>
    │       ├─ Check if user's roles match policy
    │       └─ If no match → 403 FORBIDDEN
    │
    ├─── [4. Capability Verification]
    │       │
    │       ├─ Extract required capabilities from policy
    │       ├─ Query policy_capabilities junction table
    │       │   WHERE role_id = <user_roles>
    │       ├─ Verify user has all required capabilities
    │       └─ If missing capability → 403 FORBIDDEN
    │
    ├─── [5. Data Isolation Check]
    │       │
    │       ├─ Query user_tenant_acl table
    │       │   WHERE user_id = <jwt_user_id>
    │       │   AND resource_board_id = <request_board_id>
    │       ├─ Check can_read / can_write permissions
    │       ├─ Apply row-level security filters
    │       └─ If no VPD match → empty result set (404)
    │
    ├─── [6. Resource Authorization]
    │       │
    │       ├─ Load resource from database
    │       ├─ Apply VPD filters (RLS)
    │       ├─ Check resource ownership / tenant match
    │       └─ If not authorized → filtered/hidden
    │
    └─── [7. Request Processing & Response]
            │
            ├─ Process request (GET/POST/PUT/DELETE)
            ├─ Apply VPD row filters to result set
            ├─ Filter response data based on user permissions
            └─ Return 200 OK with filtered data

Authorization Success: Request continues
Authorization Failure: Request returns 403 FORBIDDEN or 404 NOT FOUND
```

---

## Role Hierarchy & Distribution

### Role Distribution Matrix

```
┌───────────────────────────────────────────────────────────────────────┐
│                         ROLE DISTRIBUTION                             │
├─────────────────────────┬──────────────┬────────────┬─────────────────┤
│ Role Name               │ Capability % │ Count      │ Primary Purpose │
├─────────────────────────┼──────────────┼────────────┼─────────────────┤
│ PLATFORM_BOOTSTRAP      │ 56% (55/98)  │ SuperUser  │ System Bootstrap│
│ ADMIN_TECH              │ 52% (51/98)  │ Technical  │ Technical Admin │
│ ADMIN_OPS               │ 43% (42/98)  │ Operational│ Ops Admin       │
│ BOARD                   │ 17% (17/98)  │ Board Mgmt │ Board Level     │
│ EMPLOYER                │ 19% (19/98)  │ Employer   │ Employer Level  │
│ WORKER                  │ 14% (14/98)  │ Worker     │ User Level      │
│ TEST_USER               │ 51% (50/98)  │ Testing    │ QA Testing      │
└─────────────────────────┴──────────────┴────────────┴─────────────────┘
```

### Role Hierarchy Diagram

```
                    PLATFORM_BOOTSTRAP
                    (All Capabilities)
                            │
                            │ Inherits / Delegates to
                            │
                ┌───────────┴───────────┐
                │                       │
            ┌───▼────────┐      ┌──────▼─────┐
            │ ADMIN_TECH │      │ ADMIN_OPS  │
            │ (51 caps)  │      │ (42 caps)  │
            └───┬────────┘      └──────┬─────┘
                │                      │
                └────────┬─────────────┘
                         │
                ┌────────▼────────┐
                │ BOARD (17 caps) │
                └────────┬────────┘
                         │
            ┌────────────┼────────────┐
            │            │            │
        ┌───▼──────┐ ┌──▼────────┐ ┌▼──────────┐
        │EMPLOYER  │ │ WORKER    │ │ TEST_USER │
        │(19 caps) │ │ (14 caps) │ │(50 caps)  │
        └──────────┘ └───────────┘ └───────────┘

Legend:
─────── = Role Relationship
▼ = Inheritance / Delegation Flow
```

### Role Responsibility Matrix

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     ROLE RESPONSIBILITY MATRIX                          │
├──────────────────┬─────────┬────────┬──────────┬──────────┬─────┬──────┤
│ Responsibility   │BOOTSTRAP│ADMIN_T │ADMIN_OPS │BOARD     │EMP  │WORKER│
├──────────────────┼─────────┼────────┼──────────┼──────────┼─────┼──────┤
│ User Management  │   ✅    │  ✅   │    ❌    │    ❌    │ ❌  │  ❌  │
│ Role Management  │   ✅    │  ✅   │    ❌    │    ❌    │ ❌  │  ❌  │
│ System Config    │   ✅    │  ✅   │    ✅    │    ❌    │ ❌  │  ❌  │
│ Board Config     │   ✅    │  ❌   │    ❌    │    ✅    │ ❌  │  ❌  │
│ Employer Mgmt    │   ✅    │  ❌   │    ❌    │    ✅    │ ✅  │  ❌  │
│ Worker Mgmt      │   ✅    │  ❌   │    ✅    │    ✅    │ ✅  │  ❌  │
│ Payment Records  │   ✅    │  ✅   │    ✅    │    ✅    │ ✅  │  ✅  │
│ Report Access    │   ✅    │  ✅   │    ✅    │    ✅    │ ✅  │  ✅  │
│ Data Export      │   ✅    │  ✅   │    ✅    │    ✅    │ ✅  │  ❌  │
│ Audit Logs       │   ✅    │  ✅   │    ✅    │    ❌    │ ❌  │  ❌  │
└──────────────────┴─────────┴────────┴──────────┴──────────┴─────┴──────┘

✅ = Authorized  |  ❌ = Not Authorized
```

---

## Three-Layer Authorization Model

### Layer Architecture

```
┌────────────────────────────────────────────────────────────────────┐
│                    REQUEST AUTHENTICATION                          │
│  (JWT Token Validation, User Identity, Session Management)        │
└────────────────────────────┬─────────────────────────────────────┘
                             │
        ┌────────────────────▼────────────────────┐
        │  LAYER 1: ENDPOINT-POLICY AUTHORIZATION │
        │                                         │
        │  Route-level access control             │
        │  Check: Can this role access this path? │
        │                                         │
        │  Logic:                                 │
        │  1. Get endpoint path from request      │
        │  2. Query endpoint_policies table       │
        │  3. Check if user role matches policy   │
        │  4. Return: ALLOWED or DENIED           │
        └────────────────────┬────────────────────┘
                             │
                    Success ──┘
                             │
        ┌────────────────────▼──────────────────────┐
        │ LAYER 2: CAPABILITY-POLICY AUTHORIZATION  │
        │                                           │
        │ Fine-grained capability checking          │
        │ Check: Does user have required caps?      │
        │                                           │
        │ Logic:                                    │
        │ 1. Extract capabilities from policy       │
        │ 2. Get user's assigned capabilities       │
        │ 3. Verify intersection of sets            │
        │ 4. Return: ALL CAPS PRESENT or MISSING    │
        └────────────────────┬──────────────────────┘
                             │
                    Success ──┘
                             │
        ┌────────────────────▼────────────────────┐
        │ LAYER 3: DATA-ISOLATION (VPD) CONTROL   │
        │                                         │
        │ Row-level security and data filtering    │
        │ Check: Can user access this data row?    │
        │                                          │
        │ Logic:                                   │
        │ 1. Query user_tenant_acl table           │
        │ 2. Match user_id + board_id/employer_id │
        │ 3. Check can_read / can_write flags      │
        │ 4. Filter result set with RLS policies   │
        │ 5. Return: Filtered data or empty        │
        └────────────────────┬────────────────────┘
                             │
                    Success ──┘
                             │
        ┌────────────────────▼────────────────────┐
        │     RESOURCE RETURNED TO CLIENT         │
        │  (Filtered & Authorized Data Set)       │
        └────────────────────────────────────────┘
```

### Authorization Decision Tree

```
                        ┌─── Request ────┐
                        │                │
                   ┌────▼────────────┐
                   │ JWT Valid?      │
                   └────┬─────────┬──┘
                        │ NO      │ YES
                        │         │
                    ┌───▼─┐   ┌──▼──────────────────┐
                    │403  │   │ Get User Roles      │
                    │Unauth   │ from Database       │
                    └───────┘ └──┬────────────────┬─┘
                                 │                │
                            ┌────▼─────────────────┐
                            │ User Has Roles?      │
                            └────┬──────────────┬──┘
                                 │ NO           │ YES
                                 │              │
                             ┌───▼─┐      ┌────▼──────────┐
                             │403  │      │ Check Layer 1 │
                             └─────┘      │ Endpoint-     │
                                         │ Policy        │
                                         └────┬────────┬─┘
                                              │ NO     │ YES
                                              │        │
                                          ┌───▼─┐  ┌──▼────────┐
                                          │403  │  │Check Layer2│
                                          │Forbid  │Capability │
                                          └───────┘ └────┬────┬─┘
                                                         │ NO  │ YES
                                                         │     │
                                                     ┌───▼─┐ ┌▼────────┐
                                                     │403  │ │Check L3 │
                                                     │No   │ │VPD/RLS  │
                                                     │Caps │ └────┬──┬─┘
                                                     └─────┘      │ NO │ YES
                                                                  │    │
                                                              ┌───▼─┐ ┌▼────────┐
                                                              │404  │ │ 200 OK  │
                                                              │No   │ │Filtered │
                                                              │Data │ │Response │
                                                              └─────┘ └─────────┘
```

---

## Capability Framework

### Capability Naming Convention

```
Capability Format: <DOMAIN>.<SUBJECT>.<ACTION>

Examples:
├─ user.profile.view      ────> User can view own profile
├─ user.profile.update    ────> User can update own profile
├─ payment.record.create  ────> User can create payment records
├─ payment.record.delete  ────> User can delete payment records
├─ report.financial.view  ────> User can view financial reports
└─ system.config.write    ────> User can modify system config

Structure:
┌──────────┬──────────┬───────────┐
│ DOMAIN   │ SUBJECT  │ ACTION    │
├──────────┼──────────┼───────────┤
│ user     │ profile  │ view      │
│ payment  │ record   │ create    │
│ report   │ financial│ export    │
│ system   │ config   │ write     │
└──────────┴──────────┴───────────┘
```

### Capability Distribution Across Roles

```
┌─────────────────────────────────────────────────────────────┐
│        CAPABILITY DISTRIBUTION HEATMAP                      │
├─────────────────────────────────────────────────────────────┤
│ Domain          │ Bootstrap │ Admins │ Board │ Emp │ Work  │
├─────────────────┼───────────┼────────┼───────┼─────┼───────┤
│ user.*          │    ✅     │   ✅   │  ❌   │  ❌ │  ❌   │
│ role.*          │    ✅     │   ✅   │  ❌   │  ❌ │  ❌   │
│ payment.*       │    ✅     │   ✅   │  ✅   │  ✅ │  ✅   │
│ report.*        │    ✅     │   ✅   │  ✅   │  ✅ │  ✅   │
│ system.*        │    ✅     │   ✅   │  ❌   │  ❌ │  ❌   │
│ employer.*      │    ✅     │   ❌   │  ✅   │  ✅ │  ❌   │
│ worker.*        │    ✅     │   ✅   │  ✅   │  ✅ │  ❌   │
│ audit.*         │    ✅     │   ✅   │  ❌   │  ❌ │  ❌   │
└─────────────────┴───────────┴────────┴───────┴─────┴───────┘

✅ = Capabilities Assigned  |  ❌ = No Capabilities
```

---

## Policy & Role Mapping

### Policy Structure

```
Policy = <Role> + <Capabilities[]> + <Endpoints[]>

Example Policy: EMPLOYER_POLICY
┌─────────────────────────────────────────┐
│           EMPLOYER_POLICY               │
├─────────────────────────────────────────┤
│ Role: EMPLOYER                          │
│                                         │
│ Assigned Capabilities: 19 total         │
│ ├─ payment.record.view                  │
│ ├─ payment.record.create                │
│ ├─ payment.record.update                │
│ ├─ employer.profile.view                │
│ ├─ employer.profile.update              │
│ ├─ employee.list.view                   │
│ ├─ report.financial.view                │
│ └─ ... (13 more)                        │
│                                         │
│ Authorized Endpoints: 25+ total         │
│ ├─ GET /api/payments                    │
│ ├─ POST /api/payments                   │
│ ├─ PUT /api/payments/:id                │
│ ├─ GET /api/employers/:id               │
│ ├─ GET /api/reports/financial           │
│ └─ ... (20+ more)                       │
└─────────────────────────────────────────┘
```

### Policy-Capability Link Table

```
Database: policy_capabilities
┌─────────┬──────────┬──────────────────────┐
│ role_id │ policy_id│ capability_id        │
├─────────┼──────────┼──────────────────────┤
│ 3       │ 3        │ 5  (payment.record.view)      │
│ 3       │ 3        │ 6  (payment.record.create)    │
│ 3       │ 3        │ 7  (payment.record.update)    │
│ 3       │ 3        │ 45 (employer.profile.view)    │
│ 3       │ 3        │ 46 (employer.profile.update)  │
│ ...     │ ...      │ ... (288 total links)         │
└─────────┴──────────┴──────────────────────┘

Total Links: 288
Query Pattern:
  SELECT capability_id 
  FROM policy_capabilities 
  WHERE role_id = <user_role>
```

### Endpoint-Policy Mapping

```
Database: endpoint_policies
┌──────────────────────────────┬────────────┬──────────────────┐
│ endpoint_path                │ http_method│ required_role_id │
├──────────────────────────────┼────────────┼──────────────────┤
│ /api/payments                │ GET        │ 3 (EMPLOYER)     │
│ /api/payments                │ POST       │ 3 (EMPLOYER)     │
│ /api/payments/:id            │ GET        │ 3 (EMPLOYER)     │
│ /api/payments/:id            │ PUT        │ 3 (EMPLOYER)     │
│ /api/payments/:id            │ DELETE     │ 2 (ADMIN_OPS)    │
│ /api/employers/:id           │ GET        │ 3 (EMPLOYER)     │
│ /api/employers/:id           │ PUT        │ 3 (EMPLOYER)     │
│ /api/reports/financial       │ GET        │ 3 (EMPLOYER)     │
│ /api/users                   │ GET        │ 1 (BOOTSTRAP)    │
│ /api/users                   │ POST       │ 1 (BOOTSTRAP)    │
│ ...                          │ ...        │ ... (100+ total) │
└──────────────────────────────┴────────────┴──────────────────┘
```

---

## Data Isolation (VPD)

### VPD Architecture

```
┌────────────────────────────────────────────────────────────┐
│         VIRTUAL PRIVATE DATA (VPD) ARCHITECTURE            │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  User Request                                             │
│       ↓                                                   │
│  ┌────────────────────────────────────────────┐          │
│  │ Extract user_id from JWT                  │          │
│  └───────────────┬──────────────────────────┘          │
│                  ↓                                       │
│  ┌────────────────────────────────────────────┐          │
│  │ Query user_tenant_acl Table                │          │
│  │ WHERE user_id = <jwt_user_id>              │          │
│  └───────────────┬──────────────────────────┘          │
│                  ↓                                       │
│  ┌────────────────────────────────────────────┐          │
│  │ Retrieve user's board/employer scope       │          │
│  │ ├─ board_id (always BOARD-DEFAULT)        │          │
│  │ └─ employer_id (varies by role)           │          │
│  └───────────────┬──────────────────────────┘          │
│                  ↓                                       │
│  ┌────────────────────────────────────────────┐          │
│  │ Apply RLS Filters to Query                │          │
│  │ ├─ WHERE board_id IN (user_board_ids)     │          │
│  │ ├─ WHERE employer_id IN (user_emp_ids)    │          │
│  │ └─ WHERE can_read = true (READ ops)       │          │
│  │ └─ WHERE can_write = true (WRITE ops)     │          │
│  └───────────────┬──────────────────────────┘          │
│                  ↓                                       │
│  ┌────────────────────────────────────────────┐          │
│  │ Return Filtered Result Set                │          │
│  │ (Only rows user has access to)            │          │
│  └────────────────────────────────────────────┘          │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

### VPD Configuration by Role

```
┌─────────────────────────────────────────────────────────────────┐
│                 VPD CONFIGURATION MATRIX                        │
├────────────┬─────────────────────┬──────┬──────────┬────────────┤
│ Role       │ board_id            │ emp_ │ can_read │ can_write  │
│            │                     │ id   │          │            │
├────────────┼─────────────────────┼──────┼──────────┼────────────┤
│ WORKER     │ BOARD-DEFAULT       │ EMP- │ true     │ false      │
│            │                     │ 001  │ (R)      │ (read-only)│
├────────────┼─────────────────────┼──────┼──────────┼────────────┤
│ EMPLOYER   │ BOARD-DEFAULT       │ EMP- │ true     │ true       │
│            │                     │ 001  │ (R+W)    │ (full)     │
├────────────┼─────────────────────┼──────┼──────────┼────────────┤
│ BOARD      │ BOARD-DEFAULT       │ NULL │ true     │ true       │
│            │                     │      │ (R+W)    │ (full)     │
├────────────┼─────────────────────┼──────┼──────────┼────────────┤
│ ADMIN_TECH │ BOARD-DEFAULT       │ NULL │ true     │ true       │
│            │                     │      │ (R+W)    │ (full)     │
├────────────┼─────────────────────┼──────┼──────────┼────────────┤
│ ADMIN_OPS  │ BOARD-DEFAULT       │ NULL │ true     │ true       │
│            │                     │      │ (R+W)    │ (full)     │
└────────────┴─────────────────────┴──────┴──────────┴────────────┘

KEY MAPPINGS:
├─ employer_id = NULL ──────> User sees BOARD-level data only
├─ employer_id = EMP-001 ──> User sees EMPLOYER-specific data
├─ can_read = true ────────> User can READ rows
├─ can_write = true ───────> User can WRITE/UPDATE rows
└─ RLS Policy Applied ─────> Automatic row filtering at DB level
```

### VPD Sample Query with Filters

```sql
-- BEFORE VPD (Raw Query - User sees all data)
SELECT * FROM payments WHERE status = 'PENDING';

-- AFTER VPD (With RLS Filters Applied)
SELECT p.* 
FROM payments p
WHERE p.status = 'PENDING'
  AND EXISTS (
    SELECT 1 FROM user_tenant_acl uta
    WHERE uta.user_id = current_user_id
      AND uta.board_id = p.board_id
      AND (uta.employer_id IS NULL OR uta.employer_id = p.employer_id)
      AND uta.can_read = true
  );

Result:
├─ WORKER user: Sees only payments for their assigned employer
├─ EMPLOYER user: Sees payments for their employer scope
├─ BOARD user: Sees all board-level payments
└─ ADMIN user: Sees all payments (no restrictions)
```

---

## Business Workflow

### Typical User Journey: WORKER → EMPLOYER → BOARD

```
┌─────────────────────────────────────────────────────────────────┐
│                    BUSINESS WORKFLOW STATES                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  STEP 1: WORKER ROLE (Entry Level)                             │
│  ───────────────────────────────────────────                   │
│  ├─ User: worker1@example.com                                  │
│  ├─ Permissions:                                               │
│  │  ├─ View own payment records (read-only)                    │
│  │  ├─ View own profile information                            │
│  │  └─ View assigned employer data (read-only)                 │
│  ├─ VPD Scope: user_id + EMP-001 + can_read=true              │
│  ├─ Accessible: 14 capabilities                                │
│  └─ Data Isolation: Employer-scoped (can only see EMP-001)     │
│                                                                 │
│  ┌──────────────────────────────────────────────┐              │
│  │ Attempts to update payment: DENIED (no cap)  │              │
│  │ Attempts to view competitor data: DENIED     │              │
│  │ Submits expense: Updates own records only    │              │
│  └──────────────────────────────────────────────┘              │
│                                                                 │
│  ↓ (Promotion / Role Change)                                   │
│                                                                 │
│  STEP 2: EMPLOYER ROLE (Middle Management)                    │
│  ──────────────────────────────────────────                    │
│  ├─ User: employer1@example.com                                │
│  ├─ Permissions:                                               │
│  │  ├─ View/Create/Update employer records                     │
│  │  ├─ Manage worker expense submissions                       │
│  │  ├─ Generate reports for employer                           │
│  │  └─ Update employer settings                                │
│  ├─ VPD Scope: user_id + EMP-001 + can_read=true             │
│  │            + can_write=true (full permissions)              │
│  ├─ Accessible: 19 capabilities                                │
│  └─ Data Isolation: Employer-scoped (can read+write EMP-001)   │
│                                                                 │
│  ┌──────────────────────────────────────────────┐              │
│  │ Updates payment status: ALLOWED (has cap)    │              │
│  │ Deletes payment: DENIED (not in employer)    │              │
│  │ Creates new employee: ALLOWED (has cap)      │              │
│  └──────────────────────────────────────────────┘              │
│                                                                 │
│  ↓ (Promotion / Role Change)                                   │
│                                                                 │
│  STEP 3: BOARD ROLE (Leadership)                               │
│  ─────────────────────────────────                             │
│  ├─ User: board1@example.com                                   │
│  ├─ Permissions:                                               │
│  │  ├─ View all board-level data (across employers)            │
│  │  ├─ Manage employers and workers                            │
│  │  ├─ Access system configuration                             │
│  │  ├─ Generate board-wide reports                             │
│  │  └─ Full CRUD on all resources                              │
│  ├─ VPD Scope: user_id + BOARD-DEFAULT + employer_id=NULL     │
│  │            + can_read=true + can_write=true                 │
│  ├─ Accessible: 17 capabilities                                │
│  └─ Data Isolation: Board-scoped (sees all employers/workers)   │
│                                                                 │
│  ┌──────────────────────────────────────────────┐              │
│  │ Updates any employer: ALLOWED                │              │
│  │ Creates new employer: ALLOWED                │              │
│  │ Deletes payment record: ALLOWED              │              │
│  │ Accesses all dashboards: ALLOWED            │              │
│  └──────────────────────────────────────────────┘              │
│                                                                 │
│  ↓ (Optional: Admin Promotion)                                 │
│                                                                 │
│  STEP 4: ADMIN ROLE (System Administration)                   │
│  ────────────────────────────────────────                      │
│  ├─ User: admin.tech@example.com                               │
│  ├─ Permissions:                                               │
│  │  ├─ Manage all system roles and users                       │
│  │  ├─ Configure security policies                             │
│  │  ├─ Access audit logs and system monitoring                 │
│  │  ├─ Full system configuration access                        │
│  │  └─ All BOARD capabilities + system capabilities            │
│  ├─ VPD Scope: user_id + BOARD-DEFAULT + employer_id=NULL     │
│  │            + can_read=true + can_write=true                 │
│  ├─ Accessible: 51 capabilities (ADMIN_TECH)                   │
│  └─ Data Isolation: System-wide (no restrictions)              │
│                                                                 │
│  ┌──────────────────────────────────────────────┐              │
│  │ Creates new roles: ALLOWED                  │              │
│  │ Modifies system config: ALLOWED             │              │
│  │ Accesses audit logs: ALLOWED                │              │
│  │ Full system control: ALLOWED                │              │
│  └──────────────────────────────────────────────┘              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Component Interactions

### Authentication & Authorization Service Components

```
┌───────────────────────────────────────────────────────────────────┐
│                    AUTH-SERVICE COMPONENTS                        │
├───────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌─────────────────────────────────────┐                        │
│  │      JWT Authentication Filter      │                        │
│  │  (com.example.userauth.security)    │                        │
│  ├─────────────────────────────────────┤                        │
│  │ • Extract JWT from Authorization    │                        │
│  │ • Validate token signature          │                        │
│  │ • Extract claims (user_id, roles)   │                        │
│  │ • Set SecurityContext                │                        │
│  │ • Pass to next filter/controller    │                        │
│  └──────────────┬──────────────────────┘                        │
│                 │                                               │
│                 ▼                                               │
│  ┌─────────────────────────────────────┐                        │
│  │   RBAC Authorization Service        │                        │
│  │  (RoleBasedAccessControlService)    │                        │
│  ├─────────────────────────────────────┤                        │
│  │ • Check user roles against policies │                        │
│  │ • Verify required capabilities      │                        │
│  │ • Evaluate @PreAuthorize annotations│                        │
│  │ • Cache role/capability data        │                        │
│  │ • Return authorization decision     │                        │
│  └──────────────┬──────────────────────┘                        │
│                 │                                               │
│                 ▼                                               │
│  ┌─────────────────────────────────────┐                        │
│  │    Data Access Layer (DAO)          │                        │
│  │  (Role, Capability, Policy DAOs)    │                        │
│  ├─────────────────────────────────────┤                        │
│  │ • Query role assignments            │                        │
│  │ • Fetch policy definitions          │                        │
│  │ • Retrieve capabilities             │                        │
│  │ • Load VPD configuration            │                        │
│  │ • Cache frequently accessed data    │                        │
│  └──────────────┬──────────────────────┘                        │
│                 │                                               │
│                 ▼                                               │
│  ┌─────────────────────────────────────┐                        │
│  │   PostgreSQL Database Layer         │                        │
│  │  (RBAC Tables + VPD Configuration)  │                        │
│  ├─────────────────────────────────────┤                        │
│  │ ├─ roles table                      │                        │
│  │ ├─ capabilities table               │                        │
│  │ ├─ policies table                   │                        │
│  │ ├─ user_role_assignments table      │                        │
│  │ ├─ policy_capabilities table        │                        │
│  │ └─ user_tenant_acl table (VPD)      │                        │
│  └─────────────────────────────────────┘                        │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘
```

### Request Processing Flow with Component Interactions

```
                    HTTP Request
                         │
                         ▼
              ┌──────────────────────────┐
              │  Servlet Container       │
              │  (Spring Dispatcher)     │
              └──────────┬───────────────┘
                         │
         ┌───────────────┴───────────────┐
         │                               │
         ▼                               ▼
  ┌────────────────────┐        ┌─────────────────┐
  │ JWT Auth Filter    │        │ CORS Filter     │
  │ Extract & Validate │        │ Request Headers │
  └────────┬───────────┘        └─────────────────┘
           │
           ▼
  ┌────────────────────────────────────────┐
  │ Set SecurityContext                    │
  │ with user_id, principal, authorities  │
  └────────┬───────────────────────────────┘
           │
           ▼
  ┌────────────────────────────────────────────┐
  │ Route to Controller Method                 │
  │ with @PreAuthorize("hasRole('EMPLOYER')")  │
  └────────┬───────────────────────────────────┘
           │
           ▼
  ┌────────────────────────────────────────────┐
  │ Spring Security: Check Authorities         │
  │ Call RoleBasedAccessControlService         │
  └────────┬───────────────────────────────────┘
           │
           ▼
  ┌────────────────────────────────────────────┐
  │ RBAC Service: Verify Capabilities         │
  │ Query: user_id IN roles → capabilities    │
  │ Cache result for session                  │
  └────────┬───────────────────────────────────┘
           │ YES (All caps present)
           ▼
  ┌────────────────────────────────────────────┐
  │ Service Layer: Process Business Logic     │
  │ (PaymentService, ReportService, etc)      │
  └────────┬───────────────────────────────────┘
           │
           ▼
  ┌────────────────────────────────────────────┐
  │ Data Access Layer (DAO/Repository)        │
  │ Build query with VPD filters              │
  │ WHERE user_id IN (allowed_scopes)         │
  └────────┬───────────────────────────────────┘
           │
           ▼
  ┌────────────────────────────────────────────┐
  │ PostgreSQL with RLS Policies              │
  │ Apply row-level filters automatically      │
  │ Return only user-authorized rows          │
  └────────┬───────────────────────────────────┘
           │
           ▼
  ┌────────────────────────────────────────────┐
  │ Response Filter: Sanitize Data            │
  │ Remove sensitive fields if needed         │
  │ Format JSON response                      │
  └────────┬───────────────────────────────────┘
           │
           ▼
  ┌────────────────────────────────────────────┐
  │ Return 200 OK                             │
  │ with filtered data to client              │
  └────────────────────────────────────────────┘
```

---

## JWT & Token Flow

### JWT Token Structure

```
┌──────────────────────────────────────────────────────────────┐
│                    JWT TOKEN STRUCTURE                       │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  JWT = HEADER . PAYLOAD . SIGNATURE                         │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ HEADER (Base64 Encoded)                             │   │
│  │ ───────────────────────────────────────────────────  │   │
│  │ {                                                    │   │
│  │   "typ": "JWT",                                      │   │
│  │   "alg": "HS256"                                    │   │
│  │ }                                                    │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ PAYLOAD (Base64 Encoded) - Contains Claims          │   │
│  │ ──────────────────────────────────────────────────   │   │
│  │ {                                                    │   │
│  │   "sub": "user123",              ← User ID         │   │
│  │   "username": "employer1",        ← Username        │   │
│  │   "roles": ["EMPLOYER", "BOARD"], ← Assigned roles │   │
│  │   "exp": 1699401600,              ← Expiration time │   │
│  │   "iat": 1699315200,              ← Issued at time  │   │
│  │   "jti": "abc123def456",          ← JWT ID          │   │
│  │   "iss": "auth-service"           ← Issuer          │   │
│  │ }                                                    │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ SIGNATURE (HMAC-SHA256)                             │   │
│  │ ──────────────────────────────────────────────────   │   │
│  │ HMACSHA256(                                         │   │
│  │   base64UrlEncode(header) + "." +                   │   │
│  │   base64UrlEncode(payload),                         │   │
│  │   secret_key_from_config                           │   │
│  │ )                                                    │   │
│  │                                                     │   │
│  │ Verifies token authenticity and integrity          │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### Token Generation & Validation Flow

```
┌──────────────────────────────────────────────────────────────┐
│          TOKEN GENERATION (LOGIN / AUTHENTICATION)           │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  User submits credentials (username, password)              │
│         │                                                   │
│         ▼                                                   │
│  ┌────────────────────────────────────────┐               │
│  │ 1. Validate credentials                │               │
│  │    - Check username exists in DB       │               │
│  │    - Verify password hash match        │               │
│  └────────────────────────────────────────┘               │
│         │ Success                                          │
│         ▼                                                   │
│  ┌────────────────────────────────────────┐               │
│  │ 2. Fetch user record                   │               │
│  │    - Get user_id, username, email      │               │
│  │    - Load user metadata                │               │
│  └────────────────────────────────────────┘               │
│         │                                                   │
│         ▼                                                   │
│  ┌────────────────────────────────────────┐               │
│  │ 3. Fetch assigned roles                │               │
│  │    SELECT role_name FROM roles         │               │
│  │    JOIN user_role_assignments          │               │
│  │    WHERE user_id = <user_id>           │               │
│  └────────────────────────────────────────┘               │
│         │                                                   │
│         ▼                                                   │
│  ┌────────────────────────────────────────┐               │
│  │ 4. Build JWT Payload Claims            │               │
│  │    {                                   │               │
│  │      sub: user_id,                     │               │
│  │      username: username,               │               │
│  │      roles: [EMPLOYER, BOARD],        │               │
│  │      exp: now + 24 hours,              │               │
│  │      iat: now,                         │               │
│  │      iss: auth-service                │               │
│  │    }                                   │               │
│  └────────────────────────────────────────┘               │
│         │                                                   │
│         ▼                                                   │
│  ┌────────────────────────────────────────┐               │
│  │ 5. Sign Token with Secret Key          │               │
│  │    signature = HMACSHA256(             │               │
│  │      header + "." + payload,           │               │
│  │      secret_key_from_config            │               │
│  │    )                                   │               │
│  └────────────────────────────────────────┘               │
│         │                                                   │
│         ▼                                                   │
│  ┌────────────────────────────────────────┐               │
│  │ 6. Return JWT to Client                │               │
│  │    {                                   │               │
│  │      access_token: jwt_string,         │               │
│  │      token_type: Bearer,               │               │
│  │      expires_in: 86400                 │               │
│  │    }                                   │               │
│  └────────────────────────────────────────┘               │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## Implementation Patterns

### Spring Security Configuration Pattern

```java
// Example: Spring Security Configuration Structure
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    // JWT Filter Chain
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .csrf().disable()
            .authorizeRequests()
                .antMatchers("/api/auth/login").permitAll()
                .antMatchers("/api/auth/refresh").permitAll()
                .anyRequest().authenticated()
            .and()
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        
        return http.build();
    }

    // JWT Token Provider
    @Bean
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider(secretKey, expirationTime);
    }

    // JWT Auth Filter
    @Bean
    public JwtAuthenticationFilter jwtAuthFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider);
    }
}
```

---

## Summary

### Key Architecture Principles

1. **Defense in Depth**: Multiple layers of authorization (JWT, Policy, Capability, VPD)
2. **Least Privilege**: Users get minimum required permissions, escalating through role hierarchy
3. **Data Isolation**: VPD ensures users only see data they're authorized for
4. **Scalability**: Policy-based design allows easy role/capability additions
5. **Auditability**: All access decisions logged and traceable
6. **Performance**: Caching and database-level RLS for efficient queries

### Implementation Checklist

- ✅ 7 Roles defined with clear hierarchies
- ✅ 98 Atomic capabilities with standardized naming
- ✅ 100+ Endpoints mapped to role policies
- ✅ 36 UI Pages organized by access requirements
- ✅ JWT authentication with secure token validation
- ✅ @PreAuthorize method-level security
- ✅ VPD configuration for row-level data isolation
- ✅ Three-layer authorization model fully implemented
- ✅ Policy-capability-endpoint mapping completed
- ✅ Caching strategies for performance
- ✅ Comprehensive audit logging

---

## Related Documentation

- **ROLES.md** - Detailed role definitions and responsibilities
- **PHASE3_CAPABILITIES_DEFINITION.md** - Complete capability reference
- **PHASE5_ENDPOINT_POLICY_MAPPINGS.md** - Endpoint authorization matrix
- **SETUP_GUIDE.md** - System initialization guide
- **SQL Scripts** - Database initialization files (01-08)

---

**Next Steps**: Deploy RBAC system following setup guide and architecture patterns documented above.
