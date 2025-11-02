-- ============================================================================
-- VERIFICATION SCRIPT: Complete RBAC Setup Validation
-- ============================================================================
-- Purpose: Verify that all RBAC system initialization phases completed successfully
-- Run after all SQL scripts to validate the complete setup
-- ============================================================================

\set ON_ERROR_STOP off

-- Start fresh with clear output
CLEAR
\echo '================================================================================'
\echo 'RBAC System Setup Verification Report'
\echo 'Generated: '
\echo '================================================================================'
\echo ''

-- ============================================================================
-- 1. VERIFY ROLES
-- ============================================================================
\echo '1. VERIFYING ROLES (Expected: 7)'
\echo '-----------------------------------------------'
SELECT 
  name,
  description,
  is_active,
  created_at
FROM roles
ORDER BY created_at;

SELECT COUNT(*) as total_roles, 
       COUNT(CASE WHEN is_active THEN 1 END) as active_roles
FROM roles;
\echo ''

-- ============================================================================
-- 2. VERIFY CAPABILITIES
-- ============================================================================
\echo '2. VERIFYING CAPABILITIES (Expected: 98)'
\echo '-----------------------------------------------'
SELECT 
  module,
  COUNT(*) as count,
  COUNT(CASE WHEN is_active THEN 1 END) as active
FROM capabilities
GROUP BY module
ORDER BY module;

SELECT COUNT(*) as total_capabilities, 
       COUNT(CASE WHEN is_active THEN 1 END) as active_capabilities
FROM capabilities;
\echo ''

-- ============================================================================
-- 3. VERIFY POLICIES
-- ============================================================================
\echo '3. VERIFYING POLICIES (Expected: 7)'
\echo '-----------------------------------------------'
SELECT 
  name,
  policy_type,
  is_active,
  created_at
FROM policies
ORDER BY created_at;

SELECT COUNT(*) as total_policies, 
       COUNT(CASE WHEN is_active THEN 1 END) as active_policies
FROM policies;
\echo ''

-- ============================================================================
-- 4. VERIFY POLICY-CAPABILITY LINKS
-- ============================================================================
\echo '4. VERIFYING POLICY-CAPABILITY LINKS'
\echo '-----------------------------------------------'
\echo 'Expected Distribution: BOOTSTRAP=55, ADMIN_TECH=51, ADMIN_OPS=42, BOARD=17, EMPLOYER=19, WORKER=14, TEST_USER=50'
\echo ''

SELECT 
  p.name as policy_name,
  COUNT(pc.capability_id) as capability_count
FROM policies p
LEFT JOIN policy_capabilities pc ON p.id = pc.policy_id
GROUP BY p.id, p.name
ORDER BY capability_count DESC;

SELECT COUNT(*) as total_policy_capability_links FROM policy_capabilities;
\echo ''

-- ============================================================================
-- 5. VERIFY SEED USERS
-- ============================================================================
\echo '5. VERIFYING SEED USERS (Expected: 7)'
\echo '-----------------------------------------------'
SELECT 
  username,
  email,
  is_active,
  created_at
FROM users
WHERE username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user')
ORDER BY username;

SELECT COUNT(*) as total_seed_users FROM users 
WHERE username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user');
\echo ''

-- ============================================================================
-- 6. VERIFY USER-ROLE ASSIGNMENTS
-- ============================================================================
\echo '6. VERIFYING USER-ROLE ASSIGNMENTS (Expected: 7, one per user)'
\echo '-----------------------------------------------'
SELECT 
  u.username,
  r.name as role,
  ura.assigned_at
FROM user_role_assignments ura
JOIN users u ON ura.user_id = u.id
JOIN roles r ON ura.role_id = r.id
WHERE u.username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user')
ORDER BY u.username;

SELECT COUNT(*) as total_assignments FROM user_role_assignments 
WHERE user_id IN (SELECT id FROM users WHERE username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user'));
\echo ''

-- ============================================================================
-- 7. VERIFY CAPABILITY ACCESS BY ROLE
-- ============================================================================
\echo '7. CAPABILITY ACCESS SUMMARY BY ROLE'
\echo '-----------------------------------------------'
WITH role_capabilities AS (
  SELECT 
    r.name as role,
    COUNT(DISTINCT pc.capability_id) as capability_count
  FROM roles r
  LEFT JOIN user_role_assignments ura ON r.id = ura.role_id
  LEFT JOIN policy_capabilities pc ON r.id = (
    -- Get policy for this role (assuming role name corresponds to policy pattern)
    SELECT policy_id FROM policies WHERE name = CONCAT(r.name, '_POLICY')
  )
  GROUP BY r.id, r.name
)
SELECT 
  role,
  capability_count,
  ROUND(100.0 * capability_count / 98, 1) as percentage_of_total
FROM role_capabilities
ORDER BY capability_count DESC;
\echo ''

-- ============================================================================
-- 8. DATA INTEGRITY CHECKS
-- ============================================================================
\echo '8. DATA INTEGRITY CHECKS'
\echo '-----------------------------------------------'

-- Check for orphaned policy-capability links
\echo 'Checking for orphaned policy-capability links...'
SELECT COUNT(*) as orphaned_links
FROM policy_capabilities pc
WHERE pc.policy_id NOT IN (SELECT id FROM policies)
   OR pc.capability_id NOT IN (SELECT id FROM capabilities);
\echo 'Expected: 0'
\echo ''

-- Check for orphaned user-role assignments
\echo 'Checking for orphaned user-role assignments...'
SELECT COUNT(*) as orphaned_assignments
FROM user_role_assignments ura
WHERE ura.user_id NOT IN (SELECT id FROM users)
   OR ura.role_id NOT IN (SELECT id FROM roles);
\echo 'Expected: 0'
\echo ''

-- ============================================================================
-- 9. SUMMARY REPORT
-- ============================================================================
\echo '9. SETUP SUMMARY'
\echo '================================================================================'

SELECT 
  'COMPONENT' as component,
  'EXPECTED' as expected,
  'ACTUAL' as actual,
  CASE WHEN expected = actual THEN '✅ PASS' ELSE '❌ FAIL' END as status
FROM (
  SELECT 'Roles' as component, 7 as expected, (SELECT COUNT(*) FROM roles WHERE is_active) as actual
  UNION ALL
  SELECT 'Capabilities', 98, (SELECT COUNT(*) FROM capabilities WHERE is_active)
  UNION ALL
  SELECT 'Policies', 7, (SELECT COUNT(*) FROM policies WHERE is_active)
  UNION ALL
  SELECT 'Policy-Capability Links', 288, (SELECT COUNT(*) FROM policy_capabilities)
  UNION ALL
  SELECT 'Seed Users', 7, (SELECT COUNT(*) FROM users WHERE username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user'))
  UNION ALL
  SELECT 'User-Role Assignments', 7, (SELECT COUNT(*) FROM user_role_assignments WHERE user_id IN (SELECT id FROM users WHERE username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user')))
) verification;

\echo ''
\echo '================================================================================'
\echo 'NEXT STEPS:'
\echo '1. Review all verification results above'
\echo '2. If any status shows ❌ FAIL, re-run the corresponding SQL script'
\echo '3. Disable PLATFORM_BOOTSTRAP user: UPDATE users SET is_active=false WHERE username=''platform.bootstrap'';'
\echo '4. Change default passwords for all seed users'
\echo '5. Configure VPD policies for WORKER and EMPLOYER roles'
\echo '6. Register API endpoints and UI pages'
\echo '================================================================================'
\echo ''
