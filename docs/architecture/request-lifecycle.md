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
  FROM auth.endpoint_policies ep
  JOIN auth.policies p ON ep.policy_id = p.id
  JOIN auth.role_policies rp ON p.id = rp.policy_id
  JOIN auth.user_roles ur ON rp.role_id = ur.role_id
  WHERE ur.user_id = 'employee-123'
    AND ep.endpoint_id = (
      SELECT id FROM auth.endpoints 
      WHERE method = 'GET' AND path = '/api/admin/capabilities'
    )
);
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

## Permission Denied Scenarios

### **Scenario A: Missing Capability**
```
Request: DELETE /api/admin/capabilities/cap-001
User: bob.jones (VIEWER role only)
VIEWER_POLICY includes: [capability.view, audit.view]
DELETE requires: [capability.manage]

Decision: ❌ 403 Forbidden
Reason: User has VIEWER_POLICY, but it doesn't grant capability.manage
```

### **Scenario B: Endpoint Not Registered**
```
Request: GET /api/internal/debug-state
User: alice.smith (EMPLOYEE role)
Endpoint auth.endpoints: NOT FOUND

Decision: ❌ 404 Not Found OR 403 Forbidden
Reason: Endpoint doesn't exist in registration table
```

### **Scenario C: Role-Based Lockout**
```
Request: POST /api/admin/users
User: charlie.brown (RESTRICTED_USER role)
RESTRICTED_USER_POLICY: (empty - no capabilities)

Decision: ❌ 403 Forbidden
Reason: User's role has no policies granting this action
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
