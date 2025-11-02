-- ============================================================================
-- PHASE 11: ASSIGN BASIC_POLICY TO ALL USERS
-- ============================================================================
-- Purpose: Link BASIC_POLICY to BASIC_USER role and assign all users to BASIC_USER
-- This ensures all users have access to:
--   - authorization.api.access
--   - service.catalog.read
-- ============================================================================

-- Set schema for this session
SET search_path TO auth;

\set ON_ERROR_STOP on

BEGIN;

-- Step 1: Create BASIC_USER role if it doesn't exist
INSERT INTO roles (name, description, is_active, created_at, updated_at)
SELECT 'BASIC_USER', 'Basic user role - assigned to all authenticated users', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'BASIC_USER');

-- Step 2: Create BASIC_POLICY if it doesn't exist
-- INSERT INTO policies (name, description, is_active, created_at, updated_at)
-- SELECT 'BASIC_POLICY', 'Basic policy - contains fundamental capabilities for all users', true, NOW(), NOW()
-- WHERE NOT EXISTS (SELECT 1 FROM policies WHERE name = 'BASIC_POLICY');
-- NOTE: BASIC_POLICY was already created in Phase 10

-- Step 3: Get capability IDs for basic capabilities
-- (These were created in Phase 10)
WITH basic_capabilities AS (
  SELECT id FROM capabilities 
  WHERE name IN ('authorization.api.access', 'service.catalog.read')
)
INSERT INTO policy_capabilities (policy_id, capability_id)
SELECT 
  (SELECT id FROM policies WHERE name = 'BASIC_POLICY'),
  id
FROM basic_capabilities
WHERE NOT EXISTS (
  SELECT 1 FROM policy_capabilities 
  WHERE policy_id = (SELECT id FROM policies WHERE name = 'BASIC_POLICY')
  AND capability_id IN (SELECT id FROM basic_capabilities)
);

-- Step 4: Assign all users to BASIC_USER role
INSERT INTO user_roles (user_id, role_id, assigned_at)
SELECT 
  u.id,
  (SELECT id FROM roles WHERE name = 'BASIC_USER'),
  NOW()
FROM users u
WHERE NOT EXISTS (
  SELECT 1 FROM user_roles 
  WHERE user_id = u.id 
  AND role_id = (SELECT id FROM roles WHERE name = 'BASIC_USER')
);

-- Step 5: Verify the setup
SELECT 
  'BASIC_USER' as role,
  (SELECT COUNT(*) FROM user_roles WHERE role_id = (SELECT id FROM roles WHERE name = 'BASIC_USER')) as users_with_role,
  (SELECT COUNT(*) FROM policy_capabilities WHERE policy_id = (SELECT id FROM policies WHERE name = 'BASIC_POLICY')) as capabilities_in_policy;

COMMIT;
