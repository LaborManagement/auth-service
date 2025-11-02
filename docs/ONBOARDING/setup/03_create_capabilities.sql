-- ============================================================================
-- PHASE 2: CREATE CAPABILITIES (98 Total) - CORRECTED VERSION
-- ============================================================================
-- Purpose: Define all 98 atomic capabilities using <domain>.<subject>.<action> format
-- Includes: name, description, module, action, resource, is_active, timestamps
-- PostgreSQL Syntax: Tested for PostgreSQL compliance
-- Dependencies: None (initial schema setup)
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

CREATE TABLE IF NOT EXISTS capabilities_backup AS
SELECT c.*, NOW()::timestamp AS backup_created_at
FROM capabilities c
WHERE false;

ALTER TABLE capabilities_backup
    ADD COLUMN IF NOT EXISTS backup_created_at timestamp without time zone;

INSERT INTO capabilities_backup
SELECT c.*, NOW()::timestamp
FROM capabilities c;

-- Clear existing capabilities (for fresh setup)
-- Use DELETE for referential integrity instead of TRUNCATE when linked tables exist
DELETE FROM policy_capabilities WHERE capability_id IN (SELECT id FROM capabilities);
DELETE FROM capabilities;
-- Reset sequence for clean ID restart
ALTER SEQUENCE capabilities_id_seq RESTART WITH 1;

-- ============================================================================
-- MODULE 1: USER MANAGEMENT (5 capabilities)
-- ============================================================================
INSERT INTO capabilities (name, description, module, action, resource, is_active, created_at, updated_at) VALUES

('user.account.create', 'Create new user account', 'USER_MANAGEMENT', 'CREATE', 'USER', true, NOW(), NOW()),
('user.account.read', 'View user details and list users', 'USER_MANAGEMENT', 'READ', 'USER', true, NOW(), NOW()),
('user.account.update', 'Edit user information', 'USER_MANAGEMENT', 'UPDATE', 'USER', true, NOW(), NOW()),
('user.account.delete', 'Delete user account', 'USER_MANAGEMENT', 'DELETE', 'USER', true, NOW(), NOW()),
('user.status.toggle', 'Enable/disable user account', 'USER_MANAGEMENT', 'TOGGLE', 'USER_STATUS', true, NOW(), NOW()),

-- ============================================================================
-- MODULE 2: PAYMENT FILE MANAGEMENT (8 capabilities)
-- ============================================================================
('payment.file.upload', 'Upload payment CSV file', 'PAYMENT_FILE_MANAGEMENT', 'UPLOAD', 'PAYMENT_FILE', true, NOW(), NOW()),
('payment.file.read', 'View uploaded payment files', 'PAYMENT_FILE_MANAGEMENT', 'READ', 'PAYMENT_FILE', true, NOW(), NOW()),
('payment.file.download', 'Download payment file', 'PAYMENT_FILE_MANAGEMENT', 'DOWNLOAD', 'PAYMENT_FILE', true, NOW(), NOW()),
('payment.file.delete', 'Delete uploaded payment file', 'PAYMENT_FILE_MANAGEMENT', 'DELETE', 'PAYMENT_FILE', true, NOW(), NOW()),
('payment.file.validate', 'Validate payment file', 'PAYMENT_FILE_MANAGEMENT', 'VALIDATE', 'PAYMENT_FILE', true, NOW(), NOW()),
('payment.summary.read', 'View file upload summaries', 'PAYMENT_FILE_MANAGEMENT', 'READ', 'PAYMENT_SUMMARY', true, NOW(), NOW()),
('payment.record.read', 'View payment records', 'PAYMENT_FILE_MANAGEMENT', 'READ', 'PAYMENT_RECORD', true, NOW(), NOW()),
('payment.details.read', 'View detailed payment information', 'PAYMENT_FILE_MANAGEMENT', 'READ', 'PAYMENT_DETAILS', true, NOW(), NOW()),

