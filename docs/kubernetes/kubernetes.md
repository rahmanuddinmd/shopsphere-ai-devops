# Kubernetes design (industrial standards used in ShopSphere)

## Layout - kustomize base + overlays
```text
kubernetes/
├── base/                  # environment-neutral manifests
│   ├── configmap.yaml
│   ├── backend/   serviceaccount, deployment, service, hpa, pdb
│   ├── frontend/  deployment, service, pdb
│   └── database/  statefulset (+PVC template), service
├── components/aws-alb/    # Ingress (ALB), ALB health-check annotations, gp3 StorageClass
├── overlays/
│   ├── local/             # Docker Desktop/kind: 1 replica, :local images, Ollama on host
│   ├── dev/ staging/ prod # EKS: ECR images + Git SHA tag, namespace, replicas, ALB
├── monitoring/            # ServiceMonitor + PrometheusRule for kube-prometheus-stack
└── secrets.yaml.example   # never commit real secrets
```
`kubectl kustomize kubernetes/overlays/dev` prints the final YAML. Changing the image tag is a
one-line `kustomize edit set image` - the same mechanism GitOps tools (Argo CD) use.

## Why each setting exists

| Setting | Where | Why |
|---|---|---|
| `resources.requests` | all containers | Scheduler reserves this capacity; HPA % is calculated from it |
| `resources.limits` | all containers | One pod cannot starve a node; memory over limit = OOMKilled |
| `startupProbe` | backend | Spring Boot + Flyway need ~30-60 s; prevents liveness killing a starting pod |
| `readinessProbe` | backend `/actuator/health/readiness` | Pod gets traffic only when app **and DB** are ready |
| `livenessProbe` | backend `/actuator/health/liveness` | Restarts a hung JVM; does NOT include the DB (a DB outage must not restart every pod) |
| `RollingUpdate maxUnavailable 0, maxSurge 1` | Deployments | Zero-downtime deploys |
| `revisionHistoryLimit: 5` | Deployments | `kubectl rollout undo` targets |
| `preStop sleep 10` + `terminationGracePeriodSeconds` | backend | ALB deregisters the pod before Spring shuts down gracefully |
| `HorizontalPodAutoscaler` (2-4, CPU 70%) | backend | Scale out under load (needs metrics-server) |
| `PodDisruptionBudget minAvailable 1` | backend, frontend | Node drains/upgrades never take all pods |
| `topologySpreadConstraints` | backend | Pods spread across availability zones |
| `runAsNonRoot`, `readOnlyRootFilesystem`, `drop ALL`, `seccomp RuntimeDefault` | all pods | Pod Security Standard "restricted" (namespaces enforce it) |
| `ServiceAccount` + Pod Identity | backend | AWS access without keys (Bedrock only) |
| `StatefulSet` + `volumeClaimTemplates` | postgres | Stable identity `postgres-0` and its own EBS volume |
| `ConfigMap` vs `Secret` | config | Non-secret settings vs credentials (Secrets encrypted at rest by EKS/KMS) |
| `ClusterIP` Services | all | Internal only; the Ingress/ALB is the single public entry point |
| ALB `target-type: ip` | Ingress | ALB sends traffic directly to pod IPs (VPC CNI) |

## Everyday commands
```bash
kubectl get pods -n shopsphere-dev -o wide
kubectl describe pod <pod> -n shopsphere-dev             # events: probes, pulls, OOM
kubectl logs deploy/backend -n shopsphere-dev --tail=100
kubectl logs <pod> -n shopsphere-dev --previous          # logs of the crashed container
kubectl rollout status deploy/backend -n shopsphere-dev
kubectl rollout history deploy/backend -n shopsphere-dev
kubectl rollout undo deploy/backend -n shopsphere-dev    # rollback
kubectl top pods -n shopsphere-dev                       # needs metrics-server
kubectl exec -it postgres-0 -n shopsphere-dev -- psql -U shopsphere -d shopsphere
```

## Deployment strategies
| Strategy | How | Pros | Cons | ShopSphere |
|---|---|---|---|---|
| Rolling update | Replace pods gradually | Built in, no extra cost | Old+new versions run together briefly | **Default** |
| Blue/green | Two full environments, switch traffic | Instant switch & rollback | Double resources | Discussed in Phase 23 |
| Canary | Small % of traffic to new version | Lowest risk | Needs traffic splitting (ALB weights / Argo Rollouts) | Challenge exercise |
