-- ============================================================================
-- ONBOARDING PHASE 05: LINK BOOTSTRAP POLICIES TO CAPABILITIES
-- ============================================================================
-- Purpose: Map granular bootstrap policies to their respective capabilities.
-- Scope:
--   - BASIC_USER baseline capabilities
--   - BUSINESS_ADMIN slices (user + limited role operations)
--   - TECHNICAL_BOOTSTRAP slices (RBAC, UI, endpoint, and system operations)
--
-- Dependencies: 03_create_capabilities.sql and 04_create_policies.sql must run first.
-- PostgreSQL Syntax: Tested for PostgreSQL compliance.
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

-- Clear existing policy-capability links
DELETE FROM policy_capabilities;
-- Reset sequence for clean ID restart
ALTER SEQUENCE policy_capabilities_id_seq RESTART WITH 1;

-- ============================================================================
-- VALIDATE PREREQUISITES
-- ============================================================================
DO $$
DECLARE
  v_policy_count INTEGER;
  v_capability_count INTEGER;
BEGIN
  SELECT COUNT(*) INTO v_policy_count FROM policies WHERE is_active = true;
  SELECT COUNT(*) INTO v_capability_count FROM capabilities WHERE is_active = true;

  IF v_policy_count <> 17 THEN
    RAISE EXCEPTION 'Expected 17 active bootstrap policies, found %. Run 04_create_policies.sql first.', v_policy_count;
  END IF;

  IF v_capability_count <> 56 THEN
    RAISE EXCEPTION 'Expected 56 active capabilities, found %. Run 03_create_capabilities.sql first.', v_capability_count;
  END IF;

  RAISE NOTICE 'Prerequisites validated: % policies, % capabilities', v_policy_count, v_capability_count;
END $$;

-- ============================================================================
-- HELPER FUNCTION: Safe insert with FK validation
-- ============================================================================
CREATE OR REPLACE FUNCTION safe_policy_capability_link(
  p_policy_name TEXT,
  p_capability_name TEXT
)
RETURNS VOID AS $$
DECLARE
  v_policy_id BIGINT;
  v_capability_id BIGINT;
BEGIN
  -- Get policy ID with validation
  SELECT id INTO v_policy_id FROM policies 
  WHERE name = p_policy_name AND is_active = true;
  
  IF v_policy_id IS NULL THEN
    RAISE EXCEPTION 'Policy "%" not found or inactive', p_policy_name;
  END IF;
  
  -- Get capability ID with validation
  SELECT id INTO v_capability_id FROM capabilities 
  WHERE name = p_capability_name AND is_active = true;
  
  IF v_capability_id IS NULL THEN
    RAISE EXCEPTION 'Capability "%" not found or inactive', p_capability_name;
  END IF;
  
  -- Insert link
  INSERT INTO policy_capabilities (policy_id, capability_id)
  VALUES (v_policy_id, v_capability_id)
  ON CONFLICT (policy_id, capability_id) DO NOTHING;
  
EXCEPTION WHEN OTHERS THEN
  RAISE EXCEPTION 'Error linking policy "%" to capability "%": %', 
    p_policy_name, p_capability_name, SQLERRM;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- POLICY-CAPABILITY LINKS
-- ============================================================================

-- BASIC_USER baseline
SELECT safe_policy_capability_link('BASIC_USER_POLICY', 'authorization.api.access');
SELECT safe_policy_capability_link('BASIC_USER_POLICY', 'service.catalog.read');

-- BUSINESS_ADMIN policies
SELECT safe_policy_capability_link('USER_ACCOUNT_MANAGE_POLICY', 'user.account.create');
SELECT safe_policy_capability_link('USER_ACCOUNT_MANAGE_POLICY', 'user.account.read');
SELECT safe_policy_capability_link('USER_ACCOUNT_MANAGE_POLICY', 'user.account.update');
SELECT safe_policy_capability_link('USER_ACCOUNT_MANAGE_POLICY', 'user.account.delete');
SELECT safe_policy_capability_link('USER_ACCOUNT_MANAGE_POLICY', 'user.status.toggle');

SELECT safe_policy_capability_link('ROLE_READ_POLICY', 'rbac.role.read');
SELECT safe_policy_capability_link('ROLE_ASSIGN_POLICY', 'rbac.role.assign');
SELECT safe_policy_capability_link('ROLE_ASSIGN_POLICY', 'rbac.role.revoke');

