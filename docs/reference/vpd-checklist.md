# VPD Checklist

**Navigation:** Previous: [Policy Matrix](policy-matrix.md) → Next: [PostgreSQL Operations Checklist](postgres-operations.md)

Keep this checklist handy when reviewing or troubleshooting row-level security changes. It distills the detailed notes from [raw/VPD/README.md](raw/VPD/README.md).

## Before Deploying

- [ ] `auth` schema contains `user_tenant_acl` entries for every active user.
- [ ] `auth.set_user_context(user_id)` returns the expected tenant list.
- [ ] Every protected table includes `board_id` and `employer_id` (or equivalent tenant columns).
- [ ] RLS policies exist for each table and are `ENABLED`.
- [ ] Application roles (`app_auth`, `app_payment_flow`) have `USAGE` on required schemas but not superuser privileges.

## Smoke Test Script

```sql
SET ROLE app_payment_flow;
SELECT auth.set_user_context('employer.demo');
SELECT COUNT(*) FROM payment_flow.payment_requests;

SET ROLE app_payment_flow;
SELECT auth.set_user_context('worker.demo');
SELECT COUNT(*) FROM payment_flow.payment_requests;
```

- Employer count should reflect their organisation.
- Worker count should be zero or limited to their submissions.

## Production Review Questions

1. Which migration introduced or changed RLS policies?
2. Do we have audit coverage for tenant ACL updates?
3. Are integration tests asserting both allow and deny scenarios?
4. Has the UI been updated to reflect any new visibility rules?

## Common Fixes

| Symptom | Likely Cause | Action |
| --- | --- | --- |
| Everyone sees everything | Testing with superuser | Use `SET ROLE app_payment_flow` |
| Legitimate user sees nothing | Missing ACL entry | Insert correct rows into `auth.user_tenant_acl` |
| Tenant data leaked | Wrong tenant columns on data row | Backfill and reindex tenant columns |
| API returns 403 unexpectedly | Capability missing | Revisit `guides/extend-access.md` and policy mappings |

## Related Guides

- Concept primer – `../foundations/data-guardrails-101.md`
- Verification steps – `../guides/verify-permissions.md`
- Troubleshooting – `../playbooks/troubleshoot-auth.md`
- Legacy details – `raw/VPD/`
