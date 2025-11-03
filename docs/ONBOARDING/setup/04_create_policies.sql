-- ============================================================================
-- ONBOARDING PHASE 04: CREATE BOOTSTRAP POLICIES
-- ============================================================================
-- Purpose: Seed granular policies that map roles to distinct privilege slices
-- Policies link roles to capabilities for authorization decisions
--
-- Bootstrap Policy Model:
--   1. BASIC_USER_POLICY – baseline access for every authenticated user
--   2. USER_ACCOUNT_MANAGE_POLICY – user lifecycle operations
--   3. ROLE_* policies – split read/assignment/management responsibilities
--   4. CAPABILITY/POLICY/ENDPOINT/UI/SYSTEM policies – fine-grained technical ops
--
-- IMPORTANT - Expression Format & Role Validation:
-- - Expression MUST follow format: {"roles": ["ROLE_NAME", ...]}
-- - Role names MUST match roles created in 01_create_roles.sql
-- - Role validation is enforced in application code (PolicyController.validateRolesInExpression())
-- - Valid bootstrap roles: BASIC_USER (id=1), BUSINESS_ADMIN (id=2), TECHNICAL_BOOTSTRAP (id=3)
-- ============================================================================

SET search_path TO auth;
\set ON_ERROR_STOP on

BEGIN;

-- ============================================================================
-- BACKUP EXISTING DATA (with date suffix: 251201)
-- ============================================================================
CREATE TABLE IF NOT EXISTS policy_capabilities_251201 (LIKE policy_capabilities INCLUDING ALL);
DELETE FROM policy_capabilities_251201;
INSERT INTO policy_capabilities_251201 SELECT * FROM policy_capabilities;

CREATE TABLE IF NOT EXISTS endpoint_policies_251201 (LIKE endpoint_policies INCLUDING ALL);
DELETE FROM endpoint_policies_251201;
INSERT INTO endpoint_policies_251201 SELECT * FROM endpoint_policies;

CREATE TABLE IF NOT EXISTS policies_251201 (LIKE policies INCLUDING ALL);
DELETE FROM policies_251201;
INSERT INTO policies_251201 SELECT * FROM policies;

-- Clear existing policies with referential integrity
DELETE FROM policy_capabilities WHERE policy_id IN (SELECT id FROM policies);
DELETE FROM endpoint_policies WHERE policy_id IN (SELECT id FROM policies);
DELETE FROM policies;
-- Reset sequence for clean ID restart
ALTER SEQUENCE policies_id_seq RESTART WITH 1;

-- ============================================================================
-- INSERT GRANULAR BOOTSTRAP POLICIES
-- ============================================================================

INSERT INTO policies (name, description, type, expression, is_active, created_at, updated_at) VALUES

-- Baseline access
(
  'BASIC_USER_POLICY',
  'Baseline access to authorization APIs and the service catalog for all authenticated users.',
  'RBAC',
  '{"roles": ["BASIC_USER"]}',
  true,
  NOW(),
  NOW()
),

-- Business administration
(
  'USER_ACCOUNT_MANAGE_POLICY',
  'Manage user accounts and lifecycle operations.',
  'RBAC',
  '{"roles": ["BUSINESS_ADMIN", "TECHNICAL_BOOTSTRAP"]}',
  true,
  NOW(),
  NOW()
),
(
  'ROLE_READ_POLICY',
  'Read role catalog and metadata.',
  'RBAC',
  '{"roles": ["BUSINESS_ADMIN", "TECHNICAL_BOOTSTRAP"]}',
  true,
  NOW(),
  NOW()
),
(
  'ROLE_ASSIGN_POLICY',
  'Assign or revoke roles from users.',
  'RBAC',
  '{"roles": ["BUSINESS_ADMIN", "TECHNICAL_BOOTSTRAP"]}',
  true,
  NOW(),
  NOW()
),
(
  'ROLE_MANAGE_POLICY',
  'Create, update, delete, or toggle roles.',
  'RBAC',
  '{"roles": ["TECHNICAL_BOOTSTRAP"]}',
  true,
  NOW(),
  NOW()
),

-- Capability management
(
  'CAPABILITY_READ_POLICY',
  'Read capability catalog and matrices.',
  'RBAC',
  '{"roles": ["TECHNICAL_BOOTSTRAP"]}',
  true,
  NOW(),
  NOW()
),
(
  'CAPABILITY_MANAGE_POLICY',
  'Create, update, delete, or toggle capabilities.',
  'RBAC',
  '{"roles": ["TECHNICAL_BOOTSTRAP"]}',
  true,
  NOW(),
  NOW()
),

-- Policy management
(
  'POLICY_READ_POLICY',
  'Read policy catalog and attached capabilities.',
  'RBAC',
  '{"roles": ["TECHNICAL_BOOTSTRAP"]}',
  true,
  NOW(),
  NOW()
),
(
  'POLICY_MANAGE_POLICY',
  'Create, update, delete, toggle, and maintain policy-capability links.',
  'RBAC',
  '{"roles": ["TECHNICAL_BOOTSTRAP"]}',
  true,
  NOW(),
  NOW()
),

