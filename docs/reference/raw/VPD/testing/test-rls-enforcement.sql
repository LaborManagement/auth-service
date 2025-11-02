-- Test: Verify RLS is Properly Enforced
-- Usage: Confirm that RLS policies are active and working

-- ========================================
-- Check RLS Status
-- ========================================
SELECT 'RLS Enabled Tables' as check_type;
SELECT 
    schemaname,
    tablename,
    rowsecurity
FROM pg_tables 
WHERE schemaname = 'payment_flow'
ORDER BY tablename;

-- ========================================
-- Check Policies Exist
-- ========================================
SELECT 'Policies on payment_flow tables' as check_type;
SELECT 
    schemaname,
    tablename,
    policyname,
    permissive,
    cmd
FROM pg_policies 
WHERE schemaname = 'payment_flow'
ORDER BY tablename, policyname;

-- ========================================
-- Test Superuser (NO RLS)
-- ========================================
SELECT 'SUPERUSER ACCESS (No RLS):' as test_section;
SELECT current_user;
SELECT COUNT(*) as superuser_row_count FROM payment_flow.worker_uploaded_data;

-- ========================================
-- Test App Role (WITH RLS)
-- ========================================
SELECT 'APP ROLE ACCESS (With RLS):' as test_section;
SET ROLE app_payment_flow;
SELECT current_user;

-- User 8
SELECT auth.set_user_context('8');
SELECT current_setting('app.current_user_id') as user_context;
SELECT COUNT(*) as user_8_row_count FROM payment_flow.worker_uploaded_data;

-- ========================================
-- Verify RLS Functions Exist
-- ========================================
RESET ROLE;
SELECT 'RLS Functions' as check_type;
SELECT 
    proname,
    pg_get_functiondef(oid) as definition
FROM pg_proc
WHERE pronamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'auth')
AND proname IN ('set_user_context', 'get_user_context', 'can_read_row', 'can_write_row')
ORDER BY proname;

-- ========================================
-- Summary
-- ========================================
SELECT '✓ If all checks passed, RLS is properly configured' as status;
