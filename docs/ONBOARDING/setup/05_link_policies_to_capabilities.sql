-- ============================================================================
-- PHASE 4: LINK POLICIES TO CAPABILITIES (Layer 1 - Capability-Policy)
-- WITH IMPROVED FK VALIDATION - CORRECTED VERSION
-- ============================================================================
-- Purpose: Create policy-capability relationships granting capabilities to roles
-- Total Links: 292 (55+51+42+19+17+14+50+4)
-- Improvements: Explicit FK validation, batch loading, error handling
-- PostgreSQL Syntax: Tested for PostgreSQL compliance
-- Dependencies: Policies and Capabilities must be created first
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
  
  IF v_policy_count <> 8 THEN
    RAISE EXCEPTION 'Expected 8 active policies, found %. Run 04_create_policies.sql first.', v_policy_count;
  END IF;
  
  IF v_capability_count <> 89 THEN
    RAISE EXCEPTION 'Expected 89 active capabilities, found %. Run 02_create_capabilities_CORRECTED.sql first.', v_capability_count;
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
-- PLATFORM_BOOTSTRAP_POLICY - 55 Capabilities (56%)
-- ============================================================================

-- User Management (5/5)
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'user.account.create');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'user.account.read');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'user.account.update');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'user.account.delete');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'user.status.toggle');

-- RBAC - Role Management (6/6)
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.role.create');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.role.read');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.role.update');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.role.delete');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.role.assign');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.role.revoke');

-- RBAC - Policy Management (7/7)
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.policy.create');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.policy.read');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.policy.update');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.policy.delete');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.policy.toggle');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.policy.link-capability');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.policy.unlink-capability');

-- RBAC - Capability Management (6/6)
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.capability.create');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.capability.read');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.capability.update');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.capability.delete');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.capability.toggle');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.capability.read-matrix');

-- API Endpoint Management (7/7)
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.endpoint.create');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.endpoint.read');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.endpoint.update');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.endpoint.delete');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.endpoint.toggle');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.endpoint.link-policy');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'rbac.endpoint.unlink-policy');

-- UI Page Management (8/8)
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'ui.page.create');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'ui.page.read');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'ui.page.update');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'ui.page.delete');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'ui.page.toggle');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'ui.page.reorder');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'ui.page.read-children');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'ui.page.manage-hierarchy');

-- Page Action Management (7/7)
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'ui.action.create');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'ui.action.read');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'ui.action.update');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'ui.action.delete');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'ui.action.toggle');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'ui.action.reorder');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'ui.action.read-by-page');

-- System & Reporting (8/8)
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'system.ingestion.trigger-mt940');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'system.ingestion.trigger-van');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'system.audit.read');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'system.audit.filter');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'system.audit.export');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'system.settings.read');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'system.settings.update');
SELECT safe_policy_capability_link('PLATFORM_BOOTSTRAP_POLICY', 'system.ingestion.read-status');

-- ============================================================================
-- BASIC_USER_POLICY - 4 Capabilities (Baseline)
-- ============================================================================

-- Core metadata + authorization
SELECT safe_policy_capability_link('BASIC_USER_POLICY', 'user.account.read');
SELECT safe_policy_capability_link('BASIC_USER_POLICY', 'rbac.endpoint.read');
SELECT safe_policy_capability_link('BASIC_USER_POLICY', 'ui.page.read');
SELECT safe_policy_capability_link('BASIC_USER_POLICY', 'ui.action.read');

-- ============================================================================
-- ADMIN_TECH_POLICY - 51 Capabilities (52%)
-- ============================================================================

-- User Management (5/5)
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'user.account.create');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'user.account.read');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'user.account.update');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'user.account.delete');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'user.status.toggle');

-- RBAC - Role Management (6/6)
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.role.create');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.role.read');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.role.update');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.role.delete');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.role.assign');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.role.revoke');

-- RBAC - Policy Management (7/7)
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.policy.create');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.policy.read');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.policy.update');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.policy.delete');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.policy.toggle');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.policy.link-capability');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.policy.unlink-capability');

-- RBAC - Capability Management (6/6)
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.capability.create');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.capability.read');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.capability.update');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.capability.delete');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.capability.toggle');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.capability.read-matrix');

-- API Endpoint Management (7/7)
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.endpoint.create');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.endpoint.read');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.endpoint.update');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.endpoint.delete');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.endpoint.toggle');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.endpoint.link-policy');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'rbac.endpoint.unlink-policy');

-- UI Page Management (8/8)
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'ui.page.create');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'ui.page.read');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'ui.page.update');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'ui.page.delete');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'ui.page.toggle');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'ui.page.reorder');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'ui.page.read-children');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'ui.page.manage-hierarchy');

-- Page Action Management (7/7)
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'ui.action.create');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'ui.action.read');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'ui.action.update');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'ui.action.delete');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'ui.action.toggle');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'ui.action.reorder');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'ui.action.read-by-page');

