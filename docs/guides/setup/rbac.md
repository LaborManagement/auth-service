# RBAC Setup Playbook

**Navigation:** Previous: `../login-to-data.md` → Next: `vpd.md`

Follow this sequence whenever you onboard a new tenant or extend permissions. Treat it like wiring a building’s security system—each step unlocks the next one.

## Overview Of The Flow

```mermaid
flowchart TD
    A[Plan roles] --> B[Create policies]
    B --> C[Define capabilities]
    C --> D[Register endpoints]
    C --> E[Map UI pages & actions]
    D --> F[Bind endpoint to policy]
    C --> F
    E --> G[Surface in authorization matrix]
    F --> H[Assign policy to roles]
    H --> I[Assign roles to users]
    I --> J[Verify behaviour]
```

## 1. Plan Roles & Personas

- Confirm which personas exist (e.g., Worker, Employer, Board, Service Accounts).
- Decide whether new roles are needed or existing ones can be reused.
- Reference: `../../reference/role-catalog.md`.

## 2. Create Or Update Policies

Policies bundle capabilities and tie an endpoint to a set of roles.

```sql
-- Insert a policy if it doesn't exist
INSERT INTO auth.policy (name, description)
VALUES ('EMPLOYER_POLICY', 'Employer actions for payment reconciliation')
ON CONFLICT (name) DO NOTHING;
```

Link the policy to roles:

```sql
INSERT INTO auth.role_policy (role_id, policy_id)
SELECT r.id, p.id
FROM auth.role r, auth.policy p
WHERE r.name IN ('EMPLOYER', 'TEST_USER')
  AND p.name = 'EMPLOYER_POLICY'
ON CONFLICT (role_id, policy_id) DO NOTHING;
```

## 3. Define Capabilities

Use the `<domain>.<subject>.<action>` naming convention.

```sql
INSERT INTO auth.capability (name, description)
VALUES ('payment.ledger.download', 'Download employer payment ledger CSV')
ON CONFLICT (name) DO NOTHING;
```

Map capabilities to the policy:

```sql
INSERT INTO auth.policy_capability (policy_id, capability_id)
SELECT p.id, c.id
FROM auth.policy p, auth.capability c
WHERE p.name = 'EMPLOYER_POLICY'
  AND c.name = 'payment.ledger.download'
ON CONFLICT (policy_id, capability_id) DO NOTHING;
```

## 4. Register Endpoints

```sql
INSERT INTO auth.endpoint (method, path, label)
VALUES ('GET', '/api/employer/payment-ledger', 'Download employer payment ledger')
ON CONFLICT (method, path) DO NOTHING;
```

Bind the endpoint to the policy:

```sql
INSERT INTO auth.endpoint_policy (endpoint_id, policy_id)
SELECT e.id, p.id
FROM auth.endpoint e, auth.policy p
WHERE e.method = 'GET'
  AND e.path = '/api/employer/payment-ledger'
  AND p.name = 'EMPLOYER_POLICY'
ON CONFLICT (endpoint_id, policy_id) DO NOTHING;
```

Annotate the controller:

```java
@GetMapping("/api/employer/payment-ledger")
@PreAuthorize("hasAuthority('payment.ledger.download')")
public ResponseEntity<Resource> downloadLedger(...) { ... }
```

## 5. Wire UI Pages & Actions

```sql
-- Page visibility
INSERT INTO auth.ui_page (code, description)
VALUES ('EMPLOYER_DASHBOARD', 'Employer overview dashboard')
ON CONFLICT (code) DO NOTHING;

INSERT INTO auth.ui_page_capability (page_id, capability_id)
SELECT p.id, c.id
FROM auth.ui_page p, auth.capability c
WHERE p.code = 'EMPLOYER_DASHBOARD'
  AND c.name = 'payment.ledger.download'
ON CONFLICT (page_id, capability_id) DO NOTHING;

-- Button visibility
INSERT INTO auth.ui_action (code, description)
VALUES ('EMPLOYER_LEDGER_DOWNLOAD', 'Download ledger button')
ON CONFLICT (code) DO NOTHING;

INSERT INTO auth.ui_action_capability (action_id, capability_id)
SELECT a.id, c.id
FROM auth.ui_action a, auth.capability c
WHERE a.code = 'EMPLOYER_LEDGER_DOWNLOAD'
  AND c.name = 'payment.ledger.download'
ON CONFLICT (action_id, capability_id) DO NOTHING;
```

Front-end code should hide controls unless the capability appears in `/api/me/authorizations`.

## 6. Assign Roles To Users

```sql
INSERT INTO auth.user_role (user_id, role_id)
SELECT u.id, r.id
FROM auth.user u, auth.role r
WHERE u.username = 'employer.demo'
  AND r.name = 'EMPLOYER'
ON CONFLICT (user_id, role_id) DO NOTHING;
```

If creating service accounts, ensure credentials are stored securely and tokens carry the correct audience.

## 7. Verify The Setup

1. Call `/api/me/authorizations` with the user’s JWT; confirm the new capability appears.
2. Invoke the guarded endpoint with both an allowed and disallowed user (expect 200 vs 403).
3. Run `SET ROLE app_payment_flow; SELECT auth.set_user_context(':userId');` followed by a data query to ensure RLS returns the correct rows.
4. Check audit logs for recorded access decisions.

## Troubleshooting Tips

- **Capability missing** – Recheck `auth.policy_capability` and `auth.role_policy`.
- **Endpoint still open** – Confirm controller annotation and `auth.endpoint_policy` entry.
- **Button still visible for others** – Verify UI uses the authorization matrix to gate rendering.

## Next Steps

Once RBAC is wired, continue to `vpd.md` to configure tenant-level data guardrails.
