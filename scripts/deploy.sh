#!/usr/bin/env bash
# =====================================================================
# Deploys ShopSphere to Kubernetes with kustomize.
#
#   bash scripts/deploy.sh secrets <env>          create/update Secret shopsphere-secrets from .env
#   bash scripts/deploy.sh local                  local cluster (Docker Desktop/kind), images :local
#   bash scripts/deploy.sh dev <git-sha>          EKS DEV with ECR images tagged <git-sha>
#   bash scripts/deploy.sh staging <git-sha>      promote the SAME tag to STAGING
#   bash scripts/deploy.sh rollback <env>         kubectl rollout undo (backend + frontend)
# Requires: kubectl (with kustomize built in), aws CLI for EKS environments.
# =====================================================================
set -euo pipefail
cd "$(dirname "$0")/.."
ACTION="${1:?usage: deploy.sh <secrets|local|dev|staging|prod|rollback> [arg]}"
REGION="${AWS_REGION:-ap-south-1}"

ns() { echo "shopsphere-$1"; }

load_env() {
  [[ -f .env ]] || { echo "Missing .env - copy .env.example to .env and fill in values"; exit 1; }
  set -a; source .env; set +a
}

create_secret() {
  local env="$1"; load_env
  kubectl create namespace "$(ns "$env")" --dry-run=client -o yaml | kubectl apply -f -
  kubectl create secret generic shopsphere-secrets -n "$(ns "$env")" \
    --from-literal=DB_USERNAME="${DB_USERNAME:-shopsphere}" \
    --from-literal=DB_PASSWORD="${DB_PASSWORD:?DB_PASSWORD missing in .env}" \
    --from-literal=JWT_SECRET="${JWT_SECRET:?JWT_SECRET missing in .env}" \
    --from-literal=ADMIN_PASSWORD="${ADMIN_PASSWORD:?ADMIN_PASSWORD missing in .env}" \
    --dry-run=client -o yaml | kubectl apply -f -
  echo "Secret shopsphere-secrets applied in $(ns "$env") (values never printed)."
}

wait_rollout() {
  kubectl rollout status statefulset/postgres -n "$1" --timeout=300s
  kubectl rollout status deployment/backend  -n "$1" --timeout=300s
  kubectl rollout status deployment/frontend -n "$1" --timeout=180s
  kubectl get pods,svc,ingress -n "$1"
}

case "$ACTION" in
  secrets)
    create_secret "${2:?environment}" ;;
  local)
    kubectl get secret shopsphere-secrets -n shopsphere-local >/dev/null 2>&1 || create_secret local
    kubectl apply -k kubernetes/overlays/local
    wait_rollout shopsphere-local
    echo "Open the shop: kubectl port-forward svc/frontend 3000:80 -n shopsphere-local  ->  http://localhost:3000" ;;
  dev|staging|prod)
    TAG="${2:?usage: deploy.sh $ACTION <git-sha-tag>}"
    ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
    REGISTRY="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"
    for repo in shopsphere-backend shopsphere-frontend; do
      aws ecr describe-images --repository-name "$repo" --image-ids imageTag="$TAG" --region "$REGION" >/dev/null \
        || { echo "Image $repo:$TAG not found in ECR - push it first (ImagePullBackOff prevention)"; exit 1; }
    done
    kubectl get secret shopsphere-secrets -n "$(ns "$ACTION")" >/dev/null 2>&1 || create_secret "$ACTION"
    BUILD_TAG="manual-$(whoami)" jenkins/scripts/deploy-k8s.sh "$ACTION" "$TAG" "$REGISTRY"
    HOST="$(kubectl get ingress shopsphere -n "$(ns "$ACTION")" -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')"
    echo "Application URL (ALB DNS can take 2-3 minutes): http://${HOST}" ;;
  rollback)
    ENV="${2:?environment}"
    kubectl rollout history deployment/backend -n "$(ns "$ENV")"
    kubectl rollout undo deployment/backend  -n "$(ns "$ENV")"
    kubectl rollout undo deployment/frontend -n "$(ns "$ENV")"
    wait_rollout "$(ns "$ENV")" ;;
  *)
    echo "Unknown action: $ACTION"; exit 1 ;;
esac
