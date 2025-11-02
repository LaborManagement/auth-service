-- ============================================================================
-- VPD (VIRTUAL PRIVATE DATA) CONFIGURATION SCRIPT
-- ============================================================================
-- Purpose: Configure Row-Level Security (RLS) for data isolation
--          based on user roles and board/employer boundaries
-- 
-- Table Structure: auth.user_tenant_acl
-- Columns:
--   - id: Primary key (auto-increment)
--   - user_id: User identifier (FK to users.id, required)
--   - board_id: Board identifier (String, 64 chars, required)
--   - employer_id: Employer identifier (String, 64 chars, nullable)
--   - can_read: Read permission (Boolean, default true)
--   - can_write: Write permission (Boolean, default false)
--   - created_at: Creation timestamp
--   - updated_at: Last update timestamp
--
-- Unique Constraint: (user_id, board_id, employer_id)
-- 
-- VPD Configuration Patterns:
--   WORKER:   (user_id, board_id=BOARD-DEFAULT, employer_id=EMP-001, can_read=true, can_write=false)
--   EMPLOYER: (user_id, board_id=BOARD-DEFAULT, employer_id=EMP-001, can_read=true, can_write=true)
--   BOARD:    (user_id, board_id=BOARD-DEFAULT, employer_id=NULL, can_read=true, can_write=true)
--   ADMIN:    (user_id, board_id=BOARD-DEFAULT, employer_id=NULL, can_read=true, can_write=true)
--
-- Dependencies: Users and roles must be created first
-- ============================================================================

SET search_path TO auth;
\set ON_ERROR_STOP on

BEGIN;

-- ============================================================================
-- BACKUP EXISTING VPD ENTRIES FOR TARGET USERS
-- ============================================================================
CREATE TABLE IF NOT EXISTS auth.user_tenant_acl_backup AS
SELECT uta.*, NOW()::timestamp AS backup_created_at
FROM auth.user_tenant_acl uta
WHERE false;

ALTER TABLE auth.user_tenant_acl_backup
    ADD COLUMN IF NOT EXISTS backup_created_at timestamp without time zone;

INSERT INTO auth.user_tenant_acl_backup
SELECT uta.*, NOW()::timestamp
FROM auth.user_tenant_acl uta
WHERE uta.user_id IN (
  SELECT id FROM users WHERE username IN ('worker1', 'employer1', 'board1', 'admin.tech', 'admin.ops')
);
-- ============================================================================
-- SECTION 1: CONFIGURE WORKER VPD
-- ============================================================================
-- WORKER role users:
-- - Access: Read-only (can_read=true, can_write=false)
-- - Scope: Specific employer (employer_id=EMP-001)
-- - Also sees board-level data
-- ============================================================================

\echo 'Configuring WORKER VPD...'

-- Clear any existing WORKER VPD entries (fresh setup)
DELETE FROM user_tenant_acl 
WHERE user_id IN (SELECT id FROM users WHERE username = 'worker1');

-- Configure worker1: read-only access to assigned employer
INSERT INTO user_tenant_acl (user_id, board_id, employer_id, can_read, can_write, created_at, updated_at)
SELECT 
  u.id,
  'BOARD-DEFAULT',        -- Board identifier
  'EMP-001',              -- Employer identifier
  true,                   -- can_read
  false,                  -- can_write (workers are read-only)
  NOW(),
  NOW()
FROM users u
WHERE u.username = 'worker1'
ON CONFLICT (user_id, board_id, employer_id) DO UPDATE 
  SET can_read = true, can_write = false, updated_at = NOW();

-- Verify WORKER VPD
\echo 'WORKER VPD configured for:'
SELECT 
  u.username,
  r.name as role,
  uta.board_id,
  uta.employer_id,
  CASE WHEN uta.can_read AND uta.can_write THEN 'READ+WRITE'
       WHEN uta.can_read THEN 'READ ONLY'
       ELSE 'NO ACCESS' END as access_level,
  uta.created_at
FROM user_tenant_acl uta
JOIN users u ON uta.user_id = u.id
JOIN user_roles ura ON u.id = ura.user_id
JOIN roles r ON ura.role_id = r.id
WHERE u.username = 'worker1';

\echo 'WORKER VPD Configured ✅'
\echo ''

