# RBAC Troubleshooting Guide

## Quick Bootstrap

To quickly set up default RBAC structure, use:

```bash
psql -U postgres -d auth_service_db -f ../../scripts/bootstrap/bootstrap_user_seed.sql
```

This creates:
- Default roles and users
- Bootstrap account for initial setup
- Basic role mappings

See `scripts/bootstrap/bootstrap_user_seed.sql` for what it creates.

---

## Common Issues

### Issue 1: User getting 401 Unauthorized

**Cause:** User not authenticated or JWT invalid

```sql
-- Check user exists and is enabled
SELECT id, username, is_enabled FROM users WHERE username = 'worker8';

-- If not enabled, enable user
UPDATE users SET is_enabled = true WHERE username = 'worker8';

-- Check user has password hash
SELECT id, username FROM users WHERE username = 'worker8' AND password IS NOT NULL;
```

**Fix in code:**
```java
// Verify JWT expiration is set correctly
private static final long JWT_EXPIRATION = 86400000; // 24 hours
```

---

### Issue 2: User getting 403 Forbidden

**Cause:** User is authenticated but doesn't have permission

#### Step 1: Check user role
```sql
SELECT u.id, u.username, r.name as role
FROM users u
LEFT JOIN user_role_assignment ura ON u.id = ura.user_id
LEFT JOIN roles r ON ura.role_id = r.id
WHERE u.id = 8;
-- If NULL → user not assigned a role
```

#### Step 2: Assign role if missing
```sql
-- Assign worker role
INSERT INTO user_role_assignment (user_id, role_id)
SELECT 8, id FROM roles WHERE name = 'WORKER';
```

#### Step 3: Check endpoint is protected
```sql
SELECT e.method, e.path, p.name as policy
FROM endpoints e
LEFT JOIN endpoint_policy ep ON e.id = ep.endpoint_id
LEFT JOIN policies p ON ep.policy_id = p.id
WHERE e.path = '/api/users' AND e.method = 'POST';
-- If no results → endpoint not protected (should allow all)
```

#### Step 4: Check policy-capability mapping
```sql
-- Check if user's role is in the endpoint's policy
SELECT DISTINCT r.name
FROM endpoint_policy ep
JOIN policies p ON ep.policy_id = p.id
JOIN policy_capability pc ON p.id = pc.policy_id
JOIN capabilities c ON pc.capability_id = c.id
JOIN roles r ON c.role_id = r.id
WHERE ep.endpoint_id = 1;
-- Should include 'ADMIN' for /api/users POST
```

---

### Issue 3: All users can access all endpoints

**Cause:** Policies not properly configured or not linked to endpoints

#### Fix:
```sql
-- Check all endpoints have policies
SELECT e.path, e.method, COUNT(ep.policy_id) as policy_count
FROM endpoints e
LEFT JOIN endpoint_policy ep ON e.id = ep.endpoint_id
GROUP BY e.id, e.path, e.method
HAVING COUNT(ep.policy_id) = 0;
-- Should return empty (all protected)

-- Link missing endpoints to policies
INSERT INTO endpoint_policy (endpoint_id, policy_id)
SELECT id, (SELECT id FROM policies WHERE name = 'admin_all_access')
FROM endpoints
WHERE id NOT IN (SELECT endpoint_id FROM endpoint_policy);
```

---

### Issue 4: Worker can access admin endpoints

**Cause:** Wrong policy assigned to endpoint or wrong capability in policy

#### Fix:
```sql
-- Check which policy is assigned to /api/users (POST)
SELECT p.name FROM policies p
JOIN endpoint_policy ep ON p.id = ep.policy_id
JOIN endpoints e ON ep.endpoint_id = e.id
WHERE e.path = '/api/users' AND e.method = 'POST';

-- Should be 'admin_all_access', not 'worker_read_only'

-- Remove wrong policy
DELETE FROM endpoint_policy
WHERE endpoint_id = (SELECT id FROM endpoints WHERE path = '/api/users' AND method = 'POST')
AND policy_id = (SELECT id FROM policies WHERE name = 'worker_read_only');

-- Add correct policy
INSERT INTO endpoint_policy (endpoint_id, policy_id)
SELECT 
    (SELECT id FROM endpoints WHERE path = '/api/users' AND method = 'POST'),
    (SELECT id FROM policies WHERE name = 'admin_all_access');
```

