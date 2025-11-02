# User Roles Reference

**Last Updated:** November 2, 2025  
**Version:** PHASE 4-5 Complete  
**Based On:** Phase 1-5 RBAC Documentation

## Role Overview

Seven distinct user roles aligned with business workflows and security principles. Each role has:
- **Specific Capabilities:** What operations can be performed (new `<domain>.<subject>.<action>` format)
- **Endpoints:** Which API endpoints are accessible
- **UI Pages:** Which pages are visible in the user interface
- **Data Scope:** What data the user can access (with VPD for WORKER role)

---

## 1. PLATFORM_BOOTSTRAP

**Type:** System Service Account  
**Usage:** One-time system initialization and seed data loading  
**User Count:** 1 (Service Account)  
**Can Only Be Used Once:** After initialization, should be disabled

### Purpose
System initialization, database catalog seeding, and core data structure creation. This role has complete access to all 98 capabilities and 100+ endpoints.

### Service Account
- **Username:** `platform.bootstrap`
- **Email:** `platform.bootstrap@lbe.local`
- **Token Type:** Service-to-service JWT (no human login)
- **⚠️ Security:** Disable after initial bootstrap

### Granted Capabilities (55/98)
- ✅ **User Management** (5): create, read, update, delete, status.toggle
- ❌ **Payment File Management** (0)
- ❌ **Payment Request Management** (0)
- ❌ **Worker Operations** (0)
- ❌ **Employer Operations** (0)
- ❌ **Board Operations** (0)
- ✅ **RBAC - Role Management** (6): All operations
- ✅ **RBAC - Policy Management** (7): All operations
- ✅ **RBAC - Capability Management** (6): All operations
- ✅ **API Endpoint Management** (7): All operations
- ✅ **UI Page Management** (8): All operations
- ✅ **Page Action Management** (7): All operations
- ✅ **System & Reporting** (8): All operations

### Accessible Endpoints (~55 endpoints)
- ✅ All `/api/auth/*` endpoints (user management)
- ✅ All `/api/admin/roles/*` endpoints
- ✅ All `/api/admin/policies/*` endpoints
- ✅ All `/api/admin/capabilities/*` endpoints
- ✅ All `/api/admin/endpoints/*` endpoints
- ✅ All `/api/admin/ui-pages/*` endpoints
- ✅ All `/api/admin/page-actions/*` endpoints
- ✅ All `/api/admin/audit-logs/*` endpoints
- ✅ All `/api/admin/system/*` endpoints

### UI Pages Access
- ✅ **System Configuration** (all 4 pages)
- ✅ **RBAC Configuration** (all 6 pages)
- ✅ **UI Configuration** (all 3 pages)
- ✅ **User Management** (all 5 pages)
- ✅ Hidden admin initialization pages

### Access Pattern
```
PLATFORM_BOOTSTRAP can:
✅ Create users, roles, policies, capabilities, endpoints, UI pages
✅ Link policies to capabilities
✅ Link policies to endpoints
✅ Link pages to policies
✅ Access all system configuration
✅ View all audit logs
✅ Initialize system data
✅ Manage system settings

❌ Create actual business data (payments, requests, workers, etc.)
❌ Access business workflows
```

### Use Cases
1. **First-time deployment:** Initialize the system
2. **Data migration:** Seed initial catalog data
3. **System maintenance:** Reset core configurations
4. **One-time setup:** Execute once, then disable

### Implementation Checklist
```
1. [ ] Create PLATFORM_BOOTSTRAP role
2. [ ] Create 7 policies (PLATFORM_BOOTSTRAP, ADMIN_TECH, ADMIN_OPS, BOARD, EMPLOYER, WORKER, TEST_USER)
3. [ ] Create 98 capabilities
4. [ ] Link capabilities to policies (see Phase 4)
5. [ ] Register 100+ endpoints
6. [ ] Link policies to endpoints (see Phase 5)
7. [ ] Create 36 UI pages
8. [ ] Link pages to policies (see UI_PAGE_POLICY table)
9. [ ] Create 7 seed users (one per role)
10. [ ] Disable PLATFORM_BOOTSTRAP account
```

### Security Notes
- **Disable immediately after bootstrap:** This account should never be used again
- **Audit all actions:** Every operation by this account is logged
- **Limit network access:** Restrict where this service account can connect from
- **Rotate secrets:** If credentials are exposed, rotate immediately
- **No interactive login:** Should not be used by humans

---

## 2. ADMIN_TECH

**Type:** System Administrator  
**Usage:** Technical administration of RBAC, configuration, and system settings  
**User Count:** 1-3  
**Required:** Must be created before ADMIN_OPS and business roles

