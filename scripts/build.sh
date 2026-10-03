#!/usr/bin/env bash
# =====================================================================
# Builds and tests everything locally, then builds both Docker images tagged with the Git SHA.
# Usage: bash scripts/build.sh [--skip-tests]
# =====================================================================
set -euo pipefail
cd "$(dirname "$0")/.."
TAG="$(git rev-parse --short=12 HEAD 2>/dev/null || echo local)"
SKIP_TESTS="${1:-}"

echo "==> Backend (Maven)"
if [[ "$SKIP_TESTS" == "--skip-tests" ]]; then (cd backend && ./mvnw -B -ntp package -DskipTests); else (cd backend && ./mvnw -B -ntp verify); fi

echo "==> Frontend (npm)"
(cd frontend && npm ci --no-audit --no-fund && { [[ "$SKIP_TESTS" == "--skip-tests" ]] || npm test; } && npm run build)

echo "==> Docker images (tag: ${TAG} and local)"
docker build -t "shopsphere-backend:${TAG}" -t shopsphere-backend:local ./backend
docker build -t "shopsphere-frontend:${TAG}" -t shopsphere-frontend:local --build-arg VITE_BUILD_SHA="${TAG}" ./frontend

docker image ls --filter "reference=shopsphere-*" --format 'table {{.Repository}}\t{{.Tag}}\t{{.Size}}'
echo "Build complete. Scan with: trivy image --severity HIGH,CRITICAL shopsphere-backend:${TAG}"
