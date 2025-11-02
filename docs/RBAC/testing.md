# RBAC Testing Guide

Use these checks after seeding the database or deploying code that touches authorisation. They focus on verifying policies, capabilities, and RLS behaviour without repeating information kept elsewhere.

## Database Sanity Checks

```sql
-- Roles present and active
SELECT name, is_active FROM roles ORDER BY name;

-- Seed users mapped to roles
SELECT u.username, r.name
FROM users u
JOIN user_role_assignment ura ON ura.user_id = u.id
JOIN roles r ON r.id = ura.role_id
ORDER BY u.username;

-- Capabilities linked to policies
SELECT p.name, COUNT(*) AS capability_count
FROM policies p
JOIN policy_capability pc ON pc.policy_id = p.id
GROUP BY p.name
ORDER BY p.name;
```

Expect to see 7 roles, 7 policies, and counts that match the allocations listed in `ROLES.md`.

## Capability Spot Checks

```sql
-- Worker profile: should only expose read/upload operations
SELECT c.name
FROM users u
JOIN user_role_assignment ura ON ura.user_id = u.id
JOIN policy_capability pc ON pc.policy_id = ura.role_id
JOIN capabilities c ON c.id = pc.capability_id
WHERE u.username = 'worker8'
ORDER BY c.name;
```

Swap the username to validate other personas (`admin.tech`, `board1`, etc.).

## API Behaviour

```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin.tech","password":"password"}' \
  | jq -r '.token')

# Endpoint allowed for ADMIN_TECH
curl -i -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/admin/roles

# Endpoint denied for WORKER
curl -i -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/worker/uploaded-data/upload
```

Observe 200 responses where policies allow the call and 403 otherwise. Adjust credentials per role you are verifying.

## RLS Validation

```sql
SET ROLE app_payment_flow;
SELECT auth.set_user_context('8');  -- worker8
SELECT COUNT(*) FROM payment_flow.worker_uploaded_data; -- filtered rows
```

Repeat for an employer user and confirm row counts change as expected. Use the SQL snippets in `../VPD/testing/` for deeper coverage.

## Automation Hooks

- Incorporate the SQL checks into integration tests by running them via migrations or a lightweight test harness.
- Keep curl-based smoke tests in CI to ensure endpoint policies remain intact after code changes.
