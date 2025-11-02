-- ============================================================================
-- PHASE 5: CREATE SEED USERS (7 Total)
-- ============================================================================
-- Purpose: Create one test user for each role for initial testing and demonstration
-- Dependencies: Users table must exist
-- ============================================================================

\set ON_ERROR_STOP on

-- Clear existing test users (optional - only if doing fresh setup)
-- DELETE FROM users WHERE username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user');

-- Insert 7 seed users, one per role
INSERT INTO users (username, email, password, is_active, created_at, updated_at) 
VALUES 
(
  'platform.bootstrap',
  'bootstrap@system.local',
  '$2a$10$slYQmyNdGzin7olVN3p5Be0DlH.PKZbv5H8KnzzVgXXbVxzy990qm',  -- encrypted password
  true,
  NOW(),
  NOW()
)
ON CONFLICT (username) DO UPDATE SET updated_at = NOW();

INSERT INTO users (username, email, password, is_active, created_at, updated_at) 
VALUES 
(
  'admin.tech',
  'admin.tech@system.local',
  '$2a$10$slYQmyNdGzin7olVN3p5Be0DlH.PKZbv5H8KnzzVgXXbVxzy990qm',  -- encrypted password
  true,
  NOW(),
  NOW()
)
ON CONFLICT (username) DO UPDATE SET updated_at = NOW();

INSERT INTO users (username, email, password, is_active, created_at, updated_at) 
VALUES 
(
  'admin.ops',
  'admin.ops@system.local',
  '$2a$10$slYQmyNdGzin7olVN3p5Be0DlH.PKZbv5H8KnzzVgXXbVxzy990qm',  -- encrypted password
  true,
  NOW(),
  NOW()
)
ON CONFLICT (username) DO UPDATE SET updated_at = NOW();

INSERT INTO users (username, email, password, is_active, created_at, updated_at) 
VALUES 
(
  'board1',
  'board1@company.local',
  '$2a$10$slYQmyNdGzin7olVN3p5Be0DlH.PKZbv5H8KnzzVgXXbVxzy990qm',  -- encrypted password
  true,
  NOW(),
  NOW()
)
ON CONFLICT (username) DO UPDATE SET updated_at = NOW();

INSERT INTO users (username, email, password, is_active, created_at, updated_at) 
VALUES 
(
  'employer1',
  'employer1@company.local',
  '$2a$10$slYQmyNdGzin7olVN3p5Be0DlH.PKZbv5H8KnzzVgXXbVxzy990qm',  -- encrypted password
  true,
  NOW(),
  NOW()
)
ON CONFLICT (username) DO UPDATE SET updated_at = NOW();

INSERT INTO users (username, email, password, is_active, created_at, updated_at) 
VALUES 
(
  'worker1',
  'worker1@company.local',
  '$2a$10$slYQmyNdGzin7olVN3p5Be0DlH.PKZbv5H8KnzzVgXXbVxzy990qm',  -- encrypted password
  true,
  NOW(),
  NOW()
)
ON CONFLICT (username) DO UPDATE SET updated_at = NOW();

INSERT INTO users (username, email, password, is_active, created_at, updated_at) 
VALUES 
(
  'test.user',
  'test.user@system.local',
  '$2a$10$slYQmyNdGzin7olVN3p5Be0DlH.PKZbv5H8KnzzVgXXbVxzy990qm',  -- encrypted password
  true,
  NOW(),
  NOW()
)
ON CONFLICT (username) DO UPDATE SET updated_at = NOW();

-- Verify creation
SELECT username, email, is_active, created_at FROM users 
WHERE username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user')
ORDER BY username;

-- Verify count
SELECT COUNT(*) as total_seed_users FROM users 
WHERE username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user');

COMMIT;
