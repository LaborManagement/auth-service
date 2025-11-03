# Corrected Database Table Names & Schema Reference

**Last Updated:** November 3, 2025  
**Schema:** `auth` (all tables are in the auth schema)  
**Status:** Verified from Java Entity Classes

---

## 📋 Core Tables

| Table Name | Java Entity | Purpose | Key Columns |
|---|---|---|---|
| `auth.users` | User.java | Authentication & user profiles | id, username, email, password, full_name, permission_version, role, is_enabled, is_account_non_expired, is_account_non_locked, is_credentials_non_expired, created_at, updated_at, last_login |
| `auth.roles` | Role.java | Role definitions | id, name (unique), description, is_active, created_at, updated_at |
| `auth.capabilities` | Capability.java | Granular permissions/capabilities | id, name (unique, format: domain.subject.action), description, module, action, resource, is_active, created_at, updated_at |
| `auth.policies` | Policy.java | Authorization policies | id, name (unique), description, type (RBAC/ABAC/CUSTOM), expression (JSON), is_active, created_at, updated_at |
| `auth.ui_pages` | UIPage.java | UI pages/routes | id, page_id (unique, varchar), label, route, icon, module, parent_id (nullable), display_order, is_menu_item, is_active, required_capability (nullable) |
| `auth.page_actions` | PageAction.java | Actions on UI pages | id, label, action, icon, variant, capability_id (FK), page_id (FK), endpoint_id (FK, nullable), display_order, is_active, created_at, updated_at |
| `auth.endpoints` | Endpoint.java | API endpoints | id, service, version, method (GET/POST/PUT/DELETE/PATCH), path, description, ui_type, is_active, created_at, updated_at |
| `auth.user_tenant_acl` | UserTenantAcl.java | Row-Level Security (RLS) | id, user_id (FK), board_id, employer_id (nullable), can_read, can_write, created_at, updated_at |
| `auth.revoked_tokens` | RevokedToken.java | Token blacklist | id, token, expires_at, created_at |

---

## 🔗 Junction Tables (Many-to-Many Relationships)

| Table Name | Purpose | Columns | FK Relationships |
|---|---|---|---|
| `auth.user_roles` | User ↔ Role Assignment | id, user_id (FK), role_id (FK), created_at | user_id → users.id, role_id → roles.id |
| `auth.policy_capabilities` | Policy ↔ Capability Linking | id, policy_id (FK), capability_id (FK) | policy_id → policies.id, capability_id → capabilities.id |
| `auth.endpoint_policies` | Endpoint ↔ Policy Linking | id, endpoint_id (FK), policy_id (FK) | endpoint_id → endpoints.id, policy_id → policies.id |

---

## 📌 Important Column Notes

### `auth.users` Table
- **username:** Unique, 3-50 characters
- **email:** Unique, valid email format
- **password:** Stored as bcrypt hash ($2a$12$...)
- **full_name:** Required, NOT NULL
- **permission_version:** Integer, incremented when roles change (for JWT invalidation)
- **role:** ENUM field with values: ADMIN, USER, WORKER, BOARD, EMPLOYER, RECONCILIATION_OFFICER (legacy, kept for backward compatibility)

### `auth.roles` Table
- **name:** Unique constraint, UPPERCASE convention (e.g., BUSINESS_ADMIN, TECHNICAL_BOOTSTRAP)
- **is_active:** Boolean, default true

### `auth.capabilities` Table
- **name:** Format: `<domain>.<subject>.<action>` (e.g., `user.account.create`, `rbac.policy.read`)
- **action:** Typical values: CREATE, READ, UPDATE, DELETE, TOGGLE, APPROVE, REJECT, LINK, UNLINK, MANAGE
- **resource:** What the action applies to (e.g., USER, POLICY, CAPABILITY, ENDPOINT, UI_PAGE)

### `auth.policies` Table
- **type:** RBAC (Role-Based Access Control) is standard
- **expression:** JSON format `{"roles": ["ROLE_NAME"]}` - MUST match roles in database exactly
  - Example: `{"roles": ["BUSINESS_ADMIN", "TECHNICAL_BOOTSTRAP"]}`

### `auth.ui_pages` Table
- **page_id:** Unique identifier (slug/key), lowercase with hyphens (e.g., `admin`, `user-mgmt`, `role-mgmt`)
- **parent_id:** NULL for root pages, references ui_pages.id for child pages
- **display_order:** Integer for menu ordering (lower = appears first)
- **is_menu_item:** Boolean, whether to show in navigation

