# RBAC Troubleshooting

Use this checklist when a request is denied unexpectedly or the seeded data looks incorrect. The sections mirror the three enforcement layers so you can isolate the failure quickly.

## Quick Triage

| Symptom | Likely Cause | Next Step |
| --- | --- | --- |
| 401 Unauthorized | JWT missing or invalid | Reissue token, check signing secret, confirm system clock |
| 403 Forbidden | Policy or capability mismatch | Confirm endpoint mapping and user capabilities |
| 404 / Empty result | Row-level security filtered data | Ensure `auth.set_user_context` is executed and ACL entries exist |

## Layer 1 — Endpoint Policy

- Query `endpoint_policy` for the verb/path combination and confirm a policy exists.
- If missing, update the mapping in `MAPPINGS/PHASE5_ENDPOINT_POLICY_MAPPINGS.md` and reapply seeds.
- Check controller annotations (`@PreAuthorize`) align with the intended policy.

```sql
SELECT p.name
FROM endpoint_policy ep
JOIN policies p ON p.id = ep.policy_id
WHERE ep.method = 'POST' AND ep.path = '/api/worker/uploaded-data/upload';
```

## Layer 2 — Capability Bundles

- Use the SQL in `testing.md` to list capabilities granted to the affected user.
- Compare against the required capabilities described in `DEFINITIONS/PHASE3_CAPABILITIES_DEFINITION.md`.
- Re-run scripts 02–04 if the database drifted from the canonical list.

```sql
SELECT c.name
FROM policy_capability pc
JOIN capabilities c ON c.id = pc.capability_id
WHERE pc.policy_id = (SELECT id FROM policies WHERE name = 'ADMIN_OPS_POLICY');
```

## Layer 3 — Data Isolation

- Confirm `ONBOARDING/setup/08_configure_vpd.sql` executed without errors.
- Verify the `auth.user_tenant_acl` table has rows for the user, board, and employer combination you expect.
- Ensure the application is calling `auth.set_user_context` (log the filter if unsure).

```sql
SET ROLE app_payment_flow;
SELECT * FROM auth.user_tenant_acl WHERE user_id = 8;
```

## Common Fixes

1. **Policy missing** → add mapping, rerun seeds, redeploy.
2. **Capability absent** → update capability definitions, rerun 02–04, and restart the service.
3. **ACL mismatch** → insert the correct board/employer rows, then retry the request.
4. **Token stale** → log out and obtain a new JWT to pick up role changes.

Document the fix in your deployment notes so the mapping files stay aligned with production reality.
