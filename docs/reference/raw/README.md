# Raw Reference Index

**Navigation:** Previous: `../postgres-operations.md` → Next: `../../playbooks/troubleshoot-auth.md`

This folder keeps the original, exhaustive documentation produced during earlier phases. Use it when you need full tables, historical notes, or migration-ready SQL that the condensed guides summarise.

## What Lives Here

- `RBAC/` – Phase-by-phase capability, policy, endpoint, and UI mappings. Includes the original novice guide and setup scripts used before the consolidated playbooks.
- `VPD/` – Detailed RLS design notes, verification SQL, and troubleshooting snippets.
- `POSTGRES/` – Operational runbooks, testing utilities, and setup helpers for the shared database.
- `ONBOARDING_ARCHITECTURE.md` / `ONBOARDING_ROLES.md` – Legacy orientation docs preserved for context.

## How To Use It

1. Start with the condensed references (`../role-catalog.md`, etc.). If you need the raw data, follow the links within those docs into this directory.
2. When a migration or audit requires the exact SQL, copy from these files instead of retyping by hand.
3. Keep this directory read-only unless you are updating the authoritative tables; the primary narrative lives in the main docs.

## Next Steps

Head to `../../playbooks/troubleshoot-auth.md` for issue-led diagnostics, or return to `../../README.md` to restart the guided path.
