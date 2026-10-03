# ShopSphere AI — Architecture

> Diagrams referenced here live in `/diagrams`. Naming, ports and variables are defined in
> [`conventions.md`](conventions.md).

## 1. Purpose
ShopSphere AI is a deliberately small e-commerce application used as the workload for a
complete industrial delivery platform: Git → Jenkins → quality and security gates →
artifacts → Terraform-provisioned AWS → EKS → observability → AI-assisted operations.

## 2. Key architecture decisions (ADR summary)

| # | Decision | Alternatives | Why this choice |
|---|---|---|---|
| 1 | **Modular monolith** backend | Microservices from day one | One deployable keeps CI/CD, debugging and cost manageable; module boundaries (`user`, `product`, `cart`, `order`, `ai`, `admin`) allow later extraction |
| 2 | **Java 21 + Spring Boot 4.x** | Spring Boot 3.5 | 3.5 left open-source support in June 2026; 4.x is the supported line |
| 3 | **Flyway** migrations | `ddl-auto=update` | Versioned, reviewable, repeatable schema changes — the industry norm |
| 4 | **JWT stateless auth** | Server sessions | Works with multiple backend replicas behind a load balancer |
| 5 | Frontend calls relative **`/api`** | Bake backend URL into the bundle | Same image across all environments (build once, promote many) |
| 6 | **Immutable SHA image tags** | `latest` | Deployments are reproducible and roll back to an exact image |
| 7 | **Jenkins** primary CI/CD, GitHub Actions for PR checks only | Only one tool | Shows both models without duplicating pipelines |
| 8 | **Nexus** for Maven artifacts, **ECR** for EKS images | One registry | EKS pulls from ECR natively with IAM; Nexus is the classic enterprise artifact store |
| 9 | **Terraform** with local wrapper modules and S3 native state locking | Console clicks, DynamoDB locking | Reproducible infra; DynamoDB locking is deprecated in favour of `use_lockfile` |
| 10 | DEV PostgreSQL as an in-cluster **StatefulSet**, **RDS optional** | RDS always | Saves cost while training; RDS module shows the production path |
| 11 | **AWS Load Balancer Controller** + Ingress → ALB | Service type LoadBalancer (NLB/CLB) | One ALB, path-based routing, health checks |
| 12 | **kube-prometheus-stack** Helm chart on EKS | Hand-written Prometheus manifests | Bundles Prometheus Operator, Alertmanager, Grafana, Node Exporter, kube-state-metrics — the de facto standard |
| 13 | **EKS Pod Identity** for Bedrock and LB Controller | Access keys, IRSA | No static keys; simplest current AWS-recommended mechanism |
| 14 | **AI behind an interface** (`AI_PROVIDER`) | Hard-wired provider | Swap Ollama/Bedrock by configuration; `disabled` keeps the shop working without AI |
| 15 | Single NAT Gateway, STAGING as a namespace | Per-AZ NAT, separate clusters | Cost-safe training mode; production differences are documented |

## 3. Component responsibilities

