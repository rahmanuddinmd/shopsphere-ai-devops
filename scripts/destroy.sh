#!/usr/bin/env bash
# =====================================================================
# Removes the ShopSphere application from a Kubernetes namespace (NOT the AWS infrastructure).
# Deleting the namespace also deletes its Ingress, which makes the Load Balancer Controller
# delete the ALB - do this BEFORE terraform destroy, or the ALB/security groups can block VPC deletion.
#   bash scripts/destroy.sh dev
# =====================================================================
set -euo pipefail
ENV="${1:?usage: destroy.sh <local|dev|staging|prod>}"
NS="shopsphere-${ENV}"
echo "This will DELETE namespace ${NS}: deployments, database volume (data loss) and the ALB."
read -r -p "Type DELETE-${NS} to continue: " answer
[[ "$answer" == "DELETE-${NS}" ]] || { echo "Aborted - nothing deleted."; exit 1; }
kubectl delete namespace "$NS" --wait=true --timeout=10m
echo "Namespace ${NS} deleted. Check that the ALB is gone:"
echo "  aws elbv2 describe-load-balancers --query 'LoadBalancers[].LoadBalancerName'"
