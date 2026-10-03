# Troubleshooting guide

Every entry: **Symptoms → Diagnosis commands → Root cause → Fix → Verification.**
Tip: paste the failing log into **Admin → AI DevOps Assistant** for a structured analysis (secrets are redacted, nothing is executed).
General method: read the *first* error, reproduce with the smallest command, change one thing at a time, verify.

| # | Problem |
|---|---|
| 1 | Git push rejected / failure |
| 2 | GitHub authentication failure |
| 3 | Jenkins not starting |
| 4 | Java version mismatch |
| 5 | Maven build failure |
| 6 | SonarQube failure |
| 7 | Nexus failure |
| 8 | Docker failure |
| 9 | Docker image build failure |
| 10 | Trivy failure |
| 11 | Terraform failure |
| 12 | Terraform state lock |
| 13 | AWS IAM failure |
| 14 | ECR failure |
| 15 | EKS failure |
| 16 | kubectl failure |
| 17 | Ingress failure |
| 18 | ALB failure / health check |
| 19 | PostgreSQL failure |
| 20 | Pod CrashLoopBackOff |
| 21 | ImagePullBackOff |
| 22 | Readiness probe failure |
| 23 | Service port mismatch |
| 24 | Prometheus failure |
| 25 | Grafana failure |
| 26 | Alertmanager failure |
| 27 | AI failure |
| 28 | Jenkins credential failure |
| 29 | GitHub webhook failure |

## 1. Git push rejected / failure
- **Symptoms:** `! [rejected] main -> main (fetch first)` or `protected branch`
- **Diagnosis:** `git status` · `git log --oneline -5` · `git remote -v`
- **Root cause:** Remote has commits you don't have, or you pushed to a protected branch.
- **Fix:** `git pull --rebase origin <branch>` then push; for `main`/`develop` push a feature branch and open a PR.
- **Verification:** `git push` succeeds; PR visible on GitHub.

## 2. GitHub authentication failure
- **Symptoms:** `Authentication failed` / `Permission denied (publickey)`
- **Diagnosis:** `ssh -T git@github.com` · `git remote -v`
- **Root cause:** Password used instead of a PAT, expired PAT, or SSH key not added.
- **Fix:** Use a fine-grained PAT as password, or add `~/.ssh/id_ed25519.pub` in GitHub → SSH keys; switch remote with `git remote set-url origin git@github.com:rahmanuddinmd/shopsphere-ai-devops.git`.
- **Verification:** `ssh -T` greets you by username; push works.

## 3. Jenkins not starting
- **Symptoms:** Container restarts; UI on :8085 unreachable
- **Diagnosis:** `docker compose logs jenkins` · `docker compose ps`
- **Root cause:** Port 8085 in use, not enough RAM, or corrupted `jenkins_home`.
- **Fix:** Free the port (`sudo lsof -i :8085`), give Docker ≥4 GB, or stop other profiles.
- **Verification:** http://localhost:8085 shows the unlock page.

## 4. Java version mismatch
- **Symptoms:** `release version 21 not supported` / `UnsupportedClassVersionError`
- **Diagnosis:** `java -version` · `./mvnw -v` · `echo $JAVA_HOME`
- **Root cause:** Maven runs on an older JDK than the project's Java 21.
- **Fix:** Install Temurin 21 and point `JAVA_HOME` to it (`sdk use java 21...`).
- **Verification:** `./mvnw -v` shows Java 21; build passes.

## 5. Maven build failure
- **Symptoms:** `BUILD FAILURE`, compilation or test errors
- **Diagnosis:** `./mvnw -B -e verify` · open `target/surefire-reports/*.txt`
- **Root cause:** Compile error, failing test, or dependency not resolvable.
- **Fix:** Fix the first error in the log (not the last); for dependency issues run `./mvnw -U dependency:resolve`.
- **Verification:** `./mvnw verify` → BUILD SUCCESS.

## 6. SonarQube failure
- **Symptoms:** `Not authorized` / connection refused / Quality Gate timeout
- **Diagnosis:** `curl -s localhost:9000/api/system/status` · Jenkins console
- **Root cause:** Wrong token, wrong server name in Jenkins, SonarQube not ready, or missing webhook.
- **Fix:** Regenerate token → `sonarqube-token`; server name must be `sonarqube`; add webhook `http://jenkins:8080/sonarqube-webhook/`; if the *gate* failed, fix the code.
- **Verification:** Stage green; project shows Passed.