-- ============================================================================
-- SECTION 2: CONFIGURE EMPLOYER VPD
-- ============================================================================
-- EMPLOYER role users:
-- - Access: Read+Write (can_read=true, can_write=true)
-- - Scope: Specific employer (employer_id=EMP-001)
-- - Can manage employer's data
-- ============================================================================

\echo 'Configuring EMPLOYER VPD...'

-- Clear any existing EMPLOYER VPD entries (fresh setup)
DELETE FROM user_tenant_acl 
WHERE user_id IN (SELECT id FROM users WHERE username = 'employer1');

-- Configure employer1: full access to assigned employer
INSERT INTO user_tenant_acl (user_id, board_id, employer_id, can_read, can_write, created_at, updated_at)
SELECT 
  u.id,
  'BOARD-DEFAULT',        -- Board identifier
  'EMP-001',              -- Employer identifier
  true,                   -- can_read
  true,                   -- can_write (employers can read+write)
  NOW(),
  NOW()
FROM users u
WHERE u.username = 'employer1'
ON CONFLICT (user_id, board_id, employer_id) DO UPDATE 
  SET can_read = true, can_write = true, updated_at = NOW();

-- Verify EMPLOYER VPD
\echo 'EMPLOYER VPD configured for:'
SELECT 
  u.username,
  r.name as role,
  uta.board_id,
  uta.employer_id,
  CASE WHEN uta.can_read AND uta.can_write THEN 'READ+WRITE'
       WHEN uta.can_read THEN 'READ ONLY'
       ELSE 'NO ACCESS' END as access_level,
  uta.created_at
FROM user_tenant_acl uta
JOIN users u ON uta.user_id = u.id
JOIN user_roles ura ON u.id = ura.user_id
JOIN roles r ON ura.role_id = r.id
WHERE u.username = 'employer1';

\echo 'EMPLOYER VPD Configured ✅'
\echo ''

-- ============================================================================
-- SECTION 3: CONFIGURE BOARD VPD
-- ============================================================================
-- BOARD role users:
-- - Access: Full access (can_read=true, can_write=true)
-- - Scope: Board-level only (employer_id=NULL, no employer restriction)
-- - Can see and manage board-level data
-- ============================================================================

\echo 'Configuring BOARD VPD...'

-- Clear any existing BOARD VPD entries (fresh setup)
DELETE FROM user_tenant_acl 
WHERE user_id IN (SELECT id FROM users WHERE username = 'board1');

-- Configure board1: full access at board level
INSERT INTO user_tenant_acl (user_id, board_id, employer_id, can_read, can_write, created_at, updated_at)
SELECT 
  u.id,
  'BOARD-DEFAULT',        -- Board identifier
  NULL,                   -- No employer restriction
  true,                   -- can_read
  true,                   -- can_write (board has full permissions)
  NOW(),
  NOW()
FROM users u
WHERE u.username = 'board1'
ON CONFLICT (user_id, board_id, employer_id) DO UPDATE 
  SET can_read = true, can_write = true, updated_at = NOW();

-- Verify BOARD VPD
\echo 'BOARD VPD configured for:'
SELECT 
  u.username,
  r.name as role,
  uta.board_id,
  uta.employer_id,
  CASE WHEN uta.can_read AND uta.can_write THEN 'READ+WRITE'
       WHEN uta.can_read THEN 'READ ONLY'
       ELSE 'NO ACCESS' END as access_level,
  uta.created_at
FROM user_tenant_acl uta
JOIN users u ON uta.user_id = u.id
JOIN user_roles ura ON u.id = ura.user_id
JOIN roles r ON ura.role_id = r.id
WHERE u.username = 'board1';

\echo 'BOARD VPD Configured ✅'
\echo ''

-- ============================================================================
-- SECTION 4: CONFIGURE ADMIN VPD
-- ============================================================================
-- ADMIN roles (ADMIN_TECH, ADMIN_OPS):
-- - Access: Full access (can_read=true, can_write=true)
-- - Scope: Board-level (employer_id=NULL)
-- - Administrative access to all data
-- ============================================================================

\echo 'Configuring ADMIN VPD...'

-- Clear any existing ADMIN VPD entries (fresh setup)
DELETE FROM user_tenant_acl 
WHERE user_id IN (SELECT id FROM users WHERE username IN ('admin.tech', 'admin.ops'));

