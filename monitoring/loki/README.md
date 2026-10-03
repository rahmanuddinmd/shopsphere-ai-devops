# Optional logging stack: Loki + Promtail + Grafana

Kept optional so the basic monitoring stack stays light. CloudWatch Container Insights
(Phase 20) is the default log destination on EKS.

```bash
helm repo add grafana https://grafana.github.io/helm-charts
helm upgrade --install loki grafana/loki -n monitoring -f monitoring/loki/loki-values.yaml
helm upgrade --install promtail grafana/promtail -n monitoring \
  --set "config.clients[0].url=http://loki-gateway.monitoring.svc/loki/api/v1/push"
```

Add Loki as a Grafana datasource (URL `http://loki-gateway.monitoring.svc`). Backend logs are
JSON (ECS format), so in Grafana Explore you can query:

```text
{namespace="shopsphere-dev", container="backend"} | json | log_level="ERROR"
```

Remove when done: `helm uninstall promtail loki -n monitoring` (requires confirmation in training).