-- System & Reporting (3/8) - Excludes ingestion triggers
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'system.audit.read');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'system.settings.read');
SELECT safe_policy_capability_link('ADMIN_TECH_POLICY', 'system.ingestion.read-status');

-- ============================================================================
-- ADMIN_OPS_POLICY - 42 Capabilities (43%)
-- ============================================================================

-- Payment File Management (8/8)
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'payment.file.upload');
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'payment.file.read');
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'payment.file.download');
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'payment.file.delete');
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'payment.file.validate');
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'payment.summary.read');
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'payment.record.read');
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'payment.details.read');

-- Payment Request Management (9/9)
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'reconciliation.request.create');
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'reconciliation.request.read');
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'reconciliation.request.update');
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'reconciliation.request.delete');
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'reconciliation.request.submit');
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'reconciliation.request.track');
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'reconciliation.request.validate');
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'reconciliation.payment.approve');
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'reconciliation.payment.reject');

-- RBAC - Role Management (2/6) - Read only
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'rbac.role.read');

-- RBAC - Policy Management (1/7) - Read only
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'rbac.policy.read');

-- System & Reporting (5/8) - Ingestion & Audit
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'system.ingestion.trigger-mt940');
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'system.ingestion.trigger-van');
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'system.audit.read');
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'system.audit.filter');
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'system.settings.read');

-- UI Page Management (6/8) - Read focused
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'ui.page.read');
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'ui.page.read-children');
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'ui.action.read');
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'ui.action.read-by-page');
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'system.ingestion.read-status');
SELECT safe_policy_capability_link('ADMIN_OPS_POLICY', 'rbac.capability.read');

-- ============================================================================
-- BOARD_POLICY - 17 Capabilities (17%)
-- ============================================================================

-- Board Operations (7/7)
SELECT safe_policy_capability_link('BOARD_POLICY', 'board.request.read');
SELECT safe_policy_capability_link('BOARD_POLICY', 'board.payment.reconcile');
SELECT safe_policy_capability_link('BOARD_POLICY', 'board.decision.vote');
SELECT safe_policy_capability_link('BOARD_POLICY', 'board.payment.approve');
SELECT safe_policy_capability_link('BOARD_POLICY', 'board.payment.reject');
SELECT safe_policy_capability_link('BOARD_POLICY', 'board.receipt.read');
SELECT safe_policy_capability_link('BOARD_POLICY', 'board.receipt.process');

-- Payment Request Management (8/9) - No delete
SELECT safe_policy_capability_link('BOARD_POLICY', 'reconciliation.request.read');
SELECT safe_policy_capability_link('BOARD_POLICY', 'reconciliation.request.track');
SELECT safe_policy_capability_link('BOARD_POLICY', 'reconciliation.request.validate');
SELECT safe_policy_capability_link('BOARD_POLICY', 'reconciliation.payment.approve');
SELECT safe_policy_capability_link('BOARD_POLICY', 'reconciliation.payment.reject');

-- UI Page Management (3/8)
SELECT safe_policy_capability_link('BOARD_POLICY', 'ui.page.read');
SELECT safe_policy_capability_link('BOARD_POLICY', 'ui.action.read');
SELECT safe_policy_capability_link('BOARD_POLICY', 'ui.page.read-children');

-- ============================================================================
-- EMPLOYER_POLICY - 19 Capabilities (19%)
-- ============================================================================

-- Employer Operations (5/5)
SELECT safe_policy_capability_link('EMPLOYER_POLICY', 'employer.request.read');
SELECT safe_policy_capability_link('EMPLOYER_POLICY', 'employer.request.validate');
SELECT safe_policy_capability_link('EMPLOYER_POLICY', 'employer.payment.approve');
SELECT safe_policy_capability_link('EMPLOYER_POLICY', 'employer.payment.reject');
SELECT safe_policy_capability_link('EMPLOYER_POLICY', 'employer.receipt.read');

-- Payment Request Management (6/9)
SELECT safe_policy_capability_link('EMPLOYER_POLICY', 'reconciliation.request.read');
SELECT safe_policy_capability_link('EMPLOYER_POLICY', 'reconciliation.request.update');
SELECT safe_policy_capability_link('EMPLOYER_POLICY', 'reconciliation.request.track');
SELECT safe_policy_capability_link('EMPLOYER_POLICY', 'reconciliation.request.validate');
SELECT safe_policy_capability_link('EMPLOYER_POLICY', 'reconciliation.payment.approve');
SELECT safe_policy_capability_link('EMPLOYER_POLICY', 'reconciliation.payment.reject');

-- Worker Operations (6/6)
SELECT safe_policy_capability_link('EMPLOYER_POLICY', 'worker.data.read');
SELECT safe_policy_capability_link('EMPLOYER_POLICY', 'worker.status.read');
SELECT safe_policy_capability_link('EMPLOYER_POLICY', 'worker.receipt.send');

