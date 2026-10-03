#!/usr/bin/env bash
# =====================================================================
# Installs cluster-level components on EKS after `terraform apply`:
#   1. AWS Load Balancer Controller (turns Ingress into an ALB) - uses Pod Identity from Terraform
#   2. metrics-server (needed by the HorizontalPodAutoscaler and kubectl top)
#   3. kube-prometheus-stack (Prometheus, Alertmanager, Grafana, node-exporter, kube-state-metrics)
# Usage: bash scripts/install-cluster-addons.sh [cluster-name]
# =====================================================================
set -euo pipefail
cd "$(dirname "$0")/.."
CLUSTER="${1:-shopsphere-dev}"
REGION="${AWS_REGION:-ap-south-1}"

aws eks update-kubeconfig --region "$REGION" --name "$CLUSTER"
VPC_ID="$(aws eks describe-cluster --name "$CLUSTER" --region "$REGION" --query 'cluster.resourcesVpcConfig.vpcId' --output text)"

helm repo add eks https://aws.github.io/eks-charts >/dev/null
helm repo add metrics-server https://kubernetes-sigs.github.io/metrics-server/ >/dev/null
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts >/dev/null
helm repo update >/dev/null

echo "==> AWS Load Balancer Controller"
helm upgrade --install aws-load-balancer-controller eks/aws-load-balancer-controller -n kube-system \
  --set clusterName="$CLUSTER" --set region="$REGION" --set vpcId="$VPC_ID" \
  --set serviceAccount.create=true --set serviceAccount.name=aws-load-balancer-controller --wait

echo "==> metrics-server"
helm upgrade --install metrics-server metrics-server/metrics-server -n kube-system --wait

echo "==> kube-prometheus-stack"
GRAFANA_PASSWORD="${GRAFANA_ADMIN_PASSWORD:-$(openssl rand -base64 18)}"
helm upgrade --install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  -n monitoring --create-namespace -f monitoring/kube-prometheus-stack-values.yaml \
  --set grafana.adminPassword="$GRAFANA_PASSWORD" --wait --timeout 10m
kubectl apply -f kubernetes/monitoring/
bash monitoring/grafana/dashboard-configmaps.sh

echo
echo "Add-ons installed."
echo "Grafana:      kubectl port-forward svc/kps-grafana 3001:80 -n monitoring   (user admin; password set from GRAFANA_ADMIN_PASSWORD or generated)"
echo "Prometheus:   kubectl port-forward svc/kps-prometheus 9090:9090 -n monitoring"
echo "Alertmanager: kubectl port-forward svc/kps-alertmanager 9093:9093 -n monitoring"
