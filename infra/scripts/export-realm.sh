#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# export-realm.sh — Export the Keycloak "skillmatch" realm and version it
#
# Usage:
#   ./infra/scripts/export-realm.sh [--method docker|api] [--output PATH]
#
# Methods:
#   docker  (default) — runs `kc.sh export` inside the running container;
#                       works even when Keycloak is started in dev mode.
#   api             — uses the Keycloak Admin REST API (requires curl + jq).
#
# Prerequisites:
#   docker method : Docker Engine running, `keycloak` container up
#   api method    : curl, jq; Keycloak reachable on KEYCLOAK_URL
#
# Environment variables (api method):
#   KEYCLOAK_URL       Base URL of Keycloak  (default: http://localhost:8180)
#   KEYCLOAK_ADMIN     Admin username         (default: admin)
#   KEYCLOAK_PASSWORD  Admin password         (default: admin)
#   REALM              Realm to export        (default: skillmatch)
#
# Output:
#   Writes the realm JSON to OUTPUT_FILE and copies it to infra/keycloak/.
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

# ── Defaults ─────────────────────────────────────────────────────────────────
METHOD="${METHOD:-docker}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
KEYCLOAK_ADMIN="${KEYCLOAK_ADMIN:-admin}"
KEYCLOAK_PASSWORD="${KEYCLOAK_PASSWORD:-admin}"
REALM="${REALM:-skillmatch}"
CONTAINER_NAME="${CONTAINER_NAME:-infra-keycloak-1}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
OUTPUT_FILE="${OUTPUT_FILE:-${REPO_ROOT}/infra/keycloak/skillmatch-realm.json}"

# ── Argument parsing ──────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case $1 in
    --method)  METHOD="$2";      shift 2 ;;
    --output)  OUTPUT_FILE="$2"; shift 2 ;;
    *)         echo "Unknown option: $1" >&2; exit 1 ;;
  esac
done

# ── Helpers ───────────────────────────────────────────────────────────────────
log()  { echo "[export-realm] $*"; }
err()  { echo "[export-realm] ERROR: $*" >&2; exit 1; }

require_cmd() {
  command -v "$1" &>/dev/null || err "'$1' is required but not found in PATH."
}

# ── Method: docker ────────────────────────────────────────────────────────────
export_via_docker() {
  require_cmd docker

  log "Checking that container '${CONTAINER_NAME}' is running..."
  docker inspect --format='{{.State.Status}}' "${CONTAINER_NAME}" 2>/dev/null \
    | grep -q "running" || err "Container '${CONTAINER_NAME}' is not running. Start it with: cd infra && docker compose up -d keycloak"

  TMP_EXPORT_DIR="/tmp/keycloak-export-$$"

  log "Running 'kc.sh export' inside the container..."
  docker exec "${CONTAINER_NAME}" \
    /opt/keycloak/bin/kc.sh export \
      --dir "${TMP_EXPORT_DIR}" \
      --realm "${REALM}" \
      --users realm_file

  log "Copying exported file from container..."
  docker cp "${CONTAINER_NAME}:${TMP_EXPORT_DIR}/${REALM}-realm.json" "${OUTPUT_FILE}"

  # Cleanup inside the container
  docker exec "${CONTAINER_NAME}" rm -rf "${TMP_EXPORT_DIR}" 2>/dev/null || true

  log "Export complete via Docker method."
}

# ── Method: api ───────────────────────────────────────────────────────────────
export_via_api() {
  require_cmd curl
  require_cmd jq

  log "Obtaining admin access token from ${KEYCLOAK_URL}..."
  TOKEN_RESPONSE=$(curl -sf \
    -d "client_id=admin-cli" \
    -d "username=${KEYCLOAK_ADMIN}" \
    -d "password=${KEYCLOAK_PASSWORD}" \
    -d "grant_type=password" \
    "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token")

  ACCESS_TOKEN=$(echo "${TOKEN_RESPONSE}" | jq -r '.access_token')
  [[ "${ACCESS_TOKEN}" == "null" || -z "${ACCESS_TOKEN}" ]] && \
    err "Failed to obtain access token. Check KEYCLOAK_ADMIN / KEYCLOAK_PASSWORD."

  log "Fetching realm export for '${REALM}'..."
  HTTP_STATUS=$(curl -sf \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -H "Accept: application/json" \
    -o "${OUTPUT_FILE}" \
    -w "%{http_code}" \
    "${KEYCLOAK_URL}/admin/realms/${REALM}")

  [[ "${HTTP_STATUS}" -ne 200 ]] && \
    err "API returned HTTP ${HTTP_STATUS}. Verify realm name and admin permissions."

  # Pretty-print the JSON
  TMP=$(mktemp)
  jq '.' "${OUTPUT_FILE}" > "${TMP}" && mv "${TMP}" "${OUTPUT_FILE}"

  log "Export complete via API method."
}

# ── Main ──────────────────────────────────────────────────────────────────────
log "SkillMatch Keycloak Realm Exporter"
log "  Method  : ${METHOD}"
log "  Realm   : ${REALM}"
log "  Output  : ${OUTPUT_FILE}"

mkdir -p "$(dirname "${OUTPUT_FILE}")"

case "${METHOD}" in
  docker) export_via_docker ;;
  api)    export_via_api ;;
  *)      err "Unknown method '${METHOD}'. Use 'docker' or 'api'." ;;
esac

log ""
log "Realm JSON written to: ${OUTPUT_FILE}"
log ""
log "Next steps:"
log "  1. Review the exported JSON (remove production secrets before committing)."
log "  2. git add infra/keycloak/skillmatch-realm.json && git commit -m 'chore: update Keycloak realm export'"
log "  3. The Docker Compose stack will auto-import it on next 'docker compose up'."
