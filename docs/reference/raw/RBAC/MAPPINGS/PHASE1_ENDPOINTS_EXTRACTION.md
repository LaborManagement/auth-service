# PHASE 1: SYSTEM ENDPOINTS EXTRACTION & MATRIX

**Date:** November 2, 2025  
**Status:** ✅ COMPLETE - Ready for Review

---

## Summary

Extracted and organized **100+ endpoints** from 3 microservices (auth-service, payment-flow-service, reconciliation-service) across 14 controllers. Endpoints are categorized by functional area, service, and role access level, including HTTP method, path, parameters, and description.

---

## Endpoint Categories

### 1. AUTHENTICATION ENDPOINTS (`/api/auth`)
**Base Path:** `/api/auth`  
**Accessibility:** Public (No Auth Required) / Authenticated

| # | HTTP | Endpoint | Description | Auth Required | Access Level |
|---|------|----------|-------------|---------------|--------------|
| 1.1 | POST | `/api/auth/login` | User authentication - returns JWT token | ❌ No | Public |
| 1.2 | POST | `/api/auth/logout` | Token revocation and logout | ✅ Yes | Authenticated |
| 1.3 | POST | `/api/auth/users` | User registration (new account) | ❌ No | Public |
| 1.4 | GET | `/api/auth/ui-config` | Get UI configuration for current user | ✅ Yes | Authenticated |
| 1.5 | GET | `/api/auth/users` | Get all users in system | ✅ Yes | ADMIN_TECH, ADMIN_OPS |
| 1.6 | GET | `/api/auth/users/role/{role}` | Get users filtered by role | ✅ Yes | ADMIN_TECH, ADMIN_OPS |
| 1.7 | PUT | `/api/auth/users/{userId}/status` | Enable/disable user account | ✅ Yes | ADMIN_TECH, ADMIN_OPS |
| 1.8 | PUT | `/api/auth/users/{userId}/roles` | Update user roles | ✅ Yes | ADMIN_TECH, ADMIN_OPS |
| 1.9 | POST | `/api/auth/users/{userId}/invalidate-tokens` | Invalidate all tokens for user | ✅ Yes | ADMIN_TECH, ADMIN_OPS |
| 1.10 | GET | `/api/auth/roles` | Get all available roles | ✅ Yes | ADMIN_TECH, ADMIN_OPS |

---

### 2. AUTHORIZATION & PERMISSION ENDPOINTS (`/api`)
**Base Path:** `/api`  
**Accessibility:** Authenticated (provides user-specific data)

| # | HTTP | Endpoint | Description | Auth Required | Access Level |
|---|------|----------|-------------|---------------|--------------|
| 2.1 | GET | `/api/me/authorizations` | Get user's complete authorization matrix | ✅ Yes | All Authenticated Users |
| 2.2 | GET | `/api/meta/service-catalog` | Get system service catalog (endpoints + pages) | ✅ Yes | All Authenticated Users |
| 2.3 | GET | `/api/meta/endpoints` | Get all endpoints grouped by module | ✅ Yes | All Authenticated Users |
| 2.4 | GET | `/api/meta/pages` | Get all UI pages accessible to user | ✅ Yes | All Authenticated Users |

---

### 3. ADMIN: ROLES MANAGEMENT (`/api/admin/roles`)
**Base Path:** `/api/admin/roles`  
**Accessibility:** ADMIN_TECH, ADMIN_OPS (User Management)