## 7. Nexus failure
- **Symptoms:** `return code is: 401` / `405` on `mvn deploy`
- **Diagnosis:** `curl -u user:pw http://nexus:8081/service/rest/v1/status/check`
- **Root cause:** Wrong credentials, server `<id>` mismatch, deploying a release twice (write-once), or wrong URL.
- **Fix:** Fix `nexus-credentials`; ids must be `nexus-releases`/`nexus-snapshots`; bump the version for releases; run `nexus/create-repositories.sh`.
- **Verification:** Artifact appears in the repository browser.

## 8. Docker failure
- **Symptoms:** `permission denied ... docker.sock` or `Cannot connect to the Docker daemon`
- **Diagnosis:** `docker info` · `groups` · `systemctl status docker`
- **Root cause:** User not in `docker` group, or engine stopped.
- **Fix:** `sudo usermod -aG docker $USER && newgrp docker`; start Docker Desktop / `sudo systemctl start docker`.
- **Verification:** `docker ps` works without sudo.

## 9. Docker image build failure
- **Symptoms:** `COPY failed`, `npm ci` errors, `mvnw: not found`
- **Diagnosis:** `docker build --progress=plain ./backend`
- **Root cause:** Wrong build context, file excluded by `.dockerignore`, CRLF line endings in `mvnw`, missing `package-lock.json`.
- **Fix:** Build from the right folder; `git add --renormalize .` (CRLF); commit the lockfile.
- **Verification:** Image builds; `docker image ls shopsphere-*`.

## 10. Trivy failure
- **Symptoms:** Stage fails with CRITICAL findings / `failed to download DB`
- **Diagnosis:** Read `security/reports/trivy-*.txt`
- **Root cause:** Vulnerable base image/library, or no internet for the vulnerability DB.
- **Fix:** Update base image or library; for offline agents use `--skip-db-update` with a cached DB; accept risks only in `.trivyignore` with reason/date.
- **Verification:** Trivy stage green.

## 11. Terraform failure
- **Symptoms:** `Error: Invalid value for variable`, provider/plugin errors
- **Diagnosis:** `terraform validate` · `terraform version` · `TF_LOG=DEBUG terraform plan`
- **Root cause:** Variable violates a `validation` block, Terraform < 1.10, provider download blocked.
- **Fix:** Fix `terraform.tfvars` (e.g. `environment = "dev"`), upgrade Terraform, re-run `terraform init -upgrade`.
- **Verification:** `terraform plan` completes.

## 12. Terraform state lock
- **Symptoms:** `Error acquiring the state lock` (S3 `.tflock`)
- **Diagnosis:** Read `Lock Info` (who/when) · check if another apply runs
- **Root cause:** Concurrent run or a crashed run left the lock file.
- **Fix:** Wait for the other run; only if none is running: `terraform force-unlock <LOCK_ID>` (requires confirmation).
- **Verification:** `terraform plan` runs.

## 13. AWS IAM failure
- **Symptoms:** `AccessDenied` / `UnauthorizedOperation` / `is not authorized to perform`
- **Diagnosis:** `aws sts get-caller-identity` · read the action and resource in the error
- **Root cause:** The caller lacks that specific permission.
- **Fix:** Grant only that action to the role/user (least privilege); for Jenkins update `modules/iam` and `terraform apply`.
- **Verification:** The failing call succeeds.

## 14. ECR failure
- **Symptoms:** `no basic auth credentials` / `denied` / `tag invalid: already exists`
- **Diagnosis:** `aws ecr describe-repositories` · `aws ecr describe-images --repository-name shopsphere-backend`
- **Root cause:** Not logged in (token expires after 12 h), wrong account/region, or immutable tag reused.
- **Fix:** `aws ecr get-login-password ... | docker login`; use a new Git SHA tag (immutability is intentional).
- **Verification:** `docker push` succeeds.

## 15. EKS failure
- **Symptoms:** Cluster/node group stuck `CREATING`, nodes `NotReady`
- **Diagnosis:** `aws eks describe-cluster --name shopsphere-dev` · `kubectl get nodes` · `kubectl describe node`
- **Root cause:** Subnets without NAT route, instance type not available in AZ, vCPU quota.
- **Fix:** Keep the Terraform VPC module defaults (NAT + tags); change instance type; request quota increase.
- **Verification:** `kubectl get nodes` → Ready.

