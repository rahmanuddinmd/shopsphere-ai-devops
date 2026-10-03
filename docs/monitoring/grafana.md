# Grafana

| Environment | URL | Login |
|---|---|---|
| Local | http://localhost:3001 | `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` from `.env` |
| EKS | `kubectl port-forward svc/kps-grafana 3001:80 -n monitoring` | admin / value used at install |

Dashboards are code (`monitoring/dashboards/*.json`), provisioned automatically:
- **ShopSphere - Application**: backend up, orders/hour, request rate, status codes, p50/p95/p99,
  4xx and 5xx by URI, JVM heap, CPU, threads, DB connection pool, AI endpoint latency.
- **ShopSphere - Kubernetes**: running pods, restarts, unavailable deployments, ready nodes, node
  CPU/memory, pod CPU/memory, replicas available vs desired.
- kube-prometheus-stack also ships dozens of standard Kubernetes dashboards.

**AWS infrastructure (EC2, EKS, RDS, ALB)**: add a *CloudWatch* datasource (Connections → Data sources
→ CloudWatch, auth = default credential chain / an IAM role with `CloudWatchReadOnlyAccess`) and
import panels for `AWS/ApplicationELB` (RequestCount, HTTPCode_Target_5XX_Count, TargetResponseTime),
`AWS/RDS` (CPUUtilization, FreeStorageSpace, DatabaseConnections) and `AWS/EC2`.

"Dashboard shows No data" drill: check the datasource UID (`prometheus`), the time range, then run
the panel query in Prometheus → Graph. Usually the target is DOWN or a label value differs.
