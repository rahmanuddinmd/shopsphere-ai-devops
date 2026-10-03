# ShopSphere AI — Conventions Contract

This file is the **single source of truth** for names, ports, tags and environment
variables. Spring Boot, Docker, Compose, Jenkins, Terraform, Kubernetes, Helm and the
monitoring stack must all use exactly these values. If a value changes, it changes
here first, in the same PR as the code.

## 1. Identity

| Item | Value |
|---|---|
| Product name | ShopSphere AI |
| GitHub repository | `github.com/rahmanuddinmd/shopsphere-ai-devops` |
| Java package / Maven groupId | `com.shopsphere` |
| Maven artifactId | `shopsphere-backend` |
| Initial version | `1.0.0-SNAPSHOT` |
| Database name | `shopsphere` |

## 2. Container images

| Image | Local name | ECR repository |
|---|---|---|
| Backend | `shopsphere-backend` | `<YOUR_AWS_ACCOUNT_ID>.dkr.ecr.<YOUR_AWS_REGION>.amazonaws.com/shopsphere-backend` |
| Frontend | `shopsphere-frontend` | `<YOUR_AWS_ACCOUNT_ID>.dkr.ecr.<YOUR_AWS_REGION>.amazonaws.com/shopsphere-frontend` |

### Tagging policy
| Tag | Example | Used for | Mutable? |
|---|---|---|---|
| Git short SHA (12 chars) | `shopsphere-backend:3f9c2a1b7d4e` | **Every deployment** | No |
| Semantic version | `shopsphere-backend:1.2.0` | Releases from `main` | No |
| `latest` | `shopsphere-backend:latest` | Local convenience only | Yes — never deployed |

ECR repositories are created with `image_tag_mutability = IMMUTABLE`, so a tag can never be
overwritten. A deployment therefore always refers to exactly one tested image.

## 3. Ports

| Component | Container port | Host port (Compose) | Kubernetes Service port |
|---|---|---|---|
| Frontend (Nginx, unprivileged) | 8080 | 3000 | 80 → 8080 |
| Frontend (Vite dev server) | — | 5173 | — |
| Backend (Spring Boot + Actuator) | 8080 | 8080 | 8080 |
| PostgreSQL | 5432 | 5432 | 5432 |
| Jenkins UI / agent | 8080 / 50000 | **8085** / 50000 | — |
| SonarQube | 9000 | 9000 | — |
| Nexus UI / Docker hosted repo | 8081 / 8082 | 8081 / 8082 | — |
| Prometheus | 9090 | 9090 | 9090 |
| Grafana | 3000 | **3001** | 80 (Helm chart default) |
| Alertmanager | 9093 | 9093 | 9093 |
| Node Exporter | 9100 | 9100 | 9100 |
| Mailpit (local alert inbox) | 1025 SMTP / 8025 UI | 1025 / 8025 | — |
| Ollama | 11434 | 11434 | — |

Jenkins is moved to host port 8085 and Grafana to 3001 because 8080 (backend) and
3000 (frontend) are already taken on the host.

## 4. HTTP paths

| Path | Purpose | Access |
|---|---|---|
| `/api/**` | REST API | JWT (public: product/category GET, auth) |
| `/actuator/health` | Overall health (Docker HEALTHCHECK, smoke tests) | Public |
| `/actuator/health/liveness` | Kubernetes livenessProbe | Public |
| `/actuator/health/readiness` | Kubernetes readinessProbe / startupProbe | Public |
| `/actuator/prometheus` | Prometheus scrape endpoint | Cluster-internal only |
| `/actuator/info`, `/actuator/metrics` | Build info, metrics browser | ADMIN |

The frontend always calls `/api/...`. In containers, Nginx (Compose) or the Ingress (EKS)
routes `/api` to the backend. That keeps the frontend image environment-agnostic:
**the same image is promoted DEV → STAGING → PROD** without a rebuild.

## 5. Environment variables

