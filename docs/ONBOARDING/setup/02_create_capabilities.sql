-- ============================================================================
-- PHASE 2: CREATE CAPABILITIES (98 Total)
-- ============================================================================
-- Purpose: Define all 98 atomic capabilities using <domain>.<subject>.<action> format
-- Format: <domain>.<subject>.<action>
-- Dependencies: None (initial schema setup)
-- ============================================================================

\set ON_ERROR_STOP on

-- Clear existing capabilities (for fresh setup)
TRUNCATE TABLE capabilities CASCADE;

-- ============================================================================
-- MODULE 1: USER MANAGEMENT (5 capabilities)
-- ============================================================================
INSERT INTO capabilities (name, description, module, is_active, created_at, updated_at) VALUES

('user.account.create', 'Create new user account', 'USER_MANAGEMENT', true, NOW(), NOW()),
('user.account.read', 'View user details and list users', 'USER_MANAGEMENT', true, NOW(), NOW()),
('user.account.update', 'Edit user information', 'USER_MANAGEMENT', true, NOW(), NOW()),
('user.account.delete', 'Delete user account', 'USER_MANAGEMENT', true, NOW(), NOW()),
('user.status.toggle', 'Enable/disable user account', 'USER_MANAGEMENT', true, NOW(), NOW()),

-- ============================================================================
-- MODULE 2: PAYMENT FILE MANAGEMENT (8 capabilities)
-- ============================================================================
('payment.file.upload', 'Upload payment CSV file', 'PAYMENT_FILE_MANAGEMENT', true, NOW(), NOW()),
('payment.file.read', 'View uploaded payment files', 'PAYMENT_FILE_MANAGEMENT', true, NOW(), NOW()),
('payment.file.download', 'Download payment file', 'PAYMENT_FILE_MANAGEMENT', true, NOW(), NOW()),
('payment.file.delete', 'Delete uploaded payment file', 'PAYMENT_FILE_MANAGEMENT', true, NOW(), NOW()),
('payment.file.validate', 'Validate payment file', 'PAYMENT_FILE_MANAGEMENT', true, NOW(), NOW()),
('payment.summary.read', 'View file upload summaries', 'PAYMENT_FILE_MANAGEMENT', true, NOW(), NOW()),
('payment.record.read', 'View payment records', 'PAYMENT_FILE_MANAGEMENT', true, NOW(), NOW()),
('payment.details.read', 'View detailed payment information', 'PAYMENT_FILE_MANAGEMENT', true, NOW(), NOW()),

-- ============================================================================
-- MODULE 3: PAYMENT REQUEST MANAGEMENT (9 capabilities)
-- ============================================================================
('reconciliation.request.create', 'Create payment request from file', 'PAYMENT_REQUEST_MANAGEMENT', true, NOW(), NOW()),
('reconciliation.request.read', 'View payment requests', 'PAYMENT_REQUEST_MANAGEMENT', true, NOW(), NOW()),
('reconciliation.request.update', 'Update request details', 'PAYMENT_REQUEST_MANAGEMENT', true, NOW(), NOW()),
('reconciliation.request.delete', 'Delete payment request', 'PAYMENT_REQUEST_MANAGEMENT', true, NOW(), NOW()),
('reconciliation.request.submit', 'Submit request to employer', 'PAYMENT_REQUEST_MANAGEMENT', true, NOW(), NOW()),
('reconciliation.request.track', 'Track request status and workflow', 'PAYMENT_REQUEST_MANAGEMENT', true, NOW(), NOW()),
('reconciliation.request.validate', 'Validate payment request (Employer)', 'PAYMENT_REQUEST_MANAGEMENT', true, NOW(), NOW()),
('reconciliation.payment.approve', 'Approve request (Employer/Board)', 'PAYMENT_REQUEST_MANAGEMENT', true, NOW(), NOW()),
('reconciliation.payment.reject', 'Reject request with reason', 'PAYMENT_REQUEST_MANAGEMENT', true, NOW(), NOW()),

-- ============================================================================
-- MODULE 4: WORKER OPERATIONS (6 capabilities)
-- ============================================================================
('worker.data.upload', 'Upload worker payment data', 'WORKER_OPERATIONS', true, NOW(), NOW()),
('worker.data.read', 'View worker payment data', 'WORKER_OPERATIONS', true, NOW(), NOW()),
('worker.request.create', 'Create payment request as worker', 'WORKER_OPERATIONS', true, NOW(), NOW()),
('worker.request.submit', 'Submit request to employer', 'WORKER_OPERATIONS', true, NOW(), NOW()),
('worker.status.read', 'View request status', 'WORKER_OPERATIONS', true, NOW(), NOW()),
('worker.receipt.send', 'Send receipt to employer', 'WORKER_OPERATIONS', true, NOW(), NOW()),