-- ============================================================================
-- MODULE 3: PAYMENT REQUEST MANAGEMENT (9 capabilities)
-- ============================================================================
('reconciliation.request.create', 'Create payment request from file', 'PAYMENT_REQUEST_MANAGEMENT', 'CREATE', 'RECONCILIATION_REQUEST', true, NOW(), NOW()),
('reconciliation.request.read', 'View payment requests', 'PAYMENT_REQUEST_MANAGEMENT', 'READ', 'RECONCILIATION_REQUEST', true, NOW(), NOW()),
('reconciliation.request.update', 'Update request details', 'PAYMENT_REQUEST_MANAGEMENT', 'UPDATE', 'RECONCILIATION_REQUEST', true, NOW(), NOW()),
('reconciliation.request.delete', 'Delete payment request', 'PAYMENT_REQUEST_MANAGEMENT', 'DELETE', 'RECONCILIATION_REQUEST', true, NOW(), NOW()),
('reconciliation.request.submit', 'Submit request to employer', 'PAYMENT_REQUEST_MANAGEMENT', 'SUBMIT', 'RECONCILIATION_REQUEST', true, NOW(), NOW()),
('reconciliation.request.track', 'Track request status and workflow', 'PAYMENT_REQUEST_MANAGEMENT', 'READ', 'RECONCILIATION_REQUEST_STATUS', true, NOW(), NOW()),
('reconciliation.request.validate', 'Validate payment request (Employer)', 'PAYMENT_REQUEST_MANAGEMENT', 'VALIDATE', 'RECONCILIATION_REQUEST', true, NOW(), NOW()),
('reconciliation.payment.approve', 'Approve request (Employer/Board)', 'PAYMENT_REQUEST_MANAGEMENT', 'APPROVE', 'PAYMENT_REQUEST', true, NOW(), NOW()),
('reconciliation.payment.reject', 'Reject request with reason', 'PAYMENT_REQUEST_MANAGEMENT', 'REJECT', 'PAYMENT_REQUEST', true, NOW(), NOW()),

-- ============================================================================
-- MODULE 4: WORKER OPERATIONS (6 capabilities)
-- ============================================================================
('worker.data.upload', 'Upload worker payment data', 'WORKER_OPERATIONS', 'UPLOAD', 'WORKER_DATA', true, NOW(), NOW()),
('worker.data.read', 'View worker payment data', 'WORKER_OPERATIONS', 'READ', 'WORKER_DATA', true, NOW(), NOW()),
('worker.request.create', 'Create payment request as worker', 'WORKER_OPERATIONS', 'CREATE', 'WORKER_REQUEST', true, NOW(), NOW()),
('worker.request.submit', 'Submit request to employer', 'WORKER_OPERATIONS', 'SUBMIT', 'WORKER_REQUEST', true, NOW(), NOW()),
('worker.status.read', 'View request status', 'WORKER_OPERATIONS', 'READ', 'WORKER_STATUS', true, NOW(), NOW()),
('worker.receipt.send', 'Send receipt to employer', 'WORKER_OPERATIONS', 'SEND', 'WORKER_RECEIPT', true, NOW(), NOW()),

-- ============================================================================
-- MODULE 5: EMPLOYER OPERATIONS (5 capabilities)
-- ============================================================================
('employer.request.read', 'View worker payment requests', 'EMPLOYER_OPERATIONS', 'READ', 'EMPLOYER_REQUEST', true, NOW(), NOW()),
('employer.request.validate', 'Validate payment requests', 'EMPLOYER_OPERATIONS', 'VALIDATE', 'EMPLOYER_REQUEST', true, NOW(), NOW()),
('employer.payment.approve', 'Approve requests and send to board', 'EMPLOYER_OPERATIONS', 'APPROVE', 'EMPLOYER_PAYMENT', true, NOW(), NOW()),
('employer.payment.reject', 'Reject requests', 'EMPLOYER_OPERATIONS', 'REJECT', 'EMPLOYER_PAYMENT', true, NOW(), NOW()),
('employer.receipt.read', 'View payment receipts', 'EMPLOYER_OPERATIONS', 'READ', 'EMPLOYER_RECEIPT', true, NOW(), NOW()),