-- Endpoint management
(
  'ENDPOINT_READ_POLICY',
  'Read registered endpoint metadata.',
  'RBAC',
  '{"roles": ["TECHNICAL_BOOTSTRAP"]}',
  true,
  NOW(),
  NOW()
),
(
  'ENDPOINT_MANAGE_POLICY',
  'Create, update, delete, toggle, and link endpoints to policies.',
  'RBAC',
  '{"roles": ["TECHNICAL_BOOTSTRAP"]}',
  true,
  NOW(),
  NOW()
),

-- UI page management
(
  'UI_PAGE_READ_POLICY',
  'Read UI page catalog and hierarchy.',
  'RBAC',
  '{"roles": ["TECHNICAL_BOOTSTRAP"]}',
  true,
  NOW(),
  NOW()
),
(
  'UI_PAGE_MANAGE_POLICY',
  'Create, update, delete, reorder, and toggle UI pages.',
  'RBAC',
  '{"roles": ["TECHNICAL_BOOTSTRAP"]}',
  true,
  NOW(),
  NOW()
),

-- UI action management
(
  'UI_ACTION_READ_POLICY',
  'Read page actions and mappings.',
  'RBAC',
  '{"roles": ["TECHNICAL_BOOTSTRAP"]}',
  true,
  NOW(),
  NOW()
),
(
  'UI_ACTION_MANAGE_POLICY',
  'Create, update, delete, reorder, and toggle page actions.',
  'RBAC',
  '{"roles": ["TECHNICAL_BOOTSTRAP"]}',
  true,
  NOW(),
  NOW()
),

-- System operations
(
  'SYSTEM_READ_POLICY',
  'Read audit trails, system settings, and ingestion status.',
  'RBAC',
  '{"roles": ["TECHNICAL_BOOTSTRAP"]}',
  true,
  NOW(),
  NOW()
),
(
  'SYSTEM_MANAGE_POLICY',
  'Update system settings and trigger ingestion jobs.',
  'RBAC',
  '{"roles": ["TECHNICAL_BOOTSTRAP"]}',
  true,
  NOW(),
  NOW()
);

-- ============================================================================
-- VERIFICATION
-- ============================================================================
DO $$
DECLARE
  v_total_count INTEGER;
  v_missing INTEGER;
BEGIN
  -- Verify expected number of active policies
  SELECT COUNT(*) INTO v_total_count FROM policies WHERE is_active = true;
  IF v_total_count <> 17 THEN
    RAISE EXCEPTION 'Expected 17 active bootstrap policies, found %', v_total_count;
  END IF;

  -- Ensure every expected policy exists
  SELECT COUNT(*) INTO v_missing
  FROM (
    SELECT unnest(ARRAY[
      'BASIC_USER_POLICY',
      'USER_ACCOUNT_MANAGE_POLICY',
      'ROLE_READ_POLICY',
      'ROLE_ASSIGN_POLICY',
      'ROLE_MANAGE_POLICY',
      'CAPABILITY_READ_POLICY',
      'CAPABILITY_MANAGE_POLICY',
      'POLICY_READ_POLICY',
      'POLICY_MANAGE_POLICY',
      'ENDPOINT_READ_POLICY',
      'ENDPOINT_MANAGE_POLICY',
      'UI_PAGE_READ_POLICY',
      'UI_PAGE_MANAGE_POLICY',
      'UI_ACTION_READ_POLICY',
      'UI_ACTION_MANAGE_POLICY',
      'SYSTEM_READ_POLICY',
      'SYSTEM_MANAGE_POLICY'
    ]) AS expected_name
  ) expected
  LEFT JOIN policies p
    ON p.name = expected.expected_name
   AND p.is_active = true
  WHERE p.id IS NULL;

  IF v_missing <> 0 THEN
    RAISE EXCEPTION 'Bootstrap policy seeding missing % expected policies', v_missing;
  END IF;

  -- Verify all policies have non-null expressions
  IF EXISTS (SELECT 1 FROM policies WHERE is_active = true AND expression IS NULL) THEN
    RAISE EXCEPTION 'All policies must have non-null expression field';
  END IF;

  RAISE NOTICE 'Successfully created 17 bootstrap policies with granular capability slices.';
END $$;

-- Verify creation
SELECT 
  name, 
  description,
  type,
  expression,
  is_active,
  created_at
FROM policies 
ORDER BY created_at;

-- ============================================================================
-- POST-SCRIPT SUMMARY
-- ============================================================================
-- Created Policies: 17 (Granular Bootstrap Model)
--
-- Access Layering:
--   - BASIC_USER_POLICY → universal baseline (authorization + service catalog)
--   - BUSINESS_ADMIN scope → USER_ACCOUNT_MANAGE, ROLE_READ, ROLE_ASSIGN
--   - TECHNICAL scope → all read/manage policies for RBAC, UI, endpoints, and system ops
--
-- Role-Policy Mapping Highlights:
--   BASIC_USER role:
--     • BASIC_USER_POLICY
--   BUSINESS_ADMIN role:
--     • USER_ACCOUNT_MANAGE_POLICY
--     • ROLE_READ_POLICY
--     • ROLE_ASSIGN_POLICY
--   TECHNICAL_BOOTSTRAP role:
--     • All business admin policies
--     • All technical read/manage policies (role, capability, policy, endpoint, UI, system)
--
-- Next Step: Run 05_link_policies_to_capabilities.sql to associate each policy
--            with its matching capabilities by action tier.
-- ============================================================================

COMMIT;