SELECT safe_policy_capability_link('ROLE_MANAGE_POLICY', 'rbac.role.create');
SELECT safe_policy_capability_link('ROLE_MANAGE_POLICY', 'rbac.role.update');
SELECT safe_policy_capability_link('ROLE_MANAGE_POLICY', 'rbac.role.delete');

-- Technical capability management
SELECT safe_policy_capability_link('CAPABILITY_READ_POLICY', 'rbac.capability.read');
SELECT safe_policy_capability_link('CAPABILITY_READ_POLICY', 'rbac.capability.read-matrix');

SELECT safe_policy_capability_link('CAPABILITY_MANAGE_POLICY', 'rbac.capability.create');
SELECT safe_policy_capability_link('CAPABILITY_MANAGE_POLICY', 'rbac.capability.update');
SELECT safe_policy_capability_link('CAPABILITY_MANAGE_POLICY', 'rbac.capability.delete');
SELECT safe_policy_capability_link('CAPABILITY_MANAGE_POLICY', 'rbac.capability.toggle');

-- Policy management
SELECT safe_policy_capability_link('POLICY_READ_POLICY', 'rbac.policy.read');

SELECT safe_policy_capability_link('POLICY_MANAGE_POLICY', 'rbac.policy.create');
SELECT safe_policy_capability_link('POLICY_MANAGE_POLICY', 'rbac.policy.update');
SELECT safe_policy_capability_link('POLICY_MANAGE_POLICY', 'rbac.policy.delete');
SELECT safe_policy_capability_link('POLICY_MANAGE_POLICY', 'rbac.policy.toggle');
SELECT safe_policy_capability_link('POLICY_MANAGE_POLICY', 'rbac.policy.link-capability');
SELECT safe_policy_capability_link('POLICY_MANAGE_POLICY', 'rbac.policy.unlink-capability');

-- Endpoint management
SELECT safe_policy_capability_link('ENDPOINT_READ_POLICY', 'rbac.endpoint.read');

SELECT safe_policy_capability_link('ENDPOINT_MANAGE_POLICY', 'rbac.endpoint.create');
SELECT safe_policy_capability_link('ENDPOINT_MANAGE_POLICY', 'rbac.endpoint.update');
SELECT safe_policy_capability_link('ENDPOINT_MANAGE_POLICY', 'rbac.endpoint.delete');
SELECT safe_policy_capability_link('ENDPOINT_MANAGE_POLICY', 'rbac.endpoint.toggle');
SELECT safe_policy_capability_link('ENDPOINT_MANAGE_POLICY', 'rbac.endpoint.link-policy');
SELECT safe_policy_capability_link('ENDPOINT_MANAGE_POLICY', 'rbac.endpoint.unlink-policy');

-- UI page management
SELECT safe_policy_capability_link('UI_PAGE_READ_POLICY', 'ui.page.read');
SELECT safe_policy_capability_link('UI_PAGE_READ_POLICY', 'ui.page.read-children');

SELECT safe_policy_capability_link('UI_PAGE_MANAGE_POLICY', 'ui.page.create');
SELECT safe_policy_capability_link('UI_PAGE_MANAGE_POLICY', 'ui.page.update');
SELECT safe_policy_capability_link('UI_PAGE_MANAGE_POLICY', 'ui.page.delete');
SELECT safe_policy_capability_link('UI_PAGE_MANAGE_POLICY', 'ui.page.toggle');
SELECT safe_policy_capability_link('UI_PAGE_MANAGE_POLICY', 'ui.page.reorder');
SELECT safe_policy_capability_link('UI_PAGE_MANAGE_POLICY', 'ui.page.manage-hierarchy');

-- UI action management
SELECT safe_policy_capability_link('UI_ACTION_READ_POLICY', 'ui.action.read');
SELECT safe_policy_capability_link('UI_ACTION_READ_POLICY', 'ui.action.read-by-page');

SELECT safe_policy_capability_link('UI_ACTION_MANAGE_POLICY', 'ui.action.create');
SELECT safe_policy_capability_link('UI_ACTION_MANAGE_POLICY', 'ui.action.update');
SELECT safe_policy_capability_link('UI_ACTION_MANAGE_POLICY', 'ui.action.delete');
SELECT safe_policy_capability_link('UI_ACTION_MANAGE_POLICY', 'ui.action.toggle');
SELECT safe_policy_capability_link('UI_ACTION_MANAGE_POLICY', 'ui.action.reorder');

