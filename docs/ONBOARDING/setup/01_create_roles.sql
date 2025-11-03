-- ============================================================================
-- ONBOARDING PHASE 01: CREATE BOOTSTRAP ROLES (3 Total)
-- ============================================================================
-- Purpose: Define the minimal role set required for first-time onboarding.
-- Roles:
--   1. BASIC_USER           – Baseline access for every authenticated user
--   2. BUSINESS_ADMIN       – User management operations
--   3. TECHNICAL_BOOTSTRAP  – System configuration and RBAC administration
-- ============================================================================

SET search_path TO auth;
\set ON_ERROR_STOP on

BEGIN;

-- ============================================================================
-- BACKUP EXISTING DATA (with date suffix: 251201)
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_roles_251201 (LIKE user_roles INCLUDING ALL);
DELETE FROM user_roles_251201;
INSERT INTO user_roles_251201 SELECT * FROM user_roles;

CREATE TABLE IF NOT EXISTS roles_251201 (LIKE roles INCLUDING ALL);
DELETE FROM roles_251201;
INSERT INTO roles_251201 SELECT * FROM roles;

-- ============================================================================
-- RESET CURRENT ROLE CATALOG
-- ============================================================================
DELETE FROM user_roles;
DELETE FROM roles;
ALTER SEQUENCE roles_id_seq RESTART WITH 1;

-- ============================================================================
-- INSERT BOOTSTRAP ROLES
-- ============================================================================
INSERT INTO roles (name, description, is_active, created_at, updated_at) VALUES
(
  'BASIC_USER',
  'Baseline role assigned to all authenticated users. Grants access to authorization APIs and the service catalog.',
  true,
  NOW(),
  NOW()
),
(
  'BUSINESS_ADMIN',
  'Business-facing administrator responsible for managing user accounts and assigning roles.',
  true,
  NOW(),
  NOW()
),
(
  'TECHNICAL_BOOTSTRAP',
  'Technical bootstrap administrator with full RBAC, UI, and system configuration privileges.',
  true,
  NOW(),
  NOW()
);

-- ============================================================================
-- VERIFICATION
-- ============================================================================
DO $$
DECLARE
  v_total INTEGER;
BEGIN
  SELECT COUNT(*) INTO v_total FROM roles WHERE is_active = true;
  IF v_total <> 3 THEN
    RAISE EXCEPTION 'Expected 3 active bootstrap roles, found %', v_total;
  END IF;
  RAISE NOTICE 'Successfully created 3 bootstrap roles.';
END $$;

SELECT id, name, description, is_active, created_at
FROM roles
ORDER BY id;

COMMIT;

-- ============================================================================
-- POST-SCRIPT SUMMARY
-- ============================================================================
-- Roles created:
--   BASIC_USER          → Baseline access (assigned to every user)
--   BUSINESS_ADMIN      → User lifecycle management
--   TECHNICAL_BOOTSTRAP → System setup and RBAC configuration
--
-- Next Step: Run 02_create_ui_pages.sql to seed the UI navigation tree.
-- ============================================================================
