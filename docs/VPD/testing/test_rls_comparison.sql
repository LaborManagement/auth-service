-- ============================================================================
-- Test: Compare Data Access Across Users
-- Purpose: Verify different users see different data
-- Usage: psql -U postgres -d <database> -f test_rls_comparison.sql
-- ============================================================================

\echo '=== VPD/RLS USER COMPARISON TEST ==='

-- ============================================================================
-- TEST WITH APP ROLE (Correct way - respects RLS)
-- ============================================================================

\echo ''
\echo '========== TESTING WITH APP ROLE (CORRECT) =========='

SET ROLE app_payment_flow;

\echo ''
\echo 'USER 1 (Admin) - Should see most data'
\echo '───────────────────────────────────────'
SELECT auth.set_user_context('1');
SELECT COUNT(*) as "Total Rows for User 1" FROM payment_flow.worker_uploaded_data;
SELECT board_id, COUNT(*) as count FROM payment_flow.worker_uploaded_data GROUP BY board_id;

\echo ''
\echo 'USER 8 (Worker) - Should see less data'
\echo '───────────────────────────────────────'
SELECT auth.set_user_context('8');
SELECT COUNT(*) as "Total Rows for User 8" FROM payment_flow.worker_uploaded_data;
SELECT board_id, COUNT(*) as count FROM payment_flow.worker_uploaded_data GROUP BY board_id;

\echo ''
\echo 'USER 2 (Employer) - Should see specific employer data'
\echo '───────────────────────────────────────'
SELECT auth.set_user_context('2');
SELECT COUNT(*) as "Total Rows for User 2" FROM payment_flow.worker_uploaded_data;
SELECT board_id, employer_id, COUNT(*) as count 
FROM payment_flow.worker_uploaded_data 
GROUP BY board_id, employer_id;

-- Reset
RESET ROLE;

\echo ''
\echo '=== COMPARISON RESULTS ==='
\echo 'If row counts differ for different users → RLS is working! ✅'
\echo 'If all users see same row count → RLS may not be filtering! ❌'
