# Alertmanager

Local: alerts are e-mailed to **Mailpit** (http://localhost:8025) - a free local inbox.
Routing (`monitoring/alertmanager/alertmanager.yml`): `severity=critical` → `oncall@`, everything
else → `devops-team@`; grouped by alertname+severity; repeats every 1 h (critical) / 4 h.
Inhibition: when `ShopSphereBackendDown` fires, related warnings for the same instance are muted.

| Alert | Fires when | Severity |
|---|---|---|
| ShopSphereBackendDown | backend not scrapeable for 1 min | critical |
| ShopSphereHigh5xxRate | >5% 5xx for 5 min | critical |
| ShopSphereSlowResponses | p95 > 1 s for 10 min | warning |
| ShopSphereJvmHeapHigh | heap >85% for 10 min | warning |
| HostHighCpu / HostHighMemory / HostDiskAlmostFull | >85% / >90% / >85% | warning |
| ShopSpherePodCrashLooping (EKS) | CrashLoopBackOff for 5 min | critical |
| ShopSpherePodRestarting (EKS) | >3 restarts in 30 min | warning |
| ShopSphereDeploymentUnavailable (EKS) | available < desired for 10 min | warning |
| ShopSphereNodeNotReady (EKS) | node NotReady 5 min | critical |

Test locally: `docker compose stop backend` → after ~1-2 min the alert is *Firing* in
http://localhost:9093 and an e-mail appears in Mailpit. `docker compose start backend` → "resolved" mail.
Silence during maintenance: Alertmanager UI → New Silence. Production receivers: Slack, PagerDuty,
Amazon SNS (add to `receivers:`).