---

### Issue 5: "Role WORKER not found"

**Cause:** Role not created in database

#### Fix:
```sql
-- Check if role exists
SELECT id, name FROM roles WHERE name = 'WORKER';

-- If not, create it
INSERT INTO roles (name, description, is_active)
VALUES ('WORKER', 'Worker user with limited access', true);
```

---

### Issue 6: JWT token not being generated

**Cause:** Missing JWT secret or JwtProvider not configured

#### Fix in application.yml:
```yaml
app:
  jwt:
    secret: your-super-secret-key-min-32-characters
    expiration: 86400000
```

#### Verify in code:
```java
@Bean
public JwtProvider jwtProvider() {
    return new JwtProvider();
}
```

---

### Issue 7: User roles not in JWT token

**Cause:** JwtProvider not fetching roles correctly

#### Fix:
```java
public String generateToken(User user) {
    // Make sure user.getRoles() returns all roles
    List<String> roles = user.getRoles().stream()
        .map(Role::getName)
        .collect(Collectors.toList());

    return Jwts.builder()
        .setSubject(String.valueOf(user.getId()))
        .claim("username", user.getUsername())
        .claim("roles", roles)  // ← Make sure roles are included
        .signWith(SignatureAlgorithm.HS512, jwtSecret)
        .compact();
}
```

---

### Issue 8: PolicyEngine not evaluating correctly

**Cause:** Incorrect SQL logic or missing data

#### Debug:
```sql
-- Trace through the logic for User 8 accessing POST /api/users

-- 1. Get user roles
SELECT r.id, r.name FROM user_role_assignment ura
JOIN roles r ON ura.role_id = r.id
WHERE ura.user_id = 8;

-- 2. Get endpoint
SELECT id FROM endpoints WHERE method = 'POST' AND path = '/api/users';

-- 3. Get policies for endpoint
SELECT policy_id FROM endpoint_policy 
WHERE endpoint_id = <endpoint_id>;

-- 4. Check if any policy is in user's roles
SELECT COUNT(*) FROM policy_capability pc
WHERE pc.policy_id = <policy_id>
AND pc.capability_id IN (
    SELECT c.id FROM capabilities c
    WHERE c.role_id IN (<user_role_ids>)
);
```

---

### Issue 9: Slow authentication/authorization

**Cause:** Missing indexes

#### Fix:
```sql
-- Create indexes if missing
CREATE INDEX IF NOT EXISTS idx_user_role ON user_role_assignment(user_id, role_id);
CREATE INDEX IF NOT EXISTS idx_policy_capability ON policy_capability(policy_id, capability_id);
CREATE INDEX IF NOT EXISTS idx_endpoint_policy ON endpoint_policy(endpoint_id, policy_id);
CREATE INDEX IF NOT EXISTS idx_endpoints_method_path ON endpoints(method, path);
```

---

## Diagnostic Query

```sql
-- Complete RBAC health check
SELECT 'Users' as entity, COUNT(*) as count FROM users
UNION ALL
SELECT 'Roles', COUNT(*) FROM roles
UNION ALL
SELECT 'User-Role Assignments', COUNT(*) FROM user_role_assignment
UNION ALL
SELECT 'Capabilities', COUNT(*) FROM capabilities
UNION ALL
SELECT 'Policies', COUNT(*) FROM policies
UNION ALL
SELECT 'Policy-Capability Links', COUNT(*) FROM policy_capability
UNION ALL
SELECT 'Endpoints', COUNT(*) FROM endpoints
UNION ALL
SELECT 'Endpoint-Policy Links', COUNT(*) FROM endpoint_policy;
```

---

## Testing Commands

```bash
# Test login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "password"}' \
  -v

# Test protected endpoint
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer <JWT>" \
  -v

# Test forbidden endpoint
curl -X POST http://localhost:8080/api/users \
  -H "Authorization: Bearer <WORKER_JWT>" \
  -H "Content-Type: application/json" \
  -d '{}' \
  -v
```

