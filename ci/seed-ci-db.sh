#!/usr/bin/env bash
# Fills an EMPTY SalonHub database with the E2E test data (db/ci-seed.sql).
# Start the backend once before this script, so Flyway makes the schema.
#
# Needs: python3, the mysql client, and these environment variables:
#   ACCOUNT_CUSTOMER_PASSWORD, ACCOUNT_EMPLOYEE_PASSWORD,
#   ACCOUNT_OWNER_PASSWORD, ACCOUNT_SALON2EMPLOYEE_PASSWORD   (test passwords)
#   MYSQL_PWD                                                 (database password)
# Optional: DB_HOST (127.0.0.1), DB_PORT (3306), DB_USER (root), DB_NAME (salon_dbv2), MYSQL_BIN (mysql)
set -euo pipefail

for name in ACCOUNT_CUSTOMER_PASSWORD ACCOUNT_EMPLOYEE_PASSWORD ACCOUNT_OWNER_PASSWORD ACCOUNT_SALON2EMPLOYEE_PASSWORD; do
  if [ -z "${!name:-}" ]; then
    echo "Missing environment variable $name" >&2
    exit 1
  fi
done

python3 -c "import bcrypt" 2>/dev/null || python3 -m pip install --quiet bcrypt

# BCrypt hashes are made here, so no password or hash is stored in the repository.
hashes=$(python3 - <<'PY'
import bcrypt, os
def h(name):
    return bcrypt.hashpw(os.environ[name].encode(), bcrypt.gensalt(10)).decode()
print("SET @hash_customer='%s', @hash_employee='%s', @hash_owner='%s', @hash_salon2employee='%s';" % (
    h("ACCOUNT_CUSTOMER_PASSWORD"), h("ACCOUNT_EMPLOYEE_PASSWORD"),
    h("ACCOUNT_OWNER_PASSWORD"), h("ACCOUNT_SALON2EMPLOYEE_PASSWORD")))
PY
)

here="$(cd "$(dirname "$0")" && pwd)"
{ echo "$hashes"; cat "$here/../db/ci-seed.sql"; } | "${MYSQL_BIN:-mysql}" \
  -h "${DB_HOST:-127.0.0.1}" -P "${DB_PORT:-3306}" -u "${DB_USER:-root}" "${DB_NAME:-salon_dbv2}"

echo "Seeded the E2E test data into ${DB_NAME:-salon_dbv2}."
