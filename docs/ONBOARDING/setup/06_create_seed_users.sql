-- ============================================================================
-- PHASE 5: CREATE SEED USERS (8 Total) - CORRECTED VERSION
-- ============================================================================
-- Purpose: Create one test user for each role for initial testing and demonstration
-- Includes: username, email, password, full_name, role enum, enabled flags, timestamps
-- PostgreSQL Syntax: Tested for PostgreSQL compliance
-- Dependencies: Users table must exist with full_name NOT NULL column
-- ============================================================================

SET search_path TO auth;
\set ON_ERROR_STOP on

BEGIN;

-- ============================================================================
-- BACKUP EXISTING DATA (Optional - for fresh setup)
-- ============================================================================
CREATE TABLE IF NOT EXISTS auth.user_tenant_acl_backup AS
SELECT uta.*, NOW()::timestamp AS backup_created_at
FROM auth.user_tenant_acl uta
WHERE false;

ALTER TABLE auth.user_tenant_acl_backup
    ADD COLUMN IF NOT EXISTS backup_created_at timestamp without time zone;

INSERT INTO auth.user_tenant_acl_backup
SELECT uta.*, NOW()::timestamp
FROM auth.user_tenant_acl uta
WHERE uta.user_id IN (
    SELECT id FROM auth.users WHERE username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user')
);

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
    SELECT id FROM auth.users WHERE username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user')
);

CREATE TABLE IF NOT EXISTS users_backup AS
SELECT u.*, NOW()::timestamp AS backup_created_at
FROM auth.users u
WHERE false;

ALTER TABLE users_backup
    ADD COLUMN IF NOT EXISTS backup_created_at timestamp without time zone;

INSERT INTO users_backup
SELECT u.*, NOW()::timestamp
FROM auth.users u
WHERE u.username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user');

-- Clear existing seed test users (fresh setup)
DELETE FROM auth.user_tenant_acl 
WHERE user_id IN (
  SELECT id FROM auth.users WHERE username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user')
);

DELETE FROM user_roles
WHERE user_id IN (
  SELECT id FROM auth.users WHERE username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user')
);

DELETE FROM auth.users
WHERE username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user');

-- ============================================================================
-- INSERT 7 SEED USERS, ONE PER ROLE
-- Note: full_name is REQUIRED by User entity definition
-- Password hashes: All use dummy bcrypt hash (should be updated in production)
-- ============================================================================

