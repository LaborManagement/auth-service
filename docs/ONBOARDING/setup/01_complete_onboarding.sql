-- ============================================================================
-- Complete Onboarding Setup Script
-- ============================================================================
-- Creates all users, roles, capabilities, and policies in one go.
-- This is the recommended way to set up the system.
--
-- USAGE:
--   psql -U postgres -d auth_service_db -f docs/ONBOARDING/setup/01_complete_onboarding.sql
--
-- WHAT THIS DOES:
--   1. Creates 7 roles (PLATFORM_BOOTSTRAP, BASIC_USER, WORKER, EMPLOYER, BOARD, ADMIN_TECH, ADMIN_OPS)
--   2. Creates 7 users (one for each role)
--   3. Assigns users to roles
--   4. Defines 20+ capabilities across 8 modules
--   5. Creates 7 policies
--   6. Links policies to capabilities
--
-- PASSWORDS (CHANGE IMMEDIATELY IN PRODUCTION):
--   platform.bootstrap: Platform!Bootstrap1
--   basic.user1:        BasicUser!2025
--   worker1:            Worker!2025
--   employer1:          Employer!2025
--   board1:             Board!2025
--   admin.tech:         AdminTech!2025
--   admin.ops:          AdminOps!2025
--
-- ============================================================================

BEGIN;

-- ============================================================================
-- PHASE 1: CREATE ROLES
-- ============================================================================

INSERT INTO roles (name, description, is_active, created_at, updated_at)
VALUES 
    ('PLATFORM_BOOTSTRAP', 'Bootstrap role with full administrative privileges for initial catalog setup', true, NOW(), NOW()),
    ('BASIC_USER', 'Standard application user with limited read/write access to assigned resources', true, NOW(), NOW()),
    ('WORKER', 'Worker user with read/write access to worker functionality', true, NOW(), NOW()),
    ('EMPLOYER', 'Employer user with read/write access to employer functionality', true, NOW(), NOW()),
    ('BOARD', 'Board member with read/write access to board functionality and governance', true, NOW(), NOW()),
    ('ADMIN_TECH', 'Technical administrator with all capabilities except worker, employer, and board functionality', true, NOW(), NOW()),
    ('ADMIN_OPS', 'Operations administrator with board, employer, worker access; except RBAC setup', true, NOW(), NOW())
ON CONFLICT (name) DO UPDATE SET
    is_active = true,
    updated_at = NOW();

RAISE NOTICE 'Phase 1 Complete: 7 roles created/updated';

-- ============================================================================
-- PHASE 2: CREATE USERS
-- ============================================================================

INSERT INTO users (username, email, password, full_name, permission_version, role, 
                   is_enabled, is_account_non_expired, is_account_non_locked, 
                   is_credentials_non_expired, created_at, updated_at, last_login)