-- ============================================================================
-- MODULE 6: BOARD OPERATIONS (7 capabilities)
-- ============================================================================
('board.request.read', 'View all payment requests', 'BOARD_OPERATIONS', 'READ', 'BOARD_REQUEST', true, NOW(), NOW()),
('board.payment.reconcile', 'Perform reconciliation', 'BOARD_OPERATIONS', 'RECONCILE', 'BOARD_PAYMENT', true, NOW(), NOW()),
('board.decision.vote', 'Vote on board decisions', 'BOARD_OPERATIONS', 'VOTE', 'BOARD_DECISION', true, NOW(), NOW()),
('board.payment.approve', 'Give final approval', 'BOARD_OPERATIONS', 'APPROVE', 'BOARD_PAYMENT', true, NOW(), NOW()),
('board.payment.reject', 'Reject at board level', 'BOARD_OPERATIONS', 'REJECT', 'BOARD_PAYMENT', true, NOW(), NOW()),
('board.receipt.read', 'View board receipts', 'BOARD_OPERATIONS', 'READ', 'BOARD_RECEIPT', true, NOW(), NOW()),
('board.receipt.process', 'Process board receipt', 'BOARD_OPERATIONS', 'PROCESS', 'BOARD_RECEIPT', true, NOW(), NOW()),

-- ============================================================================
-- MODULE 7: RBAC - ROLE MANAGEMENT (6 capabilities)
-- ============================================================================
('rbac.role.create', 'Create new role', 'RBAC_ROLE_MANAGEMENT', 'CREATE', 'RBAC_ROLE', true, NOW(), NOW()),
('rbac.role.read', 'View roles and permissions', 'RBAC_ROLE_MANAGEMENT', 'READ', 'RBAC_ROLE', true, NOW(), NOW()),
('rbac.role.update', 'Edit role details', 'RBAC_ROLE_MANAGEMENT', 'UPDATE', 'RBAC_ROLE', true, NOW(), NOW()),
('rbac.role.delete', 'Delete role', 'RBAC_ROLE_MANAGEMENT', 'DELETE', 'RBAC_ROLE', true, NOW(), NOW()),
('rbac.role.assign', 'Assign role to user', 'RBAC_ROLE_MANAGEMENT', 'ASSIGN', 'RBAC_ROLE', true, NOW(), NOW()),
('rbac.role.revoke', 'Revoke role from user', 'RBAC_ROLE_MANAGEMENT', 'REVOKE', 'RBAC_ROLE', true, NOW(), NOW()),

-- ============================================================================
-- MODULE 8: RBAC - POLICY MANAGEMENT (7 capabilities)
-- ============================================================================
('rbac.policy.create', 'Create new security policy', 'RBAC_POLICY_MANAGEMENT', 'CREATE', 'RBAC_POLICY', true, NOW(), NOW()),
('rbac.policy.read', 'View policies', 'RBAC_POLICY_MANAGEMENT', 'READ', 'RBAC_POLICY', true, NOW(), NOW()),
('rbac.policy.update', 'Edit policy details', 'RBAC_POLICY_MANAGEMENT', 'UPDATE', 'RBAC_POLICY', true, NOW(), NOW()),
('rbac.policy.delete', 'Delete policy', 'RBAC_POLICY_MANAGEMENT', 'DELETE', 'RBAC_POLICY', true, NOW(), NOW()),
('rbac.policy.toggle', 'Toggle policy active status', 'RBAC_POLICY_MANAGEMENT', 'TOGGLE', 'RBAC_POLICY', true, NOW(), NOW()),
('rbac.policy.link-capability', 'Link capability to policy', 'RBAC_POLICY_MANAGEMENT', 'LINK', 'RBAC_POLICY_CAPABILITY', true, NOW(), NOW()),
('rbac.policy.unlink-capability', 'Remove capability from policy', 'RBAC_POLICY_MANAGEMENT', 'UNLINK', 'RBAC_POLICY_CAPABILITY', true, NOW(), NOW()),