-- Configure admins: full board-level access
INSERT INTO user_tenant_acl (user_id, board_id, employer_id, can_read, can_write, created_at, updated_at)
SELECT 
  u.id,
  'BOARD-DEFAULT',        -- Board identifier
  NULL,                   -- No employer restriction
  true,                   -- can_read
  true,                   -- can_write (admins have full permissions)
  NOW(),
  NOW()
FROM users u
WHERE u.username IN ('admin.tech', 'admin.ops')
ON CONFLICT (user_id, board_id, employer_id) DO UPDATE 
  SET can_read = true, can_write = true, updated_at = NOW();

-- Verify ADMIN VPD
\echo 'ADMIN VPD configured for:'
SELECT 
  u.username,
  r.name as role,
  uta.board_id,
  uta.employer_id,
  CASE WHEN uta.can_read AND uta.can_write THEN 'READ+WRITE'
       WHEN uta.can_read THEN 'READ ONLY'
       ELSE 'NO ACCESS' END as access_level,
  uta.created_at
FROM user_tenant_acl uta
JOIN users u ON uta.user_id = u.id
JOIN user_roles ura ON u.id = ura.user_id
JOIN roles r ON ura.role_id = r.id
WHERE u.username IN ('admin.tech', 'admin.ops');

\echo 'ADMIN VPD Configured ✅'
\echo ''

-- ============================================================================
-- SECTION 5: VPD COMPLETE SUMMARY
-- ============================================================================
\echo 'VPD Configuration Summary:'
\echo '=========================='
\echo ''

SELECT 
  u.username,
  r.name as role,
  uta.board_id,
  uta.employer_id,
  CASE WHEN uta.can_read AND uta.can_write THEN 'READ+WRITE'
       WHEN uta.can_read THEN 'READ ONLY'
       ELSE 'NO ACCESS' END as access_level,
  uta.created_at
FROM user_tenant_acl uta
JOIN users u ON uta.user_id = u.id
JOIN user_roles ura ON u.id = ura.user_id
JOIN roles r ON ura.role_id = r.id
WHERE u.username IN ('worker1', 'employer1', 'board1', 'admin.tech', 'admin.ops')
ORDER BY r.name, u.username;

\echo ''

-- ============================================================================
-- SECTION 6: VPD TABLE STRUCTURE REFERENCE
-- ============================================================================
\echo 'VPD Table Structure Reference:'
\echo '=============================='
\echo ''
\echo 'Table: auth.user_tenant_acl'
\echo ''
\echo 'Columns:'
\echo '  - id: Primary key (auto-increment)'
\echo '  - user_id: User identifier (FK to users.id)'
\echo '  - board_id: Board identifier (String, 64 chars)'
\echo '  - employer_id: Employer identifier (String, 64 chars, nullable)'
\echo '  - can_read: Read permission (Boolean, default true)'
\echo '  - can_write: Write permission (Boolean, default false)'
\echo '  - created_at: Creation timestamp'
\echo '  - updated_at: Last update timestamp'
\echo ''
\echo 'Unique Constraint: (user_id, board_id, employer_id)'
\echo ''
\echo 'Usage Patterns:'
\echo '  WORKER:   (user_id, board_id=BOARD-DEFAULT, employer_id=EMP-001, can_read=true, can_write=false)'
\echo '  EMPLOYER: (user_id, board_id=BOARD-DEFAULT, employer_id=EMP-001, can_read=true, can_write=true)'
\echo '  BOARD:    (user_id, board_id=BOARD-DEFAULT, employer_id=NULL, can_read=true, can_write=true)'
\echo '  ADMIN:    (user_id, board_id=BOARD-DEFAULT, employer_id=NULL, can_read=true, can_write=true)'
\echo ''

-- ============================================================================
-- SECTION 7: VERIFICATION QUERIES
-- ============================================================================
\echo 'VPD Verification Queries:'
\echo '========================'
\echo ''

-- Query 1: All VPD entries
\echo 'Query 1: Total VPD Entries'
SELECT 
  COUNT(*) as total_entries,
  COUNT(DISTINCT user_id) as unique_users,
  COUNT(DISTINCT board_id) as boards,
  COUNT(DISTINCT CASE WHEN employer_id IS NOT NULL THEN employer_id END) as employers
FROM user_tenant_acl;

\echo ''

