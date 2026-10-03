# ShopSphere AI — Training Roadmap

All source code and configuration for every phase is already in this repository.
The phases are the order in which to **study, run, break and fix** each part.

Rule: one phase at a time. A phase is complete only when its verification checklist passes.
Cost ratings: **FREE** · **LOW COST** (cents to a few dollars) · **POTENTIALLY EXPENSIVE**
(time-box the session and destroy afterwards).

| Phase | Topic | Main deliverables | Runs on | AWS cost |
|---|---|---|---|---|
| 0 | Architecture & setup | Repo skeleton, conventions, diagrams, toolchain, secured AWS account | Local | FREE |
| 1 | Git + GitHub | Branching, PRs, protection, tags, auth | Local | FREE |
| 2 | Java + Spring Boot | Entities, JWT, RBAC, REST API, tests | Local | FREE |
| 3 | React | Pages, components, API client, tests | Local | FREE |
| 4 | PostgreSQL | Flyway schema, seed data, indexes, repository tests | Local (Docker) | FREE |
| 5 | Docker | Multi-stage Dockerfiles, non-root, HEALTHCHECK | Local | FREE |
| 6 | Docker Compose | Profiles: app, ai, ci, monitoring | Local | FREE |
| 7 | Jenkins + CI server | Terraform `tooling` VM, Ansible roles, custom Jenkins image, credentials (or JCasC), webhook, GitHub Actions comparison | Local / training VM | FREE / LOW COST |
| 8 | SonarQube | Analysis, coverage, Quality Gate | Local / VM | FREE / LOW COST |
| 9 | Nexus | Maven + Docker repos, `mvn deploy` | Local / VM | FREE / LOW COST |
| 10 | Trivy + OWASP | fs/image/config scans, thresholds, suppressions | Local / VM | FREE |
| 11 | AWS | IAM, CLI profiles, budgets, cost guide | AWS | FREE |
| 12 | Terraform | Bootstrap state bucket, VPC, security groups | AWS | LOW COST → NAT is POTENTIALLY EXPENSIVE |
| 13 | ECR | Immutable repos, lifecycle policy, push from Jenkins | AWS | LOW COST |
| 14 | EKS | Cluster, node group, add-ons, access entries, LB Controller | AWS | POTENTIALLY EXPENSIVE |
| 15 | Kubernetes | Manifests, probes, HPA, PDB, Ingress → ALB, Helm chart | AWS | POTENTIALLY EXPENSIVE |
| 16 | Jenkins CD | DEV deploy, smoke test, approvals, STAGING/PROD promotion, rollback | AWS | POTENTIALLY EXPENSIVE |
| 17 | Prometheus | kube-prometheus-stack, ServiceMonitor, Node Exporter, kube-state-metrics | Local + AWS | LOW COST |
| 18 | Grafana | Provisioned datasources, 3 dashboards | Local + AWS | LOW COST |
| 19 | Alertmanager | Alert rules, routing, Mailpit, failure drills | Local + AWS | LOW COST |
| 20 | CloudWatch | Control-plane logs, Container Insights, alarms, Grafana CloudWatch datasource | AWS | LOW COST (watch log volume) |
| 21 | AI Product Assistant | Ollama + Bedrock providers, grounded answers | Local / AWS | FREE / pay-per-token |
| 22 | AI DevOps Assistant | Log analysis, redaction, confirmation guardrails | Local / AWS | FREE / pay-per-token |
| 23 | Production hardening | HTTPS (ACM), RDS, Secrets Manager, network policies, capstone, release v1.0.0 | AWS | POTENTIALLY EXPENSIVE |

Each phase delivers: Objective · Architecture · Prerequisites · Files to create · Commands ·
Configuration · Code · Expected output · Verification · Troubleshooting · Hands-on exercise ·
Challenge · Interview questions.

Failure drills (wrong image tag, ImagePullBackOff, CrashLoopBackOff, Quality Gate failure,
Nexus 401, Terraform state lock, Prometheus target DOWN, etc.) are embedded in the phase where
the failing component is introduced.