-- System operations
SELECT safe_policy_capability_link('SYSTEM_READ_POLICY', 'system.audit.read');
SELECT safe_policy_capability_link('SYSTEM_READ_POLICY', 'system.audit.filter');
SELECT safe_policy_capability_link('SYSTEM_READ_POLICY', 'system.audit.export');
SELECT safe_policy_capability_link('SYSTEM_READ_POLICY', 'system.settings.read');
SELECT safe_policy_capability_link('SYSTEM_READ_POLICY', 'system.ingestion.read-status');

SELECT safe_policy_capability_link('SYSTEM_MANAGE_POLICY', 'system.settings.update');
SELECT safe_policy_capability_link('SYSTEM_MANAGE_POLICY', 'system.ingestion.trigger-mt940');
SELECT safe_policy_capability_link('SYSTEM_MANAGE_POLICY', 'system.ingestion.trigger-van');

-- ============================================================================
-- VERIFICATION
-- ============================================================================
DO $$
DECLARE
  v_expected RECORD;
  v_actual INTEGER;
  v_total_links INTEGER;
BEGIN
  -- Validate capability counts per policy
  FOR v_expected IN
    SELECT * FROM (
      VALUES
        ('BASIC_USER_POLICY', 2),
        ('USER_ACCOUNT_MANAGE_POLICY', 5),
        ('ROLE_READ_POLICY', 1),
        ('ROLE_ASSIGN_POLICY', 2),
        ('ROLE_MANAGE_POLICY', 3),
        ('CAPABILITY_READ_POLICY', 2),
        ('CAPABILITY_MANAGE_POLICY', 4),
        ('POLICY_READ_POLICY', 1),
        ('POLICY_MANAGE_POLICY', 6),
        ('ENDPOINT_READ_POLICY', 1),
        ('ENDPOINT_MANAGE_POLICY', 6),
        ('UI_PAGE_READ_POLICY', 2),
        ('UI_PAGE_MANAGE_POLICY', 6),
        ('UI_ACTION_READ_POLICY', 2),
        ('UI_ACTION_MANAGE_POLICY', 5),
        ('SYSTEM_READ_POLICY', 5),
        ('SYSTEM_MANAGE_POLICY', 3)
    ) AS expected(policy_name, capability_count)
  LOOP
    SELECT COUNT(*) INTO v_actual
    FROM policy_capabilities pc
    JOIN policies p ON p.id = pc.policy_id
    WHERE p.name = v_expected.policy_name;

    IF v_actual <> v_expected.capability_count THEN
      RAISE EXCEPTION 'Policy % has % capabilities (expected %)', 
        v_expected.policy_name, v_actual, v_expected.capability_count;
    END IF;
  END LOOP;

  -- Total verification
  SELECT COUNT(*) INTO v_total_links FROM policy_capabilities;
  IF v_total_links <> 56 THEN
    RAISE EXCEPTION 'Expected 56 policy-capability links, found %', v_total_links;
  END IF;

  RAISE NOTICE '✓ Policy-capability linking completed: % total links', v_total_links;
END $$;

-- Display Summary Report
SELECT 
  p.name as policy_name,
  COUNT(pc.id) as capability_count,
  STRING_AGG(c.name, ', ' ORDER BY c.name) as capabilities
FROM policies p
LEFT JOIN policy_capabilities pc ON p.id = pc.policy_id
LEFT JOIN capabilities c ON pc.capability_id = c.id
WHERE p.is_active = true
GROUP BY p.id, p.name
ORDER BY policy_name;

COMMIT;

-- ============================================================================
-- POST-SCRIPT SUMMARY
-- ============================================================================
-- Policy-Capability Links: 56 total
--
-- Highlights:
--   - BASIC_USER baseline preserved (2 capabilities)
--   - BUSINESS_ADMIN role now derives discrete policies for user lifecycle + role assignment
--   - TECHNICAL_BOOTSTRAP role manages RBAC, UI, endpoint, and system surfaces via dedicated policies
--   - Helper function enforces FK validation and idempotent inserts
--
-- Next Step: Run 06_link_endpoints_to_policies.sql to align endpoint protections with the new policy slices.
-- ============================================================================
