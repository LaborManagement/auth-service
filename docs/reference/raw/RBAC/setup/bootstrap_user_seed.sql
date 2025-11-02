-- Bootstrap setup script
-- ============================================================================
-- Creates a PLATFORM_BOOTSTRAP role, a companion bootstrap user, and links
-- them together so the team can begin catalog seeding immediately after a
-- deployment. Run this against the auth-service database once the schema
-- migrations have executed.
--
-- Default credentials (change immediately after first login):
--   username: platform.bootstrap
--   email:    platform.bootstrap@lbe.local
--   password: Platform!Bootstrap1
--
-- The password hash below was generated with BCrypt (cost 12) to match the
-- Spring Security encoder in SecurityConfig.java.
--
-- USAGE:
--   psql -U postgres -d <database> -f bootstrap_user_seed.sql
-- ============================================================================

SET @now := NOW();
SET @bootstrapPassword := '$2b$12$ABgKvrzZNrOVlOkKOvzBAuSChaCz/16C8lkWSxuOGf/BIKuZz7vFG';

-- 1. Ensure the bootstrap role exists
INSERT INTO roles (name, description, is_active, created_at, updated_at)
SELECT 'PLATFORM_BOOTSTRAP',
       'Bootstrap role with full administrative privileges for initial catalog setup',
       1,
       @now,
       @now
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE name = 'PLATFORM_BOOTSTRAP'
);

-- Keep description/active flag up to date if the role already existed
UPDATE roles
SET description = 'Bootstrap role with full administrative privileges for initial catalog setup',
    is_active   = 1,
    updated_at  = @now
WHERE name = 'PLATFORM_BOOTSTRAP';

-- 1a. Ensure a classic ADMIN role exists for PreAuthorize checks
INSERT INTO roles (name, description, is_active, created_at, updated_at)
SELECT 'ADMIN',
       'System administrator role with legacy ADMIN authority',
       1,
       @now,
       @now
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE name = 'ADMIN'
);

UPDATE roles
SET description = 'System administrator role with legacy ADMIN authority',
    is_active   = 1,
    updated_at  = @now
WHERE name = 'ADMIN';

-- Get the role IDs for assignment
SET @bootstrapRoleId := (
    SELECT id FROM roles WHERE name = 'PLATFORM_BOOTSTRAP' LIMIT 1
);
SET @adminRoleId := (
    SELECT id FROM roles WHERE name = 'ADMIN' LIMIT 1
);

-- 2. Ensure the bootstrap user exists
SET @existingBootstrapUserId := (
    SELECT id FROM users WHERE username = 'platform.bootstrap' LIMIT 1
);

INSERT INTO users (
    username,
    email,
    password,
    full_name,
    permission_version,
    role,
    is_enabled,
    is_account_non_expired,
    is_account_non_locked,
    is_credentials_non_expired,
    created_at,
    updated_at,
    last_login
)
SELECT 'platform.bootstrap',
       'platform.bootstrap@lbe.local',
       @bootstrapPassword,
       'Platform Bootstrap',
       1,
       'ADMIN',
       1,
       1,
       1,
       1,
       @now,
       @now,
       NULL
FROM DUAL
WHERE @existingBootstrapUserId IS NULL;

SET @bootstrapUserId := (
    SELECT id FROM users WHERE username = 'platform.bootstrap' LIMIT 1
);

-- 3. Assign roles to user
INSERT INTO user_role_assignment (user_id, role_id, assigned_at)
SELECT @bootstrapUserId, @bootstrapRoleId, @now
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM user_role_assignment WHERE user_id = @bootstrapUserId AND role_id = @bootstrapRoleId
);

INSERT INTO user_role_assignment (user_id, role_id, assigned_at)
SELECT @bootstrapUserId, @adminRoleId, @now
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM user_role_assignment WHERE user_id = @bootstrapUserId AND role_id = @adminRoleId
);

-- 4. Final verification
SELECT 'Bootstrap setup complete!' as status;
SELECT id, username, email FROM users WHERE username = 'platform.bootstrap';
SELECT r.name FROM user_role_assignment ura
JOIN roles r ON ura.role_id = r.id
WHERE ura.user_id = @bootstrapUserId;
