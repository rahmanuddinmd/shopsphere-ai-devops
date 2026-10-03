#!/usr/bin/env bash
# Post-deployment smoke test. Usage: jenkins/scripts/smoke-test.sh http://<alb-dns-name>
# Fails (exit 1) if the frontend, backend health or product API is not working.
set -euo pipefail
BASE_URL="${1:?Usage: smoke-test.sh <base-url>}"
ATTEMPTS="${ATTEMPTS:-30}"

check() {
  local name="$1" path="$2" expect="$3"
  for _ in $(seq 1 "$ATTEMPTS"); do
    body="$(curl -fsS --max-time 10 "${BASE_URL}${path}" 2>/dev/null || true)"
    if [[ "$body" == *"$expect"* ]]; then
      echo "PASS  ${name}  (${path})"
      return 0
    fi
    sleep 10
  done
  echo "FAIL  ${name}  (${path}) - expected to contain: ${expect}"
  return 1
}

check "Frontend"        "/"                       "<div id=\"root\">"
check "Backend health"  "/actuator/health"        "\"UP\""
check "Product API"     "/api/products?size=1"    "\"content\""
check "Categories API"  "/api/categories"         "laptops"
echo "Smoke test passed for ${BASE_URL}"
