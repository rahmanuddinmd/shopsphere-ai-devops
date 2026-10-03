#!/usr/bin/env bash
# =====================================================================
# Creates the ShopSphere repositories in Nexus via the REST API (idempotent).
# Usage:  NEXUS_URL=http://localhost:8081 NEXUS_USER=admin NEXUS_PASSWORD='<password>' \
#           bash nexus/create-repositories.sh
# First admin password: docker exec shopsphere-nexus-1 cat /nexus-data/admin.password
# =====================================================================
set -euo pipefail
NEXUS_URL="${NEXUS_URL:-http://localhost:8081}"
: "${NEXUS_USER:?set NEXUS_USER}" "${NEXUS_PASSWORD:?set NEXUS_PASSWORD}"
API="${NEXUS_URL}/service/rest/v1"

exists() { curl -fsS -u "$NEXUS_USER:$NEXUS_PASSWORD" "${API}/repositories/$1" >/dev/null 2>&1; }

create() {
  local name="$1" type="$2" body="$3"
  if exists "$name"; then echo "exists   $name"; return; fi
  curl -fsS -u "$NEXUS_USER:$NEXUS_PASSWORD" -H 'Content-Type: application/json' \
    -X POST "${API}/repositories/${type}" -d "$body"
  echo "created  $name"
}

create shopsphere-maven-releases maven/hosted '{
  "name": "shopsphere-maven-releases", "online": true,
  "storage": {"blobStoreName": "default", "strictContentTypeValidation": true, "writePolicy": "ALLOW_ONCE"},
  "maven": {"versionPolicy": "RELEASE", "layoutPolicy": "STRICT"}}'

create shopsphere-maven-snapshots maven/hosted '{
  "name": "shopsphere-maven-snapshots", "online": true,
  "storage": {"blobStoreName": "default", "strictContentTypeValidation": true, "writePolicy": "ALLOW"},
  "maven": {"versionPolicy": "SNAPSHOT", "layoutPolicy": "STRICT"}}'

create shopsphere-docker docker/hosted '{
  "name": "shopsphere-docker", "online": true,
  "storage": {"blobStoreName": "default", "strictContentTypeValidation": true, "writePolicy": "ALLOW_ONCE"},
  "docker": {"v1Enabled": false, "forceBasicAuth": true, "httpPort": 8082}}'

echo "Done. Releases are write-once (ALLOW_ONCE): a released version can never be overwritten."
