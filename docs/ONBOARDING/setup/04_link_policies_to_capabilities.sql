-- ============================================================================
-- PHASE 4: LINK POLICIES TO CAPABILITIES (Layer 1 - Capability-Policy)
-- ============================================================================
-- Purpose: Create policy-capability relationships granting capabilities to roles
-- Total Links: 288 (55+51+42+17+19+14+50)
-- Dependencies: Policies and Capabilities must be created first
-- ============================================================================

\set ON_ERROR_STOP on

-- Clear existing policy-capability links
TRUNCATE TABLE policy_capabilities CASCADE;

-- ============================================================================
-- PLATFORM_BOOTSTRAP_POLICY - 55 Capabilities (56%)
-- ============================================================================
-- User Management (5/5)
INSERT INTO policy_capabilities (policy_id, capability_id, created_at) VALUES
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'user.account.create'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'user.account.read'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'user.account.update'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'user.account.delete'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'user.status.toggle'), NOW()),

-- RBAC - Role Management (6/6)
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.role.create'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.role.read'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.role.update'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.role.delete'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.role.assign'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.role.revoke'), NOW()),

-- RBAC - Policy Management (7/7)
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.policy.create'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.policy.read'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.policy.update'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.policy.delete'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.policy.toggle'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.policy.link-capability'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.policy.unlink-capability'), NOW()),

-- RBAC - Capability Management (6/6)
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.capability.create'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.capability.read'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.capability.update'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.capability.delete'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.capability.toggle'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.capability.read-matrix'), NOW()),

-- API Endpoint Management (7/7)
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.endpoint.create'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.endpoint.read'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.endpoint.update'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.endpoint.delete'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.endpoint.toggle'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.endpoint.link-policy'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.endpoint.unlink-policy'), NOW()),

-- UI Page Management (8/8)
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.page.create'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.page.read'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.page.update'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.page.delete'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.page.toggle'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.page.reorder'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.page.read-children'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.page.manage-hierarchy'), NOW()),

-- Page Action Management (7/7)
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.action.create'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.action.read'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.action.update'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.action.delete'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.action.toggle'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.action.reorder'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.action.read-by-page'), NOW()),

-- System & Reporting (8/8)
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'system.ingestion.trigger-mt940'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'system.ingestion.trigger-van'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'system.audit.read'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'system.audit.filter'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'system.audit.export'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'system.settings.read'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'system.settings.update'), NOW()),
((SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'system.ingestion.read-status'), NOW());

-- ============================================================================
-- ADMIN_TECH_POLICY - 51 Capabilities (52%)
-- ============================================================================
-- User Management (5/5)
INSERT INTO policy_capabilities (policy_id, capability_id, created_at) VALUES
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'user.account.create'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'user.account.read'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'user.account.update'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'user.account.delete'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'user.status.toggle'), NOW()),

-- RBAC - Role Management (6/6)
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.role.create'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.role.read'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.role.update'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.role.delete'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.role.assign'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.role.revoke'), NOW()),

-- RBAC - Policy Management (7/7)
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.policy.create'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.policy.read'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.policy.update'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.policy.delete'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.policy.toggle'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.policy.link-capability'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.policy.unlink-capability'), NOW()),

-- RBAC - Capability Management (6/6)
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.capability.create'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.capability.read'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.capability.update'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.capability.delete'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.capability.toggle'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.capability.read-matrix'), NOW()),

-- API Endpoint Management (7/7)
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.endpoint.create'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.endpoint.read'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.endpoint.update'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.endpoint.delete'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.endpoint.toggle'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.endpoint.link-policy'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'rbac.endpoint.unlink-policy'), NOW()),

-- UI Page Management (8/8)
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.page.create'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.page.read'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.page.update'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.page.delete'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.page.toggle'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.page.reorder'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.page.read-children'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.page.manage-hierarchy'), NOW()),

-- Page Action Management (7/7)
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.action.create'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.action.read'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.action.update'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.action.delete'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.action.toggle'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.action.reorder'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'ui.action.read-by-page'), NOW()),

-- System & Reporting (4/8)
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'system.audit.read'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'system.audit.filter'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'system.audit.export'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_TECH_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'system.ingestion.read-status'), NOW());

-- ============================================================================
-- ADMIN_OPS_POLICY - 42 Capabilities (43%)
-- ============================================================================
INSERT INTO policy_capabilities (policy_id, capability_id, created_at) VALUES