### Purpose
Technical system administration including RBAC management, UI configuration, and system monitoring. Handles configuration, not business operations.

### Default User
- **Username:** `admin.tech`
- **Email:** `admin.tech@lbe.local`
- **⚠️ Security:** Set strong password on first login

### Granted Capabilities (51/98 - 52%)
- ✅ **User Management** (5): create, read, update, delete, status.toggle
- ❌ **Payment File Management** (0)
- ❌ **Payment Request Management** (0)
- ❌ **Worker Operations** (0)
- ❌ **Employer Operations** (0)
- ❌ **Board Operations** (0)
- ✅ **RBAC - Role Management** (6): All operations
- ✅ **RBAC - Policy Management** (7): All operations
- ✅ **RBAC - Capability Management** (6): All operations
- ✅ **API Endpoint Management** (7): All operations
- ✅ **UI Page Management** (8): All operations
- ✅ **Page Action Management** (7): All operations
- ✅ **System & Reporting** (4): Read audit, filter, export, read settings (NOT update settings)

### Accessible Endpoints (~51 endpoints)
- ✅ All `/api/admin/*` endpoints (except system settings modification)
- ✅ `/api/meta/service-catalog` (for configuration review)
- ✅ `/api/me/authorizations` (view own permissions)
- ✅ `/api/admin/audit-logs/*` (read-only)
- ❌ `/api/mt940/ingest` (ADMIN_OPS only)
- ❌ `/api/van/ingest` (ADMIN_OPS only)
- ❌ Business operation endpoints (worker, employer, board)

### UI Pages Access
- ✅ **System Configuration** (System Settings, Endpoints, Capabilities, Audit Logs)
- ✅ **RBAC Configuration** (all 6 pages)
- ✅ **UI Configuration** (all 3 pages)
- ✅ **User Management** (User Accounts Management)
- ❌ Dashboard
- ❌ Payment Management
- ❌ Request Management
- ❌ Approvals & Reconciliation

### Access Pattern
```
ADMIN_TECH can:
✅ Manage users (create, read, update, delete, toggle status)
✅ Manage roles and policies
✅ Create and manage capabilities
✅ Register and manage endpoints
✅ Configure UI pages and actions
✅ View audit logs
✅ Read system settings (not modify)
✅ View authorization matrices

❌ Modify system settings (ADMIN_OPS only)
❌ Trigger file ingestion (ADMIN_OPS only)
❌ Access worker operations
❌ Access employer operations
❌ Access board operations
```

### Use Cases
1. **User management:** Create users, assign roles
2. **RBAC configuration:** Set up roles, policies, capabilities
3. **System setup:** Configure endpoints, UI pages, page actions
4. **Troubleshooting:** View audit logs, authorization matrix
5. **System monitoring:** Check ingestion status, audit trail

### Implementation Notes
- First admin account to create after bootstrap
- Creates other admin accounts (ADMIN_OPS)
- Should NOT manage business data
- Read-only access to audit logs
- Cannot modify system settings (separation of concerns)

### Security Notes
- **Principle:** Cannot modify system settings (ADMIN_OPS only)
- **Data access:** No access to business data
- **System config:** Full access to RBAC and UI configuration
- **Audit:** All actions logged
- **Monitoring:** Can view but not modify system settings

---

## 3. ADMIN_OPS

**Type:** Operations Administrator  
**Usage:** Operational management, file ingestion, audit access  
**User Count:** 2-5  
**Supervises:** All business operations (workers, employers, board)

### Purpose
Operational management including file ingestion triggering, payment monitoring, audit log access, and system settings management. Handles operational oversight, not configuration.

### Default User
- **Username:** `admin.ops`
- **Email:** `admin.ops@lbe.local`
- **⚠️ Security:** Set strong password on first login

### Granted Capabilities (42/98 - 43%)
- ❌ **User Management** (0)
- ✅ **Payment File Management** (5): read, download, validate, summary.read, record.read, details.read
- ✅ **Payment Request Management** (3): read, track, validate
- ✅ **Worker Operations** (3): data.read, status.read, receipt.send
- ✅ **Employer Operations** (2): request.read, receipt.read
- ✅ **Board Operations** (2): request.read, receipt.read
- ❌ **RBAC - Role Management** (0)
- ❌ **RBAC - Policy Management** (0)
- ❌ **RBAC - Capability Management** (0)
- ❌ **API Endpoint Management** (0)
- ❌ **UI Page Management** (0)
- ❌ **Page Action Management** (0)
- ✅ **System & Reporting** (8): All (ingestion triggers, audit access, settings management)

