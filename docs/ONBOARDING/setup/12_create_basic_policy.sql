-- ============================================================================
-- PHASE 10: CREATE BASIC CAPABILITIES AND BASIC_POLICY
-- ============================================================================
-- Purpose: Add basic/foundational capabilities that should be accessible to all users
-- Capabilities: Authorization API, Service Catalog API
-- Policy: BASIC_POLICY linked to all basic capabilities
-- Role: BASIC_USER (should already exist from Phase 1)
-- 
-- IMPORTANT NOTES:
-- - These capabilities are FUNDAMENTAL and must be accessible to every authenticated user
-- - Authorization API: Required for checking user permissions and access control decisions
-- - Service Catalog API: Required for service/capability discovery and metadata lookups
-- - Run this script AFTER 01_create_roles.sql but can be run anytime after Phase 4
-- - This script is IDEMPOTENT - safe to rerun multiple times
-- 
-- Execution Order (Recommended):
-- 1. Run Phase 1-6 scripts first
-- 2. Run this script to add BASIC_POLICY capabilities
-- 3. Update endpoint mappings to link endpoints to BASIC_POLICY
-- ============================================================================

-- Set schema for this session
SET search_path TO auth;

\set ON_ERROR_STOP on

BEGIN;

-- ============================================================================
-- SECTION 1: ADD BASIC CAPABILITIES (2 new capabilities)
-- ============================================================================
-- These are fundamental capabilities needed by all authenticated users

INSERT INTO capabilities (name, description, module, action, resource, is_active, created_at, updated_at)
VALUES
(
  'authorization.api.access',
  'Access Authorization APIs - Check permissions, validate access, and perform authorization lookups. Required by all authenticated users.',
  'SYSTEM_OPERATIONS',
  'READ',
  'AUTHORIZATION_API',
  true,
  NOW(),
  NOW()
),
(
  'service.catalog.read',
  'Access Service Catalog - View available services, capabilities, and API endpoints for capability discovery. Required by all authenticated users.',
  'SYSTEM_OPERATIONS',
  'READ',
  'SERVICE_CATALOG',
  true,
  NOW(),
  NOW()
)
ON CONFLICT (name) DO NOTHING;

-- ============================================================================
-- SECTION 2: CREATE OR UPDATE BASIC_POLICY
-- ============================================================================
-- This policy grants the basic/foundational capabilities to all users
-- The BASIC_USER role will be assigned to all users

-- First, check if BASIC_POLICY already exists
DO $$
DECLARE
  v_policy_exists BOOLEAN;
  v_policy_id BIGINT;
BEGIN
  SELECT EXISTS(SELECT 1 FROM policies WHERE name = 'BASIC_POLICY') INTO v_policy_exists;
  
  IF v_policy_exists THEN
    RAISE NOTICE 'BASIC_POLICY already exists. Skipping creation.';
  ELSE
    -- Create BASIC_POLICY
    INSERT INTO policies (name, description, type, expression, is_active, created_at, updated_at)
    VALUES (
      'BASIC_POLICY',
      'Basic access policy for all authenticated users (6/98 capabilities). Grants authorization API and service catalog access required by every authenticated user for core functionality.',
      'RBAC',
      '{"roles": ["BASIC_USER"]}',
      true,
      NOW(),
      NOW()
    );
    RAISE NOTICE 'BASIC_POLICY created successfully.';
  END IF;
END $$;

-- ============================================================================
-- SECTION 3: LINK BASIC CAPABILITIES TO BASIC_POLICY
-- ============================================================================
-- Link the new basic capabilities plus existing baseline capabilities to BASIC_POLICY

-- Get the policy ID
WITH policy_data AS (
  SELECT id FROM policies WHERE name = 'BASIC_POLICY' AND is_active = true
)
INSERT INTO policy_capabilities (policy_id, capability_id)
SELECT 
  pd.id,
  c.id
FROM policy_data pd
CROSS JOIN capabilities c
WHERE c.name IN (
  -- New basic capabilities
  'authorization.api.access',
  'service.catalog.read',
  -- Existing basic/foundational capabilities
  'user.account.read',
  'rbac.endpoint.read',
  'ui.page.read',
  'ui.action.read'
)
AND c.is_active = true
ON CONFLICT (policy_id, capability_id) DO NOTHING;

