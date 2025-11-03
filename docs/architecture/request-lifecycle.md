# Request Lifecycle & Decision Flowchart

**Navigation:** Previous: [Data Map](data-map.md) → Next: [Policy Binding](policy-binding.md)

This document shows the complete journey of a request through the auth service, with decision points and enforcement layers.

## High-Level Request Flow

```mermaid
flowchart TD
    A["🔴 Client Request<br/>(JWT + endpoint path)"] --> B{JWT Valid?}
    B -->|No| C["❌ 401 Unauthorized<br/>(Invalid/Expired token)"]
    B -->|Yes| D["Extract user_id<br/>& roles from JWT"]
    D --> E["Load user's roles<br/>from auth.user_roles"]
    E --> F["Resolve policies<br/>from auth.role_policies"]
    F --> G["Load capabilities<br/>from auth.policy_capabilities"]
    G --> H{Endpoint<br/>requires<br/>policy?}
    H -->|Yes| I{User has<br/>required<br/>capability?}
    I -->|No| J["❌ 403 Forbidden<br/>(Missing capability)"]
    I -->|Yes| K["✅ Allow endpoint<br/>execution"]
    H -->|No| K
    K --> L["Execute endpoint<br/>with RLS applied"]
    L --> M["Query includes<br/>user_tenant_acl<br/>predicate"]
    M --> N["Return filtered<br/>results"]
    N --> O["🟢 200 Success<br/>(Data + Audit Log)"]
    C --> P["🔴 Log denial<br/>in audit.access_log"]
    J --> P
```

## Detailed Decision Points

### **Decision 1: JWT Validation**
- Verify signature using public key
- Check expiration time
- Confirm issuer matches expected value
- If failed: Return **401 Unauthorized**

```http
GET /api/admin/capabilities HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# Response if invalid:
HTTP/1.1 401 Unauthorized
{
  "error": "Invalid or expired token",
  "timestamp": "2025-11-02T10:30:00Z"
}
```

### **Decision 2: Role Resolution**
- Load `auth.user_roles` where `user_id = :userId`
- For each role, fetch `auth.role_policies`
- Aggregate all policies

**Example:**
```sql
SELECT DISTINCT p.id, p.name
FROM auth.policies p
JOIN auth.role_policies rp ON p.id = rp.policy_id
JOIN auth.user_roles ur ON rp.role_id = ur.role_id
WHERE ur.user_id = 'employee-123'
  AND ur.is_active = true
  AND rp.is_active = true;

-- Result: EMPLOYEE_POLICY, VIEWER_POLICY
```

### **Decision 3: Capability Matching**
- For each endpoint, check `auth.endpoint_policies`
- If endpoint requires a policy, verify user has it
- Load all capabilities from that policy

**Critical Check:**
```sql
-- Is user allowed to call GET /api/admin/capabilities?
SELECT EXISTS(
  SELECT 1
  FROM auth.endpoints e
  JOIN auth.endpoint_policies ep ON e.id = ep.endpoint_id
  JOIN auth.policies p ON ep.policy_id = p.id
  JOIN auth.policy_capabilities pc ON p.id = pc.policy_id
  JOIN auth.capabilities c ON pc.capability_id = c.id
  JOIN auth.role_policies rp ON p.id = rp.policy_id
  JOIN auth.user_roles ur ON rp.role_id = ur.role_id
  WHERE ur.user_id = 'employee-123'
    AND e.method = 'GET' 
    AND e.path = '/api/admin/capabilities'
);
```

**Complete Authorization Chain:**
```
User (id=123)
  ↓ user_roles
Role (BUSINESS_ADMIN)
  ↓ role_policies (not in current schema, but policies.expression has roles)
Policy (USER_ACCOUNT_MANAGE_POLICY)
  ↓ policy_capabilities
Capability (user.account.read)
  ↓ Check if endpoint requires this capability via endpoint_policies
Endpoint (GET /api/auth/users)
  ↓ endpoint_policies
Policy (USER_ACCOUNT_MANAGE_POLICY) ✓ MATCH
```

