#!/usr/bin/env bash
# Deploys a given image tag to an EKS environment with kustomize. Used by the Jenkinsfile.
# Usage: jenkins/scripts/deploy-k8s.sh <dev|staging|prod> <image-tag> <ecr-registry>
set -euo pipefail
ENVIRONMENT="${1:?env}"; TAG="${2:?tag}"; REGISTRY="${3:?registry}"
NAMESPACE="shopsphere-${ENVIRONMENT}"
WORKDIR="$(mktemp -d)"
cp -r kubernetes "${WORKDIR}/"
cd "${WORKDIR}/kubernetes/overlays/${ENVIRONMENT}"
kustomize edit set image \
  "shopsphere-backend=${REGISTRY}/shopsphere-backend:${TAG}" \
  "shopsphere-frontend=${REGISTRY}/shopsphere-frontend:${TAG}"
kustomize build . | kubectl apply -f -
kubectl rollout status deployment/backend  -n "${NAMESPACE}" --timeout=300s
kubectl rollout status deployment/frontend -n "${NAMESPACE}" --timeout=180s
kubectl annotate deployment/backend deployment/frontend -n "${NAMESPACE}" --overwrite \
  kubernetes.io/change-cause="Jenkins ${BUILD_TAG:-manual} image ${TAG}"
rm -rf "${WORKDIR}"