| Component | Responsibility | Does NOT do |
|---|---|---|
| React frontend | UI, routing, JWT handling, calls `/api` | Business rules, price calculation |
| Spring Boot API | Auth, RBAC, catalog, cart, checkout, orders, admin, AI orchestration, metrics | Serve the UI |
| `ai` module | Product Assistant (grounded on DB products) and DevOps Assistant (log analysis, ADMIN only) | Execute commands — it only recommends |
| PostgreSQL | System of record, constraints, indexes | Store plain-text passwords |
| Nginx (frontend image) | Serve static files, proxy `/api` in Compose, security headers | — |
| GitHub | Source of truth, PRs, branch protection, webhooks | Primary CI |
| GitHub Actions | Fast PR validation (compile, unit test, lint) | Build/push images, deploy |
| Jenkins | Full CI/CD pipeline and promotions | Store secrets in code |
| SonarQube | SAST, coverage, duplication, Quality Gate | Dependency CVEs |
| OWASP Dependency-Check | Known CVEs in Java dependencies | Container OS packages |
| Trivy | Filesystem/secret scan, image CVEs, IaC misconfiguration | Code quality |
| Nexus | Versioned Maven artifacts (and optional Docker images) | Serve images to EKS |
| ECR | Immutable images for EKS | Maven artifacts |
| Terraform | Create/destroy all AWS resources (EKS stack and the CI server VM) | Configure software inside VMs |
| Ansible | Configure the CI server: packages, Docker, swap/sysctl, CI stack via Compose | Create cloud resources |
| EKS / Kubernetes | Run, heal, scale and roll out containers | Provision AWS networking |
| AWS LB Controller | Turn Ingress objects into an ALB | — |
| Prometheus | Scrape and store application/Kubernetes metrics, evaluate alert rules | AWS service metrics |
| Alertmanager | Group, deduplicate, route alerts | Detect problems |
| Grafana | Dashboards over Prometheus, CloudWatch, (Loki) | Store metrics |
| Node Exporter | Host/node OS metrics | Pod object state |
| kube-state-metrics | State of Kubernetes objects (deployments, pods, restarts) | CPU usage |
| CloudWatch | AWS infrastructure metrics and logs (EKS control plane, ALB, RDS, EC2) | App-level business metrics |

## 4. Local development architecture
Docker Compose uses **profiles** so you only start what your RAM can afford:

| Profile | Services | Approx. RAM |
|---|---|---|
| `app` | postgres, backend, frontend | ~1.5 GB |
| `ai` | ollama | 2–4 GB (model dependent) |
| `ci` | jenkins, sonarqube, nexus | ~6 GB |
| `monitoring` | prometheus, grafana, alertmanager, node-exporter, mailpit | ~1 GB |

```bash
docker compose --profile app up -d
docker compose --profile app --profile monitoring up -d
docker compose --profile ci up -d          # preferably on a dedicated training VM
docker compose down
```

On a 16 GB machine, run `ci` on a separate VM (EC2 `t3.large`, stopped when idle) or run
only one profile at a time. See diagram `11-docker-architecture.md`.

## 5. Request flow (EKS)
Browser → ALB (HTTP; HTTPS with ACM in the hardening phase) → Ingress rules:
`/` → frontend Service → frontend Pods; `/api` and `/actuator/health` → backend Service →
backend Pods → PostgreSQL (StatefulSet or RDS) and AI provider (Ollama or Bedrock via Pod Identity).

## 6. Environment promotion
Build once on `develop` → deploy SHA to DEV → automated smoke test → manual approval →
same SHA to STAGING → manual approval → same SHA (plus semver tag) to PROD.
Rollback = redeploy the previous SHA or `kubectl rollout undo`.

## 7. Diagram index
| # | File |
|---|---|
| 1 | `diagrams/01-overall-architecture.md` |
| 2 | `diagrams/02-application-architecture.md` |
| 3 | `diagrams/03-backend-architecture.md` |
| 4 | `diagrams/04-frontend-architecture.md` |
| 5 | `diagrams/05-database-architecture.md` |
| 6 | `diagrams/06-git-workflow.md` |
| 7 | `diagrams/07-jenkins-architecture.md` |
| 8 | `diagrams/08-ci-pipeline.md` |
| 9 | `diagrams/09-cd-pipeline.md` |
| 10 | `diagrams/10-devsecops-pipeline.md` |
| 11 | `diagrams/11-docker-architecture.md` |
| 12 | `diagrams/12-aws-architecture.md` |
| 13 | `diagrams/13-terraform-architecture.md` |
| 14 | `diagrams/14-eks-architecture.md` |
| 15 | `diagrams/15-kubernetes-architecture.md` |
| 16 | `diagrams/16-prometheus-architecture.md` |
| 17 | `diagrams/17-grafana-architecture.md` |
| 18 | `diagrams/18-alertmanager-architecture.md` |
| 19 | `diagrams/19-ai-architecture.md` |
| 20 | `diagrams/20-end-to-end-architecture.md` |
| 21 | `diagrams/21-ci-server-provisioning.md` |
| 22 | `diagrams/22-deployment-runbook.md` |
