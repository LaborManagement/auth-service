-- Test: Verify ACL Permissions
-- Usage: Check that users have correct ACL entries

-- ========================================
-- Check ACL Setup
-- ========================================
SELECT 'Checking ACL table existence' as step;
SELECT COUNT(*) as total_acl_entries FROM auth.user_tenant_acl;

-- ========================================
-- Verify User 8 Permissions
-- ========================================
SELECT 'User 8 ACL entries' as step;
SELECT 
    user_id,
    board_id,
    employer_id,
    can_read,
    can_write
FROM auth.user_tenant_acl 
WHERE user_id = 8
ORDER BY board_id, employer_id;

-- ========================================
-- Verify User 1 (Admin) Permissions
-- ========================================
SELECT 'User 1 ACL entries' as step;
SELECT 
    user_id,
    board_id,
    employer_id,
    can_read,
    can_write
FROM auth.user_tenant_acl 
WHERE user_id = 1
ORDER BY board_id, employer_id;

-- ========================================
-- Check all users and their access levels
-- ========================================
SELECT 'All users with read access' as step;
SELECT 
    user_id,
    COUNT(DISTINCT board_id) as board_count,
    COUNT(DISTINCT employer_id) as employer_count,
    SUM(CASE WHEN can_read THEN 1 ELSE 0 END) as read_count,
    SUM(CASE WHEN can_write THEN 1 ELSE 0 END) as write_count
FROM auth.user_tenant_acl
GROUP BY user_id
ORDER BY user_id;

-- ========================================
-- Check for missing ACL entries
-- ========================================
SELECT 'Users without ACL entries' as step;
SELECT id, username, role 
FROM auth.users
WHERE id NOT IN (SELECT DISTINCT user_id FROM auth.user_tenant_acl)
AND is_enabled = true;
