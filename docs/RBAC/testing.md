# RBAC Testing Guide

## Testing Flow

### Test 1: Verify RBAC Structure

```sql
-- Check all roles exist
SELECT * FROM roles WHERE is_active = true;

-- Check users have roles
SELECT u.id, u.username, r.name as role
FROM users u
JOIN user_role_assignment ura ON u.id = ura.user_id
JOIN roles r ON ura.role_id = r.id
WHERE u.is_enabled = true;

-- Check policies exist
SELECT name, description FROM policies WHERE is_active = true;
```

### Test 2: Verify User Capabilities

```sql
-- Check what User 8 (WORKER) can do
SELECT DISTINCT c.name, c.module, c.action
FROM users u
JOIN user_role_assignment ura ON u.id = ura.user_id
JOIN roles r ON ura.role_id = r.id
JOIN policy_capability pc ON r.id = pc.policy_id
JOIN capabilities c ON pc.capability_id = c.id
WHERE u.id = 8;
-- Expected: Only read capabilities

-- Check what User 1 (ADMIN) can do
SELECT DISTINCT c.name, c.module, c.action
FROM users u
JOIN user_role_assignment ura ON u.id = ura.user_id
JOIN roles r ON ura.role_id = r.id
JOIN policy_capability pc ON r.id = pc.policy_id
JOIN capabilities c ON pc.capability_id = c.id
WHERE u.id = 1;
-- Expected: All capabilities
```

### Test 3: Test API Endpoint Access

```bash
# 1. Login as User 1 (Admin)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "password"}'

# Expected: JWT token with ADMIN role

# 2. Try creating a user (should succeed for Admin)
curl -X POST http://localhost:8080/api/users \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"username": "newuser", "email": "new@test.com"}'

# Expected: 201 Created

# 3. Login as User 8 (Worker)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "worker8", "password": "password"}'

# Expected: JWT token with WORKER role

# 4. Try creating a user (should fail for Worker)
curl -X POST http://localhost:8080/api/users \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"username": "newuser", "email": "new@test.com"}'

# Expected: 403 Forbidden
```

### Test 4: JWT Token Verification

```bash
# Decode JWT token (use jwt.io or command line):
# The token should contain:
# {
#   "sub": "8",
#   "username": "worker8",
#   "email": "worker8@example.com",
#   "roles": ["WORKER"],
#   "iat": 1234567890,
#   "exp": 1234571490
# }
```

### Test 5: Policy Evaluation

```sql
-- Check if endpoint is protected
SELECT ep.*, p.name as policy_name
FROM endpoints e
JOIN endpoint_policy ep ON e.id = ep.endpoint_id
JOIN policies p ON ep.policy_id = p.id
WHERE e.path = '/api/users' AND e.method = 'POST';

-- Check which roles can access
SELECT DISTINCT r.name
FROM endpoint_policy ep
JOIN policies p ON ep.policy_id = p.id
JOIN policy_capability pc ON p.id = pc.policy_id
JOIN roles r ON r.id = pc.role_id
WHERE ep.endpoint_id = 1;
```

## Testing Checklist

- [ ] All roles created (ADMIN, WORKER, EMPLOYER, DATA_OPS)
- [ ] Users assigned to roles
- [ ] Capabilities defined for all operations
- [ ] Policies created mapping roles to capabilities
- [ ] Endpoints protected with policies
- [ ] Different users see different capabilities
- [ ] Admin can access all endpoints
- [ ] Worker can only access read endpoints
- [ ] Employer can access their limited endpoints
- [ ] JWT token contains role information
- [ ] Unauthenticated requests return 401
- [ ] Unauthorized requests return 403

## Performance Test

```bash
# Test authentication response time
time curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "password"}'

# Test policy evaluation response time
time curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer <JWT_TOKEN>"

# Both should complete within < 100ms
```