-- ============================================================================
-- MODULE 5: EMPLOYER OPERATIONS (5 capabilities)
-- ============================================================================
('employer.request.read', 'View worker payment requests', 'EMPLOYER_OPERATIONS', true, NOW(), NOW()),
('employer.request.validate', 'Validate payment requests', 'EMPLOYER_OPERATIONS', true, NOW(), NOW()),
('employer.payment.approve', 'Approve requests and send to board', 'EMPLOYER_OPERATIONS', true, NOW(), NOW()),
('employer.payment.reject', 'Reject requests', 'EMPLOYER_OPERATIONS', true, NOW(), NOW()),
('employer.receipt.read', 'View payment receipts', 'EMPLOYER_OPERATIONS', true, NOW(), NOW()),

-- ============================================================================
-- MODULE 6: BOARD OPERATIONS (7 capabilities)
-- ============================================================================
('board.request.read', 'View all payment requests', 'BOARD_OPERATIONS', true, NOW(), NOW()),
('board.payment.reconcile', 'Perform reconciliation', 'BOARD_OPERATIONS', true, NOW(), NOW()),
('board.decision.vote', 'Vote on board decisions', 'BOARD_OPERATIONS', true, NOW(), NOW()),
('board.payment.approve', 'Give final approval', 'BOARD_OPERATIONS', true, NOW(), NOW()),
('board.payment.reject', 'Reject at board level', 'BOARD_OPERATIONS', true, NOW(), NOW()),
('board.receipt.read', 'View board receipts', 'BOARD_OPERATIONS', true, NOW(), NOW()),
('board.receipt.process', 'Process board receipt', 'BOARD_OPERATIONS', true, NOW(), NOW()),

-- ============================================================================
-- MODULE 7: RBAC - ROLE MANAGEMENT (6 capabilities)
-- ============================================================================
('rbac.role.create', 'Create new role', 'RBAC_ROLE_MANAGEMENT', true, NOW(), NOW()),
('rbac.role.read', 'View roles and permissions', 'RBAC_ROLE_MANAGEMENT', true, NOW(), NOW()),
('rbac.role.update', 'Edit role details', 'RBAC_ROLE_MANAGEMENT', true, NOW(), NOW()),
('rbac.role.delete', 'Delete role', 'RBAC_ROLE_MANAGEMENT', true, NOW(), NOW()),
('rbac.role.assign', 'Assign role to user', 'RBAC_ROLE_MANAGEMENT', true, NOW(), NOW()),
('rbac.role.revoke', 'Revoke role from user', 'RBAC_ROLE_MANAGEMENT', true, NOW(), NOW()),

-- ============================================================================
-- MODULE 8: RBAC - POLICY MANAGEMENT (7 capabilities)
-- ============================================================================
('rbac.policy.create', 'Create new security policy', 'RBAC_POLICY_MANAGEMENT', true, NOW(), NOW()),
('rbac.policy.read', 'View policies', 'RBAC_POLICY_MANAGEMENT', true, NOW(), NOW()),
('rbac.policy.update', 'Edit policy details', 'RBAC_POLICY_MANAGEMENT', true, NOW(), NOW()),
('rbac.policy.delete', 'Delete policy', 'RBAC_POLICY_MANAGEMENT', true, NOW(), NOW()),
('rbac.policy.toggle', 'Toggle policy active status', 'RBAC_POLICY_MANAGEMENT', true, NOW(), NOW()),
('rbac.policy.link-capability', 'Link capability to policy', 'RBAC_POLICY_MANAGEMENT', true, NOW(), NOW()),
('rbac.policy.unlink-capability', 'Remove capability from policy', 'RBAC_POLICY_MANAGEMENT', true, NOW(), NOW()),

-- ============================================================================
-- MODULE 9: RBAC - CAPABILITY MANAGEMENT (6 capabilities)
-- ============================================================================
('rbac.capability.create', 'Create new capability', 'RBAC_CAPABILITY_MANAGEMENT', true, NOW(), NOW()),
('rbac.capability.read', 'View capabilities', 'RBAC_CAPABILITY_MANAGEMENT', true, NOW(), NOW()),
('rbac.capability.update', 'Edit capability details', 'RBAC_CAPABILITY_MANAGEMENT', true, NOW(), NOW()),
('rbac.capability.delete', 'Delete capability', 'RBAC_CAPABILITY_MANAGEMENT', true, NOW(), NOW()),
('rbac.capability.toggle', 'Toggle capability active status', 'RBAC_CAPABILITY_MANAGEMENT', true, NOW(), NOW()),
('rbac.capability.read-matrix', 'View capability matrix', 'RBAC_CAPABILITY_MANAGEMENT', true, NOW(), NOW()),

