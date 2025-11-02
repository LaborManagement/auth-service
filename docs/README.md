# Auth Service Journey

Follow this path step by step—each document links directly to the next so you always know where to go.

## Guided Path

1. **Step 1 · [Architecture Overview](architecture/overview.md)**  
   Meet every component and see how authentication and authorization fit together.  
   → Next: **Step 2 · [Data Map](architecture/data-map.md)**
2. **Step 2 · [Data Map](architecture/data-map.md)**  
   Understand how tables relate (users → roles → policies → capabilities → endpoints → tenant ACL).  
   → Next: **Step 3 · [Journey: Login To Data](guides/login-to-data.md)**
3. **Step 3 · [Journey: Login To Data](guides/login-to-data.md)**  
   Follow worker, employer, and board personas from login through JWT validation, authorization, and RLS.  
   → Next: **Step 4 · [RBAC Setup Playbook](guides/setup/rbac.md)**
4. **Step 4 · [RBAC Setup Playbook](guides/setup/rbac.md)**  
   Create roles, policies, capabilities, endpoints, and UI wiring in the correct order.  
   → Next: **Step 5 · [VPD Setup Playbook](guides/setup/vpd.md)**
5. **Step 5 · [VPD Setup Playbook](guides/setup/vpd.md)**  
   Configure row-level security, load tenant ACL, and test contrasting users.  
   → Next: **Step 6 · [Role Catalog](reference/role-catalog.md)**
6. **Step 6 · [Role Catalog](reference/role-catalog.md)**  
   Drop into the concise references (roles → capabilities → policies → VPD → operations) with links into raw data.  
   → Continue through the reference loop:  
   `[Capability Catalog](reference/capability-catalog.md)` → `[Policy Matrix](reference/policy-matrix.md)` → `[VPD Checklist](reference/vpd-checklist.md)` → `[PostgreSQL Operations](reference/postgres-operations.md)` → `[Raw Reference Index](reference/raw/README.md)`

## Optional Companions

- **Story Prelude** – `start/welcome.md`, `start/platform-tour.md`, `start/role-stories.md`
- **Concept Primers** – `foundations/access-control-101.md`, `foundations/data-guardrails-101.md`, `foundations/postgres-for-auth.md`
- **Troubleshooting** – `playbooks/troubleshoot-auth.md`
- **Bootstrap SQL** – `onboarding/setup/` (run alongside Steps 4 and 5)

## Keeping Docs In Sync

- Update the guided path first and verify each “Next” link.
- Surface new details in the reference summaries; point to `reference/raw/` for exhaustive tables.
- When schemas or capabilities change, adjust the playbooks and matching reference sheet together.
