# Troubleshoot Auth Playbook

**Navigation:** Jump back to the guided path via [docs/README.md](../README.md) when you finish debugging.

Something broke? Use this playbook to work from symptoms to solutions. The steps assume you already know the basics from the Foundations guides.

## Quick Triage

1. **Identify the failing role and action.**
2. **Capture the error code or log entry.**
3. **Check whether the issue is RBAC (permission denied) or RLS (data hidden).**

## Symptom: 401 Unauthorized

- **Likely cause:** Missing, expired, or malformed JWT.
- **Fixes:**
  - Confirm the client sends the `Authorization: Bearer` header.
  - Check token expiry (`exp` claim) and issue a fresh token.
  - Verify signing keys match between issuer and auth service.

## Symptom: 403 Forbidden

- **Likely causes:** Capability missing, endpoint not linked to policy, or wrong role assignment.
- **Fixes:**
  - Call `/api/me/authorizations` to see if the capability is present.
  - Query `auth.endpoint_policy` to confirm the endpoint maps to the expected policy.
  - Check `auth.user_role` to ensure the user holds the right badge.
- **If a service account:** Validate the machine role assignments and token audience.

## Symptom: 404 (But Data Should Exist)

- **Likely cause:** RLS hid the row after RBAC allowed the request.
- **Fixes:**
  - Run `SET ROLE app_payment_flow; SELECT auth.set_user_context(':userId');` followed by the original query.
  - Inspect `auth.user_tenant_acl` for missing tenant mappings.
  - Ensure the data row has correct tenant columns (`board_id`, `employer_id`).

## Symptom: UI Button Missing

- **Likely causes:** Authorization matrix lacks the capability or UI checks the wrong key.
- **Fixes:**
  - Inspect the JSON returned by `/api/me/authorizations`.
  - Confirm the UI looks for the same capability string the backend expects.
  - Refresh the token if capability caches were recently updated.

## Symptom: Machine-To-Machine Call Fails

- **Likely causes:** Service account token lacks required roles or incorrect client credentials.
- **Fixes:**
  - Regenerate client credentials and ensure the correct audience scope.
  - Assign the needed roles in `auth.user_role`.
  - Double-check TLS and DNS configuration between services.

## Symptom: Superuser Sees Different Data Than App User

- **Expected behaviour:** Superusers bypass RLS.
- **Fixes:**
  - Always test with `SET ROLE app_payment_flow`.
  - If still inconsistent, review RLS policy definitions in `reference/raw/VPD/README.md`.

## Audit & Logging Tips

- Search `audit.policy_change_log` for recent modifications.
- Tail application logs for `AccessDecisionManager` entries—they explain why an access decision was made.
- Correlate trace IDs between gateway, auth service, and downstream services for multi-hop debugging.

## Escalation Checklist

- Gather token payload, endpoint, policy ID, and relevant SQL snippet.
- Note any recent migrations or deployments touching the `auth` schema.
- Link to the specific guide you followed (`extend-access.md`, etc.) so reviewers know the intended state.

Keep this playbook handy. Debugging access should feel like following breadcrumbs, not guessing in the dark.
