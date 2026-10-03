#!/usr/bin/env bash
# =====================================================================
# End-of-session AWS cleanup for the ShopSphere training environment.
# 1. Shows every billable resource still tagged Project=ShopSphere.
# 2. Deletes Kubernetes namespaces (removes ALBs) and runs terraform destroy for DEV.
# 3. Verifies nothing expensive is left.
# Every destructive step requires typing the confirmation phrase.
#   bash scripts/aws-cleanup.sh
# =====================================================================
set -euo pipefail
cd "$(dirname "$0")/.."
REGION="${AWS_REGION:-ap-south-1}"
CLUSTER="${CLUSTER:-shopsphere-dev}"

report() {
  echo "---- EKS clusters";        aws eks list-clusters --region "$REGION" --query 'clusters' --output text
  echo "---- Load balancers";      aws elbv2 describe-load-balancers --region "$REGION" --query 'LoadBalancers[].[LoadBalancerName,State.Code]' --output text
  echo "---- NAT gateways";        aws ec2 describe-nat-gateways --region "$REGION" --filter Name=state,Values=available,pending --query 'NatGateways[].NatGatewayId' --output text
  echo "---- Running EC2";         aws ec2 describe-instances --region "$REGION" --filters Name=instance-state-name,Values=running --query 'Reservations[].Instances[].[InstanceId,InstanceType,Tags[?Key==`Name`]|[0].Value]' --output text
  echo "---- RDS instances";       aws rds describe-db-instances --region "$REGION" --query 'DBInstances[].[DBInstanceIdentifier,DBInstanceStatus]' --output text
  echo "---- Unattached EBS";      aws ec2 describe-volumes --region "$REGION" --filters Name=status,Values=available --query 'Volumes[].[VolumeId,Size]' --output text
  echo "---- Elastic IPs";         aws ec2 describe-addresses --region "$REGION" --query 'Addresses[].[PublicIp,AssociationId]' --output text
  echo "---- Tagged Project=ShopSphere"
  aws resourcegroupstaggingapi get-resources --region "$REGION" --tag-filters Key=Project,Values=ShopSphere \
    --query 'ResourceTagMappingList[].ResourceARN' --output text | tr '\t' '\n'
}

echo "=================== BEFORE CLEANUP (${REGION}) ==================="
report

echo
echo "Step 1: delete Kubernetes namespaces (removes ALBs created by Ingress objects)."
read -r -p "Type DELETE-SHOPSPHERE to continue (anything else skips): " a1
if [[ "$a1" == "DELETE-SHOPSPHERE" ]]; then
  if aws eks describe-cluster --name "$CLUSTER" --region "$REGION" >/dev/null 2>&1; then
    aws eks update-kubeconfig --region "$REGION" --name "$CLUSTER" >/dev/null
    for ns in shopsphere-dev shopsphere-staging shopsphere-prod monitoring; do
      kubectl delete namespace "$ns" --ignore-not-found --wait=true --timeout=10m || true
    done
    helm uninstall aws-load-balancer-controller -n kube-system 2>/dev/null || true
    echo "Waiting 60 s for ALBs to be deleted..."; sleep 60
  else
    echo "Cluster $CLUSTER not found - skipping Kubernetes cleanup."
  fi
fi

echo
echo "Step 2: terraform destroy terraform/environments/dev (EKS, NAT, VPC, ECR, IAM, RDS)."
read -r -p "Type DESTROY-DEV to continue (anything else skips): " a2
if [[ "$a2" == "DESTROY-DEV" ]]; then
  (cd terraform/environments/dev && terraform destroy)
fi

echo
echo "Step 3 (optional): the CI server (terraform/environments/tooling)."
echo "  STOP   = keeps Jenkins/SonarQube/Nexus data, costs only disk (~\$4/month)."
echo "  DESTROY = deletes the VM and all CI data (end of course)."
read -r -p "Type STOP-CI, DESTROY-CI, or press Enter to skip: " a3
CI_ID="$(aws ec2 describe-instances --region "$REGION" --filters Name=tag:Name,Values=shopsphere-ci-server Name=instance-state-name,Values=running,stopped --query 'Reservations[].Instances[].InstanceId' --output text)"
if [[ "$a3" == "STOP-CI" && -n "$CI_ID" ]]; then
  aws ec2 stop-instances --region "$REGION" --instance-ids $CI_ID >/dev/null && echo "Stopping $CI_ID"
elif [[ "$a3" == "DESTROY-CI" ]]; then
  (cd terraform/environments/tooling && terraform destroy)
fi

echo
echo "=================== AFTER CLEANUP ==================="
report
echo
echo "Expected after a full cleanup: no EKS clusters, no load balancers, no NAT gateways, no RDS,"
echo "no unattached EBS volumes. The S3 state bucket (terraform/bootstrap) intentionally remains (cost: cents)."
echo "Also check the Billing console > Bills tomorrow - charges appear with a delay."