### **Decision 4: Row-Level Security (RLS)**
Even if allowed at the endpoint level, rows are filtered by `auth.user_tenant_acl`.

```sql
-- User can only see data for their tenants
SELECT * FROM auth.users u
WHERE u.id IN (
  SELECT user_id FROM auth.user_tenant_acl
  WHERE user_id = :userId
);
```

## Complete Request Example: "List Capabilities"

### **Scenario:**
Employee `alice.smith` calls `GET /api/admin/capabilities`

### **Step 1: JWT Validation**
```
Token issued by: auth-service
User ID: alice-uuid
Roles claim: [EMPLOYEE, VIEWER]
Expiry: 2025-11-02 11:30:00 (valid)
✅ Signature valid
```

### **Step 2: Load Roles & Policies**
```sql
-- Query: What policies does alice have?
SELECT p.name
FROM auth.policies p
JOIN auth.role_policies rp ON p.id = rp.policy_id
JOIN auth.user_roles ur ON rp.role_id = ur.role_id
WHERE ur.user_id = 'alice-uuid';

-- Result:
-- - EMPLOYEE_POLICY
-- - VIEWER_POLICY
```

### **Step 3: Verify Endpoint Permission**
```sql
-- Query: Is EMPLOYEE_POLICY or VIEWER_POLICY linked to this endpoint?
SELECT ep.policy_id
FROM auth.endpoint_policies ep
JOIN auth.endpoints e ON ep.endpoint_id = e.id
WHERE e.method = 'GET' 
  AND e.path = '/api/admin/capabilities'
  AND ep.policy_id IN (
    SELECT p.id FROM auth.policies 
    WHERE name IN ('EMPLOYEE_POLICY', 'VIEWER_POLICY')
  );

-- Result: Found VIEWER_POLICY → ✅ ALLOW
```

### **Step 4: Load Capabilities**
```sql
-- What can alice do?
SELECT c.name, c.description
FROM auth.capabilities c
JOIN auth.policy_capabilities pc ON c.id = pc.capability_id
JOIN auth.policies p ON pc.policy_id = p.id
WHERE p.name IN ('EMPLOYEE_POLICY', 'VIEWER_POLICY');

-- Result:
-- - capability.view (View any capability)
-- - audit.view (View audit logs)
-- - user.view (View user directory)
```

### **Step 5: Execute Endpoint (with RLS)**
```sql
-- Return capabilities, but only those alice's organization uses
SELECT c.* FROM auth.capabilities c
WHERE organization_id IN (
  SELECT employer_id FROM auth.user_tenant_acl
  WHERE user_id = 'alice-uuid'
);
```

### **Result:**
```json
HTTP/1.1 200 OK
{
  "capabilities": [
    {
      "id": "cap-001",
      "name": "capability.view",
      "description": "View any capability"
    },
    {
      "id": "cap-002",
      "name": "audit.view",
      "description": "View audit logs"
    }
  ],
  "timestamp": "2025-11-02T10:30:00Z"
}
```

And audit log records:
```sql
INSERT INTO audit.access_log (user_id, endpoint, capability, result, timestamp)
VALUES ('alice-uuid', 'GET /api/admin/capabilities', 'capability.view', 'ALLOWED', NOW());
```

---

## UI Authorization Flow

### **Frontend Permission Check**
When a user loads a page, the UI queries for available actions and endpoints:

```http
GET /api/meta/endpoints?page_id=2
Authorization: Bearer eyJhbGciOi...

Response:
{
  "pageId": 2,
  "pageName": "User Management",
  "actions": [
    {
      "id": 2,
      "label": "Create User",
      "endpoint": {
        "id": 3,
        "method": "POST",
        "path": "/api/auth/users"
      },
      "capability": "user.account.create"
    },
    {
      "id": 3,
      "label": "View Users",
      "endpoint": {
        "id": 5,
        "method": "GET",
        "path": "/api/auth/users"
      },
      "capability": "user.account.read"
    },
    {
      "id": 4,
      "label": "Edit User",
      "endpoint": {
        "id": 71,
        "method": "PUT",
        "path": "/api/auth/users/{userId}"
      },
      "capability": "user.account.update"
    }
  ]
}
```

