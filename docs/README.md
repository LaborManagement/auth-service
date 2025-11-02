# Auth Service Documentation

Use this index to find the right guide quickly. Files listed below already contain the detailed instructions—no extra summaries here.

## Folder Guide

### ONBOARDING
- `ONBOARDING/README.md` – orientation checklist and glossary
- `ONBOARDING/SETUP_GUIDE.md` – local environment and service bootstrap
- `ONBOARDING/ARCHITECTURE.md` – platform and communication overview
- `ONBOARDING/ROLES.md` – business personas and ownership
- `ONBOARDING/setup/` – ordered SQL bootstrap scripts with a `README.md`

### RBAC
- `RBAC/README.md` – RBAC scope, how the docs are organised, and quick start tasks
- `RBAC/ARCHITECTURE.md` – service-level auth flow and integration notes
- `RBAC/SETUP_GUIDE.md` – database and service configuration steps
- `RBAC/ROLES.md` – condensed permission reference per platform role
- `RBAC/DEFINITIONS/` – UI page catalogue and capability naming
- `RBAC/MAPPINGS/` – policy-to-capability and endpoint mappings
- `RBAC/testing.md` / `RBAC/troubleshoot.md` – verification and issue playbooks
- `RBAC/setup/` – utility SQL (currently `bootstrap_user_seed.sql`)

### VPD
- `VPD/README.md` – row-level security model, setup checkpoints, and testing plan
- `VPD/testing/` – SQL snippets to validate RLS behaviour

### POSTGRES
- `POSTGRES/README.md` – operational overview for the shared database
- `POSTGRES/setup.md` – instance creation, credentials, and migrations
- `POSTGRES/testing.md` – health checks and smoke tests
- `POSTGRES/troubleshoot.md` – common faults and fixes

## Recommended Path
1. New engineers start with `ONBOARDING/README.md`, then follow the setup guide.
2. Implementers of auth review `RBAC/README.md` and the architecture note before touching code.
3. Anyone adjusting row-level filters should read `VPD/README.md` prior to running the SQL in `ONBOARDING/setup/`.

## Keeping Docs Fresh
- Update the relevant file when flows or SQL change; avoid duplicating the same table in multiple locations.
- Link across folders instead of copying long explanations.
- When in doubt, prefer adding detail to a focused doc (e.g. `RBAC/ARCHITECTURE.md`) rather than this index.
