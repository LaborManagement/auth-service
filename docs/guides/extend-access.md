# Extend Access Guide

Use this step-by-step recipe when you need to add or adjust permissions. It mirrors what happens in production while remaining safe for experimentation.

## Scenario

You want employers to download a payment ledger (`payment.ledger.download`). The same flow applies for any new capability.

## Overview Of Steps

1. Design the capability.
2. Update the catalogue (database).
3. Wire the backend endpoint.
4. Reveal or hide UI actions.
5. Verify with contrasting tests.

## 1. Design The Capability

- Write the plain-language job story: “Employers can download their own payment ledger.”
- Name the capability `<domain>.<subject>.<action>` → `payment.ledger.download`.
- Decide which roles receive it (EMPLOYER, TEST_USER for QA).

## 2. Update The Catalogue

> All tables live in the `auth` schema. Run these changes through migrations or manual SQL in a non-production environment first.

```sql
-- Insert the capability
INSERT INTO auth.capability (name, description)
VALUES ('payment.ledger.download', 'Download payment ledger CSV for owned employer records');

-- Link to policy
INSERT INTO auth.policy_capability (policy_id, capability_id)
SELECT p.id, c.id
FROM auth.policy p, auth.capability c
WHERE p.name IN ('EMPLOYER_POLICY', 'TEST_USER_POLICY')
  AND c.name = 'payment.ledger.download';
```

## 3. Bind The Endpoint

```sql
-- Ensure the endpoint is registered
INSERT INTO auth.endpoint (method, path, label)
VALUES ('GET', '/api/employer/payment-ledger', 'Download employer payment ledger')
ON CONFLICT (method, path) DO NOTHING;

-- Attach required policy
INSERT INTO auth.endpoint_policy (endpoint_id, policy_id)
SELECT e.id, p.id
FROM auth.endpoint e, auth.policy p
WHERE e.method = 'GET'
  AND e.path = '/api/employer/payment-ledger'
  AND p.name = 'EMPLOYER_POLICY';
```

In the Spring controller, guard the method:

```java
@PreAuthorize("hasAuthority('payment.ledger.download')")
public ResponseEntity<Resource> downloadLedger(...) { ... }
```

## 4. Update The UI

- Fetch `/api/me/authorizations` after login.
- Show the “Download Ledger” button only when the capability list includes `payment.ledger.download`.
- Hide the button for everyone else—security through obscurity is not enough, but it improves UX.

## 5. Verify Behaviour

Use two seed users:

```bash
# Employer should succeed
curl -H "Authorization: Bearer <employer-token>" \
  http://localhost:8080/api/employer/payment-ledger

# Worker should fail with 403
curl -H "Authorization: Bearer <worker-token>" \
  http://localhost:8080/api/employer/payment-ledger
```

Check the database session set the context correctly:

```sql
SET ROLE app_payment_flow;
SELECT auth.set_user_context('employer.user');
SELECT COUNT(*) FROM payment_flow.payment_ledger;
```

Expect to see only employer-owned rows.

## Ready Reference

- Capability naming help – `foundations/access-control-101.md`
- Seed users and policy map – `reference/role-catalog.md`
- Troubleshooting failures – `playbooks/troubleshoot-auth.md`
- Legacy phase docs – `reference/raw/RBAC/` (for exhaustive mappings)

Repeat this loop for any new permission. Change the names, keep the structure.
