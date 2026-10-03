# Prometheus

| Tool | Answers | Scope |
|---|---|---|
| Spring Boot Actuator + Micrometer | "How is my **application** doing?" (requests, latency, errors, JVM, DB pool, orders) | inside the app, `/actuator/prometheus` |
| Node Exporter | "How is the **machine/node** doing?" (CPU, memory, disk, network) | one per node/VM |
| kube-state-metrics | "What **state** are Kubernetes objects in?" (replicas, restarts, pod phase) | cluster |
| Prometheus | Scrapes all of the above, stores time series, evaluates alert rules | pull model |
| Grafana | Visualises Prometheus (and CloudWatch, Loki) | dashboards |
| Alertmanager | Groups, deduplicates and routes alerts from Prometheus | notifications |
| CloudWatch | AWS services: EKS control plane, ALB, RDS, EC2, NAT | AWS-native |

Rule of thumb: **Prometheus = application/Kubernetes metrics, CloudWatch = AWS infrastructure.**
Don't ship the same metric to both.

## Local
```bash
docker compose --profile app --profile monitoring up -d
```
http://localhost:9090 → Status → Targets: `shopsphere-backend`, `node-exporter`, `prometheus` must be **UP**.

Useful PromQL:
```promql
sum(rate(http_server_requests_seconds_count{application="shopsphere-backend"}[5m]))            # req/s
histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket[5m])))            # p95 latency
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))                                  # 5xx/s
shopsphere_orders_placed_total                                                                    # business metric
```

## EKS
`scripts/install-cluster-addons.sh` installs kube-prometheus-stack; `kubernetes/monitoring/servicemonitor.yaml`
makes the Operator scrape every backend pod; `prometheusrule.yaml` adds CrashLoopBackOff, restarts,
unavailable deployments, node not ready, memory/CPU near limit, 5xx and backend-down alerts.

Failure drill "Prometheus target DOWN": scale the backend to 0
(`kubectl scale deploy/backend --replicas=0 -n shopsphere-dev`), watch the target disappear and
`ShopSphereBackendDown` fire, then scale back to 2.
