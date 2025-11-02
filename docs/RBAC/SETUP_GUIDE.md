# RBAC Setup Guide

Use this guide when you need to initialise or refresh the RBAC catalogue in a database. It references the canonical SQL scripts under `../ONBOARDING/setup/`.

## Preconditions

- PostgreSQL 13+ reachable from your workstation.
- `psql` installed and authenticated as a database user with DDL rights.
- Repository checked out so the SQL scripts are available locally.

Quick smoke test:

```bash
psql -U postgres -d postgres -c "SELECT version();"
createdb -U postgres auth_service_db 2>/dev/null || true
```

## Bootstrap Steps

1. Run the ordered scripts documented in `ONBOARDING/setup/README.md` (01–08). They create roles, capabilities, policies, seed users, and VPD helpers.
2. Apply any optional helpers in `RBAC/setup/` such as `bootstrap_user_seed.sql` when you need extra local data.
3. Restart the auth service so it reloads the policy metadata and picks up new database functions.

```mermaid
flowchart LR
    A[Roles] --> B[Capabilities] --> C[Policies] --> D[Policy ↔ Capability Links]
    D --> E[Seed Users] --> F[User ↔ Role Links] --> G[Verification Script] --> H[VPD Setup]
```

## Verification Checklist

- `psql -U postgres -d auth_service_db -f docs/ONBOARDING/setup/07_verify_setup.sql`
- Run the SQL tests in `testing.md` to confirm role and capability grants.
- Call `/api/me/authorizations` with each seed user to ensure the API reflects the database state.
- Execute the RLS smoke tests in `../VPD/testing/` using non-superuser roles.

## If Something Fails

| Symptom | Likely Cause | Fix |
| --- | --- | --- |
| Roles missing | One of scripts 01–03 failed | Rerun 01–03; check psql output for conflicts |
| Capabilities mismatch | Definitions drifted | Re-run 02–04 and compare with `DEFINITIONS/PHASE3_CAPABILITIES_DEFINITION.md` |
| 403 on expected endpoints | Endpoint-policy mapping missing | Review `MAPPINGS/PHASE5_ENDPOINT_POLICY_MAPPINGS.md` and seed updates |
| Empty query results | RLS context not set | Confirm `08_configure_vpd.sql` ran and `RLSContextFilter` is active |

## Post-Bootstrap

1. Disable or rotate the `PLATFORM_BOOTSTRAP` credentials.
2. Update `application.yml` with the database URL, username, and password you used.
3. Document any environment-specific overrides separately; keep this guide environment agnostic.