## 16. kubectl failure
- **Symptoms:** `Unable to connect to the server` / `You must be logged in` / `Unauthorized`
- **Diagnosis:** `kubectl config current-context` · `aws sts get-caller-identity`
- **Root cause:** Kubeconfig points elsewhere, expired SSO session, or no EKS access entry for your principal.
- **Fix:** `aws eks update-kubeconfig --name shopsphere-dev --region ap-south-1`; `aws sso login`; add an access entry for your IAM principal.
- **Verification:** `kubectl get ns` works.

## 17. Ingress failure
- **Symptoms:** Ingress has no ADDRESS
- **Diagnosis:** `kubectl describe ingress shopsphere -n shopsphere-dev` · `kubectl logs -n kube-system deploy/aws-load-balancer-controller`
- **Root cause:** LB Controller not installed, no Pod Identity, missing subnet tags, wrong `ingressClassName`.
- **Fix:** Run `scripts/install-cluster-addons.sh`; Terraform tags public subnets with `kubernetes.io/role/elb=1`; class must be `alb`.
- **Verification:** ADDRESS shows an ALB DNS name.

## 18. ALB failure / health check
- **Symptoms:** ALB returns 502/503; targets unhealthy
- **Diagnosis:** EC2 → Target groups → Targets · `kubectl get endpoints -n shopsphere-dev`
- **Root cause:** Health-check path wrong, pods not Ready, security groups.
- **Fix:** Paths come from the Service annotations (`/actuator/health/readiness`, `/healthz`); fix readiness first.
- **Verification:** Targets healthy; site loads.

## 19. PostgreSQL failure
- **Symptoms:** `Connection refused` / `password authentication failed`
- **Diagnosis:** `kubectl get pods -l app.kubernetes.io/name=postgres -n shopsphere-dev` · `kubectl logs postgres-0 -n shopsphere-dev` · `kubectl get pvc -n shopsphere-dev`
- **Root cause:** DB pod not running (PVC Pending: no StorageClass/EBS CSI), wrong secret values, DB_HOST wrong.
- **Fix:** Ensure gp3 StorageClass + `aws-ebs-csi-driver` add-on; recreate the Secret with matching values (an existing volume keeps its first password!).
- **Verification:** `kubectl exec postgres-0 -- pg_isready` → accepting connections.

## 20. Pod CrashLoopBackOff
- **Symptoms:** STATUS CrashLoopBackOff, RESTARTS increasing
- **Diagnosis:** `kubectl logs <pod> --previous -n shopsphere-dev` · `kubectl describe pod <pod>`
- **Root cause:** App exits on start: missing env/secret (e.g. JWT_SECRET), DB unreachable, OOMKilled.
- **Fix:** Fix the cause shown in the previous logs; for OOMKilled raise memory limit.
- **Verification:** Pod Running 1/1, restarts stop increasing.

## 21. ImagePullBackOff
- **Symptoms:** STATUS ImagePullBackOff / ErrImagePull
- **Diagnosis:** `kubectl describe pod <pod>` (Events) · `aws ecr describe-images --repository-name shopsphere-backend`
- **Root cause:** Tag doesn't exist (wrong SHA), wrong repository/account/region, or private registry without permission.
- **Fix:** Deploy a tag that exists in ECR (`deploy.sh` checks this); nodes pull from ECR via their IAM role.
- **Verification:** Pod starts; `kubectl rollout status` succeeds.

## 22. Readiness probe failure
- **Symptoms:** READY 0/1, events `Readiness probe failed: HTTP 503`
- **Diagnosis:** `kubectl describe pod` · `kubectl port-forward deploy/backend 8080 -n shopsphere-dev` then `curl localhost:8080/actuator/health/readiness`
- **Root cause:** App not ready (DB down), wrong port/path, startup slower than probes allow.
- **Fix:** Fix DB connectivity; ports must be `http`/8080; startupProbe allows 150 s.
- **Verification:** READY 1/1.

## 23. Service port mismatch
- **Symptoms:** Ingress/ALB 503, `kubectl get endpoints` empty or wrong port
- **Diagnosis:** `kubectl get svc,endpoints -n shopsphere-dev`
- **Root cause:** Service `targetPort` or selector does not match the pod's named port/labels.
- **Fix:** Services target the named port `http`; labels `app.kubernetes.io/name` must match.
- **Verification:** Endpoints list pod IPs:8080.

## 24. Prometheus failure
- **Symptoms:** Target DOWN / no ShopSphere target
- **Diagnosis:** Prometheus → Status → Targets · `kubectl get servicemonitor -n monitoring`
- **Root cause:** Backend down, ServiceMonitor label `release` doesn't match chart, wrong port name/path.
- **Fix:** Label `release: kube-prometheus-stack`, port `http`, path `/actuator/prometheus`.
- **Verification:** Target UP; `up{service="backend"}` = 1.

