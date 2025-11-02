-- ============================================================================
-- Test: ACL Permission Matrix
-- Purpose: View complete user → board/employer access matrix
-- Usage: psql -U postgres -d <database> -f test_acl_matrix.sql
-- ============================================================================

\echo '=== VPD/RLS ACL PERMISSION MATRIX ==='

-- Full matrix
\echo ''
\echo 'Complete ACL Matrix:'
\echo '──────────────────────────────────────────────────────────────'
SELECT 
    user_id,
    board_id,
    COALESCE(employer_id, 'ALL') as employer_id,
    can_read,
    can_write
FROM auth.user_tenant_acl
ORDER BY user_id, board_id, employer_id;

-- Count by user
\echo ''
\echo 'Access Rights by User:'
\echo '──────────────────────────────────────────────────────────────'
SELECT 
    user_id,
    COUNT(*) as "Total Permissions",
    SUM(CASE WHEN can_read THEN 1 ELSE 0 END) as "Read Access",
    SUM(CASE WHEN can_write THEN 1 ELSE 0 END) as "Write Access"
FROM auth.user_tenant_acl
GROUP BY user_id
ORDER BY user_id;

-- Count by board
\echo ''
\echo 'Users per Board:'
\echo '──────────────────────────────────────────────────────────────'
SELECT 
    board_id,
    COUNT(DISTINCT user_id) as "Users with Access",
    COUNT(*) as "Total Permissions"
FROM auth.user_tenant_acl
GROUP BY board_id
ORDER BY board_id;

-- Users with write access
\echo ''
\echo 'Users with WRITE Access:'
\echo '──────────────────────────────────────────────────────────────'
SELECT 
    user_id,
    board_id,
    COALESCE(employer_id, 'ALL') as employer_id
FROM auth.user_tenant_acl
WHERE can_write = true
ORDER BY user_id, board_id;

-- Users with read-only access
\echo ''
\echo 'Users with READ-ONLY Access:'
\echo '──────────────────────────────────────────────────────────────'
SELECT 
    user_id,
    board_id,
    COALESCE(employer_id, 'ALL') as employer_id
FROM auth.user_tenant_acl
WHERE can_read = true AND can_write = false
ORDER BY user_id, board_id;
