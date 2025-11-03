-- ============================================================================
-- ONBOARDING PHASE 03: CREATE CAPABILITIES (56 Total)
-- ============================================================================
-- Purpose: Seed the atomic capabilities required for bootstrap roles/policies.
-- Naming convention: <domain>.<subject>.<action>
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

CREATE TABLE IF NOT EXISTS capabilities_251201 (LIKE capabilities INCLUDING ALL);
DELETE FROM capabilities_251201;
INSERT INTO capabilities_251201 SELECT * FROM capabilities;

-- ============================================================================
-- RESET CAPABILITY CATALOG
-- ============================================================================
DELETE FROM policy_capabilities WHERE capability_id IN (SELECT id FROM capabilities);
DELETE FROM capabilities;
ALTER SEQUENCE capabilities_id_seq RESTART WITH 1;

-- ============================================================================
-- INSERT CAPABILITIES
-- ============================================================================
INSERT INTO capabilities (name, description, module, action, resource, is_active, created_at, updated_at) VALUES
-- Bootstrap system
('authorization.api.access',  'Access authorization APIs',                'BOOTSTRAP',        'ACCESS', 'AUTH_API',            true, NOW(), NOW()),
('service.catalog.read',      'Read service catalog metadata',           'BOOTSTRAP',        'READ',   'SERVICE_CATALOG',     true, NOW(), NOW()),

-- User management
('user.account.create',       'Create new user accounts',                 'USER_MANAGEMENT',  'CREATE', 'USER',                true, NOW(), NOW()),
('user.account.read',         'View user accounts',                       'USER_MANAGEMENT',  'READ',   'USER',                true, NOW(), NOW()),
('user.account.update',       'Update user account details',              'USER_MANAGEMENT',  'UPDATE', 'USER',                true, NOW(), NOW()),
('user.account.delete',       'Delete user accounts',                     'USER_MANAGEMENT',  'DELETE', 'USER',                true, NOW(), NOW()),
('user.status.toggle',        'Enable or disable user accounts',          'USER_MANAGEMENT',  'TOGGLE', 'USER_STATUS',         true, NOW(), NOW()),

-- RBAC role management
('rbac.role.create',          'Create roles',                             'RBAC_ROLE',        'CREATE', 'ROLE',                true, NOW(), NOW()),
('rbac.role.read',            'Read roles and assignments',               'RBAC_ROLE',        'READ',   'ROLE',                true, NOW(), NOW()),
('rbac.role.update',          'Update role definitions',                  'RBAC_ROLE',        'UPDATE', 'ROLE',                true, NOW(), NOW()),
('rbac.role.delete',          'Delete roles',                             'RBAC_ROLE',        'DELETE', 'ROLE',                true, NOW(), NOW()),
('rbac.role.assign',          'Assign roles to users',                    'RBAC_ROLE',        'ASSIGN', 'ROLE_ASSIGNMENT',     true, NOW(), NOW()),
('rbac.role.revoke',          'Revoke roles from users',                  'RBAC_ROLE',        'REVOKE', 'ROLE_ASSIGNMENT',     true, NOW(), NOW()),

-- RBAC policy management
('rbac.policy.create',        'Create policies',                          'RBAC_POLICY',      'CREATE', 'POLICY',              true, NOW(), NOW()),
('rbac.policy.read',          'Read policies',                            'RBAC_POLICY',      'READ',   'POLICY',              true, NOW(), NOW()),
('rbac.policy.update',        'Update policies',                          'RBAC_POLICY',      'UPDATE', 'POLICY',              true, NOW(), NOW()),
('rbac.policy.delete',        'Delete policies',                          'RBAC_POLICY',      'DELETE', 'POLICY',              true, NOW(), NOW()),
('rbac.policy.toggle',        'Toggle policy active status',              'RBAC_POLICY',      'TOGGLE', 'POLICY',              true, NOW(), NOW()),
('rbac.policy.link-capability','Link capability to policy',               'RBAC_POLICY',      'LINK',   'POLICY_CAPABILITY',   true, NOW(), NOW()),
('rbac.policy.unlink-capability','Unlink capability from policy',         'RBAC_POLICY',      'UNLINK', 'POLICY_CAPABILITY',   true, NOW(), NOW()),

-- RBAC capability management
('rbac.capability.create',    'Create capabilities',                      'RBAC_CAPABILITY',  'CREATE', 'CAPABILITY',          true, NOW(), NOW()),
('rbac.capability.read',      'Read capabilities',                        'RBAC_CAPABILITY',  'READ',   'CAPABILITY',          true, NOW(), NOW()),
('rbac.capability.update',    'Update capabilities',                      'RBAC_CAPABILITY',  'UPDATE', 'CAPABILITY',          true, NOW(), NOW()),
('rbac.capability.delete',    'Delete capabilities',                      'RBAC_CAPABILITY',  'DELETE', 'CAPABILITY',          true, NOW(), NOW()),
('rbac.capability.toggle',    'Toggle capability active status',          'RBAC_CAPABILITY',  'TOGGLE', 'CAPABILITY',          true, NOW(), NOW()),
('rbac.capability.read-matrix','View capability matrix',                  'RBAC_CAPABILITY',  'READ',   'CAPABILITY_MATRIX',   true, NOW(), NOW()),