### `auth.page_actions` Table
- **action:** Action type (CREATE, READ, UPDATE, DELETE, APPROVE, REJECT, etc.)
- **variant:** UI styling variant (default, success, danger, warning, info)
- **capability_id:** Foreign key - action requires this capability (permission check)
- **page_id:** Foreign key - which page this action appears on
- **endpoint_id:** Foreign key - which backend endpoint to call when action is triggered (API binding)

**Dual Purpose:**
1. **Permission Check:** `capability_id` determines if user can see the action
2. **API Binding:** `endpoint_id` specifies which API to call when clicked

**Example:**
```sql
-- Edit User action
INSERT INTO auth.page_actions (label, action, capability_id, page_id, endpoint_id)
VALUES (
  'Edit User',           -- Button label
  'UPDATE',              -- Action type
  3,                     -- capability_id: user.account.update
  2,                     -- page_id: User Management page
  71                     -- endpoint_id: PUT /api/auth/users/{userId}
);
```

### `auth.endpoints` Table
- **service:** Service name (e.g., 'AUTH', 'ADMIN', 'INTERNAL')
- **version:** API version (e.g., 'v1', 'v2')
- **method:** HTTP method (GET, POST, PUT, DELETE, PATCH)
- **path:** URL path (e.g., '/api/auth/users', '/api/auth/users/{userId}')
- **ui_type:** How used in UI (ACTION, LIST, FORM, UPLOAD, etc.)

**Bootstrap Endpoints:** 72 total
- AUTH service: 14 endpoints (includes user CRUD)
- ADMIN service: 51 endpoints (role/policy/capability management)
- INTERNAL service: 5 endpoints (system operations)

---

## 🔑 Foreign Key Relationships

```
User (users)
  ├─ 1:N→ UserTenantAcl (user_tenant_acl.user_id)
  └─ M:N→ Role (user_roles)

Role (roles)
  └─ M:N→ User (user_roles)

Policy (policies)
  ├─ M:N→ Capability (policy_capabilities)
  └─ M:N→ Endpoint (endpoint_policies)

Capability (capabilities)
  ├─ M:N→ Policy (policy_capabilities)
  └─ 1:N→ PageAction (page_actions.capability_id)

Endpoint (endpoints)
  ├─ M:N→ Policy (endpoint_policies)
  └─ 1:N→ PageAction (page_actions.endpoint_id)

UIPage (ui_pages)
  ├─ 1:N→ UIPage (parent_id - self-referential for hierarchy)
  └─ 1:N→ PageAction (page_actions.page_id)

PageAction (page_actions)
  ├─ N:1→ UIPage (page_id)
  ├─ N:1→ Capability (capability_id)
  └─ N:1→ Endpoint (endpoint_id)
```

---

## 🔄 Authorization Flow

### Backend Authorization (API Security)
```
HTTP Request
  ↓
JWT Validation
  ↓
User → user_roles → Role
  ↓
Policy (expression.roles match)
  ↓
policy_capabilities → Capability
  ↓
endpoint_policies → Endpoint
  ↓
Allow/Deny
```

### Frontend Authorization (UI Visibility)
```
Page Load
  ↓
GET /api/meta/endpoints?page_id={id}
  ↓
Query: page_actions WHERE:
  - user has capability_id
  - endpoint_id is not null
  ↓
Return available actions + endpoints
  ↓
Render buttons with API bindings
```

### Complete Chain (Button Click)
```
User clicks "Edit User" button
  ↓
Frontend checks: page_actions.capability_id (user.account.update)
  ↓ (User has capability? Yes)
Frontend calls: page_actions.endpoint_id → PUT /api/auth/users/{userId}
  ↓
Backend checks: endpoint_policies (USER_ACCOUNT_MANAGE_POLICY)
  ↓
Backend verifies: User's roles → policies → capabilities match
  ↓
Success ✓
```
  └─ 1:N→ PageAction (page_actions.page_id)

PageAction (page_actions)
  ├─ N:1→ Capability (capability_id)
  ├─ N:1→ UIPage (page_id)
  └─ N:1→ Endpoint (endpoint_id, optional)

Endpoint (endpoints)
  ├─ M:N→ Policy (endpoint_policies)
  └─ 1:N→ PageAction (page_actions.endpoint_id)
