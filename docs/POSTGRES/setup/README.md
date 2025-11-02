# PostgreSQL Setup Scripts

This folder contains scripts for PostgreSQL setup and maintenance.

## Scripts

### reset_all_sequences.sql

Resets PostgreSQL sequences to match their tables' current MAX(id) values.

**When to use:**
- After bulk data import from MySQL
- After restoring from backup
- After manual data inserts
- When getting "duplicate key" errors on inserts

**What it does:**
- Finds all sequences in all schemas (except system schemas)
- Gets MAX(id) for each corresponding table
- Resets sequence to MAX(id) + 1

**How to use:**

```bash
# Direct usage
psql -U postgres -d <database> -f reset_all_sequences.sql

# Example
psql -U postgres -d auth_service_db -f reset_all_sequences.sql
```

**Output:**
```
NOTICE:  Resetting sequence public.users_id_seq to 100
NOTICE:  Resetting sequence public.roles_id_seq to 5
NOTICE:  Resetting sequence public.policies_id_seq to 20
...
NOTICE:  All sequences have been reset successfully!
DO
```

### reset-sequences.sh

Bash helper script that wraps `reset_all_sequences.sql` with configuration options.

**When to use:**
- When you need to reset sequences from command line
- When you want to configure connection parameters
- For automation and scripting

**How to use:**

```bash
# Basic usage (uses defaults)
bash reset-sequences.sh

# With custom host
DB_HOST=prod-db.example.com bash reset-sequences.sh

# With custom database
DB_NAME=auth_prod bash reset-sequences.sh

# Full configuration
DB_HOST=prod-db.example.com \
DB_PORT=5432 \
DB_NAME=auth_prod \
DB_USER=postgres \
bash reset-sequences.sh
```

**Configuration:**
- `DB_HOST` - PostgreSQL host (default: localhost)
- `DB_PORT` - PostgreSQL port (default: 5432)
- `DB_NAME` - Database name (default: auth-service)
- `DB_USER` - Database user (default: postgres)

**Output:**
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PostgreSQL Sequence Reset
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Database: auth-service
Host:     localhost
Port:     5432
User:     postgres
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Testing database connection...
✓ Connection successful

Resetting sequences...
✓ Sequences reset successfully!

You can now insert data without duplicate key conflicts.
```

## When to Use

### After MySQL to PostgreSQL Migration

```bash
# 1. Import data from MySQL
psql -U postgres -d auth_service_db < mysql_converted.sql

# 2. Reset sequences to match data
bash reset-sequences.sh

# 3. Verify sequences are correct
psql -U postgres -d auth_service_db -c \
  "SELECT schemaname, sequencename, last_value FROM pg_sequences;"
```

### After Database Restore

```bash
# 1. Restore from backup
pg_restore -d auth_service_db backup.bak

# 2. Reset sequences just in case
bash reset-sequences.sh
```

## Troubleshooting

**Error: "Connection refused"**
```bash
# Check PostgreSQL is running
psql -U postgres -c "SELECT 1;"

# Check host/port settings
DB_HOST=your-host DB_PORT=5432 bash reset-sequences.sh
```

**Error: "Database does not exist"**
```bash
# List available databases
psql -U postgres -l

# Use correct database name
DB_NAME=correct_name bash reset-sequences.sh
```

**Error: "permission denied"**
```bash
# Use correct PostgreSQL user
DB_USER=postgres bash reset-sequences.sh

# Or use your app user if it has permissions
DB_USER=app_auth bash reset-sequences.sh
```

## Related Documents

- [PostgreSQL Setup Guide](../setup.md) - Full PostgreSQL setup
- [PostgreSQL Troubleshooting](../troubleshoot.md) - Common issues
- [POSTGRES README](../README.md) - Overview