-- ============================================================================
-- MODULE 10: API ENDPOINT MANAGEMENT (7 capabilities)
-- ============================================================================
('rbac.endpoint.create', 'Create new API endpoint', 'RBAC_ENDPOINT_MANAGEMENT', true, NOW(), NOW()),
('rbac.endpoint.read', 'View API endpoints', 'RBAC_ENDPOINT_MANAGEMENT', true, NOW(), NOW()),
('rbac.endpoint.update', 'Edit endpoint details', 'RBAC_ENDPOINT_MANAGEMENT', true, NOW(), NOW()),
('rbac.endpoint.delete', 'Delete API endpoint', 'RBAC_ENDPOINT_MANAGEMENT', true, NOW(), NOW()),
('rbac.endpoint.toggle', 'Toggle endpoint active status', 'RBAC_ENDPOINT_MANAGEMENT', true, NOW(), NOW()),
('rbac.endpoint.link-policy', 'Link policy to endpoint', 'RBAC_ENDPOINT_MANAGEMENT', true, NOW(), NOW()),
('rbac.endpoint.unlink-policy', 'Unlink policy from endpoint', 'RBAC_ENDPOINT_MANAGEMENT', true, NOW(), NOW()),

-- ============================================================================
-- MODULE 11: UI PAGE MANAGEMENT (8 capabilities)
-- ============================================================================
('ui.page.create', 'Create new UI page', 'UI_PAGE_MANAGEMENT', true, NOW(), NOW()),
('ui.page.read', 'View UI pages', 'UI_PAGE_MANAGEMENT', true, NOW(), NOW()),
('ui.page.update', 'Edit page details', 'UI_PAGE_MANAGEMENT', true, NOW(), NOW()),
('ui.page.delete', 'Delete UI page', 'UI_PAGE_MANAGEMENT', true, NOW(), NOW()),
('ui.page.toggle', 'Toggle page active status', 'UI_PAGE_MANAGEMENT', true, NOW(), NOW()),
('ui.page.reorder', 'Reorder UI pages', 'UI_PAGE_MANAGEMENT', true, NOW(), NOW()),
('ui.page.read-children', 'Read child pages', 'UI_PAGE_MANAGEMENT', true, NOW(), NOW()),
('ui.page.manage-hierarchy', 'Manage page hierarchy', 'UI_PAGE_MANAGEMENT', true, NOW(), NOW()),

-- ============================================================================
-- MODULE 12: PAGE ACTION MANAGEMENT (7 capabilities)
-- ============================================================================
('ui.action.create', 'Create new page action', 'UI_ACTION_MANAGEMENT', true, NOW(), NOW()),
('ui.action.read', 'View page actions', 'UI_ACTION_MANAGEMENT', true, NOW(), NOW()),
('ui.action.update', 'Edit action details', 'UI_ACTION_MANAGEMENT', true, NOW(), NOW()),
('ui.action.delete', 'Delete page action', 'UI_ACTION_MANAGEMENT', true, NOW(), NOW()),
('ui.action.toggle', 'Toggle action active status', 'UI_ACTION_MANAGEMENT', true, NOW(), NOW()),
('ui.action.reorder', 'Reorder page actions', 'UI_ACTION_MANAGEMENT', true, NOW(), NOW()),
('ui.action.read-by-page', 'Read actions by page', 'UI_ACTION_MANAGEMENT', true, NOW(), NOW()),

-- ============================================================================
-- MODULE 13: SYSTEM & REPORTING (8 capabilities)
-- ============================================================================
('system.ingestion.trigger-mt940', 'Trigger MT940 file ingestion', 'SYSTEM_OPERATIONS', true, NOW(), NOW()),
('system.ingestion.trigger-van', 'Trigger VAN file ingestion', 'SYSTEM_OPERATIONS', true, NOW(), NOW()),
('system.audit.read', 'View audit logs', 'SYSTEM_OPERATIONS', true, NOW(), NOW()),
('system.audit.filter', 'Filter audit logs by criteria', 'SYSTEM_OPERATIONS', true, NOW(), NOW()),
('system.audit.export', 'Export audit logs', 'SYSTEM_OPERATIONS', true, NOW(), NOW()),
('system.settings.read', 'View system settings', 'SYSTEM_OPERATIONS', true, NOW(), NOW()),
('system.settings.update', 'Update system settings', 'SYSTEM_OPERATIONS', true, NOW(), NOW()),
('system.ingestion.read-status', 'View file ingestion status', 'SYSTEM_OPERATIONS', true, NOW(), NOW());

-- Verify creation
SELECT 
  module,
  COUNT(*) as capability_count,
  COUNT(CASE WHEN is_active THEN 1 END) as active_count
FROM capabilities
GROUP BY module
ORDER BY module;

-- Total count
SELECT COUNT(*) as total_capabilities FROM capabilities WHERE is_active = true;

COMMIT;
