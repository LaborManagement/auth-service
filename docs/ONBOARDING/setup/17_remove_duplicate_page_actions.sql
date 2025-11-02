-- ============================================================================
-- PHASE 17: REMOVE DUPLICATE PAGE ACTIONS
-- ============================================================================
-- Purpose: Remove duplicate page_actions that link to the same endpoint
-- Background: During page_action linkage, duplicates were created
-- Status: Production Ready
-- PostgreSQL Version: 12+
-- ============================================================================

SET search_path TO auth;
\set ON_ERROR_STOP on

BEGIN;

-- ============================================================================
-- IDENTIFY DUPLICATES
-- ============================================================================
\echo ''
\echo '========== DUPLICATE PAGE ACTIONS =========='
\echo ''

SELECT 
  page_id,
  label,
  endpoint_id,
  COUNT(*) as duplicate_count,
  string_agg(CAST(id AS TEXT), ', ') as action_ids
FROM page_actions
WHERE is_active = true
GROUP BY page_id, label, endpoint_id
HAVING COUNT(*) > 1
ORDER BY page_id, label;

-- ============================================================================
-- REMOVE DUPLICATES
-- Strategy: Keep the first occurrence (lowest ID), delete the rest
-- ============================================================================

DELETE FROM page_actions
WHERE id IN (
  SELECT id FROM (
    SELECT 
      id,
      ROW_NUMBER() OVER (
        PARTITION BY page_id, label, endpoint_id 
        ORDER BY id
      ) as rn
    FROM page_actions
    WHERE is_active = true
  ) ranked
  WHERE rn > 1
);

-- ============================================================================
-- VERIFICATION
-- ============================================================================
\echo ''
\echo '========== AFTER DUPLICATE REMOVAL =========='
\echo ''

-- Check if any duplicates remain
SELECT 
  page_id,
  label,
  endpoint_id,
  COUNT(*) as duplicate_count
FROM page_actions
WHERE is_active = true
GROUP BY page_id, label, endpoint_id
HAVING COUNT(*) > 1;

\echo 'If no rows shown above, duplicates have been successfully removed.'
\echo ''

-- Show final page_action counts per page
SELECT 
  up.id,
  up.page_id,
  up.label,
  COUNT(pa.id) as page_action_count,
  COUNT(DISTINCT pa.endpoint_id) as unique_endpoint_count
FROM ui_pages up
LEFT JOIN page_actions pa ON up.id = pa.page_id AND pa.is_active = true
GROUP BY up.id, up.page_id, up.label
ORDER BY up.id;

COMMIT;

\echo ''
\echo 'SUCCESS: Duplicate page_actions removed!'