| # | HTTP | Endpoint | Description | Auth Required | Access Level |
|---|------|----------|-------------|---------------|--------------|
| 3.1 | GET | `/api/admin/roles` | Get all roles | ✅ Yes | ADMIN_TECH, ADMIN_OPS |
| 3.2 | GET | `/api/admin/roles/with-permissions` | Get roles with permission counts | ✅ Yes | ADMIN_TECH, ADMIN_OPS |
| 3.3 | GET | `/api/admin/roles/{id}` | Get role by ID | ✅ Yes | ADMIN_TECH, ADMIN_OPS |
| 3.4 | GET | `/api/admin/roles/by-name/{name}` | Get role by name with permissions | ✅ Yes | ADMIN_TECH, ADMIN_OPS |
| 3.5 | POST | `/api/admin/roles` | Create new role | ✅ Yes | ADMIN_TECH |
| 3.6 | PUT | `/api/admin/roles/{id}` | Update role details | ✅ Yes | ADMIN_TECH |
| 3.7 | DELETE | `/api/admin/roles/{id}` | Delete role | ✅ Yes | ADMIN_TECH |
| 3.8 | POST | `/api/admin/roles/assign` | Assign role to user | ✅ Yes | ADMIN_TECH, ADMIN_OPS |
| 3.9 | POST | `/api/admin/roles/revoke` | Revoke role from user | ✅ Yes | ADMIN_TECH |

---

### 4. ADMIN: POLICIES MANAGEMENT (`/api/admin/policies`)
**Base Path:** `/api/admin/policies`  
**Accessibility:** ADMIN_TECH only (Configuration)

| # | HTTP | Endpoint | Description | Auth Required | Access Level |
|---|------|----------|-------------|---------------|--------------|
| 4.1 | GET | `/api/admin/policies` | Get all policies | ✅ Yes | ADMIN_TECH |
| 4.2 | GET | `/api/admin/policies/{id}` | Get policy by ID | ✅ Yes | ADMIN_TECH |
| 4.3 | POST | `/api/admin/policies` | Create new policy | ✅ Yes | ADMIN_TECH |
| 4.4 | PUT | `/api/admin/policies/{id}` | Update policy | ✅ Yes | ADMIN_TECH |
| 4.5 | DELETE | `/api/admin/policies/{id}` | Delete policy | ✅ Yes | ADMIN_TECH |
| 4.6 | PATCH | `/api/admin/policies/{id}/toggle-active` | Toggle policy active status | ✅ Yes | ADMIN_TECH |
| 4.7 | GET | `/api/admin/policies/{id}/capabilities` | Get capabilities linked to policy | ✅ Yes | ADMIN_TECH |
| 4.8 | POST | `/api/admin/policies/{id}/capabilities` | Link capability to policy | ✅ Yes | ADMIN_TECH |
| 4.9 | DELETE | `/api/admin/policies/{id}/capabilities/{capabilityId}` | Remove capability from policy | ✅ Yes | ADMIN_TECH |

---

### 5. ADMIN: CAPABILITIES MANAGEMENT (`/api/admin/capabilities`)
**Base Path:** `/api/admin/capabilities`  
**Accessibility:** ADMIN_TECH only (Configuration)

| # | HTTP | Endpoint | Description | Auth Required | Access Level |
|---|------|----------|-------------|---------------|--------------|
| 5.1 | GET | `/api/admin/capabilities` | Get all capabilities | ✅ Yes | ADMIN_TECH |
| 5.2 | GET | `/api/admin/capabilities/{id}` | Get capability by ID | ✅ Yes | ADMIN_TECH |
| 5.3 | POST | `/api/admin/capabilities` | Create new capability | ✅ Yes | ADMIN_TECH |
| 5.4 | PUT | `/api/admin/capabilities/{id}` | Update capability | ✅ Yes | ADMIN_TECH |
| 5.5 | DELETE | `/api/admin/capabilities/{id}` | Delete capability | ✅ Yes | ADMIN_TECH |
| 5.6 | PATCH | `/api/admin/capabilities/{id}/toggle-active` | Toggle capability active status | ✅ Yes | ADMIN_TECH |

---

### 6. ADMIN: ENDPOINTS MANAGEMENT (`/api/admin/endpoints`)
**Base Path:** `/api/admin/endpoints`  
**Accessibility:** ADMIN_TECH only (Configuration)

