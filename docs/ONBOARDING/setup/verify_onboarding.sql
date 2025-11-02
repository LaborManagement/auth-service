-- ============================================================================
-- Onboarding Verification Script
-- ============================================================================
-- Quick verification that all onboarding steps completed successfully.
--
-- USAGE:
--   psql -U postgres -d auth_service_db -f docs/ONBOARDING/setup/verify_onboarding.sql
--
-- ============================================================================

\echo '========================================='
\echo 'ONBOARDING VERIFICATION REPORT'
\echo '========================================='
\echo ''

-- Check 1: Roles
\echo '1. RBAC ROLES'
\echo '─────────────────────────────────────────'
SELECT 
    COUNT(*) as total_roles,
    STRING_AGG(name, ', ' ORDER BY name) as role_names
FROM roles
WHERE name IN ('PLATFORM_BOOTSTRAP', 'BASIC_USER', 'WORKER', 'EMPLOYER', 'BOARD', 'ADMIN_TECH', 'ADMIN_OPS');

\echo ''

-- Check 2: Users
\echo '2. USERS CREATED'
\echo '─────────────────────────────────────────'
SELECT 
    COUNT(*) as total_users,
    SUM(CASE WHEN is_enabled = true THEN 1 ELSE 0 END) as enabled_users
FROM users
WHERE username IN ('platform.bootstrap', 'basic.user1', 'worker1', 'employer1', 'board1', 'admin.tech', 'admin.ops');

\echo ''
\echo 'User Details:'
SELECT username, email, role, is_enabled, created_at
FROM users
WHERE username IN ('platform.bootstrap', 'basic.user1', 'worker1', 'employer1', 'board1', 'admin.tech', 'admin.ops')
ORDER BY username;

\echo ''

-- Check 3: User-Role Assignments
\echo '3. USER-ROLE ASSIGNMENTS'
\echo '─────────────────────────────────────────'
SELECT 
    COUNT(*) as total_assignments
FROM user_role_assignment ura
JOIN users u ON ura.user_id = u.id
WHERE u.username IN ('platform.bootstrap', 'basic.user1', 'worker1', 'employer1', 'board1', 'admin.tech', 'admin.ops');

\echo ''
\echo 'Assignment Details:'
SELECT 
    u.username as user_name,
    r.name as role_name
FROM user_role_assignment ura
JOIN users u ON ura.user_id = u.id
JOIN roles r ON ura.role_id = r.id
WHERE u.username IN ('platform.bootstrap', 'basic.user1', 'worker1', 'employer1', 'board1', 'admin.tech', 'admin.ops')
ORDER BY u.username;

\echo ''

-- Check 4: Capabilities
\echo '4. CAPABILITIES BY MODULE'
\echo '─────────────────────────────────────────'
SELECT 
    module,
    COUNT(*) as capability_count,
    STRING_AGG(name, ', ' ORDER BY name) as capabilities
FROM capabilities
WHERE module IN ('user', 'worker', 'employer', 'board', 'rbac', 'report', 'system', 'resource')
GROUP BY module
ORDER BY module;

\echo ''
\echo 'Total Capabilities:'
SELECT COUNT(*) as total_capabilities FROM capabilities;

\echo ''

-- Check 5: Policies
\echo '5. POLICIES CREATED'
\echo '─────────────────────────────────────────'
SELECT 
    name,
    description,
    is_active
FROM policies
WHERE name IN ('bootstrap_all_policy', 'basic_user_policy', 'worker_policy', 
               'employer_policy', 'board_policy', 'admin_tech_policy', 'admin_ops_policy')
ORDER BY name;

\echo ''

-- Check 6: Policy-Capability Mappings
\echo '6. POLICY CAPABILITY MAPPINGS'
\echo '─────────────────────────────────────────'
SELECT 
    p.name as policy,
    COUNT(pc.capability_id) as capability_count
FROM policies p
LEFT JOIN policy_capability pc ON p.id = pc.policy_id
WHERE p.name IN ('bootstrap_all_policy', 'basic_user_policy', 'worker_policy', 
                 'employer_policy', 'board_policy', 'admin_tech_policy', 'admin_ops_policy')
GROUP BY p.name
ORDER BY p.name;