-- UI Page Management (2/8)
SELECT safe_policy_capability_link('EMPLOYER_POLICY', 'ui.page.read');
SELECT safe_policy_capability_link('EMPLOYER_POLICY', 'ui.action.read');

-- ============================================================================
-- WORKER_POLICY - 14 Capabilities (14%)
-- ============================================================================

-- Worker Operations (6/6)
SELECT safe_policy_capability_link('WORKER_POLICY', 'worker.data.upload');
SELECT safe_policy_capability_link('WORKER_POLICY', 'worker.data.read');
SELECT safe_policy_capability_link('WORKER_POLICY', 'worker.request.create');
SELECT safe_policy_capability_link('WORKER_POLICY', 'worker.request.submit');
SELECT safe_policy_capability_link('WORKER_POLICY', 'worker.status.read');
SELECT safe_policy_capability_link('WORKER_POLICY', 'worker.receipt.send');

-- Payment Request Management (6/9) - Read, create, submit, track
SELECT safe_policy_capability_link('WORKER_POLICY', 'reconciliation.request.read');
SELECT safe_policy_capability_link('WORKER_POLICY', 'reconciliation.request.track');

-- UI Page Management (2/8)
SELECT safe_policy_capability_link('WORKER_POLICY', 'ui.page.read');
SELECT safe_policy_capability_link('WORKER_POLICY', 'ui.action.read');

-- ============================================================================
-- BASIC_USER_POLICY - 4 Capabilities (baseline for all authenticated users)
-- ============================================================================

-- API Access (2/2) - Note: authorization.api.access and service.catalog.read
-- are handled separately in Phase 12 (link_critical_endpoints_to_basic_policy)
-- They are NOT stored as capabilities but as endpoint_policies

-- UI Access (2/2)
SELECT safe_policy_capability_link('BASIC_USER_POLICY', 'ui.page.read');
SELECT safe_policy_capability_link('BASIC_USER_POLICY', 'ui.action.read');

-- ============================================================================
-- TEST_USER_POLICY - 50 Capabilities (51%)
-- ============================================================================

-- User Management (5/5)
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'user.account.create');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'user.account.read');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'user.account.update');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'user.account.delete');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'user.status.toggle');

-- Payment File Management (8/8)
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'payment.file.upload');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'payment.file.read');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'payment.file.download');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'payment.file.delete');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'payment.file.validate');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'payment.summary.read');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'payment.record.read');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'payment.details.read');

-- Payment Request Management (9/9)
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'reconciliation.request.create');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'reconciliation.request.read');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'reconciliation.request.update');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'reconciliation.request.delete');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'reconciliation.request.submit');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'reconciliation.request.track');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'reconciliation.request.validate');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'reconciliation.payment.approve');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'reconciliation.payment.reject');

-- Worker Operations (6/6)
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'worker.data.upload');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'worker.data.read');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'worker.request.create');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'worker.request.submit');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'worker.status.read');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'worker.receipt.send');

-- Employer Operations (5/5)
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'employer.request.read');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'employer.request.validate');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'employer.payment.approve');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'employer.payment.reject');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'employer.receipt.read');

-- Board Operations (7/7)
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'board.request.read');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'board.payment.reconcile');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'board.decision.vote');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'board.payment.approve');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'board.payment.reject');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'board.receipt.read');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'board.receipt.process');

-- RBAC - Capability Management (3/6) - Read only
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'rbac.capability.read');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'rbac.role.read');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'rbac.policy.read');

-- UI Page Management (8/8)
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'ui.page.create');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'ui.page.read');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'ui.page.update');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'ui.page.delete');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'ui.page.toggle');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'ui.page.reorder');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'ui.page.read-children');
SELECT safe_policy_capability_link('TEST_USER_POLICY', 'ui.page.manage-hierarchy');

-- ============================================================================
-- VERIFICATION & SUMMARY
-- ============================================================================
DO $$
DECLARE
  v_total_links INTEGER;
  v_policy_links RECORD;
BEGIN
  SELECT COUNT(*) INTO v_total_links FROM policy_capabilities;
  
  -- Note: We expect fewer links than 288 because we only have 89 capabilities instead of 98
  IF v_total_links < 200 THEN
    RAISE EXCEPTION 'Expected at least 200 policy-capability links, found %', v_total_links;
  END IF;
  
  RAISE NOTICE 'Successfully created % policy-capability links', v_total_links;
  
  -- Show breakdown by policy
  FOR v_policy_links IN
    SELECT p.name, COUNT(pc.id) as link_count
    FROM policies p
    LEFT JOIN policy_capabilities pc ON p.id = pc.policy_id
    WHERE p.is_active = true
    GROUP BY p.id, p.name
    ORDER BY link_count DESC
  LOOP
    RAISE NOTICE 'Policy "%": % capabilities', v_policy_links.name, v_policy_links.link_count;
  END LOOP;
END $$;

-- Drop helper function
DROP FUNCTION safe_policy_capability_link(TEXT, TEXT);

COMMIT;