-- ============================================================================
-- SECTION 4: VALIDATION AND VERIFICATION
-- ============================================================================

DO $$
DECLARE
  v_policy_id BIGINT;
  v_capability_count INTEGER;
  v_auth_api_exists BOOLEAN;
  v_service_catalog_exists BOOLEAN;
BEGIN
  -- Check if BASIC_POLICY exists
  SELECT id INTO v_policy_id FROM policies WHERE name = 'BASIC_POLICY' AND is_active = true;
  
  IF v_policy_id IS NULL THEN
    RAISE EXCEPTION 'BASIC_POLICY not found. This is a critical error.';
  END IF;
  
  -- Check if new capabilities were created
  SELECT EXISTS(SELECT 1 FROM capabilities WHERE name = 'authorization.api.access' AND is_active = true) 
  INTO v_auth_api_exists;
  
  SELECT EXISTS(SELECT 1 FROM capabilities WHERE name = 'service.catalog.read' AND is_active = true) 
  INTO v_service_catalog_exists;
  
  -- Count capabilities linked to BASIC_POLICY
  SELECT COUNT(*) INTO v_capability_count 
  FROM policy_capabilities pc
  WHERE pc.policy_id = v_policy_id;
  
  -- Log results
  RAISE NOTICE '====== BASIC_POLICY SETUP VERIFICATION ======';
  RAISE NOTICE 'BASIC_POLICY ID: %', v_policy_id;
  RAISE NOTICE 'Total capabilities linked to BASIC_POLICY: %', v_capability_count;
  RAISE NOTICE 'Authorization API capability exists: %', v_auth_api_exists;
  RAISE NOTICE 'Service Catalog capability exists: %', v_service_catalog_exists;
  RAISE NOTICE '============================================';
  
  IF NOT v_auth_api_exists THEN
    RAISE WARNING 'authorization.api.access capability not found!';
  END IF;
  
  IF NOT v_service_catalog_exists THEN
    RAISE WARNING 'service.catalog.read capability not found!';
  END IF;
  
  IF v_capability_count < 4 THEN
    RAISE WARNING 'BASIC_POLICY has less than 4 capabilities. Expected at least 6.';
  END IF;
END $$;

-- ============================================================================
-- SECTION 5: DETAILED VERIFICATION REPORT
-- ============================================================================
SELECT 'BASIC_POLICY verification completed' as status;

-- Show BASIC_POLICY details
SELECT 
  p.name AS policy_name,
  p.description AS policy_description,
  COUNT(c.id) AS total_capabilities
FROM policies p
LEFT JOIN policy_capabilities pc ON p.id = pc.policy_id
LEFT JOIN capabilities c ON pc.capability_id = c.id AND c.is_active = true
WHERE p.name = 'BASIC_POLICY'
GROUP BY p.id, p.name, p.description;

-- Show capabilities in BASIC_POLICY
SELECT 
  c.name,
  c.description,
  c.module,
  c.action,
  c.resource
FROM policy_capabilities pc
JOIN policies p ON pc.policy_id = p.id
JOIN capabilities c ON pc.capability_id = c.id
WHERE p.name = 'BASIC_POLICY'
AND c.is_active = true
ORDER BY c.module, c.name;

COMMIT;

-- ============================================================================
-- POST-SCRIPT INSTRUCTIONS
-- ============================================================================
-- 
-- Next Steps:
-- 1. Verify BASIC_POLICY is created and linked to all users via BASIC_USER role
-- 2. Update endpoint policy mappings to link endpoints to BASIC_POLICY
-- 3. Ensure all authorization-related endpoints use BASIC_POLICY
-- 4. Test access to Authorization API and Service Catalog endpoints
-- 
-- To assign BASIC_POLICY to all users:
-- - Ensure all users have BASIC_USER role assigned (via user_roles table)
-- - BASIC_POLICY is already linked to BASIC_USER via expression: {"roles": ["BASIC_USER"]}
-- 
-- Expected Capabilities in BASIC_POLICY (6):
-- 1. user.account.read
-- 2. rbac.endpoint.read
-- 3. ui.page.read
-- 4. ui.action.read
-- 5. authorization.api.access (NEW)
-- 6. service.catalog.read (NEW)
--
-- ============================================================================
