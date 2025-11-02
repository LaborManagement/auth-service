-- ============================================================================
-- Test: Basic RLS Verification
-- Purpose: Verify all RLS components are working
-- Usage: psql -U postgres -d <database> -f test_rls_basic.sql
-- ============================================================================

\echo '=== VPD/RLS BASIC TEST ==='

-- 1. Check RLS functions
\echo ''
\echo '1. RLS Functions Status:'
SELECT proname FROM pg_proc 
WHERE pronamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'auth')
AND proname IN ('set_user_context', 'get_user_context', 'can_read_row', 'can_write_row')
ORDER BY proname;

-- 2. Check RLS enabled on tables
\echo ''
\echo '2. Tables with RLS Enabled:'
SELECT schemaname, tablename, rowsecurity 
FROM pg_tables 
WHERE rowsecurity = true
ORDER BY tablename;

-- 3. Check policies exist
\echo ''
\echo '3. RLS Policies Count:'
SELECT schemaname, tablename, COUNT(*) as policy_count
FROM pg_policies
GROUP BY schemaname, tablename
ORDER BY tablename;

-- 4. Check ACL table
\echo ''
\echo '4. ACL Table Status:'
SELECT COUNT(*) as total_acl_entries FROM auth.user_tenant_acl;
SELECT COUNT(DISTINCT user_id) as unique_users FROM auth.user_tenant_acl;

-- 5. Sample ACL entries
\echo ''
\echo '5. Sample ACL Entries (first 5):'
SELECT user_id, board_id, employer_id, can_read, can_write 
FROM auth.user_tenant_acl 
LIMIT 5;

-- 6. Check columns exist
\echo ''
\echo '6. Tenant Columns Check:'
SELECT COUNT(*) as tables_with_tenant_cols
FROM information_schema.columns 
WHERE column_name IN ('board_id', 'employer_id')
GROUP BY table_schema;

-- 7. Test context functions
\echo ''
\echo '7. Testing Context Functions:'
SELECT auth.set_user_context('8') as set_context;
SELECT auth.get_user_context() as current_context;

-- 8. Test permission functions
\echo ''
\echo '8. Testing Permission Functions (User 8):'
SELECT auth.can_read_row('BOARD_1', 'EMP_2') as can_read_emp2;
SELECT auth.can_write_row('BOARD_1', 'EMP_2') as can_write_emp2;

\echo ''
\echo '✅ Basic RLS verification complete!'
