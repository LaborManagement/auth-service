# Policy Matrix

**Navigation:** Previous: [Capability Catalog](capability-catalog.md) → Next: [VPD Checklist](vpd-checklist.md)

Policies link roles to capabilities and endpoints. Use this matrix to understand how decisions flow without wading through every SQL script.

## Policy Overview

| Policy | Activated By Roles | Primary Capabilities | Typical Endpoints |
| --- | --- | --- | --- |
| `PLATFORM_BOOTSTRAP_POLICY` | `PLATFORM_BOOTSTRAP` | Full catalog management, endpoint registration | `/api/admin/*`, `/api/system/*` |
| `ADMIN_TECH_POLICY` | `ADMIN_TECH`, `TEST_USER` | RBAC maintenance, user management, audit viewing | `/api/admin/roles/*`, `/api/admin/policies/*` |
| `ADMIN_OPS_POLICY` | `ADMIN_OPS`, `TEST_USER` | Operational dashboards, exception handling | `/api/ops/*`, `/api/audit/*` |
| `BOARD_POLICY` | `BOARD`, `TEST_USER` | Approval workflows, strategic reporting | `/api/board/*`, `/api/reporting/*` |
| `EMPLOYER_POLICY` | `EMPLOYER`, `TEST_USER` | Employer payment lifecycle actions | `/api/employer/*`, `/api/payment-requests/*` |
| `WORKER_POLICY` | `WORKER`, `TEST_USER` | Worker submissions and tracking | `/api/worker/*`, `/api/uploads/*` |

## Visual Flow

```mermaid
flowchart LR
    Role["Role"]
    Policy["Policy"]
    Capability["Capability"]
    Endpoint["Endpoint"]

    Role --> Policy
    Policy --> Capability
    Capability --> Endpoint
```

- Roles determine which policies activate.
- Policies bundle capabilities.
- Endpoints (and UI actions) require capabilities.

## Useful Queries

```sql
-- Which policies does a role trigger?
SELECT p.name
FROM auth.policy p
JOIN auth.role_policy rp ON rp.policy_id = p.id
JOIN auth.role r ON r.id = rp.role_id
WHERE r.name = 'EMPLOYER';

-- Which endpoints are protected by a policy?
SELECT e.method, e.path
FROM auth.endpoint e
JOIN auth.endpoint_policy ep ON ep.endpoint_id = e.id
JOIN auth.policy p ON p.id = ep.policy_id
WHERE p.name = 'EMPLOYER_POLICY'
ORDER BY e.path;
```

## When Adding Policies

- Policies are rare; most changes only adjust capabilities within existing policies.
- If a brand-new persona emerges, create a policy with clear naming (`<ROLE>_POLICY`).
- Update migrations, authorization endpoints, and documentation simultaneously.

## Deep Reference

- Full policy-capability mapping: `raw/RBAC/MAPPINGS/PHASE4_POLICY_CAPABILITY_MAPPINGS.md`
- Endpoint-policy matrix: `raw/RBAC/MAPPINGS/PHASE5_ENDPOINT_POLICY_MAPPINGS.md`
- Phase 1 endpoint extraction: `raw/RBAC/MAPPINGS/PHASE1_ENDPOINTS_EXTRACTION.md`

Use this matrix to understand the shape of the authorization catalogue before reaching for the exhaustive tables.
