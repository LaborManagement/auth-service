-- ============================================================================
-- PHASE 1: CREATE ROLES (7 Total)
-- ============================================================================
-- Purpose: Define the 7 core roles with descriptions and capability allocations
-- Dependencies: None (initial schema setup)
-- ============================================================================

-- Enable error handling
\set ON_ERROR_STOP on

-- Clear any existing roles (for fresh setup)
TRUNCATE TABLE roles CASCADE;

-- Insert 7 roles
INSERT INTO roles (name, description, is_active, created_at, updated_at) VALUES
(
  'PLATFORM_BOOTSTRAP',
  'One-time system initialization account. Used only during initial setup. Disabled after bootstrap phase. Grants 56% of capabilities (55/98).',
  true,
  NOW(),
  NOW()
),
(
  'ADMIN_TECH',
  'Technical system administrator. Manages RBAC configuration, user accounts, API endpoints, UI pages, and system settings. No access to business operations. Grants 52% of capabilities (51/98).',
  true,
  NOW(),
  NOW()
),
(
  'ADMIN_OPS',
  'Operations administrator. Manages file ingestion, audit logs, system monitoring, and operational workflows. Can trigger MT940/VAN ingestion. Grants 43% of capabilities (42/98).',
  true,
  NOW(),
  NOW()
),
(
  'BOARD',
  'Board member for financial approvals. Can view payment requests from all employers/workers, perform reconciliation, vote on decisions, and provide final approval. Grants 17% of capabilities (17/98).',
  true,
  NOW(),
  NOW()
),
(
  'EMPLOYER',
  'Employer staff member. Can view worker payment requests, validate submissions, approve/reject, and manage employer-level operations. Organization-scoped via VPD (UserTenantAcl). Grants 19% of capabilities (19/98).',
  true,
  NOW(),
  NOW()
),
(
  'WORKER',
  'Worker/employee account. Can create and submit payment requests, view status, and send receipts. User-scoped via VPD (UserTenantAcl) - sees only own data. Grants 14% of capabilities (14/98).',
  true,
  NOW(),
  NOW()
),
(
  'TEST_USER',
  'QA/Testing account. Comprehensive testing access for validation workflows. Non-destructive operations preferred. Grants 51% of capabilities (50/98).',
  true,
  NOW(),
  NOW()
);

-- Verify creation
SELECT 
  name, 
  description, 
  is_active,
  created_at
FROM roles 
ORDER BY created_at;

COMMIT;
