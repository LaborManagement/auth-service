-- ============================================================================
-- PHASE 3: CREATE POLICIES (8 Total) - CORRECTED VERSION
-- ============================================================================
-- Purpose: Create 7 policies, one for each role, as the link between roles and capabilities
-- Includes: name, description, type, expression (JSON), is_active, timestamps
-- PostgreSQL Syntax: Tested for PostgreSQL compliance
-- Dependencies: Roles and Capabilities must be created first
-- 
-- IMPORTANT - Expression Format & Role Validation:
-- - Expression MUST follow format: {"roles": ["ROLE_NAME"]}
-- - Role names in expression MUST EXACTLY match roles created in 01_create_roles.sql
-- - Role validation is ENFORCED in application code (PolicyController.validateRolesInExpression())
-- - If role doesn't exist in database, policy creation/update will be REJECTED
-- - Valid roles in system: PLATFORM_BOOTSTRAP, BASIC_USER, ADMIN_TECH, ADMIN_OPS, BOARD, EMPLOYER, WORKER, TEST_USER
-- ============================================================================

SET search_path TO auth;
\set ON_ERROR_STOP on

BEGIN;

-- ============================================================================
-- BACKUP EXISTING DATA (Optional - for fresh setup)
-- ============================================================================
CREATE TABLE IF NOT EXISTS policy_capabilities_backup AS
SELECT pc.*, NOW()::timestamp AS backup_created_at
FROM policy_capabilities pc
WHERE false;

ALTER TABLE policy_capabilities_backup
    ADD COLUMN IF NOT EXISTS backup_created_at timestamp without time zone;

INSERT INTO policy_capabilities_backup
SELECT pc.*, NOW()::timestamp
FROM policy_capabilities pc;

CREATE TABLE IF NOT EXISTS endpoint_policies_backup AS
SELECT ep.*, NOW()::timestamp AS backup_created_at
FROM endpoint_policies ep
WHERE false;

ALTER TABLE endpoint_policies_backup
    ADD COLUMN IF NOT EXISTS backup_created_at timestamp without time zone;

INSERT INTO endpoint_policies_backup
SELECT ep.*, NOW()::timestamp
FROM endpoint_policies ep;

CREATE TABLE IF NOT EXISTS policies_backup AS
SELECT p.*, NOW()::timestamp AS backup_created_at
FROM policies p
WHERE false;

ALTER TABLE policies_backup
    ADD COLUMN IF NOT EXISTS backup_created_at timestamp without time zone;

INSERT INTO policies_backup
SELECT p.*, NOW()::timestamp
FROM policies p;

-- Clear existing policies with referential integrity
DELETE FROM policy_capabilities WHERE policy_id IN (SELECT id FROM policies);
DELETE FROM endpoint_policies WHERE policy_id IN (SELECT id FROM policies);
DELETE FROM policies;
-- Reset sequence for clean ID restart
ALTER SEQUENCE policies_id_seq RESTART WITH 1;

-- ============================================================================
-- INSERT 8 POLICIES, ONE PER ROLE
-- ============================================================================

INSERT INTO policies (name, description, type, expression, is_active, created_at, updated_at) VALUES

(
  'PLATFORM_BOOTSTRAP_POLICY',
  'Bootstrap policy with full system access (55/98 capabilities). Used for one-time system initialization. Grants all RBAC, UI, and system capabilities but no business operations.',
  'RBAC',
  '{"roles": ["PLATFORM_BOOTSTRAP"]}',
  true,
  NOW(),
  NOW()
),

(
  'BASIC_USER_POLICY',
  'Baseline access policy (4/98 capabilities). Grants metadata endpoints and authorization lookups required by every authenticated user.',
  'RBAC',
  '{"roles": ["BASIC_USER"]}',
  true,
  NOW(),
  NOW()
),

(
  'ADMIN_TECH_POLICY',
  'Technical administration policy (51/98 capabilities). Grants RBAC configuration, user account management, API endpoint management, UI page management, and system settings access. No business operations access.',
  'RBAC',
  '{"roles": ["ADMIN_TECH"]}',
  true,
  NOW(),
  NOW()
),

(
  'ADMIN_OPS_POLICY',
  'Operations administration policy (42/98 capabilities). Grants file ingestion capabilities (MT940/VAN), audit log access, system monitoring, and operational workflow capabilities. Limited RBAC access.',
  'RBAC',
  '{"roles": ["ADMIN_OPS"]}',
  true,
  NOW(),
  NOW()
),

(
  'BOARD_POLICY',
  'Board member policy (17/98 capabilities). Grants financial approval and reconciliation capabilities. Can view all payment requests, vote on decisions, and provide final approval. No data entry or configuration access.',
  'RBAC',
  '{"roles": ["BOARD"]}',
  true,
  NOW(),
  NOW()
),

(
  'EMPLOYER_POLICY',
  'Employer staff policy (19/98 capabilities). Grants request validation, payment approval/rejection, and employer-level payment operations. Organization-scoped via VPD (UserTenantAcl). Cannot see other organizations'' data.',
  'RBAC',
  '{"roles": ["EMPLOYER"]}',
  true,
  NOW(),
  NOW()
),

(
  'WORKER_POLICY',
  'Worker/employee policy (14/98 capabilities). Grants payment data upload, request creation/submission, and receipt management. User-scoped via VPD (UserTenantAcl) - can only see own data. Cannot approve payments.',
  'RBAC',
  '{"roles": ["WORKER"]}',
  true,
  NOW(),
  NOW()
),

(
  'TEST_USER_POLICY',
  'QA/Testing policy (50/98 capabilities). Comprehensive testing access covering most capabilities for validation workflows. Excludes destructive operations and dangerous system commands.',
  'RBAC',
  '{"roles": ["TEST_USER"]}',
  true,
  NOW(),
  NOW()
);

-- ============================================================================
-- VERIFICATION
-- ============================================================================
DO $$
DECLARE
  v_count INTEGER;
BEGIN
  SELECT COUNT(*) INTO v_count FROM policies WHERE is_active = true;
  IF v_count <> 8 THEN
    RAISE EXCEPTION 'Expected 8 active policies, found %', v_count;
  END IF;
  
  -- Verify all policies have non-null expressions
  IF EXISTS (SELECT 1 FROM policies WHERE is_active = true AND expression IS NULL) THEN
    RAISE EXCEPTION 'All policies must have non-null expression field';
  END IF;
  
  RAISE NOTICE 'Successfully created % policies', v_count;
END $$;

-- Verify creation
SELECT 
  name, 
  description,
  type,
  is_active,
  created_at
FROM policies 
ORDER BY created_at;

COMMIT;
