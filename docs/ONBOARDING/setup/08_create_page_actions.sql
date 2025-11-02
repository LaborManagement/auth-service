-- ============================================================================
-- ONBOARDING PHASE 02: CREATE PAGE ACTIONS
-- ============================================================================
-- Purpose: Link all UI pages to capabilities via page_actions
-- All users must have corresponding page_actions for pages to appear
-- ============================================================================

SET search_path TO auth;
\set ON_ERROR_STOP on

BEGIN;

-- Delete existing page actions to start fresh
DELETE FROM page_actions;

-- ============================================================================
-- DASHBOARD PAGE ACTIONS (Page 1)
-- ============================================================================
-- All roles need at least one action on Dashboard

-- BASIC_USER: View Dashboard
INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
SELECT 'View Dashboard', 'read', 1, id, 1, true, NOW(), NOW(), 'eye', 'default'
FROM capabilities WHERE name = 'user.account.read' LIMIT 1;

-- BOARD: View Board Requests
INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
SELECT 'View Board Requests', 'read', 1, id, 2, true, NOW(), NOW(), 'list', 'info'
FROM capabilities WHERE name = 'board.request.read' LIMIT 1;

-- BOARD: Approve Payments
INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
SELECT 'Approve Payments', 'approve', 1, id, 3, true, NOW(), NOW(), 'check-circle', 'success'
FROM capabilities WHERE name = 'board.payment.approve' LIMIT 1;

-- ============================================================================
-- ADMINISTRATION PAGE ACTIONS (Page 2 - parent)
-- ============================================================================

-- ADMIN_TECH: Access Admin
INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
SELECT 'Access Administration', 'read', 2, id, 1, true, NOW(), NOW(), 'settings', 'default'
FROM capabilities WHERE name = 'user.account.read' LIMIT 1;

-- ============================================================================
-- USER MANAGEMENT PAGE ACTIONS (Page 4)
-- ============================================================================

-- Create User
INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
SELECT 'Create User', 'create', 4, id, 1, true, NOW(), NOW(), 'plus', 'success'
FROM capabilities WHERE name = 'user.account.create' LIMIT 1;

-- View Users
INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
SELECT 'View Users', 'read', 4, id, 2, true, NOW(), NOW(), 'eye', 'default'
FROM capabilities WHERE name = 'user.account.read' LIMIT 1;

-- Edit User
INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
SELECT 'Edit User', 'update', 4, id, 3, true, NOW(), NOW(), 'edit', 'info'
FROM capabilities WHERE name = 'user.account.update' LIMIT 1;

-- Delete User
INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
SELECT 'Delete User', 'delete', 4, id, 4, true, NOW(), NOW(), 'trash', 'danger'
FROM capabilities WHERE name = 'user.account.delete' LIMIT 1;

-- ============================================================================
-- ROLE MANAGEMENT PAGE ACTIONS (Page 5)
-- ============================================================================

-- View Roles
INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
SELECT 'View Roles', 'read', 5, id, 1, true, NOW(), NOW(), 'eye', 'default'
FROM capabilities WHERE name = 'rbac.role.read' LIMIT 1;

-- Manage Roles
INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
SELECT 'Manage Roles', 'manage', 5, id, 2, true, NOW(), NOW(), 'edit', 'warning'
FROM capabilities WHERE name = 'rbac.role.assign' LIMIT 1;

-- ============================================================================
-- SYSTEM PAGE ACTIONS (Page 3 - parent)
-- ============================================================================

INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
SELECT 'Access System', 'read', 3, id, 1, true, NOW(), NOW(), 'tool', 'default'
FROM capabilities WHERE name = 'rbac.endpoint.read' LIMIT 1;

-- ============================================================================
-- CAPABILITIES PAGE ACTIONS (Page 6)
-- ============================================================================

INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
SELECT 'View Capabilities', 'read', 6, id, 1, true, NOW(), NOW(), 'eye', 'default'
FROM capabilities WHERE name = 'rbac.endpoint.read' LIMIT 1;

-- ============================================================================
-- POLICIES PAGE ACTIONS (Page 7)
-- ============================================================================

INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
SELECT 'View Policies', 'read', 7, id, 1, true, NOW(), NOW(), 'eye', 'default'
FROM capabilities WHERE name = 'rbac.endpoint.read' LIMIT 1;

-- ============================================================================
-- ENDPOINTS PAGE ACTIONS (Page 8)
-- ============================================================================

INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
SELECT 'View Endpoints', 'read', 8, id, 1, true, NOW(), NOW(), 'eye', 'default'
FROM capabilities WHERE name = 'rbac.endpoint.read' LIMIT 1;

-- ============================================================================
-- UI PAGES PAGE ACTIONS (Page 9)
-- ============================================================================

INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
SELECT 'View UI Pages', 'read', 9, id, 1, true, NOW(), NOW(), 'eye', 'default'
FROM capabilities WHERE name = 'ui.page.read' LIMIT 1;

-- ============================================================================
-- PAGE ACTIONS PAGE ACTIONS (Page 10)
-- ============================================================================

INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
SELECT 'View Page Actions', 'read', 10, id, 1, true, NOW(), NOW(), 'eye', 'default'
FROM capabilities WHERE name = 'ui.action.read' LIMIT 1;

-- ============================================================================
-- SYSTEM LOGS PAGE ACTIONS (Page 11)
-- ============================================================================

INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
SELECT 'View System Logs', 'read', 11, id, 1, true, NOW(), NOW(), 'eye', 'default'
FROM capabilities WHERE name = 'rbac.endpoint.read' LIMIT 1;

-- ============================================================================
-- BACKUP & RESTORE PAGE ACTIONS (Page 12)
-- ============================================================================

INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
SELECT 'Manage Backups', 'manage', 12, id, 1, true, NOW(), NOW(), 'save', 'warning'
FROM capabilities WHERE name = 'rbac.endpoint.read' LIMIT 1;

-- ============================================================================
-- WORKER MANAGEMENT PAGE ACTIONS (Page 13 - parent)
-- ============================================================================

INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
SELECT 'Access Worker Management', 'read', 13, id, 1, true, NOW(), NOW(), 'briefcase', 'default'
FROM capabilities WHERE name = 'worker.data.read' LIMIT 1;

-- ============================================================================
-- WORKER UPLOADED DATA PAGE ACTIONS (Page 14)
-- ============================================================================

INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
SELECT 'View Worker Data', 'read', 14, id, 1, true, NOW(), NOW(), 'eye', 'default'
FROM capabilities WHERE name = 'worker.data.read' LIMIT 1;

INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
SELECT 'Upload Data', 'upload', 14, id, 2, true, NOW(), NOW(), 'upload', 'info'
FROM capabilities WHERE name = 'worker.data.upload' LIMIT 1;

-- ============================================================================
-- UPLOAD FILE SUMMARY PAGE ACTIONS (Page 15)
-- ============================================================================

INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
SELECT 'View Summary', 'read', 15, id, 1, true, NOW(), NOW(), 'eye', 'default'
FROM capabilities WHERE name = 'payment.summary.read' LIMIT 1;

COMMIT;

-- Verification
SELECT 
  pa.id,
  pa.label,
  up.label as page,
  c.name as capability,
  pa.display_order
FROM page_actions pa
JOIN ui_pages up ON pa.page_id = up.id
JOIN capabilities c ON pa.capability_id = c.id
ORDER BY up.id, pa.display_order;
