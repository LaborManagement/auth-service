# Verify Permissions Guide

Run these checks whenever you finish seeding the database, add a new capability, or suspect something is off. They combine RBAC and RLS so you know the end-to-end story still holds.

## 1. Confirm Authorization Matrix

```bash
curl -H "Authorization: Bearer <employer-token>" \
  http://localhost:8080/api/me/authorizations | jq
```

Expect to see:

- `roles` array with `EMPLOYER`
- `capabilities` list containing `payment.details.read`, etc.
- `pages` and `actions` that match the employer experience

If the capability is missing here, the policy mapping is incomplete.

## 2. Endpoint Access Test

```bash
# Should succeed
curl -i -H "Authorization: Bearer <employer-token>" \
  http://localhost:8080/api/payment-requests/42

# Should fail with 403 (worker accessing employer route)
curl -i -H "Authorization: Bearer <worker-token>" \
  http://localhost:8080/api/payment-requests/42
```

Interpretation:

- `200 OK` for the authorised user confirms RBAC is wired.
- `403 Forbidden` for the unauthorised user proves the policy check blocks as expected.

## 3. UI Sanity Check

- Log in as both employer and worker in the front-end.
- Ensure buttons/pages controlled by the capability appear only for the employer.
- Inspect the network tab to confirm `/api/me/authorizations` updates after login.

## 4. RLS Spot Check

```sql
SET ROLE app_payment_flow;
SELECT auth.set_user_context('employer.user');
SELECT COUNT(*) FROM payment_flow.payment_requests;

SET ROLE app_payment_flow;
SELECT auth.set_user_context('worker.user');
SELECT COUNT(*) FROM payment_flow.payment_requests;
```

Outcome:

- Employer sees multiple rows (their organisation’s data).
- Worker sees zero or only their own entries.

If counts look wrong, verify `auth.user_tenant_acl` entries.

## 5. Audit Trail Review

```sql
SELECT created_at, actor, action, details
FROM audit.policy_change_log
ORDER BY created_at DESC
LIMIT 10;
```

- Confirms recent policy or capability changes are recorded.
- Useful when validating production deployments.

## When Things Fail

- Missing capabilities → Review `guides/extend-access.md`.
- Endpoint returns 404 instead of 403 → RLS likely hiding the data; check tenant assignments.
- UI still shows buttons → Ensure the front-end refreshes the authorization matrix after login.

Keep these snippets in a scratch file. Running them often builds confidence in the guardrails.