-- RBAC endpoint management
('rbac.endpoint.create',      'Register endpoints',                       'RBAC_ENDPOINT',    'CREATE', 'ENDPOINT',            true, NOW(), NOW()),
('rbac.endpoint.read',        'Read endpoint catalog',                    'RBAC_ENDPOINT',    'READ',   'ENDPOINT',            true, NOW(), NOW()),
('rbac.endpoint.update',      'Update endpoints',                         'RBAC_ENDPOINT',    'UPDATE', 'ENDPOINT',            true, NOW(), NOW()),
('rbac.endpoint.delete',      'Delete endpoints',                         'RBAC_ENDPOINT',    'DELETE', 'ENDPOINT',            true, NOW(), NOW()),
('rbac.endpoint.toggle',      'Toggle endpoint active status',            'RBAC_ENDPOINT',    'TOGGLE', 'ENDPOINT',            true, NOW(), NOW()),
('rbac.endpoint.link-policy', 'Attach policies to endpoints',             'RBAC_ENDPOINT',    'LINK',   'ENDPOINT_POLICY',     true, NOW(), NOW()),
('rbac.endpoint.unlink-policy','Detach policies from endpoints',          'RBAC_ENDPOINT',    'UNLINK', 'ENDPOINT_POLICY',     true, NOW(), NOW()),

-- UI page management
('ui.page.create',            'Create UI pages',                          'UI_PAGE',          'CREATE', 'UI_PAGE',             true, NOW(), NOW()),
('ui.page.read',              'Read UI pages',                            'UI_PAGE',          'READ',   'UI_PAGE',             true, NOW(), NOW()),
('ui.page.update',            'Update UI pages',                          'UI_PAGE',          'UPDATE', 'UI_PAGE',             true, NOW(), NOW()),
('ui.page.delete',            'Delete UI pages',                          'UI_PAGE',          'DELETE', 'UI_PAGE',             true, NOW(), NOW()),
('ui.page.toggle',            'Toggle UI page active status',             'UI_PAGE',          'TOGGLE', 'UI_PAGE',             true, NOW(), NOW()),
('ui.page.reorder',           'Reorder UI pages',                         'UI_PAGE',          'REORDER','UI_PAGE',             true, NOW(), NOW()),
('ui.page.read-children',     'Read child pages',                         'UI_PAGE',          'READ',   'UI_PAGE_CHILD',       true, NOW(), NOW()),
('ui.page.manage-hierarchy',  'Manage UI page hierarchy',                 'UI_PAGE',          'MANAGE', 'UI_PAGE_TREE',        true, NOW(), NOW()),

-- UI action management
('ui.action.create',          'Create UI actions',                        'UI_ACTION',        'CREATE', 'UI_ACTION',           true, NOW(), NOW()),
('ui.action.read',            'Read UI actions',                          'UI_ACTION',        'READ',   'UI_ACTION',           true, NOW(), NOW()),
('ui.action.update',          'Update UI actions',                        'UI_ACTION',        'UPDATE', 'UI_ACTION',           true, NOW(), NOW()),
('ui.action.delete',          'Delete UI actions',                        'UI_ACTION',        'DELETE', 'UI_ACTION',           true, NOW(), NOW()),
('ui.action.toggle',          'Toggle UI action active status',           'UI_ACTION',        'TOGGLE', 'UI_ACTION',           true, NOW(), NOW()),
('ui.action.reorder',         'Reorder UI actions',                       'UI_ACTION',        'REORDER','UI_ACTION',           true, NOW(), NOW()),
('ui.action.read-by-page',    'Read actions by page',                     'UI_ACTION',        'READ',   'UI_ACTION_PAGE',      true, NOW(), NOW()),

-- System operations
('system.audit.read',         'Read audit logs',                          'SYSTEM',           'READ',   'AUDIT_LOG',           true, NOW(), NOW()),
('system.audit.filter',       'Filter audit logs',                        'SYSTEM',           'FILTER', 'AUDIT_LOG',           true, NOW(), NOW()),
('system.audit.export',       'Export audit logs',                        'SYSTEM',           'EXPORT', 'AUDIT_LOG',           true, NOW(), NOW()),
('system.settings.read',      'Read system settings',                     'SYSTEM',           'READ',   'SYSTEM_SETTINGS',     true, NOW(), NOW()),
('system.settings.update',    'Update system settings',                   'SYSTEM',           'UPDATE', 'SYSTEM_SETTINGS',     true, NOW(), NOW()),
('system.ingestion.read-status','Read ingestion status',                  'SYSTEM',           'READ',   'INGESTION_STATUS',    true, NOW(), NOW()),
('system.ingestion.trigger-mt940','Trigger MT940 ingestion',              'SYSTEM',           'TRIGGER','INGESTION_MT940',     true, NOW(), NOW()),
('system.ingestion.trigger-van','Trigger VAN ingestion',                  'SYSTEM',           'TRIGGER','INGESTION_VAN',       true, NOW(), NOW());

-- ============================================================================
-- VERIFICATION
-- ============================================================================
DO $$
DECLARE
  v_total INTEGER;
BEGIN
  SELECT COUNT(*) INTO v_total FROM capabilities WHERE is_active = true;
  IF v_total <> 56 THEN
    RAISE EXCEPTION 'Expected 56 active capabilities, found %', v_total;
  END IF;
  RAISE NOTICE 'Successfully created % bootstrap capabilities.', v_total;
END $$;

SELECT name, module, action, resource
FROM capabilities
ORDER BY name;

COMMIT;

-- ============================================================================
-- POST-SCRIPT SUMMARY
-- ============================================================================
-- Capabilities grouped by module: user management, RBAC (roles/policies/capabilities/endpoints),
-- UI (pages/actions), system operations, and baseline bootstrap access.
--
-- Next Step: Run 04_create_policies.sql.
-- ============================================================================