-- Payment File Management (5/8)
((SELECT id FROM policies WHERE name = 'ADMIN_OPS_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.file.read'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_OPS_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.file.download'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_OPS_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.summary.read'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_OPS_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.record.read'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_OPS_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.details.read'), NOW()),

-- Payment Request Management (3/9)
((SELECT id FROM policies WHERE name = 'ADMIN_OPS_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'reconciliation.request.read'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_OPS_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'reconciliation.request.track'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_OPS_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'reconciliation.request.validate'), NOW()),

-- Worker Operations (3/6)
((SELECT id FROM policies WHERE name = 'ADMIN_OPS_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'worker.data.read'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_OPS_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'worker.status.read'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_OPS_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'worker.receipt.send'), NOW()),

-- Employer Operations (2/5)
((SELECT id FROM policies WHERE name = 'ADMIN_OPS_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'employer.request.read'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_OPS_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'employer.receipt.read'), NOW()),

-- Board Operations (2/7)
((SELECT id FROM policies WHERE name = 'ADMIN_OPS_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'board.request.read'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_OPS_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'board.receipt.read'), NOW()),

-- System & Reporting (8/8)
((SELECT id FROM policies WHERE name = 'ADMIN_OPS_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'system.ingestion.trigger-mt940'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_OPS_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'system.ingestion.trigger-van'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_OPS_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'system.audit.read'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_OPS_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'system.audit.filter'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_OPS_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'system.audit.export'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_OPS_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'system.settings.read'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_OPS_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'system.settings.update'), NOW()),
((SELECT id FROM policies WHERE name = 'ADMIN_OPS_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'system.ingestion.read-status'), NOW());

-- ============================================================================
-- BOARD_POLICY - 17 Capabilities (17%)
-- ============================================================================
INSERT INTO policy_capabilities (policy_id, capability_id, created_at) VALUES

-- Payment File Management (5/8)
((SELECT id FROM policies WHERE name = 'BOARD_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.file.read'), NOW()),
((SELECT id FROM policies WHERE name = 'BOARD_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.file.download'), NOW()),
((SELECT id FROM policies WHERE name = 'BOARD_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.summary.read'), NOW()),
((SELECT id FROM policies WHERE name = 'BOARD_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.record.read'), NOW()),
((SELECT id FROM policies WHERE name = 'BOARD_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.details.read'), NOW()),

-- Board Operations (7/7)
((SELECT id FROM policies WHERE name = 'BOARD_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'board.request.read'), NOW()),
((SELECT id FROM policies WHERE name = 'BOARD_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'board.payment.reconcile'), NOW()),
((SELECT id FROM policies WHERE name = 'BOARD_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'board.decision.vote'), NOW()),
((SELECT id FROM policies WHERE name = 'BOARD_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'board.payment.approve'), NOW()),
((SELECT id FROM policies WHERE name = 'BOARD_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'board.payment.reject'), NOW()),
((SELECT id FROM policies WHERE name = 'BOARD_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'board.receipt.read'), NOW()),
((SELECT id FROM policies WHERE name = 'BOARD_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'board.receipt.process'), NOW());

-- ============================================================================
-- EMPLOYER_POLICY - 19 Capabilities (19%)
-- ============================================================================
INSERT INTO policy_capabilities (policy_id, capability_id, created_at) VALUES

-- Payment File Management (5/8)
((SELECT id FROM policies WHERE name = 'EMPLOYER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.file.read'), NOW()),
((SELECT id FROM policies WHERE name = 'EMPLOYER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.file.download'), NOW()),
((SELECT id FROM policies WHERE name = 'EMPLOYER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.summary.read'), NOW()),
((SELECT id FROM policies WHERE name = 'EMPLOYER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.record.read'), NOW()),
((SELECT id FROM policies WHERE name = 'EMPLOYER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.details.read'), NOW()),

-- Payment Request Management (9/9)
((SELECT id FROM policies WHERE name = 'EMPLOYER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'reconciliation.request.create'), NOW()),
((SELECT id FROM policies WHERE name = 'EMPLOYER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'reconciliation.request.read'), NOW()),
((SELECT id FROM policies WHERE name = 'EMPLOYER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'reconciliation.request.update'), NOW()),
((SELECT id FROM policies WHERE name = 'EMPLOYER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'reconciliation.request.delete'), NOW()),
((SELECT id FROM policies WHERE name = 'EMPLOYER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'reconciliation.request.submit'), NOW()),
((SELECT id FROM policies WHERE name = 'EMPLOYER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'reconciliation.request.track'), NOW()),
((SELECT id FROM policies WHERE name = 'EMPLOYER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'reconciliation.request.validate'), NOW()),
((SELECT id FROM policies WHERE name = 'EMPLOYER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'reconciliation.payment.approve'), NOW()),
((SELECT id FROM policies WHERE name = 'EMPLOYER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'reconciliation.payment.reject'), NOW()),

-- Employer Operations (5/5)
((SELECT id FROM policies WHERE name = 'EMPLOYER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'employer.request.read'), NOW()),
((SELECT id FROM policies WHERE name = 'EMPLOYER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'employer.request.validate'), NOW()),
((SELECT id FROM policies WHERE name = 'EMPLOYER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'employer.payment.approve'), NOW()),
((SELECT id FROM policies WHERE name = 'EMPLOYER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'employer.payment.reject'), NOW()),
((SELECT id FROM policies WHERE name = 'EMPLOYER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'employer.receipt.read'), NOW());

-- ============================================================================
-- WORKER_POLICY - 14 Capabilities (14%)
-- ============================================================================
INSERT INTO policy_capabilities (policy_id, capability_id, created_at) VALUES

-- Worker Operations (6/6)
((SELECT id FROM policies WHERE name = 'WORKER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'worker.data.upload'), NOW()),
((SELECT id FROM policies WHERE name = 'WORKER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'worker.data.read'), NOW()),
((SELECT id FROM policies WHERE name = 'WORKER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'worker.request.create'), NOW()),
((SELECT id FROM policies WHERE name = 'WORKER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'worker.request.submit'), NOW()),
((SELECT id FROM policies WHERE name = 'WORKER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'worker.status.read'), NOW()),
((SELECT id FROM policies WHERE name = 'WORKER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'worker.receipt.send'), NOW()),

-- Payment File Management (5/8)
((SELECT id FROM policies WHERE name = 'WORKER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.file.read'), NOW()),
((SELECT id FROM policies WHERE name = 'WORKER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.file.download'), NOW()),
((SELECT id FROM policies WHERE name = 'WORKER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.summary.read'), NOW()),
((SELECT id FROM policies WHERE name = 'WORKER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.record.read'), NOW()),
((SELECT id FROM policies WHERE name = 'WORKER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.details.read'), NOW()),

-- Payment Request Management (3/9)
((SELECT id FROM policies WHERE name = 'WORKER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'reconciliation.request.read'), NOW()),
((SELECT id FROM policies WHERE name = 'WORKER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'reconciliation.request.track'), NOW()),
((SELECT id FROM policies WHERE name = 'WORKER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'reconciliation.request.submit'), NOW());

-- ============================================================================
-- TEST_USER_POLICY - 50 Capabilities (51%)
-- ============================================================================
INSERT INTO policy_capabilities (policy_id, capability_id, created_at) VALUES

-- User Management (5/5)
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'user.account.create'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'user.account.read'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'user.account.update'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'user.account.delete'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'user.status.toggle'), NOW()),

-- Payment File Management (8/8)
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.file.upload'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.file.read'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.file.download'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.file.delete'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.file.validate'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.summary.read'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.record.read'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'payment.details.read'), NOW()),

-- Payment Request Management (9/9)
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'reconciliation.request.create'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'reconciliation.request.read'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'reconciliation.request.update'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'reconciliation.request.delete'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'reconciliation.request.submit'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'reconciliation.request.track'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'reconciliation.request.validate'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'reconciliation.payment.approve'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'reconciliation.payment.reject'), NOW()),

-- Worker Operations (6/6)
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'worker.data.upload'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'worker.data.read'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'worker.request.create'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'worker.request.submit'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'worker.status.read'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'worker.receipt.send'), NOW()),

-- Employer Operations (5/5)
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'employer.request.read'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'employer.request.validate'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'employer.payment.approve'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'employer.payment.reject'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'employer.receipt.read'), NOW()),

-- Board Operations (6/7) - excludes vote
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'board.request.read'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'board.payment.reconcile'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'board.payment.approve'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'board.payment.reject'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'board.receipt.read'), NOW()),
((SELECT id FROM policies WHERE name = 'TEST_USER_POLICY'), 
 (SELECT id FROM capabilities WHERE name = 'board.receipt.process'), NOW());

-- Verify creation
SELECT 
  p.name,
  COUNT(pc.capability_id) as capability_count
FROM policies p
LEFT JOIN policy_capabilities pc ON p.id = pc.policy_id
GROUP BY p.id, p.name
ORDER BY capability_count DESC;

-- Verify total
SELECT COUNT(*) as total_links FROM policy_capabilities;

COMMIT;
