# ShopSphere AI - Developer Command Center

No passwords in this file. "Credentials" says *where* the secret lives.

| Component | URL / endpoint | Port | Purpose | Credentials location | Health check |
|---|---|---|---|---|---|
| GitHub | https://github.com/rahmanuddinmd/shopsphere-ai-devops | - | Source, PRs, branch protection | Your GitHub account / PAT in password manager | Actions tab green |
| CI server (EC2) | `terraform output public_ip` in terraform/environments/tooling | 22 | Hosts Jenkins/SonarQube/Nexus (Ansible-managed) | EC2 key pair `~/.ssh/<key>.pem`; app secrets in `/opt/shopsphere/.env` (mode 600) | `ansible all -m ping` |
| Jenkins | http://localhost:8085 (or http://<ci-server-ip>:8085) | 8085 | CI/CD | Jenkins admin user; pipeline creds in Jenkins Credentials | `curl -I localhost:8085/login` |
| SonarQube | http://localhost:9000 | 9000 | Code quality, Quality Gate | SonarQube admin; token in Jenkins `sonarqube-token` | `curl localhost:9000/api/system/status` |
| Nexus | http://localhost:8081 | 8081/8082 | Maven artifacts | Nexus admin; deployer in Jenkins `nexus-credentials` | `curl localhost:8081/service/rest/v1/status` |
| Amazon ECR | AWS console → ECR | - | Container images | IAM (Jenkins user `shopsphere-dev-jenkins`) | `aws ecr describe-repositories` |
| Terraform state | S3 `shopsphere-tfstate-<account>` | - | Infrastructure state | Your AWS admin identity | `terraform plan` (no changes) |
| Amazon EKS | `shopsphere-dev` | 443 | Kubernetes | EKS access entries (IAM) | `kubectl get nodes` |
| ShopSphere (local) | http://localhost:3000 · http://localhost:5173 (dev) | 3000/5173 | Shop UI | `.env` ADMIN_EMAIL/ADMIN_PASSWORD | `curl localhost:3000/healthz` |
| ShopSphere API (local) | http://localhost:8080 | 8080 | REST API | JWT from `/api/auth/login` | `curl localhost:8080/actuator/health` |
| ShopSphere (EKS) | `http://<alb-dns>` from `kubectl get ingress -n shopsphere-dev` | 80 | Shop on AWS | K8s Secret `shopsphere-secrets` | `bash scripts/health-check.sh k8s` |
| PostgreSQL | localhost:5432 · `postgres:5432` in cluster | 5432 | Database | `.env` / K8s Secret | `pg_isready` |
| Prometheus | http://localhost:9090 · port-forward `svc/kps-prometheus` | 9090 | Metrics | none (internal only) | `/-/ready` |
| Grafana | http://localhost:3001 · port-forward `svc/kps-grafana` | 3001 | Dashboards | `.env` GRAFANA_* / Helm `--set` | `/api/health` |
| Alertmanager | http://localhost:9093 · port-forward `svc/kps-alertmanager` | 9093 | Alert routing | none | `/-/ready` |
| Mailpit | http://localhost:8025 | 8025 | Local alert inbox | none | UI loads |
| Ollama | http://localhost:11434 | 11434 | Local LLM | none | `curl localhost:11434/api/tags` |
| CloudWatch | AWS console → CloudWatch | - | AWS metrics & logs | IAM | Log group `/aws/eks/shopsphere-dev/cluster` |