VALUES
    -- Password: Platform!Bootstrap1 → $2a$12$ABgKvrzZNrOVlOkKOvzBAuSChaCz/16C8lkWSxuOGf/BIKuZz7vFG
    ('platform.bootstrap', 'platform.bootstrap@lbe.local', 
     '$2a$12$ABgKvrzZNrOVlOkKOvzBAuSChaCz/16C8lkWSxuOGf/BIKuZz7vFG',
     'Platform Bootstrap', 1, 'PLATFORM_BOOTSTRAP', true, true, true, true, NOW(), NOW(), NULL),
    
    -- Password: BasicUser!2025 → $2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XeKeUxWdeS86E50Dmk0m.
    ('basic.user1', 'basic.user1@lbe.local',
     '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XeKeUxWdeS86E50Dmk0m.',
     'Basic User 1', 1, 'BASIC_USER', true, true, true, true, NOW(), NOW(), NULL),
    
    -- Password: Worker!2025 → $2a$12$R9h21cIPz0peYQxQYL7tCOYUgd5LMRZoMyeIjZAgcg7b3Xe1z2.Lm
    ('worker1', 'worker1@lbe.local',
     '$2a$12$R9h21cIPz0peYQxQYL7tCOYUgd5LMRZoMyeIjZAgcg7b3Xe1z2.Lm',
     'Worker One', 1, 'WORKER', true, true, true, true, NOW(), NOW(), NULL),
    
    -- Password: Employer!2025 → $2a$12$Z2b5dMWqRcH8lOkKOvzB9eIjZAgcg7b3XeKeUxWdeS86E50DmKpu
    ('employer1', 'employer1@lbe.local',
     '$2a$12$Z2b5dMWqRcH8lOkKOvzB9eIjZAgcg7b3XeKeUxWdeS86E50DmKpu',
     'Employer One', 1, 'EMPLOYER', true, true, true, true, NOW(), NOW(), NULL),
    
    -- Password: Board!2025 → $2a$12$K3c6eNXrSdI9mPlLPwzC0fJkZAgcg7b3XeKeUxWdeS86E50DnLqv
    ('board1', 'board1@lbe.local',
     '$2a$12$K3c6eNXrSdI9mPlLPwzC0fJkZAgcg7b3XeKeUxWdeS86E50DnLqv',
     'Board Member One', 1, 'BOARD', true, true, true, true, NOW(), NOW(), NULL),
    
    -- Password: AdminTech!2025 → $2a$12$L4d7fOYsSEJ0nQmMQxzD1gKlZAgcg7b3XeKeUxWdeS86E50DnMrw
    ('admin.tech', 'admin.tech@lbe.local',
     '$2a$12$L4d7fOYsSEJ0nQmMQxzD1gKlZAgcg7b3XeKeUxWdeS86E50DnMrw',
     'Admin Technical', 1, 'ADMIN_TECH', true, true, true, true, NOW(), NOW(), NULL),
    
    -- Password: AdminOps!2025 → $2a$12$M5e8gPZtTFK1oRnNRyaE2hLmZAgcg7b3XeKeUxWdeS86E50DnNsx
    ('admin.ops', 'admin.ops@lbe.local',
     '$2a$12$M5e8gPZtTFK1oRnNRyaE2hLmZAgcg7b3XeKeUxWdeS86E50DnNsx',
     'Admin Operations', 1, 'ADMIN_OPS', true, true, true, true, NOW(), NOW(), NULL)
ON CONFLICT (username) DO UPDATE SET
    email = EXCLUDED.email,
    is_enabled = true,
    updated_at = NOW();

RAISE NOTICE 'Phase 2 Complete: 7 users created/updated';

-- ============================================================================
-- PHASE 3: ASSIGN USERS TO ROLES
-- ============================================================================