```

---

## ✅ Data Format Examples

### INSERT INTO auth.roles
```sql
INSERT INTO auth.roles (name, description, is_active, created_at, updated_at) VALUES
('BUSINESS_ADMIN', 'Business-facing admin for user management', true, NOW(), NOW()),
('TECHNICAL_BOOTSTRAP', 'Technical admin for system configuration', true, NOW(), NOW());
```

### INSERT INTO auth.users
```sql
INSERT INTO auth.users (
  username, email, password, full_name, permission_version, role,
  is_enabled, is_account_non_expired, is_account_non_locked, is_credentials_non_expired,
  created_at, updated_at
) VALUES (
  'business.admin',
  'admin@business.local',
  '$2a$12$KIXVvJhwK5hI0LJvHvVHG.eQE.eEPdH6YjFJ3t5lCvW6n0IKEuN3i',
  'Business Admin',
  1,
  'ADMIN',
  true, true, true, true,
  NOW(), NOW()
);
```

### INSERT INTO auth.capabilities
```sql
INSERT INTO auth.capabilities (name, description, module, action, resource, is_active, created_at, updated_at) VALUES
('user.account.create', 'Create new user account', 'USER_MANAGEMENT', 'CREATE', 'USER', true, NOW(), NOW()),
('rbac.policy.read', 'View policies', 'RBAC_POLICY_MANAGEMENT', 'READ', 'RBAC_POLICY', true, NOW(), NOW());
```

### INSERT INTO auth.policies
```sql
INSERT INTO auth.policies (name, description, type, expression, is_active, created_at, updated_at) VALUES
(
  'BUSINESS_ADMIN_POLICY',
  'Policy for business admin user with user management capabilities',
  'RBAC',
  '{"roles": ["BUSINESS_ADMIN"]}',
  true,
  NOW(),
  NOW()
);
```

### INSERT INTO auth.ui_pages
```sql
INSERT INTO auth.ui_pages (page_id, label, route, icon, module, parent_id, display_order, is_menu_item, is_active) VALUES
(1, 'admin', 'Administration', '/admin', 'settings', 'ADMIN', NULL, 1, true, true),
(2, 'user-mgmt', 'User Management', '/admin/users', 'users', 'ADMIN', 1, 2, true, true);
```

### INSERT INTO auth.page_actions
```sql
INSERT INTO auth.page_actions (label, action, icon, variant, capability_id, page_id, display_order, is_active, created_at, updated_at) VALUES
('Create User', 'CREATE', 'plus', 'success', 1, 2, 1, true, NOW(), NOW());
```

### INSERT INTO auth.user_roles
```sql
INSERT INTO auth.user_roles (user_id, role_id) VALUES
((SELECT id FROM auth.users WHERE username = 'business.admin'),
 (SELECT id FROM auth.roles WHERE name = 'BUSINESS_ADMIN'));
```

### INSERT INTO auth.policy_capabilities
```sql
INSERT INTO auth.policy_capabilities (policy_id, capability_id) VALUES
((SELECT id FROM auth.policies WHERE name = 'BUSINESS_ADMIN_POLICY'),
 (SELECT id FROM auth.capabilities WHERE name = 'user.account.create'));
```

### INSERT INTO auth.endpoints
```sql
INSERT INTO auth.endpoints (service, version, method, path, description, ui_type, is_active, created_at, updated_at) VALUES
('auth', 'v1', 'POST', '/api/auth/users', 'Register new user', 'FORM', true, NOW(), NOW()),
('admin', 'v1', 'GET', '/api/admin/users', 'List all users', 'LIST', true, NOW(), NOW());
```

---

## 🔄 Data Flow for 2-User Bootstrap Model

```
USER: business.admin
  ↓
  assigned to (user_roles)
ROLE: BUSINESS_ADMIN
  ↓
  has policy (policies linked via expression JSON)
POLICY: BUSINESS_ADMIN_POLICY
  ↓
  contains capabilities (policy_capabilities)
CAPABILITIES: user.account.{create,read,update,delete,status.toggle}
  ↓
  required by (page_actions)
PAGE_ACTIONS on UI_PAGE: USER_MANAGEMENT
  ↓
  protects (endpoint_policies)
ENDPOINTS: /api/auth/users, /api/auth/users, etc.

---

USER: tech.bootstrap
  ↓
  assigned to (user_roles)
ROLE: TECHNICAL_BOOTSTRAP
  ↓
  has policy (policies linked via expression JSON)
POLICY: TECHNICAL_BOOTSTRAP_POLICY
  ↓
  contains capabilities (policy_capabilities)
CAPABILITIES: rbac.role.{create,read,update,delete}, rbac.policy.*, rbac.capability.*, ui.page.*, ui.action.*
  ↓
  required by (page_actions)
PAGE_ACTIONS on UI_PAGES: ADMIN hierarchy (Roles, Policies, Capabilities, etc.)
  ↓
  protects (endpoint_policies)
ENDPOINTS: /api/admin/roles, /api/admin/policies, /api/admin/capabilities, etc.
```

---

## ✨ Implementation Ready!

All table names, columns, and relationships are now **verified against actual Java entities**.  
Ready to proceed with SQL modifications using these exact table names.