| # | HTTP | Endpoint | Description | Auth Required | Access Level |
|---|------|----------|-------------|---------------|--------------|
| 6.1 | GET | `/api/admin/endpoints` | Get all endpoints | ✅ Yes | ADMIN_TECH |
| 6.2 | GET | `/api/admin/endpoints/{id}` | Get endpoint by ID | ✅ Yes | ADMIN_TECH |
| 6.3 | POST | `/api/admin/endpoints` | Create new endpoint | ✅ Yes | ADMIN_TECH |
| 6.4 | PUT | `/api/admin/endpoints/{id}` | Update endpoint | ✅ Yes | ADMIN_TECH |
| 6.5 | DELETE | `/api/admin/endpoints/{id}` | Delete endpoint | ✅ Yes | ADMIN_TECH |
| 6.6 | PATCH | `/api/admin/endpoints/{id}/toggle-active` | Toggle endpoint active status | ✅ Yes | ADMIN_TECH |
| 6.7 | GET | `/api/admin/endpoints/{id}/policies` | Get policies linked to endpoint | ✅ Yes | ADMIN_TECH |
| 6.8 | POST | `/api/admin/endpoints/{id}/policies` | Link policy to endpoint | ✅ Yes | ADMIN_TECH |
| 6.9 | DELETE | `/api/admin/endpoints/{id}/policies/{policyId}` | Remove policy from endpoint | ✅ Yes | ADMIN_TECH |
| 6.10 | POST | `/api/admin/endpoints/bulk-policy-assignment` | Bulk assign policies to endpoints | ✅ Yes | ADMIN_TECH |

---

### 7. ADMIN: UI PAGES MANAGEMENT (`/api/admin/ui-pages`)
**Base Path:** `/api/admin/ui-pages`  
**Accessibility:** ADMIN_TECH only (Configuration)

| # | HTTP | Endpoint | Description | Auth Required | Access Level |
|---|------|----------|-------------|---------------|--------------|
| 7.1 | GET | `/api/admin/ui-pages` | Get all active UI pages | ✅ Yes | ADMIN_TECH |
| 7.2 | GET | `/api/admin/ui-pages/all` | Get all UI pages (including inactive) | ✅ Yes | ADMIN_TECH |
| 7.3 | GET | `/api/admin/ui-pages/{id}` | Get UI page by ID | ✅ Yes | ADMIN_TECH |
| 7.4 | POST | `/api/admin/ui-pages` | Create new UI page | ✅ Yes | ADMIN_TECH |
| 7.5 | PUT | `/api/admin/ui-pages/{id}` | Update UI page | ✅ Yes | ADMIN_TECH |
| 7.6 | DELETE | `/api/admin/ui-pages/{id}` | Delete UI page | ✅ Yes | ADMIN_TECH |
| 7.7 | PATCH | `/api/admin/ui-pages/{id}/toggle-active` | Toggle page active status | ✅ Yes | ADMIN_TECH |
| 7.8 | PATCH | `/api/admin/ui-pages/{id}/reorder` | Reorder UI pages | ✅ Yes | ADMIN_TECH |
| 7.9 | GET | `/api/admin/ui-pages/{id}/children` | Get child pages of a parent page | ✅ Yes | ADMIN_TECH |

---

### 8. ADMIN: PAGE ACTIONS MANAGEMENT (`/api/admin/page-actions`)
**Base Path:** `/api/admin/page-actions`  
**Accessibility:** ADMIN_TECH only (Configuration)