### Accessible Endpoints (~42 endpoints)
- ✅ `/api/worker/uploaded-data/*` (file viewing, validation)
- ✅ `/api/v1/worker-payments/secure` (payment records)
- ✅ `/api/v1/board-receipts/secure` (board receipts)
- ✅ `/api/employer/receipts/available/secure` (employer payments)
- ✅ `/api/mt940/ingest` (ADMIN_OPS exclusive)
- ✅ `/api/van/ingest` (ADMIN_OPS exclusive)
- ✅ `/api/admin/audit-logs/*` (full access including export)
- ✅ `/api/admin/system/settings` (read and update)
- ✅ `/api/system/ingestion-status` (check ingestion status)
- ❌ User management endpoints
- ❌ Role/policy/capability management
- ❌ UI configuration endpoints

### UI Pages Access
- ✅ **Dashboard** (Operations Dashboard)
- ✅ **System Configuration** (Audit Logs Viewer, System Settings)
- ✅ **Payment Management** (File Upload Monitor, Payment Records View, Payment Details View)
- ✅ **Request Management** (Request Tracking, Request Status Monitor)
- ✅ **Approvals & Reconciliation** (Reconciliation Status View)
- ❌ RBAC Configuration
- ❌ UI Configuration
- ❌ User Management

### Access Pattern
```
ADMIN_OPS can:
✅ View uploaded payment files
✅ Trigger MT940 file ingestion
✅ Trigger VAN file ingestion
✅ Monitor payment records
✅ View worker/employer/board data
✅ Check ingestion status
✅ Access audit logs (read and export)
✅ Manage system settings

❌ Create users or modify roles
❌ Configure RBAC policies
❌ Configure UI pages
❌ Access system configuration
```

### Use Cases
1. **File ingestion:** Trigger MT940 and VAN file processing
2. **Payment monitoring:** Track payment status and records
3. **Operations oversight:** Monitor workers, employers, board
4. **Audit access:** Review all system actions and audit logs
5. **System management:** Modify system settings

### Implementation Notes
- Second admin account to create (after ADMIN_TECH)
- Only role that can trigger file ingestion
- Only role that can modify system settings
- Handles all operational monitoring
- Bridges admin configuration (ADMIN_TECH) and business operations

### Security Notes
- **Separation:** Cannot configure RBAC (ADMIN_TECH only)
- **File ingestion:** Exclusive access to MT940/VAN triggers
- **Settings:** Exclusive access to modify system settings
- **Audit:** Can read and export audit logs
- **Business data:** Can view but not modify business workflows

---

## 4. BOARD

**Type:** Board Member  
**Usage:** Financial approval and reconciliation operations  
**User Count:** 3-5  
**Reports To:** Finance Director

### Purpose
Financial approval, reconciliation, and board-level decision making. BOARD role processes approved requests from EMPLOYER and makes final payment decisions.

### Default User
- **Username:** `board1`
- **Email:** `board1@lbe.local`
- **⚠️ Security:** Set strong password on first login

### Granted Capabilities (17/98 - 17%)
- ❌ **User Management** (0)
- ✅ **Payment File Management** (5): read, download, summary.read, record.read, details.read
- ❌ **Payment Request Management** (0)
- ❌ **Worker Operations** (0)
- ❌ **Employer Operations** (0)
- ✅ **Board Operations** (7): All (request.read, payment.reconcile, decision.vote, payment.approve, payment.reject, receipt.read, receipt.process)
- ❌ **RBAC** (0)
- ❌ **UI Configuration** (0)
- ❌ **System & Reporting** (0)

### Accessible Endpoints (~17 endpoints)
- ✅ `/api/v1/board-receipts/secure` (view board receipts)
- ✅ `/api/v1/board-receipts/process` (process receipts)
- ✅ `/api/v1/board-receipts/{id}` (update receipts)
- ✅ `/api/v1/worker-payments/secure` (view payment records)
- ✅ `/api/v1/worker-payments/{id}` (payment details)
- ✅ `/api/uploaded-files/{id}/download` (download files)
- ✅ `/api/meta/pages` (get accessible pages)
- ✅ `/api/me/authorizations` (view own permissions)
- ❌ File upload endpoints
- ❌ Request creation endpoints
- ❌ User/RBAC management
- ❌ System configuration

### UI Pages Access
- ✅ **Dashboard** (Board Dashboard)
- ✅ **Payment Management** (Payment Records View)
- ✅ **Approvals & Reconciliation** (Board Approvals, Reconciliation Matrix, Board Receipts, Board Decision Tracker)
- ❌ Request Management (except read from view pages)
- ❌ User Management
- ❌ RBAC Configuration
- ❌ UI Configuration
- ❌ System Configuration