## 25. Grafana failure
- **Symptoms:** Login fails or dashboards show No data
- **Diagnosis:** `docker compose logs grafana` · Explore → run the panel query
- **Root cause:** Wrong admin password, datasource UID mismatch, wrong time range, target DOWN.
- **Fix:** Reset password (`grafana cli admin reset-admin-password`), datasource UID `prometheus`, fix the target first.
- **Verification:** Panels show data.

## 26. Alertmanager failure
- **Symptoms:** Alerts never arrive
- **Diagnosis:** http://localhost:9093 · `amtool check-config monitoring/alertmanager/alertmanager.yml`
- **Root cause:** Rule never fires (condition/`for`), route doesn't match, SMTP wrong.
- **Fix:** Check Prometheus → Alerts first; verify Mailpit host `mailpit:1025`.
- **Verification:** Mail visible in Mailpit.

## 27. AI failure
- **Symptoms:** Assistant answers "(AI model unavailable)" / timeouts
- **Diagnosis:** `curl localhost:8080/api/ai/status` · backend logs `Ollama call failed` / `Bedrock call failed`
- **Root cause:** Ollama not running or model not pulled; Bedrock model access not enabled / wrong model ID / no IAM permission.
- **Fix:** `ollama pull llama3.2:3b`; enable the model in Bedrock and set `BEDROCK_MODEL_ID`; check Pod Identity association.
- **Verification:** `fallback: false` in `/api/ai/ask` responses.

## 28. Jenkins credential failure
- **Symptoms:** `Could not find credentials entry with ID 'aws-jenkins'`
- **Diagnosis:** Manage Jenkins → Credentials
- **Root cause:** Credential missing, wrong ID/kind, or created in a folder scope the job can't see.
- **Fix:** Create it with the exact ID and kind from docs/devops/jenkins.md in the Global domain.
- **Verification:** Stage passes; secret masked as ****.

## 29. GitHub webhook failure
- **Symptoms:** Pushes don't trigger builds
- **Diagnosis:** GitHub → Settings → Webhooks → Recent deliveries (status codes)
- **Root cause:** Jenkins not reachable from the internet, wrong URL (missing `/github-webhook/`), wrong job type.
- **Fix:** Expose Jenkins on a reachable host or use periodic branch scanning; Multibranch job.
- **Verification:** Delivery shows 200; build starts.

## Failure exercises (break it on purpose, then fix it)

| Exercise | How to break it | Expected symptom | Section |
|---|---|---|---|
| Wrong Docker image tag | `bash scripts/deploy.sh dev deadbeef0000` (bypass check by `kubectl set image deploy/backend backend=<ecr>/shopsphere-backend:deadbeef0000`) | ImagePullBackOff | 21 |
| Wrong ECR repository | set image to `.../shopsphere-api:<tag>` | ImagePullBackOff (repository not found) | 21 |
| Invalid IAM permission | remove `ecr:PutImage` from `modules/iam`, apply, push from Jenkins | AccessDenied | 13 |
| SonarQube gate failure | add duplicated untested code | Quality Gate FAILED | 6 |
| Nexus authentication | change `nexus-credentials` password | 401 on deploy | 7 |
| Maven dependency failure | change a dependency version to `9.9.9` | Could not resolve dependencies | 5 |
| Trivy HIGH vulnerability | build on an old base image | Trivy stage fails | 10 |
| Terraform variable error | `environment = "qa"` in tfvars | validation error | 11 |
| Terraform state problem | run two `terraform plan` at once | state lock error | 12 |
| EKS authentication | `kubectl config use-context docker-desktop`, then deploy | wrong cluster / Unauthorized | 16 |
| CrashLoopBackOff | delete `JWT_SECRET` from the Secret | CrashLoopBackOff | 20 |
| Readiness failure | change readiness path to `/actuator/health/ready` | READY 0/1 | 22 |
| Service port mismatch | backend Service `targetPort: 9090` | 503 via ALB, empty endpoints | 23 |
| ALB health check failure | frontend health-check path `/health` | targets unhealthy | 18 |
| PostgreSQL connection | `kubectl scale statefulset postgres --replicas=0` | readiness fails, 5xx | 19 |
| Prometheus target DOWN | `kubectl scale deploy backend --replicas=0` | target DOWN + alert | 24 |
| Grafana missing data | change datasource UID in a dashboard | No data | 25 |
