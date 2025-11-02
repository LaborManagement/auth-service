# Local Environment Guide

This guide walks you through preparing a workstation that can run the auth service and execute the onboarding scripts. Follow it in order; treat it like assembling furniture with a trusted manual.

## 1. Prerequisites Checklist

- Java 17+
- Docker Desktop (or Podman) running
- psql CLI access to your local or shared PostgreSQL instance
- Access to the repo with permissions to run the SQL scripts

## 2. Clone And Branch

1. Clone the repository.
2. Create a personal branch for onboarding experiments.
3. Keep the branch around so you can refer back to changes you made while learning.

## 3. Build The Service

```bash
./mvnw clean package
```

- Confirms dependencies resolve.
- Builds the Spring Boot service you’ll run after the database is ready.

## 4. Seed The Database

- Scripts live in `../ONBOARDING/setup/` and are already ordered.
- Use the README inside that folder to run the SQL files sequentially.
- Recommended command pattern:

```bash
psql "$DATABASE_URL" -f docs/ONBOARDING/setup/01_create_roles.sql
```

- Repeat for each script until the README checklist is complete.
- These scripts create roles, policies, capabilities, endpoints, seed users, and RLS configuration.

## 5. Configure Local Secrets

- Copy the sample `.env` or application config (if provided) and set:
  - Database URL and credentials
  - JWT signing keys
  - Any feature flags required for auth
- Store secrets securely (do not commit them).

## 6. Run The Service

```bash
./mvnw spring-boot:run
```

- Verify the service starts without errors.
- Hit `/actuator/health` to confirm it responds.

## 7. Smoke Test Permissions

- Follow `guides/verify-permissions.md` to ensure the seeded roles behave as expected.
- Use Postman or curl with the provided seed user credentials (`reference/role-catalog.md` lists them).

## 8. Next Steps

- Ready to add a new capability? Open `guides/extend-access.md`.
- Need to integrate another service? See `guides/integrate-your-service.md`.
- Stuck on an error? `playbooks/troubleshoot-auth.md` is organised by symptom.

Treat this guide as your baseline. Once you can repeat these steps confidently, you’re ready for feature work.