| # | HTTP | Endpoint | Description | Auth Required | Access Level |
|---|------|----------|-------------|---------------|--------------|
| 8.1 | GET | `/api/admin/page-actions` | Get all page actions | ✅ Yes | ADMIN_TECH |
| 8.2 | GET | `/api/admin/page-actions/{id}` | Get page action by ID | ✅ Yes | ADMIN_TECH |
| 8.3 | GET | `/api/admin/page-actions/page/{pageId}` | Get actions for specific page | ✅ Yes | ADMIN_TECH |
| 8.4 | POST | `/api/admin/page-actions` | Create new page action | ✅ Yes | ADMIN_TECH |
| 8.5 | PUT | `/api/admin/page-actions/{id}` | Update page action | ✅ Yes | ADMIN_TECH |
| 8.6 | DELETE | `/api/admin/page-actions/{id}` | Delete page action | ✅ Yes | ADMIN_TECH |
| 8.7 | PATCH | `/api/admin/page-actions/{id}/toggle-active` | Toggle action active status | ✅ Yes | ADMIN_TECH |
| 8.8 | PATCH | `/api/admin/page-actions/{id}/reorder` | Reorder page actions | ✅ Yes | ADMIN_TECH |

---

### 9. INTERNAL/MICROSERVICE ENDPOINTS (`/internal`)
**Base Path:** `/internal`  
**Accessibility:** Internal (Service-to-Service Communication)
**Purpose:** Used by other microservices (payment-flow-service, reconciliation-service, etc.)

| # | HTTP | Endpoint | Description | Auth Required | Access Level |
|---|------|----------|-------------|---------------|--------------|
| 9.1 | POST | `/internal/auth/introspect` | Token introspection (validate & get user info) | Service Auth | Internal Services |
| 9.2 | GET | `/internal/authz/endpoints/{endpointId}` | Get endpoint authorization info | Service Auth | Internal Services |
| 9.3 | GET | `/internal/authz/users/{userId}/matrix` | Get user's full authorization matrix | Service Auth | Internal Services |
| 9.4 | GET | `/internal/authz/endpoints/metadata` | Get endpoints metadata | Service Auth | Internal Services |
| 9.5 | POST | `/internal/authz/policies/evaluate` | Evaluate policies for a user | Service Auth | Internal Services |

---

---

## 10. PAYMENT FLOW SERVICE: WORKER ENDPOINTS (`/api/worker/`, `/api/v1/worker-payments`)
**Base Path:** `/api/worker/` and `/api/v1/worker-payments`  
**Accessibility:** WORKER, ADMIN_OPS
**Purpose:** Worker payment file upload and request management

| # | HTTP | Endpoint | Description | Auth Required | Access Level |
|---|------|----------|-------------|---------------|--------------|
| 10.1 | POST | `/api/v1/worker-payments` | Create new worker payment record | ✅ Yes | WORKER, ADMIN_OPS |
| 10.2 | POST | `/api/v1/worker-payments/secure` | Get worker payments with secure pagination | ✅ Yes | WORKER, ADMIN_OPS |
| 10.3 | GET | `/api/v1/worker-payments/{id}` | Get worker payment by ID | ✅ Yes | WORKER, ADMIN_OPS |
| 10.4 | GET | `/api/v1/worker-payments/by-reference-prefix` | Find payments by reference prefix | ✅ Yes | WORKER, ADMIN_OPS |
| 10.5 | PUT | `/api/v1/worker-payments/{id}` | Update worker payment | ✅ Yes | WORKER, ADMIN_OPS |
| 10.6 | DELETE | `/api/v1/worker-payments/{id}` | Delete worker payment | ✅ Yes | WORKER, ADMIN_OPS |
| 10.7 | GET | `/api/v1/worker-payments/by-uploaded-file-ref/{uploadedFileRef}` | Get payments by uploaded file reference | ✅ Yes | WORKER, ADMIN_OPS |
| 10.8 | POST | `/api/worker/uploaded-data/secure-paginated` | Get uploaded data with secure pagination | ✅ Yes | WORKER, ADMIN_OPS |
| 10.9 | POST | `/api/worker/uploaded-data/upload` | Upload payment file (multipart) | ✅ Yes | WORKER, ADMIN_OPS |
| 10.10 | POST | `/api/worker/uploaded-data/files/secure-summaries` | Get file upload summaries | ✅ Yes | WORKER, ADMIN_OPS |
| 10.11 | POST | `/api/worker/uploaded-data/file/{fileId}/validate` | Validate uploaded file | ✅ Yes | WORKER, ADMIN_OPS |
| 10.12 | GET | `/api/worker/uploaded-data/results/{fileId}` | Get validation results for file | ✅ Yes | WORKER, ADMIN_OPS |
| 10.13 | POST | `/api/worker/uploaded-data/file/{fileId}/generate-request` | Generate payment request from file | ✅ Yes | WORKER, ADMIN_OPS |
| 10.14 | DELETE | `/api/worker/uploaded-data/file/{fileId}` | Delete uploaded file | ✅ Yes | WORKER, ADMIN_OPS |
| 10.15 | GET | `/api/worker/uploaded-data/receipt/{receiptNumber}` | Get data by receipt number | ✅ Yes | WORKER, ADMIN_OPS |
| 10.16 | POST | `/api/worker/receipts/all/secure` | Get all worker receipts with pagination | ✅ Yes | WORKER, ADMIN_OPS |
| 10.17 | GET | `/api/worker/receipts/{receiptNumber}` | Get receipt by number | ✅ Yes | WORKER, ADMIN_OPS |
| 10.18 | POST | `/api/worker/receipts/{receiptNumber}/send-to-employer` | Send receipt to employer | ✅ Yes | WORKER, ADMIN_OPS |