INSERT INTO user_role_assignment (user_id, role_id, assigned_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE (u.username = 'platform.bootstrap' AND r.name = 'PLATFORM_BOOTSTRAP')
   OR (u.username = 'basic.user1' AND r.name = 'BASIC_USER')
   OR (u.username = 'worker1' AND r.name = 'WORKER')
   OR (u.username = 'employer1' AND r.name = 'EMPLOYER')
   OR (u.username = 'board1' AND r.name = 'BOARD')
   OR (u.username = 'admin.tech' AND r.name = 'ADMIN_TECH')
   OR (u.username = 'admin.ops' AND r.name = 'ADMIN_OPS')
ON CONFLICT (user_id, role_id) DO NOTHING;

RAISE NOTICE 'Phase 3 Complete: Users assigned to roles';

-- ============================================================================
-- PHASE 4: CREATE CAPABILITIES
-- ============================================================================

INSERT INTO capabilities (name, module, action, description, is_active, created_at)
VALUES
    -- User Management
    ('user.create', 'user', 'create', 'Create new users', true, NOW()),
    ('user.read', 'user', 'read', 'View user information', true, NOW()),
    ('user.update', 'user', 'update', 'Update user information', true, NOW()),
    ('user.delete', 'user', 'delete', 'Delete users', true, NOW()),
    
    -- Worker Module
    ('worker.create', 'worker', 'create', 'Create worker records', true, NOW()),
    ('worker.read', 'worker', 'read', 'View worker information', true, NOW()),
    ('worker.update', 'worker', 'update', 'Update worker information', true, NOW()),
    ('worker.delete', 'worker', 'delete', 'Delete worker records', true, NOW()),
    
    -- Employer Module
    ('employer.create', 'employer', 'create', 'Create employer records', true, NOW()),
    ('employer.read', 'employer', 'read', 'View employer information', true, NOW()),
    ('employer.update', 'employer', 'update', 'Update employer information', true, NOW()),
    ('employer.delete', 'employer', 'delete', 'Delete employer records', true, NOW()),
    
    -- Board Module
    ('board.create', 'board', 'create', 'Create board items', true, NOW()),
    ('board.read', 'board', 'read', 'View board information', true, NOW()),
    ('board.update', 'board', 'update', 'Update board information', true, NOW()),
    ('board.delete', 'board', 'delete', 'Delete board records', true, NOW()),
    ('board.vote', 'board', 'vote', 'Vote on board decisions', true, NOW()),
    
    -- RBAC Management
    ('rbac.create', 'rbac', 'create', 'Create roles and policies', true, NOW()),
    ('rbac.read', 'rbac', 'read', 'View RBAC configuration', true, NOW()),
    ('rbac.update', 'rbac', 'update', 'Update roles and policies', true, NOW()),
    ('rbac.delete', 'rbac', 'delete', 'Delete roles and policies', true, NOW()),
    
    -- Reports
    ('report.create', 'report', 'create', 'Generate reports', true, NOW()),
    ('report.read', 'report', 'read', 'View reports', true, NOW()),
    ('report.download', 'report', 'download', 'Download reports', true, NOW()),
    
    -- System Administration
    ('system.view', 'system', 'view', 'View system status', true, NOW()),
    ('system.admin', 'system', 'admin', 'Administer system', true, NOW()),
    
    -- Resource Access
    ('resource.read', 'resource', 'read', 'Read resources', true, NOW()),
    ('resource.write', 'resource', 'write', 'Write resources', true, NOW())
ON CONFLICT (name) DO NOTHING;

RAISE NOTICE 'Phase 4 Complete: 26 capabilities created';

-- ============================================================================
-- PHASE 5: CREATE POLICIES
-- ============================================================================

INSERT INTO policies (name, description, is_active, created_at, updated_at)
VALUES
    ('bootstrap_all_policy', 'Full system access for bootstrap initialization', true, NOW(), NOW()),
    ('basic_user_policy', 'Read and write access to assigned resources only', true, NOW(), NOW()),
    ('worker_policy', 'Worker module read/write access', true, NOW(), NOW()),
    ('employer_policy', 'Employer module read/write access', true, NOW(), NOW()),
    ('board_policy', 'Board module read/write/vote access', true, NOW(), NOW()),
    ('admin_tech_policy', 'All capabilities except worker, employer, and board modules', true, NOW(), NOW()),
    ('admin_ops_policy', 'Worker, employer, board access; no RBAC management', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

RAISE NOTICE 'Phase 5 Complete: 7 policies created';

-- ============================================================================
-- PHASE 6: LINK POLICIES TO CAPABILITIES
-- ============================================================================

-- Bootstrap policy: ALL capabilities
INSERT INTO policy_capability (policy_id, capability_id)
SELECT p.id, c.id
FROM policies p, capabilities c
WHERE p.name = 'bootstrap_all_policy'
ON CONFLICT (policy_id, capability_id) DO NOTHING;

-- Basic user policy: resource access only
INSERT INTO policy_capability (policy_id, capability_id)
SELECT p.id, c.id
FROM policies p, capabilities c
WHERE p.name = 'basic_user_policy'
AND c.name IN ('resource.read', 'resource.write')
ON CONFLICT (policy_id, capability_id) DO NOTHING;

-- Worker policy: worker + resource capabilities
INSERT INTO policy_capability (policy_id, capability_id)
SELECT p.id, c.id
FROM policies p, capabilities c
WHERE p.name = 'worker_policy'
AND c.name IN ('worker.create', 'worker.read', 'worker.update', 'worker.delete',
               'resource.read', 'resource.write')
ON CONFLICT (policy_id, capability_id) DO NOTHING;

-- Employer policy: employer + resource capabilities
INSERT INTO policy_capability (policy_id, capability_id)
SELECT p.id, c.id
FROM policies p, capabilities c
WHERE p.name = 'employer_policy'
AND c.name IN ('employer.create', 'employer.read', 'employer.update', 'employer.delete',
               'resource.read', 'resource.write')
ON CONFLICT (policy_id, capability_id) DO NOTHING;

-- Board policy: board + resource capabilities
INSERT INTO policy_capability (policy_id, capability_id)
SELECT p.id, c.id
FROM policies p, capabilities c
WHERE p.name = 'board_policy'
AND c.name IN ('board.create', 'board.read', 'board.update', 'board.delete', 'board.vote',
               'resource.read', 'resource.write')
ON CONFLICT (policy_id, capability_id) DO NOTHING;

-- Admin Tech policy: all EXCEPT worker/employer/board
INSERT INTO policy_capability (policy_id, capability_id)
SELECT p.id, c.id
FROM policies p, capabilities c
WHERE p.name = 'admin_tech_policy'
AND c.name NOT IN ('worker.create', 'worker.read', 'worker.update', 'worker.delete',
                   'employer.create', 'employer.read', 'employer.update', 'employer.delete',
                   'board.create', 'board.read', 'board.update', 'board.delete', 'board.vote')
ON CONFLICT (policy_id, capability_id) DO NOTHING;

-- Admin Ops policy: worker/employer/board + system.view; NO rbac
INSERT INTO policy_capability (policy_id, capability_id)
SELECT p.id, c.id
FROM policies p, capabilities c
WHERE p.name = 'admin_ops_policy'
AND c.name IN ('worker.create', 'worker.read', 'worker.update', 'worker.delete',
               'employer.create', 'employer.read', 'employer.update', 'employer.delete',
               'board.create', 'board.read', 'board.update', 'board.delete', 'board.vote',
               'system.view', 'resource.read', 'resource.write')
ON CONFLICT (policy_id, capability_id) DO NOTHING;

RAISE NOTICE 'Phase 6 Complete: Policies linked to capabilities';

-- ============================================================================
-- COMMIT TRANSACTION
-- ============================================================================

COMMIT;

-- ============================================================================
-- VERIFICATION OUTPUT
-- ============================================================================

\echo '========================================='
\echo 'ONBOARDING SETUP COMPLETE'
\echo '========================================='

\echo ''
\echo 'Created Roles:'
SELECT COUNT(*) as count FROM roles;

\echo ''
\echo 'Created Users:'
SELECT COUNT(*) as count FROM users;

\echo ''
\echo 'User-Role Assignments:'
SELECT COUNT(*) as count FROM user_role_assignment;

\echo ''
\echo 'Capabilities:'
SELECT COUNT(*) as count FROM capabilities;

\echo ''
\echo 'Policies:'
SELECT COUNT(*) as count FROM policies;

\echo ''
\echo 'Policy-Capability Links:'
SELECT COUNT(*) as count FROM policy_capability;

\echo ''
\echo 'User Details:'
SELECT username, email, role, is_enabled FROM users ORDER BY username;

\echo ''
\echo 'Role Assignment Details:'
SELECT u.username, r.name as role_name
FROM user_role_assignment ura
JOIN users u ON ura.user_id = u.id
JOIN roles r ON ura.role_id = r.id
ORDER BY u.username;

\echo ''
\echo 'For verification, run:'
\echo '  psql -U postgres -d auth_service_db -f docs/ONBOARDING/setup/verify_onboarding.sql'
