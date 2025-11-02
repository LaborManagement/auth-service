# Capability Catalog

**Navigation:** Previous: [Role Catalog](role-catalog.md) → Next: [Policy Matrix](policy-matrix.md)

This catalog explains how capabilities are organised and where to find the full list. Capabilities follow the naming convention `<domain>.<subject>.<action>` and live in the `auth.capability` table.

## High-Level Buckets

| Domain | Purpose | Examples |
| --- | --- | --- |
| `payment` | Worker and employer payment workflows | `payment.request.create`, `payment.details.read`, `payment.ledger.download` |
| `employer` | Employer organisation management | `employer.profile.update`, `employer.user.invite` |
| `worker` | Worker onboarding and documentation | `worker.document.upload`, `worker.timeline.view` |
| `rbac` | Administration of the catalog itself | `rbac.policy.edit`, `rbac.capability.create` |
| `ui` | Visibility of pages and actions | `ui.page.employer.dashboard.view`, `ui.action.board.approve.click` |
| `system` | Cross-cutting operations and reporting | `system.audit.view`, `system.metrics.read` |

## Naming Tips

1. **Domain** – Business area (`payment`, `worker`, `rbac`).
2. **Subject** – Specific entity (`details`, `profile`, `document`).
3. **Action** – Verb in lowercase (`read`, `update`, `download`).

This keeps the catalogue searchable and self-explanatory.

## Adding A Capability

- Use the recipe in [Extend Access Guide](../guides/extend-access.md).
- Document the new capability’s intent in your migration or change log.
- Update front-end authorization checks if a UI element depends on it.

## Retrieving Capabilities

```sql
-- Full list
SELECT name, description
FROM auth.capability
ORDER BY name;

-- Filter by domain
SELECT name
FROM auth.capability
WHERE name LIKE 'payment.%'
ORDER BY name;
```

## Deep Dive

- The complete phase 3 catalogue lives in `raw/RBAC/DEFINITIONS/PHASE3_CAPABILITIES_DEFINITION.md`.
- UI page → capability mapping is in `raw/RBAC/DEFINITIONS/PHASE2_UI_PAGES_ACTIONS.md`.
- Endpoint → policy bindings are tracked in `raw/RBAC/MAPPINGS/`.

Keep this summary handy when naming new permissions or reviewing feature proposals.