---

## 11. PAYMENT FLOW SERVICE: EMPLOYER ENDPOINTS (`/api/employer/`)
**Base Path:** `/api/employer/`  
**Accessibility:** EMPLOYER, ADMIN_OPS
**Purpose:** Employer payment receipt validation and approval

| # | HTTP | Endpoint | Description | Auth Required | Access Level |
|---|------|----------|-------------|---------------|--------------|
| 11.1 | POST | `/api/employer/receipts/available/secure` | Get available receipts for employer | ✅ Yes | EMPLOYER, ADMIN_OPS |
| 11.2 | POST | `/api/employer/receipts/validate` | Validate payment receipt | ✅ Yes | EMPLOYER, ADMIN_OPS |

---

## 12. PAYMENT FLOW SERVICE: BOARD ENDPOINTS (`/api/v1/board-receipts`)
**Base Path:** `/api/v1/board-receipts`  
**Accessibility:** BOARD, ADMIN_OPS
**Purpose:** Board receipt processing and final approval

| # | HTTP | Endpoint | Description | Auth Required | Access Level |
|---|------|----------|-------------|---------------|--------------|
| 12.1 | POST | `/api/v1/board-receipts` | Create new board receipt | ✅ Yes | BOARD, ADMIN_OPS |
| 12.2 | POST | `/api/v1/board-receipts/secure` | Get board receipts with secure pagination | ✅ Yes | BOARD, ADMIN_OPS |
| 12.3 | GET | `/api/v1/board-receipts/{id}` | Get board receipt by ID | ✅ Yes | BOARD, ADMIN_OPS |
| 12.4 | POST | `/api/v1/board-receipts/process` | Process board receipt (approve/reject) | ✅ Yes | BOARD, ADMIN_OPS |
| 12.5 | PUT | `/api/v1/board-receipts/{id}` | Update board receipt | ✅ Yes | BOARD, ADMIN_OPS |
| 12.6 | DELETE | `/api/v1/board-receipts/{id}` | Delete board receipt | ✅ Yes | BOARD, ADMIN_OPS |

---

## 13. PAYMENT FLOW SERVICE: UTILITIES ENDPOINTS (`/api/uploaded-files`)
**Base Path:** `/api/uploaded-files`  
**Accessibility:** WORKER, EMPLOYER, BOARD, ADMIN_OPS
**Purpose:** File upload and download management

