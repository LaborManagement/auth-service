#!/bin/bash
# ============================================================================
# PostgreSQL Sequence Reset Helper Script
# ============================================================================
# Quick helper to reset PostgreSQL sequences after bootstrap data insertion
# to prevent duplicate key constraint violations.
#
# USAGE:
#   bash reset-sequences.sh
#
# CONFIGURATION:
#   Set these environment variables before running:
#   - DB_HOST (default: localhost)
#   - DB_PORT (default: 5432)
#   - DB_NAME (default: auth-service)
#   - DB_USER (default: postgres)
#
# EXAMPLES:
#   # Run with defaults
#   bash reset-sequences.sh
#
#   # Run against production
#   DB_HOST=prod-db.example.com DB_NAME=auth_prod bash reset-sequences.sh
#
#   # Run against different user
#   DB_USER=app_auth bash reset-sequences.sh
# ============================================================================

set -e

# Configuration with defaults
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-auth-service}"
DB_USER="${DB_USER:-postgres}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../setup" && pwd)"
RESET_SCRIPT="$SCRIPT_DIR/reset_all_sequences.sql"

# Validation
if [ ! -f "$RESET_SCRIPT" ]; then
    echo "❌ Error: reset_all_sequences.sql not found at $RESET_SCRIPT"
    echo "   Make sure you're running this script from the correct directory"
    exit 1
fi

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "PostgreSQL Sequence Reset"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Database: $DB_NAME"
echo "Host:     $DB_HOST"
echo "Port:     $DB_PORT"
echo "User:     $DB_USER"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Test connection
echo "Testing database connection..."
if ! psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1;" >/dev/null 2>&1; then
    echo "❌ Error: Cannot connect to database"
    echo "   Check your connection settings and try again"
    exit 1
fi
echo "✓ Connection successful"
echo ""

# Run the reset script
echo "Resetting sequences..."
if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$RESET_SCRIPT"; then
    echo ""
    echo "✓ Sequences reset successfully!"
    echo ""
    echo "You can now insert data without duplicate key conflicts."
else
    echo ""
    echo "❌ Error: Failed to reset sequences"
    exit 1
fi
