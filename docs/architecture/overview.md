# Auth Platform Architecture

**Navigation:** Start here → Next: [Data Map](data-map.md)

This document gives you the birds-eye view of how the auth service keeps payment reconciliation safe. Think of it as the control tower diagram before you dive into the step-by-step flight plan.

## System Topology

```mermaid
flowchart TD
    subgraph Clients
        Web["Web App"]
        Mobile["Mobile App"]
        Partner["Partner Service"]
    end
    Gateway["API Gateway / Load Balancer"]
    Auth["Auth Service"]
    Policy["Policy Catalogue (PostgreSQL)"]
    RLS["Business Schemas (PostgreSQL with RLS)"]
    Services["Other Platform Services"]
    Audit["Audit / Observability"]

    Clients -->|Bearer JWT| Gateway
    Gateway --> Auth
    Auth --> Policy
    Auth --> RLS
    Auth --> Services
    Auth --> Audit
    Policy --> Auth
    RLS --> Auth
    Services --> Auth
```

- **API Gateway** – terminates TLS, enforces coarse routing, forwards only authenticated traffic.
- **Auth Service** – validates tokens, resolves roles/policies/capabilities, sets database context, and returns a decision.
- **Policy Catalogue** – PostgreSQL tables storing roles, policies, capabilities, endpoint bindings, and UI permissions.
- **Business Schemas** – Payment and employer data protected by row-level security (RLS).
- **Audit Layer** – Logs why access was granted or denied for compliance and debugging.

## Authentication Components

1. **Identity Provider (IdP)** issues JWTs after credential checks.
2. **Auth Service** verifies signatures and expirations using the shared secret/public key.
3. **Token Cache** (optional) accelerates repeat validations.
4. **Audit Hooks** record login success/failure for monitoring.

Key questions answered here: *Is the caller who they claim to be?* and *Is the token still valid?*

## Authorization Components

### Backend Security (API Authorization)
1. **Role Resolution** – `auth.user_roles` links users to their assigned roles.
2. **Policy Selection** – `auth.endpoint_policies` matches the HTTP method+path to policies.
3. **Capability Check** – `auth.policy_capabilities` proves the policy carries the required permission.
4. **Endpoint Protection** – Only requests with matching capabilities can execute the endpoint.

### Frontend Visibility (UI Authorization)
1. **Page Actions** – `auth.page_actions` define UI buttons/actions with both:
   - `capability_id`: Permission check (what the user can do)
   - `endpoint_id`: API binding (which endpoint to call)
2. **UI Matrix** – `/api/meta/endpoints?page_id={id}` returns available endpoints for a page.
3. **Dynamic UI** – Frontend shows/hides buttons based on user's capabilities.

### Dual Relationship Model
```
Backend Authorization:
User → Role → Policy → Capability ↔ Endpoint (via endpoint_policies)

Frontend UI Binding:
PageAction → Capability (permission check)
PageAction → Endpoint (API call target)
```

**Key Insight:** `page_actions` serves dual purpose:
- `capability_id`: Determines if user has permission
- `endpoint_id`: Determines which API to call when button is clicked

Only when both the capability check passes AND the endpoint authorization succeeds does the action complete.

## Request Lifecycle Snapshot

```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant AuthService
    participant PolicyDb as "Policy Catalogue"
    participant DataDb as "PostgreSQL (RLS)"

    Client->>Gateway: HTTP request + JWT
    Gateway->>AuthService: Forward request
    AuthService->>AuthService: Validate JWT signature & expiry
    AuthService->>PolicyDb: Load roles & policy requirements
    PolicyDb-->>AuthService: Capabilities + tenant rules
    AuthService->>PolicyDb: Check endpoint_policies
    AuthService->>DataDb: SELECT auth.set_user_context(user_id)
    AuthService->>DataDb: Execute business query
    DataDb-->>AuthService: Rows filtered by RLS
    AuthService-->>Client: 200 / 403 / 404
````

- **200 OK** – Capability and RLS checks pass.
- **403 Forbidden** – Capability check fails.
- **404 Not Found** – Data exists but RLS hides it (to prevent leaking existence).

## Operational Guardrails

- **Stateless JWT validation** keeps auth service horizontally scalable.
- **Database transactions** must set user context before touching tenant-protected tables.
- **Audit trails** ensure every decision can be reconstructed.
- **Legacy deep dives** remain in `../reference/raw/` for exhaustive tables and historical docs.

## Up Next

Continue to [Data Map](data-map.md) for entity relationships. Then explore these visual guides:

- **[Request Lifecycle Flowchart](request-lifecycle.md)** – See how requests flow through decision points
- **[Policy Binding Relationships](policy-binding.md)** – Understand how policies, capabilities, and endpoints connect
- **[Common Permission Patterns](permission-patterns.md)** – Learn real-world setup patterns

Finally, proceed to [Journey: Login To Data](../guides/login-to-data.md) for an end-to-end walkthrough.

````