| # | HTTP | Endpoint | Description | Auth Required | Access Level |
|---|------|----------|-------------|---------------|--------------|
| 13.1 | POST | `/api/uploaded-files/secure-paginated` | Get uploaded files with pagination | ✅ Yes | WORKER, EMPLOYER, BOARD, ADMIN_OPS |
| 13.2 | GET | `/api/uploaded-files/{id}/download` | Download uploaded file | ✅ Yes | WORKER, EMPLOYER, BOARD, ADMIN_OPS |

---

## 14. RECONCILIATION SERVICE: MT940 INGESTION (`/api/mt940`)
**Base Path:** `/api/mt940`  
**Accessibility:** ADMIN_TECH, ADMIN_OPS
**Purpose:** MT940 bank statement file ingestion and processing

| # | HTTP | Endpoint | Description | Auth Required | Access Level |
|---|------|----------|-------------|---------------|--------------|
| 14.1 | POST | `/api/mt940/ingest` | Trigger MT940 file ingestion | ✅ Yes | ADMIN_TECH, ADMIN_OPS |

---

## 15. RECONCILIATION SERVICE: VAN INGESTION (`/api/van`)
**Base Path:** `/api/van`  
**Accessibility:** ADMIN_TECH, ADMIN_OPS
**Purpose:** VAN CSV file ingestion and processing

| # | HTTP | Endpoint | Description | Auth Required | Access Level |
|---|------|----------|-------------|---------------|--------------|
| 15.1 | POST | `/api/van/ingest` | Trigger VAN file ingestion | ✅ Yes | ADMIN_TECH, ADMIN_OPS |

---

## Endpoint Summary Statistics

| Category | Count | Auth Required | Service |
|----------|-------|----------------|---------|
| Authentication | 10 | Mixed (5 Yes, 5 No) | Auth Service |
| Authorization & Permissions | 4 | All Yes | Auth Service |
| Roles Management | 9 | All Yes | Auth Service |
| Policies Management | 9 | All Yes | Auth Service |
| Capabilities Management | 6 | All Yes | Auth Service |
| Endpoints Management | 10 | All Yes | Auth Service |
| UI Pages Management | 9 | All Yes | Auth Service |
| Page Actions Management | 8 | All Yes | Auth Service |
| Internal/Microservices | 5 | Service Auth | Auth Service |
| Worker Payment Management | 18 | All Yes | Payment Flow Service |
| Employer Payment Management | 2 | All Yes | Payment Flow Service |
| Board Receipt Management | 6 | All Yes | Payment Flow Service |
| File Upload Utilities | 2 | All Yes | Payment Flow Service |
| MT940 Ingestion | 1 | All Yes | Reconciliation Service |
| VAN Ingestion | 1 | All Yes | Reconciliation Service |
| **TOTAL ENDPOINTS** | **100+** | **95+ Authenticated** | **All Services** |

---

## Access Level Distribution

