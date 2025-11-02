-- Test: Compare What Different Users See
-- Usage: Run this to verify different users have different data access

-- ========================================
-- Step 1: Set up as app role (mimics production)
-- ========================================
SET ROLE app_payment_flow;

-- ========================================
-- Step 2: Test User 8 (Restricted Access)
-- ========================================
SELECT auth.set_user_context('8');
SELECT current_setting('app.current_user_id') as current_user;

SELECT 'User 8 - worker_uploaded_data' as test;
SELECT COUNT(*) as row_count FROM payment_flow.worker_uploaded_data;

SELECT 'User 8 - worker_payment_receipts' as test;
SELECT COUNT(*) as row_count FROM payment_flow.worker_payment_receipts;

-- ========================================
-- Step 3: Test User 1 (Admin - Full Access)
-- ========================================
SELECT auth.set_user_context('1');
SELECT current_setting('app.current_user_id') as current_user;

SELECT 'User 1 - worker_uploaded_data' as test;
SELECT COUNT(*) as row_count FROM payment_flow.worker_uploaded_data;

SELECT 'User 1 - worker_payment_receipts' as test;
SELECT COUNT(*) as row_count FROM payment_flow.worker_payment_receipts;

-- ========================================
-- Step 4: Compare Results
-- ========================================
-- If User 8 counts are LESS than User 1 counts → RLS is working!
-- If counts are the SAME → RLS is NOT filtering properly

-- ========================================
-- Step 5: Reset
-- ========================================
RESET ROLE;
SELECT 'Connection reset to superuser' as status;
