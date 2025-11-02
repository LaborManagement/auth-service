# PostgreSQL Operations Checklist

**Navigation:** Previous: `vpd-checklist.md` → Next: `raw/README.md`

Use this checklist when managing the PostgreSQL instance that powers the auth service. It summarises the operational guidance from `reference/raw/POSTGRES/`.

## Routine Tasks

- [ ] Review daily backups and retention policies.
- [ ] Monitor replication lag (if replicas exist).
- [ ] Watch connection counts; auth service should reuse a small pool.
- [ ] Rotate credentials when onboarding/offboarding team members.
- [ ] Confirm `auth.set_user_context` remains performant (index on `user_tenant_acl`).

## Migration Workflow

1. Write migrations that add capabilities, policies, or RLS changes.
2. Test against a staging database using the app role (`app_auth`).
3. Coordinate with application deployment so caches refresh after rollout.
4. Record the change in the team changelog with links to scripts.

## Performance Checks

```sql
-- Identify slow queries involving RBAC tables
SELECT query, calls, total_time
FROM pg_stat_statements
WHERE query ILIKE '%auth.%'
ORDER BY total_time DESC
LIMIT 10;

-- Ensure sessions set the user context
SELECT pid, usename, query
FROM pg_stat_activity
WHERE query ILIKE '%set_user_context%';
```

## Access Control Hygiene

- Never run application workloads as `postgres` superuser.
- Keep separate credentials for automated scripts vs. humans.
- Audit `auth.role` and `auth.policy` changes via `audit.policy_change_log`.

## Incident Response

- Capture `pg_stat_activity` and relevant table snapshots before making changes.
- Disable offending accounts by removing role assignments or rotating secrets.
- Verify RLS policies are still `USING` the expected predicate.

## Further Reading

- Concept primer – `../foundations/postgres-for-auth.md`
- Troubleshooting flow – `../playbooks/troubleshoot-auth.md`
- Legacy operations manual – `raw/POSTGRES/README.md`