```
PUBLIC (No Auth):
  ├─ POST /api/auth/login
  ├─ POST /api/auth/users
  └─ GET  /api/auth/ui-config (Redirects unauthenticated to login)

AUTHENTICATED (All Users):
  ├─ GET /api/me/authorizations
  ├─ GET /api/meta/service-catalog
  ├─ GET /api/meta/endpoints
  └─ GET /api/meta/pages

ADMIN_TECH (System Configuration + Reconciliation):
  ├─ All /api/admin/policies/* endpoints (Full CRUD)
  ├─ All /api/admin/capabilities/* endpoints (Full CRUD)
  ├─ All /api/admin/endpoints/* endpoints (Full CRUD)
  ├─ All /api/admin/ui-pages/* endpoints (Full CRUD)
  ├─ All /api/admin/page-actions/* endpoints (Full CRUD)
  ├─ All /api/admin/roles/* (Full CRUD)
  ├─ All /api/auth/users/* (Create, Update, Roles)
  ├─ POST /api/mt940/ingest (Trigger MT940 ingestion)
  └─ POST /api/van/ingest (Trigger VAN ingestion)

ADMIN_OPS (Operations + Limited Admin + Reconciliation):
  ├─ GET /api/admin/roles (View)
  ├─ GET /api/admin/roles/{id} (View)
  ├─ POST /api/admin/roles/assign (Assign Role)
  ├─ GET /api/auth/users (View)
  ├─ PUT /api/auth/users/{userId}/status (Enable/Disable)
  ├─ PUT /api/auth/users/{userId}/roles (Update Roles)
  ├─ All /api/v1/worker-payments/* (Worker operations)
  ├─ All /api/worker/uploaded-data/* (Worker uploads)
  ├─ All /api/worker/receipts/* (Worker receipts)
  ├─ All /api/employer/receipts/* (Employer operations)
  ├─ All /api/v1/board-receipts/* (Board operations)
  ├─ All /api/uploaded-files/* (File management)
  ├─ POST /api/mt940/ingest (Trigger MT940 ingestion)
  └─ POST /api/van/ingest (Trigger VAN ingestion)

WORKER (Payment Flow):
  ├─ POST /api/v1/worker-payments/* (Create/View/Update/Delete)
  ├─ POST /api/worker/uploaded-data/* (Upload, Validate, Generate Request)
  ├─ POST /api/worker/receipts/* (View, Send to Employer)
  └─ GET /api/uploaded-files/{id}/download (Download files)

EMPLOYER (Payment Flow - Validation):
  ├─ POST /api/employer/receipts/available/secure (View receipts)
  ├─ POST /api/employer/receipts/validate (Validate payments)
  ├─ POST /api/v1/worker-payments/secure (View worker payments)
  └─ GET /api/uploaded-files/{id}/download (Download files)

BOARD (Payment Flow - Approval):
  ├─ POST /api/v1/board-receipts/* (View/Process/Update/Delete)
  ├─ POST /api/employer/receipts/* (View employer receipts)
  ├─ POST /api/v1/worker-payments/secure (View payments)
  └─ GET /api/uploaded-files/{id}/download (Download files)

INTERNAL SERVICES:
  ├─ POST /internal/auth/introspect
  ├─ GET  /internal/authz/endpoints/{endpointId}
  ├─ GET  /internal/authz/users/{userId}/matrix
  ├─ GET  /internal/authz/endpoints/metadata
  └─ POST /internal/authz/policies/evaluate
```

---

## ❓ **REVIEW QUESTIONS - Please Answer:**

1. ✅ **All 3 Services Included?** I've now included:
   - **Auth Service:** 9 categories (70 endpoints)
   - **Payment Flow Service:** 4 categories (28 endpoints)
   - **Reconciliation Service:** 2 categories (2 endpoints)
   - Are all endpoints from all services captured correctly?

2. ✅ **Access Levels Correct?** 
   - WORKER: Can perform all payment file operations
   - EMPLOYER: Can validate and approve worker submissions
   - BOARD: Can reconcile and give final approval
   - ADMIN_OPS: Can operate as all roles + user management
   - ADMIN_TECH: Full system configuration + reconciliation trigger
   - Does this alignment look correct?

3. ✅ **Missing Endpoints?** Should we add:
   - Reconciliation query endpoints? (e.g., `/api/reconciliation/reports`)
   - Payment report endpoints? (e.g., `/api/reports/payment-summary`)
   - Audit log endpoints? (e.g., `/api/admin/audit-logs`)

4. ✅ **Role-Endpoint Mapping:** Should we create a detailed matrix showing:
   - Which roles can access which endpoints?
   - Which capabilities enable which endpoints?
   - Example: `/api/worker/receipts/{id}/send-to-employer` requires `REQUEST_SUBMIT` capability

5. ✅ **Ready to Proceed?** Once you confirm, I'll move to **Phase 2: UI Pages & Actions**

---

**Waiting for your feedback! Please review all 100+ endpoints before we proceed! 👋**
