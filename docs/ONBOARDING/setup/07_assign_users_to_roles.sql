-- ============================================================================
-- PHASE 6: ASSIGN USERS TO ROLES
-- ============================================================================
-- Purpose: Create user-role assignments so users get their role's capabilities
-- Dependencies: Users and Roles must be created first
-- ============================================================================

SET search_path TO auth;
\set ON_ERROR_STOP on

BEGIN;

-- Backup existing assignments for target users
CREATE TABLE IF NOT EXISTS user_roles_backup AS
SELECT ur.*, NOW()::timestamp AS backup_created_at
FROM user_roles ur
WHERE false;

ALTER TABLE user_roles_backup
    ADD COLUMN IF NOT EXISTS backup_created_at timestamp without time zone;

INSERT INTO user_roles_backup
SELECT ur.*, NOW()::timestamp
FROM user_roles ur
WHERE ur.user_id IN (
  SELECT id FROM users WHERE username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user')
);

-- Clear existing assignments for these users
DELETE FROM user_roles
WHERE user_id IN (
  SELECT id FROM users WHERE username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user')
);

-- Assign users to their respective roles
WITH target_assignments AS (
  SELECT 
    u.id  AS user_id,
    r.id  AS role_id
  FROM users u
  JOIN roles r ON (
        (u.username = 'platform.bootstrap' AND r.name = 'PLATFORM_BOOTSTRAP')
     OR (u.username = 'admin.tech'        AND r.name = 'ADMIN_TECH')
     OR (u.username = 'admin.ops'         AND r.name = 'ADMIN_OPS')
     OR (u.username = 'board1'            AND r.name = 'BOARD')
     OR (u.username = 'employer1'         AND r.name = 'EMPLOYER')
     OR (u.username = 'worker1'           AND r.name = 'WORKER')
     OR (u.username = 'test.user'         AND r.name = 'TEST_USER')
  )
)
INSERT INTO user_roles (user_id, role_id, assigned_at)
SELECT 
  ta.user_id,
  ta.role_id,
  NOW()
FROM target_assignments ta;

-- Validate referential integrity
DO $$
DECLARE
  v_user_count INTEGER;
  v_role_misses INTEGER;
BEGIN
  SELECT COUNT(*) INTO v_user_count
  FROM user_roles
  WHERE user_id IN (
    SELECT id FROM users WHERE username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user')
  );

  IF v_user_count <> 7 THEN
    RAISE EXCEPTION 'Expected 7 user-role assignments, found %', v_user_count;
  END IF;

  SELECT COUNT(*) INTO v_role_misses
  FROM user_roles ur
  WHERE ur.user_id IN (
    SELECT id FROM users WHERE username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user')
  )
  AND ur.role_id NOT IN (SELECT id FROM roles);

  IF v_role_misses > 0 THEN
    RAISE EXCEPTION 'Detected % assignments referencing missing roles', v_role_misses;
  END IF;
END $$;

-- Verify assignments
SELECT 
  u.username,
  r.name as role,
  ura.assigned_at,
  r.is_active
FROM user_roles ura
JOIN users u ON ura.user_id = u.id
JOIN roles r ON ura.role_id = r.id
WHERE u.username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user')
ORDER BY u.username;

-- Verify count
SELECT COUNT(*) as total_assignments FROM user_roles 
WHERE user_id IN (SELECT id FROM users WHERE username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user'));

COMMIT;