### Access Pattern
```
BOARD can:
✅ View board payment receipts
✅ Reconcile payments
✅ Vote on decisions
✅ Approve payments
✅ Reject payments
✅ Process receipts
✅ View payment records
✅ Download payment files

❌ Create payment requests
❌ Upload files
❌ Manage workers or employers
❌ Create users
❌ Manage RBAC
❌ Modify system settings
```

### Use Cases
1. **Payment approval:** Review and approve employer-validated payments
2. **Reconciliation:** Reconcile payment discrepancies
3. **Board decisions:** Vote on financial matters
4. **Payment processing:** Process final receipts
5. **Audit trail:** Review payment history

### Data Access (VPD-Free)
- ✅ Can see all board-level payments and receipts
- ✅ Can see all worker and employer submissions
- ✅ Not subject to VPD restrictions (board-level access)
- ✅ View only - cannot modify source data

### Implementation Notes
- Created after ADMIN_OPS
- Represents financial decision-makers
- Limited to board-specific operations
- No system administration access
- No business data creation

### Security Notes
- **Read-heavy:** Mostly read operations
- **Approvals:** Can approve/reject payments
- **Data scope:** Board-level only (no department boundaries)
- **Audit:** All approval actions logged
- **Segregation:** Cannot modify payment sources

---

## 5. EMPLOYER

**Type:** Business User (Employer Organization)  
**Usage:** Request validation and approval workflow  
**User Count:** 10-20 per employer organization  
**Reports To:** Employer Admin

### Purpose
Request validation and approval from workers. EMPLOYER role validates worker-submitted payments and routes approved requests to BOARD for final approval.

### Default User
- **Username:** `employer1`
- **Email:** `employer1@lbe.local`
- **⚠️ Security:** Set strong password on first login

### Granted Capabilities (19/98 - 19%)
- ❌ **User Management** (0)
- ✅ **Payment File Management** (5): read, download, summary.read, record.read, details.read
- ✅ **Payment Request Management** (9): All (create, read, update, delete, submit, track, validate, approve, reject)
- ❌ **Worker Operations** (0)
- ✅ **Employer Operations** (5): All (request.read, request.validate, payment.approve, payment.reject, receipt.read)
- ❌ **Board Operations** (0)
- ❌ **RBAC** (0)
- ❌ **UI Configuration** (0)
- ❌ **System & Reporting** (0)

### Accessible Endpoints (~19 endpoints)
- ✅ `/api/v1/worker-payments/secure` (view worker payments)
- ✅ `/api/v1/worker-payments/{id}` (payment details)
- ✅ `/api/worker/uploaded-data/secure-paginated` (view uploaded files)
- ✅ `/api/uploaded-files/{id}/download` (download files)
- ✅ `/api/employer/receipts/available/secure` (view available payments)
- ✅ `/api/employer/receipts/validate` (validate requests)
- ✅ `/api/v1/board-receipts` (submit to board)
- ✅ `/api/meta/pages` (get accessible pages)
- ✅ `/api/me/authorizations` (view own permissions)
- ❌ File upload endpoints
- ❌ Worker receipt sending
- ❌ Board approval operations
- ❌ User/RBAC management
- ❌ System configuration

### UI Pages Access
- ✅ **Dashboard** (Employer Dashboard)
- ✅ **Payment Management** (Payment Records View, Payment Details View)
- ✅ **Request Management** (All 5 pages: Creation, List, Details, Approval, Status Tracking)
- ❌ Approvals & Reconciliation (not approval, board-level)
- ❌ User Management
- ❌ RBAC Configuration
- ❌ UI Configuration
- ❌ System Configuration

### Access Pattern
```
EMPLOYER can:
✅ View worker payment submissions
✅ Create payment requests
✅ View request details
✅ Update request information
✅ Validate payment requests
✅ Approve requests for board submission
✅ Reject requests with reason
✅ Track request status
✅ Submit approved requests to board
✅ Download payment files

❌ Upload files (worker only)
❌ Create worker records (worker only)
❌ Board-level approvals (board only)
❌ Create users
❌ Manage RBAC
❌ Modify system settings
```

### Use Cases
1. **Request validation:** Review worker-submitted payments for accuracy
2. **Request approval:** Approve validated payments for board submission
3. **Request rejection:** Reject payments with reasons (returned to worker)
4. **Status tracking:** Monitor request progress through workflow
5. **File review:** Download and examine payment files

### Data Access (VPD-Protected)
- ✅ Can see all requests from workers in their organization
- ✅ Cannot see requests from other organizations (VPD enforced)
- ✅ VPD filter: organization_id matches user's organization
- ✅ Read/write access to requests within organization scope