| Variable | Consumer | Local source | Kubernetes source |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Backend | `.env` | ConfigMap |
| `SERVER_PORT` | Backend | `.env` | ConfigMap |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | Backend | `.env` | ConfigMap |
| `DB_USERNAME`, `DB_PASSWORD` | Backend | `.env` | Secret |
| `JWT_SECRET` | Backend | `.env` | Secret |
| `JWT_EXPIRATION_MINUTES` | Backend | `.env` | ConfigMap |
| `CORS_ALLOWED_ORIGINS` | Backend | `.env` | ConfigMap |
| `ADMIN_EMAIL` | Backend | `.env` | ConfigMap |
| `ADMIN_PASSWORD` | Backend | `.env` | Secret |
| `AI_PROVIDER` (`ollama`/`bedrock`/`disabled`) | Backend | `.env` | ConfigMap |
| `LOGGING_STRUCTURED_FORMAT_CONSOLE` (`ecs`) | Backend (JSON logs) | Dockerfile default | ConfigMap |
| `OLLAMA_BASE_URL`, `OLLAMA_MODEL` | Backend | `.env` (`OLLAMA_BASE_URL_DOCKER` inside Compose) | ConfigMap |
| `AWS_REGION`, `BEDROCK_MODEL_ID` | Backend | `.env` | ConfigMap |
| `VITE_API_URL` | Frontend build | default `/api` (Vite proxy) | Build arg `/api` |
| `BACKEND_URL` | Frontend Nginx proxy | Compose `http://backend:8080` | Deployment env `http://backend:8080` |
| `IMAGE_TAG` | Backend `/actuator/info` | `local` | set by pipeline |

AWS credentials are **never** environment variables in Kubernetes. Pods get AWS access
through **EKS Pod Identity** (an IAM role bound to a Kubernetes ServiceAccount).

## 6. Environments and Kubernetes namespaces

| Environment | Namespace | EKS cluster | Terraform folder | Spring profile |
|---|---|---|---|---|
| Local | — | — | — | `local` / `docker` |
| DEV | `shopsphere-dev` | `shopsphere-dev` | `terraform/environments/dev` | `dev` |
| STAGING | `shopsphere-staging` | `shopsphere-dev` (shared, cost-safe) | `terraform/environments/staging` | `staging` |
| PROD | `shopsphere-prod` | `shopsphere-prod` (created only for the capstone) | `terraform/environments/prod` | `prod` |
| Monitoring | `monitoring` | same cluster | — | — |

Training trade-off: STAGING is a **namespace** in the DEV cluster, so you get a real
promotion flow without paying for a second EKS control plane.

## 7. AWS naming and tagging

| Resource | Name pattern |
|---|---|
| Default region | `ap-south-1` (Mumbai) — write it as `<YOUR_AWS_REGION>` in reusable files |
| Terraform state bucket | `shopsphere-tfstate-<YOUR_AWS_ACCOUNT_ID>` |
| State key | `<environment>/terraform.tfstate` |
| Resource prefix | `shopsphere-<environment>` (e.g. `shopsphere-dev-vpc`) |

Mandatory tags on every AWS resource (applied via provider `default_tags`):
`Project=ShopSphere`, `Environment=<env>`, `ManagedBy=Terraform`, `Owner=rahmanuddinmd`.
Tags let you find — and verify deletion of — every billable resource.

## 8. Jenkins credential IDs

| Credential ID | Type | Used for |
|---|---|---|
| `github-credentials` | Username + PAT | Checkout, commit status |
| `sonarqube-token` | Secret text | SonarQube analysis |
| `nexus-credentials` | Username + password | `mvn deploy`, Docker login to Nexus |
| `aws-jenkins` | AWS credentials (training) → IAM role on EC2 (hardening) | ECR push, EKS deploy, Terraform |
| `nvd-api-key` | Secret text | OWASP Dependency-Check NVD feed |

The `Jenkinsfile` references these **IDs only**, never the values.

## 9. Nexus repositories

| Repository | Format | Type |
|---|---|---|
| `shopsphere-maven-releases` | maven2 | hosted, release policy |
| `shopsphere-maven-snapshots` | maven2 | hosted, snapshot policy |
| `shopsphere-docker` | docker | hosted, HTTP connector on 8082 |

## 10. Git conventions
See `docs/devops/git.md`. Branches: `main`, `develop`, `feature/*`, `release/*`, `hotfix/*`.
Commits: Conventional Commits (`feat:`, `fix:`, `ci:`, `infra:`, `docs:`, `test:`, `refactor:`, `chore:`).

## 11. Kubernetes layout
`kubernetes/base` (shared) + `kubernetes/components/aws-alb` (EKS-only pieces) +
`kubernetes/overlays/{local,dev,staging,prod}`. Secret name: `shopsphere-secrets`
(keys `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_PASSWORD`). ConfigMap: `shopsphere-config`.
Backend ServiceAccount: `shopsphere-backend` (Pod Identity → Bedrock).
