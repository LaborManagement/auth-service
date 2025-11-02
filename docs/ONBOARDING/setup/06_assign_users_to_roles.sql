-- ============================================================================
-- PHASE 6: ASSIGN USERS TO ROLES
-- ============================================================================
-- Purpose: Create user-role assignments so users get their role's capabilities
-- Dependencies: Users and Roles must be created first
-- ============================================================================

\set ON_ERROR_STOP on

-- Clear existing assignments (optional)
-- TRUNCATE TABLE user_role_assignments CASCADE;

-- Assign users to their respective roles
INSERT INTO user_role_assignments (user_id, role_id, assigned_at, assigned_by)
SELECT u.id, r.id, NOW(), 'SYSTEM'
FROM users u, roles r
WHERE u.username = 'platform.bootstrap' AND r.name = 'PLATFORM_BOOTSTRAP'
ON CONFLICT (user_id, role_id) DO UPDATE SET assigned_at = NOW();

INSERT INTO user_role_assignments (user_id, role_id, assigned_at, assigned_by)
SELECT u.id, r.id, NOW(), 'SYSTEM'
FROM users u, roles r
WHERE u.username = 'admin.tech' AND r.name = 'ADMIN_TECH'
ON CONFLICT (user_id, role_id) DO UPDATE SET assigned_at = NOW();

INSERT INTO user_role_assignments (user_id, role_id, assigned_at, assigned_by)
SELECT u.id, r.id, NOW(), 'SYSTEM'
FROM users u, roles r
WHERE u.username = 'admin.ops' AND r.name = 'ADMIN_OPS'
ON CONFLICT (user_id, role_id) DO UPDATE SET assigned_at = NOW();

INSERT INTO user_role_assignments (user_id, role_id, assigned_at, assigned_by)
SELECT u.id, r.id, NOW(), 'SYSTEM'
FROM users u, roles r
WHERE u.username = 'board1' AND r.name = 'BOARD'
ON CONFLICT (user_id, role_id) DO UPDATE SET assigned_at = NOW();

INSERT INTO user_role_assignments (user_id, role_id, assigned_at, assigned_by)
SELECT u.id, r.id, NOW(), 'SYSTEM'
FROM users u, roles r
WHERE u.username = 'employer1' AND r.name = 'EMPLOYER'
ON CONFLICT (user_id, role_id) DO UPDATE SET assigned_at = NOW();

INSERT INTO user_role_assignments (user_id, role_id, assigned_at, assigned_by)
SELECT u.id, r.id, NOW(), 'SYSTEM'
FROM users u, roles r
WHERE u.username = 'worker1' AND r.name = 'WORKER'
ON CONFLICT (user_id, role_id) DO UPDATE SET assigned_at = NOW();

INSERT INTO user_role_assignments (user_id, role_id, assigned_at, assigned_by)
SELECT u.id, r.id, NOW(), 'SYSTEM'
FROM users u, roles r
WHERE u.username = 'test.user' AND r.name = 'TEST_USER'
ON CONFLICT (user_id, role_id) DO UPDATE SET assigned_at = NOW();

-- Verify assignments
SELECT 
  u.username,
  r.name as role,
  ura.assigned_at,
  r.is_active
FROM user_role_assignments ura
JOIN users u ON ura.user_id = u.id
JOIN roles r ON ura.role_id = r.id
WHERE u.username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user')
ORDER BY u.username;

-- Verify count
SELECT COUNT(*) as total_assignments FROM user_role_assignments 
WHERE user_id IN (SELECT id FROM users WHERE username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user'));

COMMIT;