-- ============================================================================
-- MODULE 9: RBAC - CAPABILITY MANAGEMENT (6 capabilities)
-- ============================================================================
('rbac.capability.create', 'Create new capability', 'RBAC_CAPABILITY_MANAGEMENT', 'CREATE', 'RBAC_CAPABILITY', true, NOW(), NOW()),
('rbac.capability.read', 'View capabilities', 'RBAC_CAPABILITY_MANAGEMENT', 'READ', 'RBAC_CAPABILITY', true, NOW(), NOW()),
('rbac.capability.update', 'Edit capability details', 'RBAC_CAPABILITY_MANAGEMENT', 'UPDATE', 'RBAC_CAPABILITY', true, NOW(), NOW()),
('rbac.capability.delete', 'Delete capability', 'RBAC_CAPABILITY_MANAGEMENT', 'DELETE', 'RBAC_CAPABILITY', true, NOW(), NOW()),
('rbac.capability.toggle', 'Toggle capability active status', 'RBAC_CAPABILITY_MANAGEMENT', 'TOGGLE', 'RBAC_CAPABILITY', true, NOW(), NOW()),
('rbac.capability.read-matrix', 'View capability matrix', 'RBAC_CAPABILITY_MANAGEMENT', 'READ', 'RBAC_CAPABILITY_MATRIX', true, NOW(), NOW()),

-- ============================================================================
-- MODULE 10: API ENDPOINT MANAGEMENT (7 capabilities)
-- ============================================================================
('rbac.endpoint.create', 'Create new API endpoint', 'RBAC_ENDPOINT_MANAGEMENT', 'CREATE', 'RBAC_ENDPOINT', true, NOW(), NOW()),
('rbac.endpoint.read', 'View API endpoints', 'RBAC_ENDPOINT_MANAGEMENT', 'READ', 'RBAC_ENDPOINT', true, NOW(), NOW()),
('rbac.endpoint.update', 'Edit endpoint details', 'RBAC_ENDPOINT_MANAGEMENT', 'UPDATE', 'RBAC_ENDPOINT', true, NOW(), NOW()),
('rbac.endpoint.delete', 'Delete API endpoint', 'RBAC_ENDPOINT_MANAGEMENT', 'DELETE', 'RBAC_ENDPOINT', true, NOW(), NOW()),
('rbac.endpoint.toggle', 'Toggle endpoint active status', 'RBAC_ENDPOINT_MANAGEMENT', 'TOGGLE', 'RBAC_ENDPOINT', true, NOW(), NOW()),
('rbac.endpoint.link-policy', 'Link policy to endpoint', 'RBAC_ENDPOINT_MANAGEMENT', 'LINK', 'RBAC_ENDPOINT_POLICY', true, NOW(), NOW()),
('rbac.endpoint.unlink-policy', 'Unlink policy from endpoint', 'RBAC_ENDPOINT_MANAGEMENT', 'UNLINK', 'RBAC_ENDPOINT_POLICY', true, NOW(), NOW()),

-- ============================================================================
-- MODULE 11: UI PAGE MANAGEMENT (8 capabilities)
-- ============================================================================
('ui.page.create', 'Create new UI page', 'UI_PAGE_MANAGEMENT', 'CREATE', 'UI_PAGE', true, NOW(), NOW()),
('ui.page.read', 'View UI pages', 'UI_PAGE_MANAGEMENT', 'READ', 'UI_PAGE', true, NOW(), NOW()),
('ui.page.update', 'Edit page details', 'UI_PAGE_MANAGEMENT', 'UPDATE', 'UI_PAGE', true, NOW(), NOW()),
('ui.page.delete', 'Delete UI page', 'UI_PAGE_MANAGEMENT', 'DELETE', 'UI_PAGE', true, NOW(), NOW()),
('ui.page.toggle', 'Toggle page active status', 'UI_PAGE_MANAGEMENT', 'TOGGLE', 'UI_PAGE', true, NOW(), NOW()),
('ui.page.reorder', 'Reorder UI pages', 'UI_PAGE_MANAGEMENT', 'REORDER', 'UI_PAGE', true, NOW(), NOW()),
('ui.page.read-children', 'Read child pages', 'UI_PAGE_MANAGEMENT', 'READ', 'UI_PAGE_CHILDREN', true, NOW(), NOW()),
('ui.page.manage-hierarchy', 'Manage page hierarchy', 'UI_PAGE_MANAGEMENT', 'MANAGE', 'UI_PAGE_HIERARCHY', true, NOW(), NOW()),

