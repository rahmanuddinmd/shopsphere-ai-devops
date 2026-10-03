# Jenkins

## Start Jenkins
On the CI server Ansible already started it (`docs/deployment/ci-server.md`). Locally:
```bash
docker compose --profile ci up -d --build jenkins
docker compose exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```
Open http://localhost:8085 → paste the password → **Install suggested plugins** (our image already
contains the pipeline plugins from `jenkins/plugins.txt`) → create your admin user.

> Training trade-off: the container mounts `/var/run/docker.sock` and runs as root so the pipeline
> can build images. That gives Jenkins root-equivalent access to the host - acceptable on a
> single-user training VM, never in production (use dedicated agents, Kaniko/BuildKit, or
> ephemeral Kubernetes agents).

## Credentials (Manage Jenkins → Credentials → System → Global → Add credentials)

Shortcut: Jenkins Configuration-as-Code (`jenkins/casc/enabled/jenkins.yaml`) creates all of these
from `.env` values - see `docs/deployment/ci-server.md` §5. Manual steps:
| ID | Kind | Value |
|---|---|---|
| `github-credentials` | Username with password | GitHub username + fine-grained PAT |
| `sonarqube-token` | Secret text | SonarQube → My Account → Security → Generate token |
| `nexus-credentials` | Username with password | Nexus user with deploy rights |
| `aws-jenkins` | AWS Credentials | Access key of IAM user `shopsphere-dev-jenkins` (created by Terraform; create the key in the IAM console) |
| `nvd-api-key` | Secret text | Free key from https://nvd.nist.gov/developers/request-an-api-key |

Credentials are encrypted in `jenkins_home`, masked in logs, and referenced only by ID in the Jenkinsfile.

## SonarQube server (Manage Jenkins → System → SonarQube servers)
Name **`sonarqube`** (must match `withSonarQubeEnv('sonarqube')`), URL `http://sonarqube:9000`,
token credential `sonarqube-token`.

## Create the job
New Item → **Multibranch Pipeline** `shopsphere-ai-devops` → Branch source GitHub, credentials
`github-credentials`, repository `https://github.com/rahmanuddinmd/shopsphere-ai-devops` →
Build configuration: by Jenkinsfile. Multibranch is required for the `when { branch 'develop' }` rules.

## GitHub webhook
GitHub repo → Settings → Webhooks → Payload URL `http://<jenkins-public-host>:8085/github-webhook/`,
content type JSON, events: push + pull requests. A laptop behind NAT is not reachable from GitHub:
use a training EC2 VM, a tunnel, or "Scan Multibranch Pipeline Triggers → Periodically (5 min)".

## Pipeline stages
Checkout → Backend build & tests (JUnit, JaCoCo) → Frontend build & tests → SonarQube →
Quality Gate → OWASP Dependency-Check → Trivy fs & IaC → Docker build → Trivy images →
Publish JAR to Nexus → Push images to ECR → Terraform validate → Deploy DEV → Smoke test →
Approve STAGING → Deploy STAGING → Approve PROD (main only) → Deploy PROD.
CD stages run only on `develop`/`main`; feature branches and PRs run CI only.