-- Query 2: Entries by access level
\echo 'Query 2: Access Level Distribution'
SELECT 
  CASE 
    WHEN can_read AND can_write THEN 'READ+WRITE'
    WHEN can_read THEN 'READ ONLY'
    ELSE 'NO ACCESS'
  END as access_level,
  COUNT(*) as count
FROM user_tenant_acl
GROUP BY access_level;

\echo ''

-- Query 3: VPD by role
\echo 'Query 3: VPD Entries by Role'
SELECT 
  r.name as role,
  COUNT(DISTINCT u.id) as users,
  COUNT(uta.id) as vpd_entries,
  STRING_AGG(DISTINCT u.username, ', ') as usernames
FROM user_tenant_acl uta
JOIN users u ON uta.user_id = u.id
JOIN user_roles ura ON u.id = ura.user_id
JOIN roles r ON ura.role_id = r.id
GROUP BY r.id, r.name
ORDER BY r.name;

\echo ''

-- ============================================================================
-- SECTION 8: IMPORTANT NOTES
-- ============================================================================
\echo 'IMPORTANT NOTES:'
\echo '================'
\echo ''
\echo '1. BOARD_ID AND EMPLOYER_ID:'
\echo '   - board_id: Identifies the board/entity (required, e.g., BOARD-DEFAULT)'
\echo '   - employer_id: Identifies the employer (nullable, e.g., EMP-001)'
\echo '   - Replace placeholder values with actual identifiers from your system'
\echo ''
\echo '2. PERMISSION MATRIX:'
\echo '   - can_read=true, can_write=false: READ ONLY (workers)'
\echo '   - can_read=true, can_write=true: READ+WRITE (employers, board, admins)'
\echo '   - can_read=false, can_write=false: NO ACCESS (restricted users)'
\echo ''
\echo '3. DATA ISOLATION:'
\echo '   - WORKER: Sees assigned employer + board data (read-only)'
\echo '   - EMPLOYER: Sees assigned employer + board data (read+write)'
\echo '   - BOARD: Sees board-level data (full access)'
\echo '   - ADMIN: Full access (board-level entries for tracking)'
\echo ''
\echo '4. UNIQUE CONSTRAINT:'
\echo '   - (user_id, board_id, employer_id) prevents duplicate entries'
\echo '   - NULL employer_id allows multiple board entries per user'
\echo ''
\echo '5. RECOMMENDED INDEXES:'
\echo '   - CREATE INDEX idx_user_board ON user_tenant_acl(user_id, board_id);'
\echo '   - CREATE INDEX idx_user_employer ON user_tenant_acl(user_id, employer_id);'
\echo ''
\echo '6. PRODUCTION SETUP:'
\echo '   - Replace BOARD-DEFAULT with actual board identifiers'
\echo '   - Replace EMP-001 with actual employer identifiers'
\echo '   - Add indexes for optimal query performance'
\echo '   - Monitor VPD query performance'
\echo ''

-- ============================================================================
-- SECTION 9: TEMPLATE FOR ADDING NEW USERS TO VPD
-- ============================================================================
\echo 'TEMPLATE: Adding New Users to VPD'
\echo '=================================='
\echo ''
\echo 'For WORKER role:'
\echo '  INSERT INTO user_tenant_acl (user_id, board_id, employer_id, can_read, can_write, created_at, updated_at)'
\echo '  VALUES (<user_id>, ''BOARD-DEFAULT'', ''EMP-XXX'', true, false, NOW(), NOW());'
\echo ''
\echo 'For EMPLOYER role:'
\echo '  INSERT INTO user_tenant_acl (user_id, board_id, employer_id, can_read, can_write, created_at, updated_at)'
\echo '  VALUES (<user_id>, ''BOARD-DEFAULT'', ''EMP-XXX'', true, true, NOW(), NOW());'
\echo ''
\echo 'For BOARD role:'
\echo '  INSERT INTO user_tenant_acl (user_id, board_id, employer_id, can_read, can_write, created_at, updated_at)'
\echo '  VALUES (<user_id>, ''BOARD-DEFAULT'', NULL, true, true, NOW(), NOW());'
\echo ''

-- Final status
\echo ''
\echo '✅ VPD Configuration Complete!'
\echo ''
\echo 'Total VPD Entries Created:'
SELECT COUNT(*) as vpd_entries FROM user_tenant_acl;

COMMIT;