-- ============================================================================
-- MODULE 12: PAGE ACTION MANAGEMENT (7 capabilities)
-- ============================================================================
('ui.action.create', 'Create new page action', 'UI_ACTION_MANAGEMENT', 'CREATE', 'UI_ACTION', true, NOW(), NOW()),
('ui.action.read', 'View page actions', 'UI_ACTION_MANAGEMENT', 'READ', 'UI_ACTION', true, NOW(), NOW()),
('ui.action.update', 'Edit action details', 'UI_ACTION_MANAGEMENT', 'UPDATE', 'UI_ACTION', true, NOW(), NOW()),
('ui.action.delete', 'Delete page action', 'UI_ACTION_MANAGEMENT', 'DELETE', 'UI_ACTION', true, NOW(), NOW()),
('ui.action.toggle', 'Toggle action active status', 'UI_ACTION_MANAGEMENT', 'TOGGLE', 'UI_ACTION', true, NOW(), NOW()),
('ui.action.reorder', 'Reorder page actions', 'UI_ACTION_MANAGEMENT', 'REORDER', 'UI_ACTION', true, NOW(), NOW()),
('ui.action.read-by-page', 'Read actions by page', 'UI_ACTION_MANAGEMENT', 'READ', 'UI_ACTION_BY_PAGE', true, NOW(), NOW()),

-- ============================================================================
-- MODULE 13: SYSTEM & REPORTING (8 capabilities)
-- ============================================================================
('system.ingestion.trigger-mt940', 'Trigger MT940 file ingestion', 'SYSTEM_OPERATIONS', 'TRIGGER', 'SYSTEM_INGESTION_MT940', true, NOW(), NOW()),
('system.ingestion.trigger-van', 'Trigger VAN file ingestion', 'SYSTEM_OPERATIONS', 'TRIGGER', 'SYSTEM_INGESTION_VAN', true, NOW(), NOW()),
('system.audit.read', 'View audit logs', 'SYSTEM_OPERATIONS', 'READ', 'SYSTEM_AUDIT', true, NOW(), NOW()),
('system.audit.filter', 'Filter audit logs', 'SYSTEM_OPERATIONS', 'FILTER', 'SYSTEM_AUDIT', true, NOW(), NOW()),
('system.audit.export', 'Export audit logs', 'SYSTEM_OPERATIONS', 'EXPORT', 'SYSTEM_AUDIT', true, NOW(), NOW()),
('system.settings.read', 'View system settings', 'SYSTEM_OPERATIONS', 'READ', 'SYSTEM_SETTINGS', true, NOW(), NOW()),
('system.settings.update', 'Update system settings', 'SYSTEM_OPERATIONS', 'UPDATE', 'SYSTEM_SETTINGS', true, NOW(), NOW()),
('system.ingestion.read-status', 'View ingestion status', 'SYSTEM_OPERATIONS', 'READ', 'SYSTEM_INGESTION_STATUS', true, NOW(), NOW());

-- ============================================================================
-- VERIFICATION
-- ============================================================================
DO $$
DECLARE
  v_count INTEGER;
BEGIN
  SELECT COUNT(*) INTO v_count FROM capabilities WHERE is_active = true;
  IF v_count <> 89 THEN
    RAISE EXCEPTION 'Expected 89 active capabilities, found %', v_count;
  END IF;
  RAISE NOTICE 'Successfully created % capabilities', v_count;
END $$;

COMMIT;
