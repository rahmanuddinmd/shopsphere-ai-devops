#!/usr/bin/env bash
# Loads the JSON dashboards into Grafana on EKS (kube-prometheus-stack sidecar picks up label grafana_dashboard=1).
set -euo pipefail
cd "$(dirname "$0")/../dashboards"
for f in *.json; do
  name="shopsphere-$(basename "$f" .json)"
  kubectl create configmap "$name" -n monitoring --from-file="$f" --dry-run=client -o yaml \
    | kubectl label --local -f - grafana_dashboard=1 -o yaml \
    | kubectl apply -f -
done
echo "Dashboards loaded. Open Grafana: kubectl port-forward svc/kps-grafana 3001:80 -n monitoring"
