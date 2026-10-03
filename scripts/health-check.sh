#!/usr/bin/env bash
# =====================================================================
# Health check for local (docker compose) or Kubernetes environments.
#   bash scripts/health-check.sh local
#   bash scripts/health-check.sh k8s shopsphere-dev
# Exit code 0 = everything healthy, 1 = at least one check failed.
# =====================================================================
set -uo pipefail
MODE="${1:-local}"
FAIL=0
pass() { printf '  \033[32m[ OK ]\033[0m %s\n' "$*"; }
fail() { printf '  \033[31m[FAIL]\033[0m %s\n' "$*"; FAIL=1; }
http() { local name="$1" url="$2" expect="${3:-}"; body="$(curl -fsS --max-time 8 "$url" 2>/dev/null)" && [[ -z "$expect" || "$body" == *"$expect"* ]] && pass "$name  $url" || fail "$name  $url"; }

if [[ "$MODE" == "local" ]]; then
  echo "Local Docker Compose environment"
  http "Frontend"      http://localhost:3000/healthz ok
  http "Backend"       http://localhost:8080/actuator/health '"UP"'
  http "Readiness/DB"  http://localhost:8080/actuator/health/readiness '"UP"'
  http "Product API"   "http://localhost:8080/api/products?size=1" '"content"'
  http "AI status"     http://localhost:8080/api/ai/status provider
  docker compose ps --format '{{.Service}} {{.Health}}' 2>/dev/null | grep -q "postgres healthy" && pass "PostgreSQL container healthy" || fail "PostgreSQL container healthy"
  curl -fsS --max-time 3 http://localhost:9090/-/ready >/dev/null 2>&1 && pass "Prometheus" || echo "  [SKIP] Prometheus (monitoring profile not running)"
  curl -fsS --max-time 3 http://localhost:3001/api/health >/dev/null 2>&1 && pass "Grafana" || echo "  [SKIP] Grafana (monitoring profile not running)"
  curl -fsS --max-time 3 http://localhost:9093/-/ready >/dev/null 2>&1 && pass "Alertmanager" || echo "  [SKIP] Alertmanager (monitoring profile not running)"
else
  NS="${2:-shopsphere-dev}"
  echo "Kubernetes namespace ${NS}"
  kubectl cluster-info >/dev/null 2>&1 && pass "kubectl can reach the cluster" || { fail "kubectl cannot reach the cluster (aws eks update-kubeconfig ...)"; exit 1; }
  for d in backend frontend; do
    want=$(kubectl get deploy "$d" -n "$NS" -o jsonpath='{.spec.replicas}' 2>/dev/null)
    have=$(kubectl get deploy "$d" -n "$NS" -o jsonpath='{.status.availableReplicas}' 2>/dev/null)
    [[ -n "$want" && "$want" == "${have:-0}" ]] && pass "Deployment $d $have/$want available" || fail "Deployment $d ${have:-0}/${want:-?} available"
  done
  [[ "$(kubectl get pod postgres-0 -n "$NS" -o jsonpath='{.status.containerStatuses[0].ready}' 2>/dev/null)" == "true" ]] && pass "PostgreSQL pod ready" || fail "PostgreSQL pod ready"
  restarts=$(kubectl get pods -n "$NS" -o jsonpath='{range .items[*]}{.status.containerStatuses[0].restartCount}{"\n"}{end}' | awk '{s+=$1} END {print s+0}')
  [[ "$restarts" -lt 5 ]] && pass "Total container restarts: $restarts" || fail "Total container restarts: $restarts (check kubectl describe pod)"
  HOST=$(kubectl get ingress shopsphere -n "$NS" -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null)
  if [[ -n "$HOST" ]]; then
    pass "Ingress has ALB: $HOST"
    http "Frontend via ALB" "http://$HOST/" 'id="root"'
    http "Backend via ALB"  "http://$HOST/actuator/health" '"UP"'
  else
    fail "Ingress has no ALB address yet (kubectl describe ingress shopsphere -n $NS)"
  fi
  kubectl get pods -n monitoring 2>/dev/null | grep -q "kps-prometheus\|prometheus-kps" && pass "Prometheus running in monitoring" || echo "  [SKIP] kube-prometheus-stack not installed"
  kubectl get deploy -n monitoring 2>/dev/null | grep -q grafana && pass "Grafana running in monitoring" || echo "  [SKIP] Grafana not installed"
fi
echo; [[ $FAIL -eq 0 ]] && echo "All checks passed." || echo "Some checks failed - see above (or paste into the AI DevOps Assistant)."
exit $FAIL