### Implementation Notes
- Created after ADMIN_OPS
- Multiple users per employer organization
- Each user typically belongs to one organization (VPD-enforced)
- Represents employer approval authority
- Can create new request records

### Security Notes
- **VPD enforced:** Cannot see other organizations' data
- **Organization scope:** Operates only within organization
- **Approval authority:** Can approve or reject
- **Audit:** All approvals logged
- **Data isolation:** Database row-level security enforces boundaries

---

## 6. WORKER

**Type:** End User (Worker/Employee)  
**Usage:** Payment request submission and status tracking  
**User Count:** 50-100+  
**Reports To:** Employer

### Purpose
Personal payment submission and status tracking. WORKER role uploads personal payment data and creates payment requests which are routed through EMPLOYER → BOARD for approval.

### Default User
- **Username:** `worker1`
- **Email:** `worker1@lbe.local`
- **⚠️ Security:** Set strong password on first login

### Granted Capabilities (14/98 - 14%)
- ❌ **User Management** (0)
- ✅ **Payment File Management** (3): upload, read, validate
- ✅ **Payment Request Management** (5): create, read, submit, track, validate
- ✅ **Worker Operations** (6): All (data.upload, data.read, request.create, request.submit, status.read, receipt.send)
- ❌ **Employer Operations** (0)
- ❌ **Board Operations** (0)
- ❌ **RBAC** (0)
- ❌ **UI Configuration** (0)
- ❌ **System & Reporting** (0)

### Accessible Endpoints (~14 endpoints)
- ✅ `/api/worker/uploaded-data/upload` (upload payment file)
- ✅ `/api/worker/uploaded-data/secure-paginated` (view own files)
- ✅ `/api/worker/uploaded-data/file/{fileId}/validate` (validate file)
- ✅ `/api/worker/uploaded-data/file/{fileId}/generate-request` (create request)
- ✅ `/api/v1/worker-payments/secure` (view own payments)
- ✅ `/api/v1/worker-payments/{id}` (payment details)
- ✅ `/api/worker/receipts/{receiptNumber}/send-to-employer` (send receipt)
- ✅ `/api/uploaded-files/{id}/download` (download own files)
- ✅ `/api/meta/pages` (get accessible pages)
- ✅ `/api/me/authorizations` (view own permissions)
- ❌ Other workers' data (VPD-enforced)
- ❌ Employer operations
- ❌ Board operations
- ❌ User/RBAC management
- ❌ System configuration

### UI Pages Access
- ✅ **Dashboard** (Worker Dashboard)
- ✅ **Payment Management** (File Upload, Payment Records View, Payment Status View)
- ✅ **Request Management** (My Requests)
- ❌ Approvals & Reconciliation
- ❌ User Management
- ❌ RBAC Configuration
- ❌ UI Configuration
- ❌ System Configuration

### Access Pattern
```
WORKER can:
✅ Upload personal payment files
✅ View own uploaded files
✅ Validate payment files
✅ Create payment requests
✅ View own payment records
✅ View own request status
✅ Submit requests to employer
✅ Send receipts to employer
✅ Download own files

❌ View other workers' data (VPD-protected)
❌ Create employer records
❌ Approve payments (employer+ only)
❌ Create users
❌ Manage RBAC
❌ Modify system settings
```

### Data Access (VPD-Protected)
- ✅ Can see ONLY their own payment records
- ✅ Cannot see other workers' data (VPD enforced at database)
- ✅ VPD filter: user_id or employee_id matches authenticated user
- ✅ Even if endpoint called, database returns only user's data
- ✅ Read/write access limited to own data

### Implementation Notes
- Created after ADMIN_OPS and business roles
- Highest number of users (50-100+)
- Each worker typically belongs to one organization
- Data isolation enforced by VPD at database level
- Submit workflow: WORKER → EMPLOYER → BOARD

### Security Notes
- **VPD critical:** Database-level row security isolates worker data
- **Cannot bypass:** Even if SQL called directly, only own data visible
- **Organization scope:** Operates within assigned organization
- **Limited endpoints:** Only payment-related operations
- **Audit:** All data access logged
- **No RBAC access:** Cannot create users or manage roles

---

## 7. TEST_USER

**Type:** QA/Testing Account  
**Usage:** Testing and QA with comprehensive non-destructive access  
**User Count:** 1-3 (QA Team)  
**Reports To:** QA Lead

### Purpose
Comprehensive testing and QA access to all business workflows with limited destructive operations. Allows testing all user journeys (WORKER → EMPLOYER → BOARD) in single account.

### Default User
- **Username:** `test.user`
- **Email:** `test.user@lbe.local`
- **⚠️ Security:** Only for test environment or data-masked production

