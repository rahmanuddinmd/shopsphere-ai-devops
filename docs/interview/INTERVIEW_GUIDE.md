# Interview guide - explain ShopSphere like a real project

## 2-minute project pitch
"ShopSphere AI is a Spring Boot + React e-commerce app I deliver end to end. Jenkins builds and tests
it, SonarQube and OWASP/Trivy gate quality and security, images are tagged with the Git SHA and pushed
to ECR, Terraform provisions VPC, EKS and IAM with Pod Identity, and kustomize deploys DEV → STAGING →
PROD with approvals. Prometheus/Grafana/Alertmanager and CloudWatch cover observability, and an AI
assistant answers product questions from live data and helps diagnose pipeline failures without ever
executing commands."

## Scenario questions (with the expected approach)
1. **Jenkins is green but the pod is in ImagePullBackOff.** `kubectl describe pod` → read the exact
   image; compare with `aws ecr describe-images`; check tag/repo/account/region; check node IAM pull
   permission. Prevent: deploy only tags that exist (deploy.sh checks), immutable SHA tags.
2. **Pods CrashLoopBackOff after a config change.** `kubectl logs --previous`; diff ConfigMap/Secret;
   `kubectl rollout undo`; fix and redeploy; add a startup validation (JWT_SECRET check).
3. **Quality Gate fails on a hotfix at 2 AM.** Don't lower the gate; fix or, with approval, document a
   time-boxed exception; add tests afterwards.
4. **`terraform apply` fails with a state lock.** Read Lock Info; confirm no other run; `force-unlock`
   only then; explain S3 `use_lockfile` vs DynamoDB.
5. **The AWS bill doubled.** Cost Explorer by service/tag; usual suspects NAT data, forgotten ALB/EKS,
   extended-support EKS version, CloudWatch Logs; add budgets/tags; destroy idle environments.
6. **Latency p95 jumped after a release.** Grafana: which URI; correlate with deploy time
   (change-cause annotation); check DB pool, JVM GC/heap, HPA; roll back first, then investigate.
7. **A secret was committed to GitHub.** Rotate immediately, then purge history (git filter-repo), add
   Trivy secret scanning + pre-commit hooks, review access logs.
8. **Trivy reports a CRITICAL CVE with no fix.** Assess exploitability/reachability, mitigate (config,
   network), time-boxed `.trivyignore` with owner and expiry, track upstream.
9. **ALB returns 503.** Target group health → readiness probe → Service selector/targetPort →
   endpoints → pods.
10. **How would you make PROD highly available?** Multi-AZ nodes and NAT, RDS Multi-AZ, PDBs, topology
    spread, HPA + cluster autoscaling (Karpenter), backups/restore tests, blue/green or canary.

## Core questions by area (answer them with ShopSphere examples)
- **Git:** merge vs rebase · Gitflow vs trunk-based · why branch protection · semantic versioning and tags.
- **Docker:** image vs container · layer caching · multi-stage · why non-root · CMD vs ENTRYPOINT · volumes vs bind mounts.
- **Jenkins:** declarative vs scripted · credentials binding · multibranch · input/approval · agents vs controller.
- **Maven/Nexus:** lifecycle phases · SNAPSHOT vs release · settings.xml servers · why write-once releases.
- **SonarQube:** bug vs vulnerability vs hotspot vs smell · quality gate on new code · coverage source (JaCoCo).
- **Security:** SAST vs SCA vs container vs IaC scanning · false positives · shift-left.
- **AWS:** VPC public vs private subnets · NAT vs IGW · IAM roles vs users · Pod Identity vs IRSA vs keys.
- **Terraform:** state & locking · plan vs apply · modules · workspaces vs folders per env · drift.
- **Kubernetes/EKS:** Deployment vs StatefulSet · probes (startup/readiness/liveness) · requests vs limits · HPA · PDB · Service types · Ingress + LB Controller · rollout undo.
- **Observability:** metrics vs logs vs traces · RED/USE methods · Prometheus pull model · alert fatigue.
- **AI + DevOps:** RAG vs fine-tuning · prompt injection · redaction · human-in-the-loop for destructive actions.
