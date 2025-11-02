# Auth Service Journey

Start here and follow the sequence—each document ends with a pointer to the next stop so you never lose the thread. Detours (welcome stories, primers, raw tables) are still available if you need them, but the core onboarding path runs straight through the files below.

## Primary Path

1. **Architecture Overview** – `architecture/overview.md`
   - High-level topology, authentication touchpoints, and enforcement layers.
2. **Data Map** – `architecture/data-map.md`
   - ER diagram linking users, roles, policies, capabilities, endpoints, and tenant ACL.
3. **Journey: Login To Data** – `guides/login-to-data.md`
   - Persona-led walkthrough of JWT issuance, validation, authorization, and RLS.
4. **RBAC Setup Playbook** – `guides/setup/rbac.md`
   - Exact sequence for creating roles, policies, capabilities, endpoints, and UI bindings.
5. **VPD Setup Playbook** – `guides/setup/vpd.md`
   - How to wire tenant-aware row-level security and test contrasting users.
6. **Reference Loop** – begin at `reference/role-catalog.md`
   - Concise catalogues (roles → capabilities → policies → VPD → operations) with links back to raw data.

## Supporting Material

- `start/` – Story-driven onboarding for brand-new teammates (optional pre-read).
- `foundations/` – Plain-language primers on RBAC, data guardrails, and PostgreSQL duties.
- `playbooks/troubleshoot-auth.md` – Symptom-based fixes when something misbehaves.
- `reference/raw/` – Original detailed documents, migration notes, and exhaustive matrices.
- `onboarding/setup/` – SQL bootstrap scripts (kept intact); run them alongside the setup playbooks.

## Keeping Docs In Sync

- Update the primary path first; ensure each doc’s “Next” pointer stays accurate.
- Link to reference summaries instead of duplicating tables.
- When schemas or capabilities change, refresh both the playbooks and the relevant reference sheet.