### **Page Action Resolution**
```sql
-- Get page actions with endpoints for a specific page
SELECT 
    pa.id,
    pa.label,
    c.name as capability,
    e.id as endpoint_id,
    e.method,
    e.path
FROM auth.page_actions pa
JOIN auth.capabilities c ON pa.capability_id = c.id
JOIN auth.endpoints e ON pa.endpoint_id = e.id
WHERE pa.page_id = :pageId
  AND pa.capability_id IN (
    -- User's capabilities from their roles
    SELECT DISTINCT pc.capability_id
    FROM auth.user_roles ur
    JOIN auth.policies p ON p.expression->>'roles' ? ur.role_id::text
    JOIN auth.policy_capabilities pc ON p.id = pc.policy_id
    WHERE ur.user_id = :userId
  );
```

### **Dual Relationship in page_actions**
Each page action has two critical fields:
- **capability_id**: Which permission is required to see this action
- **endpoint_id**: Which API endpoint to call when action is triggered

```
User clicks "Edit User" button
  ↓
UI checks: Does user have capability "user.account.update"? ✓
  ↓
UI calls endpoint: PUT /api/auth/users/{userId}
  ↓
Backend checks: Does endpoint_policies allow this? ✓
  ↓
Success!
```

**Why both relationships?**
- **Frontend**: Needs capability to decide button visibility
- **Backend**: Needs endpoint-policy link to enforce security
- **page_actions**: Bridges both by storing capability_id + endpoint_id

---

## Permission Denied Scenarios

### **Scenario A: Missing Capability**
```
Request: DELETE /api/auth/users/123
User: bob.jones (BASIC_USER role only)
BASIC_USER has capabilities: [service.catalog.read, permission.check]
DELETE /api/auth/users/{userId} requires: [user.account.delete]

Decision: ❌ 403 Forbidden
Reason: User's roles don't grant user.account.delete capability
```

### **Scenario B: UI Action Hidden**
```
Page: User Management (page_id=2)
User: charlie.brown (BASIC_USER)
Available actions: Only "View Dashboard" (no user management capabilities)

UI Decision: ✓ Hide "Edit User" and "Delete User" buttons
Reason: page_actions query returns no results for those actions
        because user lacks user.account.update and user.account.delete
```

### **Scenario C: Endpoint Not Linked**
```
Request: PATCH /api/auth/users/123
User: alice.smith (BUSINESS_ADMIN)
Endpoint: Not registered in auth.endpoints table

Decision: ❌ 404 Not Found OR 403 Forbidden
Reason: Endpoint doesn't exist in registration table
```

### **Scenario D: Role-Based Lockout**
```
Request: POST /api/admin/roles
User: business.admin (BUSINESS_ADMIN role)
BUSINESS_ADMIN policies: USER_ACCOUNT_MANAGE_POLICY (user management only)
Endpoint requires: Role management capability

Decision: ❌ 403 Forbidden
Reason: User's policies don't include role management capabilities
```

---

## Performance Considerations

### **Optimization Points**

1. **JWT Caching** – Cache decoded JWT in request context (5-10ms savings)
2. **Role/Policy Cache** – Cache per-user capabilities with TTL (100ms refresh)
3. **Endpoint Registry** – In-memory map of endpoints (instant lookup)
4. **Index Hints** – Add indexes on `user_id`, `policy_id`, `endpoint_id`

### **Typical Latency**
- JWT validation: ~2-3ms
- Role resolution: ~5-10ms (cached: <1ms)
- Capability check: ~3-5ms
- RLS predicate: ~10-20ms (depends on tenant size)
- **Total: ~20-40ms per request**

---

## Next Steps

- See [Policy Binding Relationships](policy-binding.md) for how policies interconnect.
- Refer to [Common Permission Patterns](permission-patterns.md) for real-world examples.
- Read [Journey: Login To Data](../guides/login-to-data.md) for narrative walkthrough.