\echo ''

-- Check 7: Detailed Policy-Capability Mappings
\echo '7. DETAILED POLICY CAPABILITIES'
\echo '─────────────────────────────────────────'
SELECT 
    p.name as policy,
    STRING_AGG(c.name, ', ' ORDER BY c.module, c.action) as capabilities
FROM policies p
LEFT JOIN policy_capability pc ON p.id = pc.policy_id
LEFT JOIN capabilities c ON pc.capability_id = c.id
WHERE p.name IN ('bootstrap_all_policy', 'basic_user_policy', 'worker_policy', 
                 'employer_policy', 'board_policy', 'admin_tech_policy', 'admin_ops_policy')
GROUP BY p.name, p.id
ORDER BY p.name;

\echo ''

-- Check 8: Permission Matrix Summary
\echo '8. PERMISSION MATRIX SUMMARY'
\echo '─────────────────────────────────────────'
\echo ''
\echo 'User Type Permissions:'
\echo ''

WITH user_policies AS (
    SELECT 
        u.username,
        r.name as role_name,
        STRING_AGG(DISTINCT c.module, '|' ORDER BY DISTINCT c.module) as modules
    FROM users u
    JOIN user_role_assignment ura ON u.id = ura.user_id
    JOIN roles r ON ura.role_id = r.id
    JOIN policies p ON r.name || '_policy' = p.name
    LEFT JOIN policy_capability pc ON p.id = pc.policy_id
    LEFT JOIN capabilities c ON pc.capability_id = c.id
    WHERE u.username IN ('platform.bootstrap', 'basic.user1', 'worker1', 'employer1', 'board1', 'admin.tech', 'admin.ops')
    GROUP BY u.username, r.name
)
SELECT 
    username,
    role_name,
    modules,
    CASE 
        WHEN role_name = 'PLATFORM_BOOTSTRAP' THEN '✅ All modules (full access)'
        WHEN role_name = 'BASIC_USER' THEN '✅ Resources only'
        WHEN role_name = 'WORKER' THEN '✅ Worker + Resources'
        WHEN role_name = 'EMPLOYER' THEN '✅ Employer + Resources'
        WHEN role_name = 'BOARD' THEN '✅ Board + Resources'
        WHEN role_name = 'ADMIN_TECH' THEN '✅ All except Worker/Employer/Board'
        WHEN role_name = 'ADMIN_OPS' THEN '✅ Worker/Employer/Board (no RBAC)'
    END as permission_summary
FROM user_policies
ORDER BY username;

\echo ''

-- Check 9: Overall Summary
\echo '9. SETUP SUMMARY'
\echo '─────────────────────────────────────────'

WITH summary AS (
    SELECT
        (SELECT COUNT(*) FROM roles WHERE name IN ('PLATFORM_BOOTSTRAP', 'BASIC_USER', 'WORKER', 'EMPLOYER', 'BOARD', 'ADMIN_TECH', 'ADMIN_OPS')) as roles_count,
        (SELECT COUNT(*) FROM users WHERE username IN ('platform.bootstrap', 'basic.user1', 'worker1', 'employer1', 'board1', 'admin.tech', 'admin.ops')) as users_count,
        (SELECT COUNT(*) FROM user_role_assignment) as assignments_count,
        (SELECT COUNT(*) FROM capabilities) as capabilities_count,
        (SELECT COUNT(*) FROM policies) as policies_count,
        (SELECT COUNT(*) FROM policy_capability) as mappings_count
)
SELECT
    roles_count || ' / 7' as "Roles Created",
    users_count || ' / 7' as "Users Created",
    assignments_count || ' / 7' as "Assignments",
    capabilities_count || ' / 26' as "Capabilities",
    policies_count || ' / 7' as "Policies",
    mappings_count || ' / 100+' as "Policy Mappings"
FROM summary;

\echo ''
\echo '========================================='
\echo 'VERIFICATION COMPLETE'
\echo '========================================='
\echo ''
\echo 'Next Steps:'
\echo '1. Test user authentication'
\echo '2. Verify role-based access control'
\echo '3. Check RLS policies at database level'
\echo '4. Change default passwords immediately'
\echo ''
