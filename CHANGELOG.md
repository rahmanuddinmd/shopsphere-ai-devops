# Changelog

All notable changes to this project are documented here.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) · Versioning: [SemVer](https://semver.org/).

## [Unreleased]

### Added
- Ansible (`ansible/`): roles `common`, `docker`, `shopsphere_ci` to provision the CI server; passes ansible-lint (production profile).
- Terraform `environments/tooling`: Ubuntu 24.04 CI server EC2 (IMDSv2, encrypted gp3, SSM role, IP-restricted security group).
- `scripts/install-tools.sh` (Docker, JDK 21, Node 22, AWS CLI v2, Terraform, kubectl, Helm, Trivy, Ansible) and `docs/INSTALLATION.md`.
- Optional Jenkins Configuration-as-Code (`jenkins/casc/`).
- `docs/DEPLOYMENT_RUNBOOK.md` (10 stages, start to finish), `docs/deployment/ci-server.md`, diagrams 21-22.
- Spring Boot 4.1 backend (Java 21): JWT auth with ADMIN/CUSTOMER roles, catalog search, cart,
  transactional checkout, orders, admin dashboard, Flyway schema + seed data, Actuator/Prometheus metrics.
- AI: provider-neutral client (Ollama / AWS Bedrock / disabled), ShopSphere AI Assistant grounded on
  catalog data, AI DevOps Assistant with secret redaction and destructive-command flagging.
- React 19 + Vite storefront and admin console matching the approved design, with product images and tests.
- Dockerfiles (multi-stage, non-root, health checks) and Docker Compose with app/ai/ci/monitoring profiles.
- Kubernetes (kustomize base, aws-alb component, local/dev/staging/prod overlays), HPA, PDB, probes,
  ServiceMonitor and PrometheusRule.
- Terraform: S3 state bootstrap with native locking; VPC, ECR, EKS, IAM/Pod Identity, RDS, security
  groups and budget modules; dev/staging/prod environments.
- Jenkins pipeline (build, tests, SonarQube, Quality Gate, OWASP, Trivy, Nexus, ECR, deploy, smoke
  test, approvals), custom Jenkins image, GitHub Actions PR validation.
- Prometheus/Alertmanager/Grafana configuration and dashboards; kube-prometheus-stack values; optional Loki.
- Scripts: setup, build, deploy, rollback, health check, cluster add-ons, destroy, AWS cleanup.
- Documentation: application, deployment (local/Docker/Kubernetes/EKS), DevOps tools, monitoring,
  security, AI guide, AWS cost guide, troubleshooting (29 problems + failure drills), interview guide.

### Phase 0
- Repository structure, conventions, 20 architecture diagrams, Git workflow, security policy.
