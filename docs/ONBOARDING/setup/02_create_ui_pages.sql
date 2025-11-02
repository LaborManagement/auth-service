-- ============================================================================
-- ONBOARDING PHASE 01: CREATE UI PAGES
-- ============================================================================
-- Purpose: Define all UI pages with proper hierarchy and module organization
-- This is a PREREQUISITE for page_actions linking in Phase 02
-- ============================================================================

SET search_path TO auth;
\set ON_ERROR_STOP on

BEGIN;

-- Delete existing UI pages to start fresh
DELETE FROM ui_pages;

-- ============================================================================
-- UI PAGES DEFINITION (15 total pages)
-- ============================================================================
-- Structure: id, page_id, label, route, icon, module, parent_id, display_order, is_menu_item, is_active

INSERT INTO ui_pages (id, page_id, label, route, icon, module, parent_id, display_order, is_menu_item, is_active, required_capability)
VALUES
-- 1. Dashboard (root level)
(1, 'dashboard', 'Dashboard', '/dashboard', 'home', 'DASHBOARD', NULL, 1, true, true, NULL),

-- 2-5. Administration section
(2, 'admin', 'Administration', '/admin', 'settings', 'ADMIN', NULL, 2, true, true, 'USER_MANAGE'),
(4, 'user-mgmt', 'User Management', '/admin/users', 'users', 'ADMIN', 2, 2, true, true, 'USER_MANAGE'),
(5, 'role-mgmt', 'Role Management', '/admin/roles', 'shield', 'ADMIN', 2, 3, true, true, 'ROLE_MANAGE'),

-- 6-8. System Management section
(3, 'system', 'System', '/system', 'tool', 'SYSTEM', NULL, 3, true, true, 'SYSTEM_MAINTENANCE'),
(6, 'capabilities', 'Capabilities', '/admin/capabilities', 'unlock', 'SYSTEM', 3, 1, true, true, 'SYSTEM_MAINTENANCE'),
(7, 'policies', 'Policies', '/admin/policies', 'lock', 'SYSTEM', 3, 2, true, true, 'SYSTEM_MAINTENANCE'),
(8, 'endpoints', 'Endpoints', '/admin/endpoints', 'link', 'SYSTEM', 3, 3, true, true, 'SYSTEM_MAINTENANCE'),
(9, 'ui-pages', 'UI Pages', '/admin/ui-pages', 'layout', 'SYSTEM', 3, 4, true, true, 'SYSTEM_MAINTENANCE'),
(10, 'page-actions', 'Page Actions', '/admin/page-actions', 'mouse-pointer', 'SYSTEM', 3, 5, true, true, 'SYSTEM_MAINTENANCE'),
(11, 'system-logs', 'System Logs', '/admin/system-logs', 'activity', 'SYSTEM', 3, 6, true, true, 'SYSTEM_MAINTENANCE'),

-- 12. Backup & Restore
(12, 'backup-restore', 'Backup & Restore', '/admin/backup', 'save', 'ADMIN', 2, 4, true, true, 'SYSTEM_MAINTENANCE'),

-- 13-15. Reconciliation & Worker section
(13, 'worker-mgmt', 'Worker Management', '/workers', 'briefcase', 'WORKER', NULL, 4, true, true, 'WORKER_MANAGE'),
(14, 'worker-data', 'Worker Uploaded Data', '/workers/data', 'database', 'WORKER', 13, 1, true, true, 'WORKER_MANAGE'),
(15, 'file-summary', 'Upload File Summary', '/workers/summary', 'file-text', 'WORKER', 13, 2, true, true, 'WORKER_MANAGE');

COMMIT;

-- Verification
SELECT id, label, route, module, parent_id, display_order, is_active 
FROM ui_pages 
ORDER BY display_order, id;
