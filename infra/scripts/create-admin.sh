#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# create-admin.sh — Interactively create a new ADMIN account in Keycloak
#
# Usage:
#   ./infra/scripts/create-admin.sh
#
# Prerequisites: curl, jq, openssl; Keycloak reachable on KEYCLOAK_URL with an
# admin account able to create users and assign realm roles.
#
# Environment variables:
#   KEYCLOAK_URL       Base URL of Keycloak       (default: http://localhost:8180)
#   KEYCLOAK_ADMIN     Admin username             (default: admin)
#   KEYCLOAK_PASSWORD  Admin password             (default: admin)
#   REALM              Realm to create the user in (default: skillmatch)
#
# The new admin gets a random temporary password; Keycloak forces a password
# change on their very first login (native "temporary" credential, not custom
# logic). Unlike PROFESSIONAL/COMPANY, admins have no user-service database
# row — the Keycloak identity plus the ADMIN realm role is the whole account,
# per the platform's existing design (see frontend/src/hooks/useDisplayName.js).
#
# ADMIN accounts are deliberately never self-service (see AuthController's
# rejectAdminRole) — this script is the only way to create one, run by
# whoever already holds Keycloak admin credentials.
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
KEYCLOAK_ADMIN="${KEYCLOAK_ADMIN:-admin}"
KEYCLOAK_PASSWORD="${KEYCLOAK_PASSWORD:-admin}"
REALM="${REALM:-skillmatch}"

log()  { echo "[create-admin] $*"; }
err()  { echo "[create-admin] ERROR: $*" >&2; exit 1; }

require_cmd() {
  command -v "$1" &>/dev/null || err "'$1' is required but not found in PATH."
}

require_cmd curl
require_cmd jq
require_cmd openssl

# ── Prompts ──────────────────────────────────────────────────────────────────
read -rp "Email del nuovo admin: " ADMIN_EMAIL
[[ "${ADMIN_EMAIL}" =~ ^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$ ]] || err "Email non valida."

read -rp "Nome: " ADMIN_FIRST_NAME
[[ -n "${ADMIN_FIRST_NAME}" ]] || err "Il nome non può essere vuoto."

read -rp "Cognome: " ADMIN_LAST_NAME
[[ -n "${ADMIN_LAST_NAME}" ]] || err "Il cognome non può essere vuoto."

TEMP_PASSWORD=$(openssl rand -base64 18 | tr -dc 'A-Za-z0-9' | head -c 16)

# ── Admin token ──────────────────────────────────────────────────────────────
log "Ottenimento token admin da ${KEYCLOAK_URL}..."
TOKEN_RESPONSE=$(curl -sf \
  -d "client_id=admin-cli" \
  -d "username=${KEYCLOAK_ADMIN}" \
  -d "password=${KEYCLOAK_PASSWORD}" \
  -d "grant_type=password" \
  "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token")

ACCESS_TOKEN=$(echo "${TOKEN_RESPONSE}" | jq -r '.access_token')
[[ "${ACCESS_TOKEN}" == "null" || -z "${ACCESS_TOKEN}" ]] && \
  err "Impossibile ottenere il token admin. Controlla KEYCLOAK_ADMIN / KEYCLOAK_PASSWORD."

# ── Duplicate check ──────────────────────────────────────────────────────────
log "Verifica che l'email non sia già in uso..."
EXISTING_COUNT=$(curl -sf -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  "${KEYCLOAK_URL}/admin/realms/${REALM}/users?email=${ADMIN_EMAIL}&exact=true" | jq 'length')
[[ "${EXISTING_COUNT}" -eq 0 ]] || err "Esiste già un utente con l'email ${ADMIN_EMAIL}."

# ── User creation ────────────────────────────────────────────────────────────
log "Creazione utente in Keycloak..."
CREATE_RESPONSE_HEADERS=$(curl -sf -D - -o /dev/null -X POST \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  "${KEYCLOAK_URL}/admin/realms/${REALM}/users" \
  -d "$(jq -n \
    --arg email "${ADMIN_EMAIL}" \
    --arg first "${ADMIN_FIRST_NAME}" \
    --arg last "${ADMIN_LAST_NAME}" \
    --arg pwd "${TEMP_PASSWORD}" \
    '{username:$email, email:$email, firstName:$first, lastName:$last, enabled:true, emailVerified:true,
      credentials:[{type:"password", value:$pwd, temporary:true}]}')")

USER_ID=$(echo "${CREATE_RESPONSE_HEADERS}" | grep -i '^location:' | grep -oE '[0-9a-fA-F-]{36}' | tr -d '\r')
[[ -n "${USER_ID}" ]] || err "Creazione utente fallita (nessun id restituito da Keycloak)."

# ── Role assignment ──────────────────────────────────────────────────────────
log "Assegnazione del ruolo realm ADMIN..."
ROLE_JSON=$(curl -sf -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  "${KEYCLOAK_URL}/admin/realms/${REALM}/roles/ADMIN")

curl -sf -X POST \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  "${KEYCLOAK_URL}/admin/realms/${REALM}/users/${USER_ID}/role-mappings/realm" \
  -d "[${ROLE_JSON}]" > /dev/null

# ── Done ──────────────────────────────────────────────────────────────────────
log ""
log "Admin creato con successo."
log "  Email:               ${ADMIN_EMAIL}"
log "  Password temporanea: ${TEMP_PASSWORD}"
log ""
log "Keycloak obbligherà il cambio password al primo login."
log "Comunica queste credenziali all'admin su un canale sicuro (non via email in chiaro)."
