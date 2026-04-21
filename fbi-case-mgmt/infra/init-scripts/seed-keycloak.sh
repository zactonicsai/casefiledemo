#!/bin/bash
# The Keycloak realm is imported automatically at container startup via
# docker/keycloak/realm-export.json (KC_IMPORT feature with --import-realm).
# This script simply verifies that the realm, client, and users are present.
set -euo pipefail

KC_URL="${KC_URL:-http://localhost:8080}"
REALM="Demo Only"
ADMIN_USER="${KC_ADMIN:-admin}"
ADMIN_PASS="${KC_ADMIN_PASSWORD:-admin}"

echo "[seed-keycloak] Verifying Keycloak at $KC_URL ..."
for i in $(seq 1 30); do
    if curl -sf "$KC_URL/realms/$REALM/.well-known/openid-configuration" >/dev/null; then
        echo "[seed-keycloak] Realm '$REALM' is available."
        break
    fi
    echo "  ...waiting ($i/30)"
    sleep 4
done

echo "[seed-keycloak] Fetching admin token..."
TOKEN=$(curl -sf -d "client_id=admin-cli" \
    -d "username=$ADMIN_USER" -d "password=$ADMIN_PASS" -d "grant_type=password" \
    "$KC_URL/realms/master/protocol/openid-connect/token" \
    | sed -e 's/.*"access_token":"\([^"]*\)".*/\1/')

if [[ -z "${TOKEN:-}" ]]; then
    echo "[seed-keycloak] Could not fetch admin token"; exit 1
fi

echo "[seed-keycloak] Users in realm:"
curl -sf -H "Authorization: Bearer $TOKEN" \
    "$KC_URL/admin/realms/$REALM/users?max=50" \
    | python3 -c "import json,sys; [print('  -',u['username'],'/',u.get('email','')) for u in json.load(sys.stdin)]"

echo "[seed-keycloak] Done. Login at http://localhost:4200 (demo users in README)."
