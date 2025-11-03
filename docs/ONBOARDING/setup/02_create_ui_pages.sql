-- ============================================================================
-- ONBOARDING PHASE 02: CREATE UI PAGES
-- ============================================================================
-- Purpose: Seed the minimal navigation tree required for bootstrap operations.
-- Pages are referenced by their `page_id` slugs in later phases.
-- ============================================================================

SET search_path TO auth;
\set ON_ERROR_STOP on

BEGIN;

-- ============================================================================
-- BACKUP EXISTING DATA (with date suffix: 251201)
-- ============================================================================
CREATE TABLE IF NOT EXISTS page_actions_251201 (LIKE page_actions INCLUDING ALL);
DELETE FROM page_actions_251201;
INSERT INTO page_actions_251201 SELECT * FROM page_actions;

CREATE TABLE IF NOT EXISTS ui_pages_251201 (LIKE ui_pages INCLUDING ALL);
DELETE FROM ui_pages_251201;
INSERT INTO ui_pages_251201 SELECT * FROM ui_pages;

-- ============================================================================
-- RESET UI PAGES
-- ============================================================================
DELETE FROM page_actions;
DELETE FROM ui_pages;
ALTER SEQUENCE ui_pages_id_seq RESTART WITH 1;

-- Root level pages
INSERT INTO ui_pages (page_id, label, route, icon, module, parent_id, display_order, is_menu_item, is_active, required_capability)
VALUES
('dashboard', 'Dashboard', '/dashboard', 'home', 'CORE', NULL, 1, true, true, NULL);

INSERT INTO ui_pages (page_id, label, route, icon, module, parent_id, display_order, is_menu_item, is_active, required_capability)
VALUES
('admin', 'Administration', '/admin', 'settings', 'ADMIN', NULL, 2, true, true, NULL);

-- Administration children
WITH admin_page AS (
  SELECT id FROM ui_pages WHERE page_id = 'admin'
)
INSERT INTO ui_pages (page_id, label, route, icon, module, parent_id, display_order, is_menu_item, is_active, required_capability)
SELECT child.page_id,
       child.label,
       child.route,
       child.icon,
       child.module,
       admin_page.id,
       child.display_order,
       true,
       true,
       child.required_capability
FROM admin_page
CROSS JOIN (
  VALUES
    ('user-mgmt',       'User Management',   '/admin/users',        'users',   'ADMIN', 1, 'user.account.read'),
    ('role-mgmt',       'Role Management',   '/admin/roles',        'shield',  'ADMIN', 2, 'rbac.role.read'),
    ('policy-mgmt',     'Policy Management', '/admin/policies',     'lock',    'ADMIN', 3, 'rbac.policy.read'),
    ('capability-mgmt', 'Capability Catalog','/admin/capabilities', 'key',     'ADMIN', 4, 'rbac.capability.read'),
    ('endpoint-mgmt',   'Endpoint Catalog',  '/admin/endpoints',    'link',    'ADMIN', 5, 'rbac.endpoint.read'),
    ('system-ops',      'System Operations', '/admin/system',       'tool',    'ADMIN', 6, 'system.settings.read'),
    ('ui-assets',       'UI Assets',         '/admin/ui',           'layout',  'ADMIN', 7, NULL)
) AS child(page_id, label, route, icon, module, display_order, required_capability);

-- UI Assets children
WITH assets_page AS (
  SELECT id FROM ui_pages WHERE page_id = 'ui-assets'
)
INSERT INTO ui_pages (page_id, label, route, icon, module, parent_id, display_order, is_menu_item, is_active, required_capability)
SELECT child.page_id,
       child.label,
       child.route,
       child.icon,
       child.module,
       assets_page.id,
       child.display_order,
       true,
       true,
       child.required_capability
FROM assets_page
CROSS JOIN (
  VALUES
    ('ui-pages',   'UI Pages',   '/admin/ui/pages',   'layers',        'ADMIN', 1, 'ui.page.read'),
    ('ui-actions', 'UI Actions', '/admin/ui/actions', 'mouse-pointer', 'ADMIN', 2, 'ui.action.read')
) AS child(page_id, label, route, icon, module, display_order, required_capability);

-- ============================================================================
-- VERIFICATION
-- ============================================================================
DO $$
DECLARE
  v_total INTEGER;
BEGIN
  SELECT COUNT(*) INTO v_total FROM ui_pages;
  IF v_total <> 11 THEN
    RAISE EXCEPTION 'Expected 11 bootstrap UI pages, found %', v_total;
  END IF;
  RAISE NOTICE 'Successfully created % UI pages for bootstrap navigation.', v_total;
END $$;

SELECT id, page_id, label, route, parent_id, display_order
FROM ui_pages
ORDER BY parent_id NULLS FIRST, display_order, label;

COMMIT;

-- ============================================================================
-- POST-SCRIPT SUMMARY
-- ============================================================================
-- Navigation structure:
--   dashboard           – user landing page
--   admin               – parent menu for administration
--     ├─ user-mgmt
--     ├─ role-mgmt
--     ├─ policy-mgmt
--     ├─ capability-mgmt
--     ├─ endpoint-mgmt
--     ├─ system-ops
--     └─ ui-assets
--         ├─ ui-pages
--         └─ ui-actions
--
-- Next Step: Run 03_create_capabilities.sql to seed capability catalog.
-- ============================================================================
