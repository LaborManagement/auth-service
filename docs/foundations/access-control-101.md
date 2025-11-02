# Access Control 101

**Optional Primer:** Read alongside [Journey: Login To Data](../guides/login-to-data.md) before you tackle the RBAC setup in [RBAC Setup Playbook](../guides/setup/rbac.md).

This guide explains role-based access control (RBAC) the way you’d explain it to a friend. No SQL required—just a story about who gets to do what.

## The Theme Park Analogy

Imagine a theme park with different areas: rides, backstage workshops, control rooms, and VIP lounges.

- **Visitors** get wristbands (roles) when they enter.
- **Staff** have job badges with extra permissions.
- Each gate has a **guard** (policy) consulting a rulebook.
- The guard gives or denies entry based on **capabilities** (think “Ride Operator: start rollercoaster”).

The park works smoothly because every ride, door, and control panel has a matching capability. RBAC in our auth service follows the same model.

## Key Ingredients

| Concept | In The Theme Park | In Our Platform |
| --- | --- | --- |
| Role | Wristband color | `ADMIN_OPS`, `EMPLOYER`, etc. |
| Policy | Gate guard with a rulebook | Policy rows in the database |
| Capability | Permission slip | `payment.details.read`, `rbac.policy.edit` |
| Endpoint | Door to a ride | `/api/admin/policies/:id`, controller methods |
| UI Action | Start button on the ride | Approve payment button, menu links |

```mermaid
flowchart TD
    User --> Role
    Role --> Policy
    Policy --> Capability
    Capability --> Endpoint
    Capability --> "UI Action"
```

## Reasoning About A Request

1. **Who are you?** Validate the JWT and collect roles.
2. **What do you need?** Identify which capability the endpoint expects.
3. **Do you have the card?** Check if the role’s policies grant that capability.
4. **Should this button exist?** The UI hides controls unless the capability is present.
5. **Will you see the data?** RLS enforces tenant boundaries even after the endpoint allows the action.

If any step fails, access is denied before the data leaves the database.

## Real-Life Example: Opening The Cash Office

- Only supervisors (ADMIN_OPS role) should open the cash office.
- The capability `cash.office.unlock` is created to describe this action.
- The policy `ADMIN_OPS_POLICY` includes that capability.
- The endpoint `POST /operations/cash/open` requires it.
- The UI button “Open Cash Office” checks the authorization matrix and hides itself for non-supervisors.

Just like a supervisor key, the capability is only issued to trusted staff.

## Designing New Capabilities

1. **Write the job story** in plain language: “An employer can download their payment ledger.”
2. **Name the capability** using `<domain>.<subject>.<action>` → `payment.ledger.download`.
3. **Pick the roles** that should receive it.
4. **Link the endpoint** or page action to the capability.
5. **Verify with contrasting tests**: one user who should pass, one who should fail.

## Where The Data Lives

- Policies, capabilities, and role assignments live in PostgreSQL tables.
- The onboarding scripts (`../ONBOARDING/setup/`) load the initial catalogue.
- Updates typically happen through SQL migrations or dedicated admin APIs.

## What To Read Next

- `foundations/data-guardrails-101.md` to see how RLS backs up RBAC.
- `guides/extend-access.md` for the hands-on steps to add a new capability.
- `reference/role-catalog.md` to check which roles currently hold which cards.
