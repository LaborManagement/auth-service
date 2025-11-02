SET search_path TO auth;
\set ON_ERROR_STOP on

BEGIN;

-- ============================================================================
-- PHASE 14: LINK CRITICAL ENDPOINTS TO BASIC_POLICY
-- ============================================================================
-- Purpose: Link 5 critical authorization endpoints to BASIC_POLICY
-- These endpoints must be accessible to ALL authenticated users (BASIC_USER role)
-- BASIC_POLICY ID: 9 (created in Phase 12)
--
-- Critical Endpoints:
--   1. /api/auth/authorization (ID: 5) - Check user permissions
--   2. /api/me/authorizations (ID: 184) - Get user's full authorization data
--   3. /api/meta/service-catalog (ID: 185) - Service/capability discovery
--   4. /api/meta/endpoints (ID: 186) - API endpoint metadata
--   5. /api/meta/pages (ID: 187) - UI pages metadata
-- ============================================================================

-- Clear any existing endpoint-policy links for these endpoints
DELETE FROM endpoint_policies 
WHERE endpoint_id IN (5, 184, 185, 186, 187);

-- Link all 5 critical endpoints to BASIC_POLICY (ID: 9)
INSERT INTO endpoint_policies (endpoint_id, policy_id) VALUES (5, 9);
INSERT INTO endpoint_policies (endpoint_id, policy_id) VALUES (184, 9);
INSERT INTO endpoint_policies (endpoint_id, policy_id) VALUES (185, 9);
INSERT INTO endpoint_policies (endpoint_id, policy_id) VALUES (186, 9);
INSERT INTO endpoint_policies (endpoint_id, policy_id) VALUES (187, 9);

-- Verify all links created correctly
SELECT 
  ep.id,
  ep.method,
  ep.path,
  p.name as policy_name,
  p.id as policy_id
FROM endpoint_policies epp
JOIN endpoints ep ON epp.endpoint_id = ep.id
JOIN policies p ON epp.policy_id = p.id
WHERE ep.id IN (5, 184, 185, 186, 187)
ORDER BY ep.id;

COMMIT;
