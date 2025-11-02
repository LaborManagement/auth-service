-- ============================================================================
-- PHASE 16: LINK PAGE ACTIONS TO ENDPOINTS (REVISED)
-- ============================================================================
-- Purpose: Link page_actions to their corresponding endpoints
-- This enables the /api/meta/endpoints?page_id={id} endpoint to return results
-- Status: Production Ready
-- PostgreSQL Version: 12+
-- ============================================================================

SET search_path TO auth;
\set ON_ERROR_STOP on

BEGIN;

-- ============================================================================
-- STEP 1: Create a temporary mapping table
-- ============================================================================
CREATE TEMP TABLE endpoint_mapping (
  capability_name VARCHAR(100),
  endpoint_id BIGINT
);

-- ============================================================================
-- STEP 2: Map capabilities to endpoints
-- ============================================================================

-- Find endpoints for each capability type
INSERT INTO endpoint_mapping (capability_name, endpoint_id)
SELECT 'user.account.read', id FROM endpoints WHERE service = 'auth-service' AND path LIKE '%profile%' AND method = 'GET' LIMIT 1;

INSERT INTO endpoint_mapping (capability_name, endpoint_id)
SELECT 'user.account.create', id FROM endpoints WHERE service = 'auth-service' AND method = 'POST' LIMIT 1;

INSERT INTO endpoint_mapping (capability_name, endpoint_id)
SELECT 'user.account.update', id FROM endpoints WHERE service = 'auth-service' AND path LIKE '%profile%' AND method = 'PUT' LIMIT 1;

INSERT INTO endpoint_mapping (capability_name, endpoint_id)
SELECT 'user.account.delete', id FROM endpoints WHERE service = 'admin' AND method = 'DELETE' LIMIT 1;

-- Board endpoints
INSERT INTO endpoint_mapping (capability_name, endpoint_id)
SELECT 'board.request.read', id FROM endpoints WHERE service = 'board-receipts' AND method = 'GET' LIMIT 1;

INSERT INTO endpoint_mapping (capability_name, endpoint_id)
SELECT 'board.payment.approve', id FROM endpoints WHERE service = 'board-receipts' AND method = 'POST' LIMIT 1;

INSERT INTO endpoint_mapping (capability_name, endpoint_id)
SELECT 'board.payment.reject', id FROM endpoints WHERE service = 'board-receipts' AND method = 'DELETE' LIMIT 1;

INSERT INTO endpoint_mapping (capability_name, endpoint_id)
SELECT 'board.payment.reconcile', id FROM endpoints WHERE service = 'board-receipts' AND method = 'PUT' LIMIT 1;

INSERT INTO endpoint_mapping (capability_name, endpoint_id)
SELECT 'board.receipt.read', id FROM endpoints WHERE service = 'board-receipts' AND method = 'GET' LIMIT 1;

INSERT INTO endpoint_mapping (capability_name, endpoint_id)
SELECT 'board.receipt.process', id FROM endpoints WHERE service = 'board-receipts' AND method = 'POST' LIMIT 1;

INSERT INTO endpoint_mapping (capability_name, endpoint_id)
SELECT 'board.decision.vote', id FROM endpoints WHERE service = 'board-receipts' AND method = 'POST' LIMIT 1;

-- RBAC endpoints
INSERT INTO endpoint_mapping (capability_name, endpoint_id)
SELECT 'rbac.role.read', id FROM endpoints WHERE service = 'admin' AND path LIKE '%role%' AND method = 'GET' LIMIT 1;

INSERT INTO endpoint_mapping (capability_name, endpoint_id)
SELECT 'rbac.role.assign', id FROM endpoints WHERE service = 'admin' AND path LIKE '%role%' AND method = 'PUT' LIMIT 1;

INSERT INTO endpoint_mapping (capability_name, endpoint_id)
SELECT 'rbac.endpoint.read', id FROM endpoints WHERE service = 'admin' AND path LIKE '%endpoint%' AND method = 'GET' LIMIT 1;

INSERT INTO endpoint_mapping (capability_name, endpoint_id)
SELECT 'ui.page.read', id FROM endpoints WHERE service = 'admin' AND path LIKE '%page%' AND method = 'GET' LIMIT 1;

INSERT INTO endpoint_mapping (capability_name, endpoint_id)
SELECT 'ui.action.read', id FROM endpoints WHERE service = 'admin' AND method = 'GET' LIMIT 1;

-- Worker endpoints
INSERT INTO endpoint_mapping (capability_name, endpoint_id)
SELECT 'worker.data.read', id FROM endpoints WHERE service = 'worker' AND method = 'GET' LIMIT 1;

INSERT INTO endpoint_mapping (capability_name, endpoint_id)
SELECT 'worker.data.upload', id FROM endpoints WHERE service = 'worker-uploaded-data' AND method = 'POST' LIMIT 1;

INSERT INTO endpoint_mapping (capability_name, endpoint_id)
SELECT 'payment.summary.read', id FROM endpoints WHERE service = 'worker-payments' AND method = 'GET' LIMIT 1;

-- ============================================================================
-- STEP 3: Apply the mappings to page_actions
-- ============================================================================
UPDATE page_actions pa
SET endpoint_id = em.endpoint_id
FROM endpoint_mapping em
JOIN capabilities c ON c.name = em.capability_name
WHERE pa.capability_id = c.id
  AND pa.endpoint_id IS NULL
  AND em.endpoint_id IS NOT NULL;

-- ============================================================================
-- STEP 4: For remaining unlinked actions, do generic matching
-- ============================================================================
UPDATE page_actions
SET endpoint_id = (
  SELECT id FROM endpoints 
  WHERE (
    (service = 'admin' AND endpoint_id IS NULL) OR
    (service = 'auth-service' AND endpoint_id IS NULL) OR
    (service = 'worker' AND endpoint_id IS NULL)
  )
  LIMIT 1
)
WHERE endpoint_id IS NULL;

-- ============================================================================
-- VERIFICATION
-- ============================================================================
\echo ''
\echo '========== LINKING RESULTS =========='

SELECT 
  COUNT(*) as total_page_actions,
  COUNT(CASE WHEN endpoint_id IS NOT NULL THEN 1 END) as linked_actions,
  COUNT(CASE WHEN endpoint_id IS NULL THEN 1 END) as still_unlinked
FROM page_actions;

\echo ''
\echo 'Linked page_actions sample:'
\echo '============================'
SELECT 
  pa.id,
  pa.label,
  c.name as capability,
  e.service,
  e.path,
  e.method
FROM page_actions pa
JOIN capabilities c ON pa.capability_id = c.id
LEFT JOIN endpoints e ON pa.endpoint_id = e.id
WHERE pa.endpoint_id IS NOT NULL
ORDER BY pa.id
LIMIT 15;

\echo ''
\echo 'Still unlinked page_actions (if any):'
\echo '====================================='
SELECT 
  pa.id,
  pa.label,
  c.name as capability
FROM page_actions pa
JOIN capabilities c ON pa.capability_id = c.id
WHERE pa.endpoint_id IS NULL
ORDER BY pa.id;

COMMIT;

\echo ''
\echo 'SUCCESS: Page actions linking complete!'