### Granted Capabilities (50/98 - 51%)
- ✅ **User Management** (1): read only
- ✅ **Payment File Management** (8): All (upload, read, download, delete, validate, summary.read, record.read, details.read)
- ✅ **Payment Request Management** (6): create, read, update, delete, track, validate (NOT submit/approve/reject)
- ✅ **Worker Operations** (6): All (data.upload, data.read, request.create, request.submit, status.read, receipt.send)
- ✅ **Employer Operations** (5): All (request.read, request.validate, payment.approve, payment.reject, receipt.read)
- ✅ **Board Operations** (7): All (request.read, payment.reconcile, decision.vote, payment.approve, payment.reject, receipt.read, receipt.process)
- ✅ **RBAC - Role Management** (2): read, assign (NOT create, delete)
- ✅ **RBAC - Policy Management** (2): read, link-capability (NOT create, delete)
- ✅ **RBAC - Capability Management** (2): read, read-matrix (NOT create, delete)
- ✅ **API Endpoint Management** (2): read, link-policy (NOT create, delete)
- ✅ **UI Page Management** (2): read, read-children (NOT create, delete)
- ✅ **Page Action Management** (2): read, read-by-page (NOT create, delete)
- ✅ **System & Reporting** (4): audit.read, audit.filter, ingestion.read-status, settings.read (NOT trigger, modify)

### Accessible Endpoints (~50 endpoints)
- ✅ All read-heavy endpoints for business operations
- ✅ Most POST/PUT endpoints for testing workflows
- ✅ All business flow endpoints (worker, employer, board operations)
- ✅ All audit log endpoints (read-only)
- ✅ All metadata endpoints (pages, endpoints, roles, policies, capabilities)
- ❌ `/api/mt940/ingest` (ADMIN_OPS only)
- ❌ `/api/van/ingest` (ADMIN_OPS only)
- ❌ User creation/deletion endpoints
- ❌ Role/policy deletion endpoints
- ❌ System settings modification
- ❌ Capability/endpoint deletion

### UI Pages Access
- ✅ **Dashboard** (all 3: Worker, Employer, Board dashboards)
- ✅ **Payment Management** (all 5 pages)
- ✅ **Request Management** (all 5 pages)
- ✅ **Approvals & Reconciliation** (all 5 pages)
- ✅ **User Management** (1 page: read-only)
- ✅ **RBAC Configuration** (all 6 pages - read-only)
- ✅ **UI Configuration** (all 3 pages - read-only)
- ✅ **System Configuration** (4 pages - read-only for audit/settings)
- Total: 35/36 pages accessible

### Access Pattern
```
TEST_USER can:
✅ Upload payment files
✅ Create payment requests
✅ Validate payments (both as employer and worker)
✅ Approve payments (both as employer and board)
✅ Reconcile payments (as board)
✅ Vote on board decisions
✅ Track request status
✅ View all dashboards
✅ Read all RBAC configuration
✅ Access all audit logs
✅ View system settings

❌ Trigger file ingestion (MT940/VAN)
❌ Create/delete users
❌ Create/delete roles
❌ Create/delete policies
❌ Create/delete capabilities
❌ Modify system settings
❌ Export audit logs (read-only)
```

### Use Cases
1. **End-to-end testing:** Test complete WORKER → EMPLOYER → BOARD workflow
2. **Regression testing:** Verify all business operations work correctly
3. **API testing:** Call all endpoints with valid/invalid data
4. **UI testing:** Verify all pages render and work correctly
5. **Audit verification:** Review all audit logs for test operations

### Data Access (Non-Destructive)
- ✅ Can create and read all business data
- ✅ Cannot delete core system data (roles, policies, capabilities)
- ✅ Cannot trigger system processes (file ingestion)
- ✅ Cannot modify system configuration
- ✅ Safe for testing without risk of breaking system

### Implementation Notes
- Created in test environment (or data-masked production)
- Single account tests all workflows
- Represents comprehensive testing coverage
- Useful for regression testing
- Cannot break core system configuration

### Security Notes
- **Test environment only:** Should never exist in production with real data
- **Data-masked:** If used in production, data must be masked/anonymized
- **Non-destructive:** Cannot delete or break system configuration
- **Read audit logs:** Can review all test operations
- **Audit:** All TEST_USER operations marked in audit logs

---

## Role Access Comparison Matrix

