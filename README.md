# Auth Service

Spring Boot microservice for JWT authentication, RBAC authorization, multi-tenant RLS, and audit logging for the payment reconciliation platform.

**Stack:** Java 17 | Spring Boot 3.2.5 | PostgreSQL | jOOQ | JWT

## Features

- Secure JWT authentication
- Role-based access control (RBAC)
- Row-level security (RLS) for multi-tenancy
- Comprehensive audit logging (API & entity level)
- jOOQ and JPA for data access

## Key Docs

- See `documentation/LBE/README.md` for system overview
- See `copilot-instructions.md` for coding standards and audit rules

## Build & Run

- `mvn clean install` to build
- `docker build -t user-auth-service:latest .` to build Docker image

## Folder Structure

- `src/main/java/com.example.userauth/` — code
- `src/main/resources/` — configs
- `migrations/` — DB migrations
