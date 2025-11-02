# RBAC Documentation Hub

Everything in this folder explains how access control is structured in the auth service. Each file has a single job—use the index below to reach the right depth and avoid re-reading the same content elsewhere.

## What Lives Here
- `ARCHITECTURE.md` – request flow, enforcement layers, integration points.
- `SETUP_GUIDE.md` – applying the SQL, wiring Spring Security, and validating the rollout.
- `NOVICE_GUIDE.md` – beginner-friendly RBAC primer without implementation dependencies.
- `ROLES.md` – concise view of the seven production roles and their scope.
- `DEFINITIONS/` – source of truth for capability names and UI page inventory.
- `MAPPINGS/` – endpoint, policy, and capability relationships.
- `testing.md` – SQL and API checks to confirm RBAC decisions.
- `troubleshoot.md` – symptoms, probable causes, and fixes.
- `setup/bootstrap_user_seed.sql` – helper script for seeding local data.

VPD/RLS specifics live in `../VPD/README.md`. RBAC docs only reference VPD to explain when to jump to that file.

## Quick Start
- **Understand the model** – read `ARCHITECTURE.md` for the three enforcement layers and token flow.
- **Provision data** – run the scripts listed in `ONBOARDING/setup/README.md`, then apply any local extras from `RBAC/setup/`.
- **Check your work** – execute the SQL snippets in `testing.md` before exposing endpoints.

## Picking the Right Reference
| Need to know | Open this file | Why |
| --- | --- | --- |
| Which role can perform an action | `ROLES.md` | compact summary of grants and data scope |
| The capability name for a UI element | `DEFINITIONS/PHASE2_UI_PAGES_ACTIONS.md` | page-to-capability mapping |
| The policy protecting an endpoint | `MAPPINGS/PHASE5_ENDPOINT_POLICY_MAPPINGS.md` | endpoint matrix |
| How capabilities attach to policies | `MAPPINGS/PHASE4_POLICY_CAPABILITY_MAPPINGS.md` | full join table |
| What happens during a request | `ARCHITECTURE.md` | sequence diagram plus integration notes |

## Editing Expectations
- Update the single authoritative file; link to it from summaries instead of copying tables.
- Keep role counts, capability totals, and similar stats in `ROLES.md` or the mapping files (not in multiple places).
- When VPD behaviour is involved, describe the high-level trigger here and delegate the mechanics to the VPD docs.

Questions while implementing? Start with `troubleshoot.md` for errors or `testing.md` to confirm behaviour, then dive deeper via the links above.