INSERT INTO auth.users (
  username,
  email,
  password,
  full_name,
  permission_version,
  role,
  is_enabled,
  is_account_non_expired,
  is_account_non_locked,
  is_credentials_non_expired,
  created_at,
  updated_at,
  last_login
) VALUES
(
  'platform.bootstrap',
  'bootstrap@system.local',
  '$2a$12$ABgKvrzZNrOVlOkKOvzBAuSChaCz/16C8lkWSxuOGf/BIKuZz7vFG',  -- encrypted password: Bootstrap!2025
  'Platform Bootstrap',
  1,
  'ADMIN',
  true,
  true,
  true,
  true,
  NOW(),
  NOW(),
  NULL
),
(
  'admin.tech',
  'admin.tech@system.local',
  '$2a$12$slYQmyNdGzin7olVN3p5Be0DlH.PKZbv5H8KnzzVgXXbVxzy990qm',  -- encrypted password: AdminTech!2025
  'Tech Administrator',
  1,
  'ADMIN',
  true,
  true,
  true,
  true,
  NOW(),
  NOW(),
  NULL
),
(
  'admin.ops',
  'admin.ops@system.local',
  '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XeKeUxWdeS86E50Dmk0m.',  -- encrypted password: AdminOps!2025
  'Operations Administrator',
  1,
  'ADMIN',
  true,
  true,
  true,
  true,
  NOW(),
  NOW(),
  NULL
),
(
  'board1',
  'board1@company.local',
  '$2a$12$KIXVvJhwK5hI0LJvHvVHG.eQE.eEPdH6YjFJ3t5lCvW6n0IKEuN3i',  -- encrypted password: Board!2025
  'Board Member One',
  1,
  'BOARD',
  true,
  true,
  true,
  true,
  NOW(),
  NOW(),
  NULL
),
(
  'employer1',
  'employer1@company.local',
  '$2a$12$KIXVvJhwK5hI0LJvHvVHG.eQE.eEPdH6YjFJ3t5lCvW6n0IKEuN3i',  -- encrypted password: Employer!2025
  'Employer Staff One',
  1,
  'EMPLOYER',
  true,
  true,
  true,
  true,
  NOW(),
  NOW(),
  NULL
),
(
  'worker1',
  'worker1@company.local',
  '$2a$12$KIXVvJhwK5hI0LJvHvVHG.eQE.eEPdH6YjFJ3t5lCvW6n0IKEuN3i',  -- encrypted password: Worker!2025
  'Worker User One',
  1,
  'WORKER',
  true,
  true,
  true,
  true,
  NOW(),
  NOW(),
  NULL
),
(
  'test.user',
  'test.user@system.local',
  '$2a$12$KIXVvJhwK5hI0LJvHvVHG.eQE.eEPdH6YjFJ3t5lCvW6n0IKEuN3i',  -- encrypted password: TestUser!2025
  'Test User For QA',
  1,
  'USER',
  true,
  true,
  true,
  true,
  NOW(),
  NOW(),
  NULL
)
ON CONFLICT (username) DO UPDATE SET
  email = EXCLUDED.email,
  password = EXCLUDED.password,
  full_name = EXCLUDED.full_name,
  permission_version = EXCLUDED.permission_version,
  role = EXCLUDED.role,
  is_enabled = EXCLUDED.is_enabled,
  is_account_non_expired = EXCLUDED.is_account_non_expired,
  is_account_non_locked = EXCLUDED.is_account_non_locked,
  is_credentials_non_expired = EXCLUDED.is_credentials_non_expired,
  updated_at = NOW();

-- ============================================================================
-- VERIFICATION
-- ============================================================================
DO $$
DECLARE
  v_count INTEGER;
  v_missing_fullname INTEGER;
BEGIN
  -- Verify 7 seed users created
  SELECT COUNT(*) INTO v_count FROM auth.users 
  WHERE username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user');
  
  IF v_count <> 7 THEN
    RAISE EXCEPTION 'Expected 7 seed users, found %', v_count;
  END IF;
  
  -- Verify all users have full_name
  SELECT COUNT(*) INTO v_missing_fullname FROM auth.users
  WHERE username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user')
  AND (full_name IS NULL OR full_name = '');
  
  IF v_missing_fullname > 0 THEN
    RAISE EXCEPTION 'Found % users with missing or empty full_name', v_missing_fullname;
  END IF;
  
  IF EXISTS (
    SELECT 1 FROM auth.users 
    WHERE username IN ('platform.bootstrap', 'basic.user1', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user')
      AND is_enabled = false
  ) THEN
    RAISE EXCEPTION 'All seed users must be enabled';
  END IF;
  
  IF EXISTS (
    SELECT 1 FROM auth.users 
    WHERE username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user')
      AND role NOT IN ('ADMIN', 'BOARD', 'EMPLOYER', 'WORKER', 'USER')
  ) THEN
    RAISE EXCEPTION 'Unexpected role enum detected for seed users';
  END IF;
  
  RAISE NOTICE 'Successfully created % seed users with valid roles and enabled status', v_count;
END $$;

-- Verify creation details
SELECT username, email, full_name, role, is_enabled, created_at FROM auth.users 
WHERE username IN ('platform.bootstrap', 'basic.user1', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user')
ORDER BY username;

-- Verify count
SELECT COUNT(*) as total_seed_users FROM auth.users 
WHERE username IN ('platform.bootstrap', 'basic.user1', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user');

COMMIT;