| Capability Domain | Total | BOOTSTRAP | ADMIN_TECH | ADMIN_OPS | BOARD | EMPLOYER | WORKER | TEST_USER |
|-----------------|-------|-----------|-----------|-----------|-------|----------|--------|-----------|
| User Management | 5 | ✅ All | ✅ All | ❌ None | ❌ None | ❌ None | ❌ None | 🔍 Read |
| Payment File Management | 8 | ❌ None | ❌ None | ✅ 5/8 | ✅ 5/8 | ✅ 5/8 | ✅ 3/8 | ✅ All |
| Payment Request Management | 9 | ❌ None | ❌ None | ✅ 3/9 | ❌ None | ✅ All | ✅ 5/9 | ✅ 6/9 |
| Worker Operations | 6 | ❌ None | ❌ None | ✅ 3/6 | ❌ None | ❌ None | ✅ All | ✅ All |
| Employer Operations | 5 | ❌ None | ❌ None | ✅ 2/5 | ❌ None | ✅ All | ❌ None | ✅ All |
| Board Operations | 7 | ❌ None | ❌ None | ✅ 2/7 | ✅ All | ❌ None | ❌ None | ✅ All |
| RBAC - Role Management | 6 | ✅ All | ✅ All | ❌ None | ❌ None | ❌ None | ❌ None | 🔍 2/6 |
| RBAC - Policy Management | 7 | ✅ All | ✅ All | ❌ None | ❌ None | ❌ None | ❌ None | 🔍 2/7 |
| RBAC - Capability Management | 6 | ✅ All | ✅ All | ❌ None | ❌ None | ❌ None | ❌ None | 🔍 2/6 |
| API Endpoint Management | 7 | ✅ All | ✅ All | ❌ None | ❌ None | ❌ None | ❌ None | 🔍 2/7 |
| UI Page Management | 8 | ✅ All | ✅ All | ❌ None | ❌ None | ❌ None | ❌ None | 🔍 2/8 |
| Page Action Management | 7 | ✅ All | ✅ All | ❌ None | ❌ None | ❌ None | ❌ None | 🔍 2/7 |
| System & Reporting | 8 | ✅ All | ✅ 4/8 | ✅ All | ❌ None | ❌ None | ❌ None | 🔍 4/8 |
| **TOTAL** | **98** | **55** | **51** | **42** | **17** | **19** | **14** | **50** |
| **Percentage** | - | **56%** | **52%** | **43%** | **17%** | **19%** | **14%** | **51%** |

**Legend:** ✅ = Full Access | 🔍 = Read-Only | ❌ = No Access

---

## Role Hierarchy & Business Workflow

### Settlement Flow (Data Progression)
```
WORKER
├─ Uploads payment file
├─ Creates payment request
├─ Submits to EMPLOYER
│
EMPLOYER
├─ Receives request from WORKER
├─ Validates request
├─ Approves/rejects
├─ If approved, submits to BOARD
│
BOARD
├─ Receives approved request from EMPLOYER
├─ Reviews and reconciles
├─ Votes on decision
├─ Approves/rejects payment
├─ Processes final receipt
│
ADMIN_OPS (Oversight)
├─ Monitors all operations
├─ Triggers file ingestion
├─ Reviews audit logs
├─ Manages system settings
│
ADMIN_TECH (Configuration)
├─ Sets up users
├─ Manages RBAC policies
├─ Configures UI pages
├─ Manages endpoints
│
PLATFORM_BOOTSTRAP (One-time)
└─ Initializes system (disabled after setup)
```

### Admin Hierarchy
```
PLATFORM_BOOTSTRAP
├─ System Initialization (one-time only)
│
├─ ADMIN_TECH
│  └─ System Configuration
│     ├─ User Management (create users)
│     ├─ RBAC Management (roles, policies)
│     ├─ Endpoint Management
│     └─ UI Configuration
│
└─ ADMIN_OPS
   └─ Operations Management
      ├─ File Ingestion
      ├─ System Monitoring
      ├─ Settings Management
      └─ Audit Access
```

---

## Creation Order (Recommended)

1. **PLATFORM_BOOTSTRAP** (system service account)
   - Used only for initial bootstrap
   - Create before any other roles

2. **ADMIN_TECH** (technical administrator)
   - Created via PLATFORM_BOOTSTRAP account
   - Sets up remaining infrastructure

3. **ADMIN_OPS** (operations administrator)
   - Created by ADMIN_TECH user
   - Handles operations and monitoring

4. **Business Roles** (BOARD, EMPLOYER, WORKER)
   - Created by ADMIN_TECH user
   - Ready for operational use

5. **TEST_USER** (QA account)
   - Created by ADMIN_TECH user
   - Only in test environment

---

## User Assignment Guidelines

### ADMIN_TECH
- Assign to: IT staff, system administrators
- Number: 1-3 trusted individuals
- Responsibility: System administration
- No business data access

### ADMIN_OPS
- Assign to: Operations managers, supervisors
- Number: 2-5 per organization
- Responsibility: Operations oversight
- Can trigger file ingestion

