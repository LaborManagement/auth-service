# PostgreSQL Setup Guide

## Prerequisites

- PostgreSQL 14+ installed
- PostgreSQL client tools (psql)
- Database credentials

## Phase 1: Install PostgreSQL

### macOS (via Homebrew)
```bash
brew install postgresql@15
brew services start postgresql@15
```

### Linux (Ubuntu)
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
```

### Windows
Download and install from: https://www.postgresql.org/download/windows/

## Phase 2: Create Database User and Database

```bash
# Connect as superuser
psql -U postgres

# Inside psql:
```

```sql
-- Create app user
CREATE ROLE app_auth LOGIN PASSWORD 'secure_password';

-- Create database
CREATE DATABASE auth_service_db OWNER app_auth;

-- Grant permissions
GRANT CONNECT ON DATABASE auth_service_db TO app_auth;
GRANT USAGE ON SCHEMA public TO app_auth;
GRANT ALL ON ALL TABLES IN SCHEMA public TO app_auth;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO app_auth;

-- Verify
\du
\l
```

## Phase 3: Install PostgreSQL JDBC Driver

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.1</version>
</dependency>
```

Run:
```bash
mvn clean install
```

## Phase 4: Update Application Configuration

### application-dev.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/auth_service_db
    username: app_auth
    password: secure_password
    driver-class-name: org.postgresql.Driver
    hikari:
      auto-commit: true
      maximum-pool-size: 20
      minimum-idle: 5
  
  jpa:
    hibernate:
      ddl-auto: validate
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
```

### application-prod.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://<prod-host>:5432/auth_service_db
    username: app_auth
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      auto-commit: true
      maximum-pool-size: 50
```

## Phase 5: Create Schema and Tables

```bash
# Option 1: Let Flyway/Liquibase handle migrations
mvn spring-boot:run
# Migrations run automatically

# Option 2: Manual schema creation
psql -U app_auth -d auth_service_db -f schema.sql
```

## Phase 6: Migrate Data from MySQL (If Needed)

### Step 1: Export from MySQL
```bash
mysqldump -u root -p user_auth_db > mysql_backup.sql
```

### Step 2: Convert SQL
```bash
# Use conversion tool (e.g., pgloader or manual conversion)
# Update AUTO_INCREMENT → SEQUENCES
# Update syntax differences
```

### Step 3: Import to PostgreSQL
```bash
psql -U app_auth -d auth_service_db < converted_schema.sql
```

### Step 4: Align Sequences

After importing data, sequences may be out of sync. Use the provided script:

```bash
psql -U postgres -d auth_service_db -f ../../scripts/postgres/reset_all_sequences.sql
```

Or use the shell wrapper:
```bash
bash ../../scripts/reset-sequences.sh
```

**What this does:**
- Finds all sequences in the database
- Sets each sequence to MAX(id) of its table
- Ensures new inserts don't fail with "duplicate key" errors

## Phase 7: Fix JSONB Bindings in Code

### Find all NamedParameterJdbcTemplate usages:

```java
// BEFORE
params.addValue("expression", jsonString);

// AFTER
params.addValue("expression", jsonString, Types.OTHER);
```

Search for: `addValue` in the codebase and fix all JSON bindings.

## Phase 8: Test Connection

```bash
# Test from command line
psql -U app_auth -d auth_service_db -c "SELECT 1;"
# Should return: 1

# Test from application
mvn spring-boot:run
# Check logs for successful connection
```

## Phase 9: Verify Schema

```bash
# Connect to database
psql -U app_auth -d auth_service_db

# List tables
\dt

# List sequences
\ds

# Describe a table
\d users

# Exit
\q
```

## Phase 10: Performance Setup

### Create Indexes

```sql
-- Connect as app_auth user
psql -U app_auth -d auth_service_db

-- Create indexes for common queries
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_policies_name ON policies(name);
CREATE INDEX idx_capabilities_name ON capabilities(name);
CREATE INDEX idx_roles_name ON roles(name);

-- Create GIN index for JSONB searches
CREATE INDEX idx_policies_expression_gin ON policies USING gin(expression);

-- Analyze tables for query planner
ANALYZE;
```

### Sequence Alignment (After Data Import)

If you imported data from MySQL and need to reset sequences:

```bash
# Use the provided sequence reset script
psql -U postgres -d auth_service_db -f ../../scripts/postgres/reset_all_sequences.sql

# Or use the shell wrapper
bash ../../scripts/reset-sequences.sh
```

## Phase 11: Enable RLS (Optional but Recommended)

See: `/docs/VPD/README.md`

```bash
psql -U postgres -d auth_service_db -f infra/db-migration/01-postgres-roles-setup.sql
```

## Verification Checklist

- [ ] PostgreSQL installed and running
- [ ] Database `auth_service_db` created
- [ ] User `app_auth` created with correct permissions
- [ ] JDBC driver added to pom.xml
- [ ] application.yml updated with PostgreSQL config
- [ ] Application connects successfully
- [ ] Tables created in PostgreSQL
- [ ] JSONB bindings fixed in code
- [ ] Tests pass
- [ ] Sequences aligned (if data imported)
- [ ] Indexes created for performance

## Backup and Restore

### Backup
```bash
# Full backup
pg_dump -U app_auth -d auth_service_db > backup.sql

# Compressed backup
pg_dump -U app_auth -d auth_service_db | gzip > backup.sql.gz

# Custom format (faster restore)
pg_dump -U app_auth -d auth_service_db -F c -f backup.bak
```

### Restore
```bash
# From SQL file
psql -U app_auth -d auth_service_db < backup.sql

# From compressed file
gunzip -c backup.sql.gz | psql -U app_auth -d auth_service_db

# From custom format
pg_restore -U app_auth -d auth_service_db backup.bak
```

## Post-Migration: Align Sequences

After importing data from MySQL, sequence values may be out of sync. Use the provided script:

### Option 1: Using the Shell Script (Recommended)
```bash
# Make script executable
chmod +x setup/reset-sequences.sh

# Run with defaults (uses environment variables or defaults to localhost)
./setup/reset-sequences.sh

# Run with custom database
DB_HOST=prod.example.com DB_PORT=5432 DB_NAME=auth_service_db DB_USER=postgres ./setup/reset-sequences.sh
```

### Option 2: Direct SQL
```bash
psql -U postgres -d auth_service_db -f setup/reset_all_sequences.sql
```

This script:
- Queries all sequences in the database
- Gets the maximum ID from each table
- Resets each sequence to MAX(id) + 1
- Provides feedback for each sequence reset

See [setup/README.md](./setup/README.md) for more details.

## Troubleshooting

### Connection Refused
```bash
# Check if PostgreSQL is running
ps aux | grep postgres

# Start PostgreSQL
brew services start postgresql@15  # macOS
sudo systemctl start postgresql      # Linux

# Check listen address
sudo grep "^listen_addresses" /etc/postgresql/*/main/postgresql.conf
# Should be: listen_addresses = 'localhost'
```

### Permission Denied
```bash
# Grant permissions
psql -U postgres -d auth_service_db

CREATE ROLE app_auth LOGIN PASSWORD 'password';
ALTER DATABASE auth_service_db OWNER TO app_auth;
GRANT ALL ON ALL TABLES IN SCHEMA public TO app_auth;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO app_auth;
```

### Sequence Out of Sync
```bash
# Align sequences using the provided script
./setup/reset-sequences.sh
```

