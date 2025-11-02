-- ============================================================================
-- PHASE 3: CREATE POLICIES (7 Total)
-- ============================================================================
-- Purpose: Create 7 policies, one for each role, as the link between roles and capabilities
-- Dependencies: Roles and Capabilities must be created first
-- ============================================================================

\set ON_ERROR_STOP on

-- Clear existing policies (for fresh setup)
TRUNCATE TABLE policies CASCADE;

-- Insert 7 policies, one per role
INSERT INTO policies (name, description, policy_type, is_active, created_at, updated_at) VALUES

(
  'PLATFORM_BOOTSTRAP_POLICY',
  'Bootstrap policy with full system access (55/98 capabilities). Used for one-time system initialization. Grants all RBAC, UI, and system capabilities but no business operations.',
  'SYSTEM_POLICY',
  true,
  NOW(),
  NOW()
),

(
  'ADMIN_TECH_POLICY',
  'Technical administration policy (51/98 capabilities). Grants RBAC configuration, user account management, API endpoint management, UI page management, and system settings access. No business operations access.',
  'SYSTEM_POLICY',
  true,
  NOW(),
  NOW()
),

(
  'ADMIN_OPS_POLICY',
  'Operations administration policy (42/98 capabilities). Grants file ingestion capabilities (MT940/VAN), audit log access, system monitoring, and operational workflow capabilities. Limited RBAC access.',
  'OPERATIONAL_POLICY',
  true,
  NOW(),
  NOW()
),

(
  'BOARD_POLICY',
  'Board member policy (17/98 capabilities). Grants financial approval and reconciliation capabilities. Can view all payment requests, vote on decisions, and provide final approval. No data entry or configuration access.',
  'BUSINESS_POLICY',
  true,
  NOW(),
  NOW()
),

(
  'EMPLOYER_POLICY',
  'Employer staff policy (19/98 capabilities). Grants request validation, payment approval/rejection, and employer-level payment operations. Organization-scoped via VPD (UserTenantAcl). Cannot see other organizations'' data.',
  'BUSINESS_POLICY',
  true,
  NOW(),
  NOW()
),

(
  'WORKER_POLICY',
  'Worker/employee policy (14/98 capabilities). Grants payment data upload, request creation/submission, and receipt management. User-scoped via VPD (UserTenantAcl) - can only see own data. Cannot approve payments.',
  'BUSINESS_POLICY',
  true,
  NOW(),
  NOW()
),

(
  'TEST_USER_POLICY',
  'QA/Testing policy (50/98 capabilities). Comprehensive testing access covering most capabilities for validation workflows. Excludes destructive operations and dangerous system commands.',
  'TEST_POLICY',
  true,
  NOW(),
  NOW()
);

-- Verify creation
SELECT 
  name, 
  description,
  policy_type,
  is_active,
  created_at
FROM policies 
ORDER BY created_at;

-- Verify count
SELECT COUNT(*) as total_policies FROM policies WHERE is_active = true;

COMMIT;
