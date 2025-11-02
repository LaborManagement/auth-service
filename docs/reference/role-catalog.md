# Role Catalog

**Navigation:** Previous: `../guides/setup/vpd.md` → Next: `capability-catalog.md`

Use this catalog when you need a concise snapshot of each platform role. For full historical detail, see the legacy document in `reference/raw/ONBOARDING_ROLES.md`.

| Role | Capability Count | Typical User | Highlights | Default UI Areas |
| --- | --- | --- | --- | --- |
| `PLATFORM_BOOTSTRAP` | 98 | `platform.bootstrap@lbe.local` (service) | Seeds catalog, links policies, should be disabled after use | Hidden admin setup pages |
| `ADMIN_TECH` | 51 | `admin.tech@lbe.local` | Manages users, roles, policies, endpoints, UI pages | System settings, RBAC admin, audit views |
| `ADMIN_OPS` | 42 | `admin.ops@lbe.local` | Operates day-to-day reconciliation without altering RBAC wiring | Operational dashboards, audit logs, ticket triage |
| `BOARD` | 17 | `board.member@lbe.local` | Reviews escalated employer requests and approves payouts | Board overview, approvals, reporting |
| `EMPLOYER` | 19 | `employer.demo@lbe.local` | Manages payment submissions within their organisation | Employer dashboard, request status, payment ledger |
| `WORKER` | 14 | `worker.demo@lbe.local` | Uploads documents, tracks personal payments | Worker portal, upload screens |
| `TEST_USER` | 50 | `qa.test@lbe.local` | Broad sandbox access mirroring production for QA | Mirrors ADMIN_TECH + EMPLOYER for testing |

## Reference Queries

```sql
-- List capabilities for a role
SELECT c.name
FROM auth.capability c
JOIN auth.policy_capability pc ON pc.capability_id = c.id
JOIN auth.policy p ON p.id = pc.policy_id
JOIN auth.role_policy rp ON rp.policy_id = p.id
JOIN auth.role r ON r.id = rp.role_id
WHERE r.name = 'EMPLOYER'
ORDER BY c.name;

-- Show endpoint policies for a role
SELECT e.method, e.path, p.name as policy
FROM auth.endpoint e
JOIN auth.endpoint_policy ep ON ep.endpoint_id = e.id
JOIN auth.policy p ON p.id = ep.policy_id
JOIN auth.role_policy rp ON rp.policy_id = p.id
JOIN auth.role r ON r.id = rp.role_id
WHERE r.name = 'EMPLOYER'
ORDER BY e.path;
```

## Capability Buckets

- **Worker journeys** – Upload, update, and track own payment requests.
- **Employer journeys** – Approve worker submissions, view organisation-wide data.
- **Board oversight** – Approve final payouts and view cross-organisation reports.
- **Tech administration** – Manage RBAC catalog, system settings, and audit logs.
- **Operational support** – Monitor queues, resolve exceptions, and run reports.

## When Updating Roles

1. Adjust the policy-capability mapping first.
2. Update `/api/me/authorizations` contract tests if capabilities change.
3. Notify front-end owners when UI access changes—hidden buttons may need design updates.

## Further Reading

- Stories and analogies – `../start/role-stories.md`
- How to add permissions – `../guides/extend-access.md`
- Raw role breakdown – `raw/ONBOARDING_ROLES.md`
