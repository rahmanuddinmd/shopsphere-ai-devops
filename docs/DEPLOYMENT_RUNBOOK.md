# ShopSphere AI - complete deployment runbook (from an empty laptop to production)

Follow the stages in order. Each stage lists **what you run**, **what success looks like**, and the
**cost**. Sequence diagram: `diagrams/22-deployment-runbook.md`.
If any command fails: stop, read the first error, check `docs/troubleshooting/troubleshooting.md`
or paste it into Admin → AI DevOps Assistant.

| Stage | Goal | Guide | Cost |
|---|---|---|---|
| 0 | Install tools | `docs/INSTALLATION.md` | FREE |
| 1 | Run the app locally | `docs/deployment/local.md`, `docs/deployment/docker.md` | FREE |
| 2 | Push to GitHub with branch protection | `docs/devops/git.md` | FREE |
| 3 | Terraform state bucket | `docs/deployment/eks.md` §2 | FREE |
| 4 | CI server (Terraform + Ansible) | `docs/deployment/ci-server.md` | LOW (stop when idle) |
| 5 | Configure Jenkins, SonarQube, Nexus | `docs/devops/*.md` | - |
| 6 | AWS infrastructure (VPC, EKS, ECR, IAM) | `docs/deployment/eks.md` §3 | POTENTIALLY EXPENSIVE |
| 7 | Cluster add-ons + monitoring | `docs/deployment/eks.md` §4 | included |
| 8 | Pipeline deploys DEV | `docs/devops/jenkins.md` | included |
| 9 | Promote STAGING → PROD | `docs/deployment/eks.md` (promotion) | extra ALB per env |
| 10 | Clean up | `docs/aws/AWS_COST_GUIDE.md` | stops billing |

## Stage 0 - tools
```bash
bash scripts/install-tools.sh && bash scripts/setup.sh      # expect: 0 failed
```

## Stage 1 - local application
```bash
cp .env.example .env            # fill <CHANGE_ME>; JWT_SECRET from: openssl rand -base64 64 | tr -d '\n'
docker compose --profile app up -d --build
bash scripts/health-check.sh local                          # expect: All checks passed.
```
Open http://localhost:3000, log in as admin, place a test order.

## Stage 2 - GitHub
Push to `develop` via a pull request; protect `main` and `develop` (`docs/devops/git.md` §3).

## Stage 3 - Terraform state
```bash
cd terraform/bootstrap && terraform init && terraform apply && terraform output state_bucket
```

## Stage 4 - CI server
```bash
cd terraform/environments/tooling && terraform init -backend-config=backend.hcl && terraform apply
cd ../../../ansible && ansible-playbook playbooks/ci-server.yml
```
Expect the final Ansible task to list the Jenkins, SonarQube and Nexus URLs.

## Stage 5 - tool configuration (checklist)
- [ ] Jenkins: admin user, 5 credentials (`github-credentials`, `sonarqube-token`, `nexus-credentials`, `aws-jenkins`, `nvd-api-key`), SonarQube server named `sonarqube`, Multibranch job
- [ ] SonarQube: new admin password, "ShopSphere Way" Quality Gate as default, webhook to Jenkins
- [ ] Nexus: new admin password, `nexus/create-repositories.sh`, deploy user
- [ ] GitHub webhook or periodic scan

## Stage 6 - AWS infrastructure
```bash
cd terraform/environments/dev
terraform init -backend-config=backend.hcl && terraform plan -out tfplan && terraform apply tfplan
aws eks update-kubeconfig --region ap-south-1 --name shopsphere-dev && kubectl get nodes
```
Create an access key for IAM user `shopsphere-dev-jenkins` → Jenkins credential `aws-jenkins`.

## Stage 7 - cluster add-ons
```bash
export GRAFANA_ADMIN_PASSWORD='<choose>'
bash scripts/install-cluster-addons.sh shopsphere-dev
bash scripts/deploy.sh secrets dev
bash scripts/deploy.sh secrets staging
```

## Stage 8 - first pipeline run
Push to `develop` (or "Build Now"). Every stage must be green; the smoke test prints the ALB URL.
```bash
bash scripts/health-check.sh k8s shopsphere-dev
```

## Stage 9 - promotion
Approve "Promote to STAGING" in Jenkins (same image tag). For PROD: create
`terraform/environments/prod`, merge `develop` → `main` via a release branch, approve "Deploy to PROD".

## Stage 10 - cleanup (every session)
```bash
bash scripts/aws-cleanup.sh                                    # namespaces -> terraform destroy -> report
cd terraform/environments/tooling && terraform output -raw stop_command | bash   # stop the CI server
```

## Final verification checklist
- [ ] Shop works through the ALB URL (browse, cart, checkout, order history, admin)
- [ ] AI assistant answers (provider shown on `/assistant`)
- [ ] Jenkins pipeline green including Quality Gate, OWASP and Trivy
- [ ] Image tags in ECR = Git SHA; JAR in `shopsphere-maven-snapshots`
- [ ] Grafana dashboards show traffic; a test alert reaches Alertmanager
- [ ] `kubectl rollout undo` rollback tested
- [ ] `aws-cleanup.sh` report shows no EKS, NAT, ALB, RDS