### BOARD
- Assign to: Finance team, board members
- Number: 3-5 per board
- Responsibility: Final payment approval
- Votes on decisions

### EMPLOYER
- Assign to: Employer staff, coordinators
- Number: 10-20 per employer
- Responsibility: Request validation
- Organization-scoped (VPD)

### WORKER
- Assign to: Employees, contractors
- Number: 50-100+
- Responsibility: Payment submission
- Personal data only (VPD)

### TEST_USER
- Assign to: QA team
- Number: 1-3
- Responsibility: Testing and verification
- Test environment only

---

## Data Isolation & Security

### Virtual Private Data (VPD)
Three roles use VPD for data isolation:

#### WORKER VPD
- **Isolation:** user_id filter
- **Scope:** Only own payment records
- **Enforced:** Database row-level security
- **Cannot bypass:** Even direct SQL shows only own data

#### EMPLOYER VPD
- **Isolation:** organization_id filter
- **Scope:** Only own organization's requests
- **Enforced:** Database row-level security
- **Cannot see:** Other organizations' data

### Data Visibility Chart
```
WORKER
├─ Sees: Own data only (VPD-protected)
├─ Creates: Own payment requests
└─ Cannot see: Other workers' data, employer reviews, board decisions

EMPLOYER
├─ Sees: Own organization's data (VPD-protected)
├─ Receives: Requests from workers in organization
├─ Sends: Approved requests to BOARD
└─ Cannot see: Other organizations' data

BOARD
├─ Sees: All requests (board-level, no VPD)
├─ Receives: Requests from all employers
├─ Approves: Final payment decisions
└─ Can see: All data across all organizations

ADMIN_OPS
├─ Sees: All operational data (monitoring)
├─ Cannot modify: Source payment records
└─ Can review: Audit logs of all actions

ADMIN_TECH
├─ Sees: System configuration only
├─ Cannot access: Business data
└─ Manages: User accounts and RBAC
```

---

## Transitioning Between Roles

### Same User, Multiple Roles
A user can have multiple roles if needed:

```sql
-- Assign additional role to user
INSERT INTO user_role_assignment (user_id, role_id, assigned_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.username = 'john.doe' AND r.name = 'ADMIN_OPS'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- Verify user's roles
SELECT u.username, array_agg(r.name) as roles
FROM users u
LEFT JOIN user_role_assignment ura ON u.id = ura.user_id
LEFT JOIN roles r ON ura.role_id = r.id
WHERE u.username = 'john.doe'
GROUP BY u.id, u.username;

-- Remove role if needed
DELETE FROM user_role_assignment
WHERE user_id = (SELECT id FROM users WHERE username = 'john.doe')
AND role_id = (SELECT id FROM roles WHERE name = 'WORKER');
```

### Role Changes Best Practices
1. **Review current permissions** before change
2. **Document reason** for role change in audit
3. **Test new access** immediately after assignment
4. **Invalidate tokens** to force re-authentication
5. **Verify audit logs** show correct new permissions

---

## JWT Token Structure

JWT tokens issued to users contain role information:

```json
{
  "sub": "john.doe",
  "uid": 123,
  "roles": ["EMPLOYER"],
  "pv": 1,
  "jti": "token-uuid",
  "iat": 1667410800,
  "exp": 1667497200
}
```

**Token claims:**
- `sub`: Username (subject)
- `uid`: User ID (required for audit)
- `roles`: Array of role names
- `pv`: Permission version (bumped on capability changes)
- `jti`: Token ID (for token revocation)
- `iat`: Issued at (timestamp)
- `exp`: Expiration (timestamp)

---

## Related Documentation

- **[Setup Guide](setup.md)** - How to create these roles and policies
- **[Testing Guide](testing.md)** - How to verify roles work correctly
- **[RBAC README](../RBAC/README.md)** - Complete RBAC system details
- **[Phase 1: Endpoints](../../PHASE1_ENDPOINTS_EXTRACTION.md)** - All 100+ endpoints
- **[Phase 2: UI Pages](../../PHASE2_UI_PAGES_ACTIONS.md)** - All 36 UI pages
- **[Phase 3: Capabilities](../../PHASE3_CAPABILITIES_DEFINITION.md)** - All 98 capabilities
- **[Phase 4: Policies](../../PHASE4_POLICY_CAPABILITY_MAPPINGS.md)** - Policy-capability mappings
- **[Phase 5: Endpoints](../../PHASE5_ENDPOINT_POLICY_MAPPINGS.md)** - Endpoint-policy mappings

---

**Last Updated:** November 2, 2025  
**Version:** Phase 4-5 Complete (98 Capabilities, 100+ Endpoints, 7 Roles)
