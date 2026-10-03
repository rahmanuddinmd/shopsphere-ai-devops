# Deploy ShopSphere to Amazon EKS (step by step)

> **COST:** from step 3 until step 10, AWS bills roughly **$0.30-0.40 per hour** (EKS control plane,
> 2 x t3.medium, NAT Gateway, ALB). A 3-4 hour lab costs about $1.5. Forgetting it for a month costs
> ~$200. **Always finish with step 10.**

## 0. What you get
```text
Internet → ALB (Ingress) ─┬─ /api, /actuator/health → backend Service → backend pods (2) → postgres-0 (EBS gp3)
                          └─ /                       → frontend Service → frontend pods (2)
backend pods ──Pod Identity──► Bedrock (optional)      Prometheus/Grafana in namespace monitoring
```

## 1. Tools
`aws`, `terraform >= 1.10`, `kubectl`, `helm`, `docker` (check with `bash scripts/setup.sh`).

## 2. State bucket (once per account) - FREE
```bash
cd terraform/bootstrap
terraform init
terraform apply                       # type yes
terraform output state_bucket         # shopsphere-tfstate-<account-id>
```

## 3. Infrastructure - POTENTIALLY EXPENSIVE
```bash
cd ../environments/dev
cp backend.hcl.example backend.hcl            # set bucket + region
cp terraform.tfvars.example terraform.tfvars  # set budget_emails, public_access_cidrs (your IP/32)
terraform init -backend-config=backend.hcl
terraform fmt -recursive ../..
terraform validate
terraform plan -out tfplan                    # READ IT: ~70 resources, 1 NAT, 1 EKS, 1 node group
terraform apply tfplan                        # ~15-20 minutes
terraform output
```
What each command does:

| Command | What happens | Cost impact |
|---|---|---|
| `init` | Downloads providers/modules, connects to the S3 backend | none |
| `fmt` | Rewrites files to canonical format | none |
| `validate` | Checks syntax and types offline | none |
| `plan` | Compares code with real AWS and state; shows what would change | none |
| `apply` | Creates/changes resources; writes state to S3 (locked by `.tflock`) | starts billing |
| `output` | Prints outputs (cluster name, ECR registry, kubectl command) | none |
| `destroy` | Deletes everything in this state | stops billing |

## 4. Connect kubectl and install cluster add-ons
```bash
aws eks update-kubeconfig --region ap-south-1 --name shopsphere-dev
kubectl get nodes                                  # 2 nodes Ready
export GRAFANA_ADMIN_PASSWORD='<choose-one>'
bash scripts/install-cluster-addons.sh shopsphere-dev   # LB Controller, metrics-server, kube-prometheus-stack
kubectl get pods -n kube-system | grep aws-load-balancer
```

## 5. Build and push images to ECR - LOW COST
```bash
TAG=$(git rev-parse --short=12 HEAD)
REGISTRY=$(cd terraform/environments/dev && terraform output -raw ecr_registry)
aws ecr get-login-password --region ap-south-1 | docker login --username AWS --password-stdin $REGISTRY
docker build -t $REGISTRY/shopsphere-backend:$TAG ./backend
docker build -t $REGISTRY/shopsphere-frontend:$TAG --build-arg VITE_BUILD_SHA=$TAG ./frontend
docker push $REGISTRY/shopsphere-backend:$TAG
docker push $REGISTRY/shopsphere-frontend:$TAG
```
(Jenkins does exactly this in the "Push images to ECR" stage.)

## 6. Secrets and deployment
```bash
bash scripts/deploy.sh secrets dev       # Secret from .env (values never printed)
bash scripts/deploy.sh dev $TAG          # kustomize: set image -> apply -> wait for rollout
kubectl get pods,svc,ingress -n shopsphere-dev
```
The ALB DNS name appears in `kubectl get ingress` after 2-3 minutes.

## 7. Verify
```bash
bash scripts/health-check.sh k8s shopsphere-dev
jenkins/scripts/smoke-test.sh http://$(kubectl get ingress shopsphere -n shopsphere-dev -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
```
Open `http://<alb-dns-name>` and sign in with ADMIN_EMAIL / ADMIN_PASSWORD.

## 8. Optional: AI with Bedrock (pay per token)
Enable a model in the Bedrock console, then edit `kubernetes/overlays/dev/config-patch.yaml`:
`AI_PROVIDER: "bedrock"`, `BEDROCK_MODEL_ID: "<model-or-inference-profile-id>"`, redeploy
(`bash scripts/deploy.sh dev $TAG`). The backend pod gets AWS credentials from **EKS Pod Identity**
(role `shopsphere-dev-backend-bedrock`) - no keys anywhere.

## 9. Monitoring
```bash
kubectl port-forward svc/kps-grafana 3001:80 -n monitoring        # dashboards "ShopSphere - Application/Kubernetes"
kubectl port-forward svc/kps-prometheus 9090:9090 -n monitoring    # Status > Targets: shopsphere-backend UP
```

## 10. Rollback and cleanup
```bash
bash scripts/deploy.sh rollback dev            # kubectl rollout undo
bash scripts/aws-cleanup.sh                    # deletes namespaces (ALB) -> terraform destroy -> verifies
```
Order matters: delete Kubernetes Ingresses/namespaces **before** `terraform destroy`, otherwise the
ALB and its security groups (created by the controller, not Terraform) block VPC deletion.

## Promotion to STAGING / PROD
STAGING is a namespace in the same cluster: `bash scripts/deploy.sh staging $TAG` (same tag that
passed DEV - build once, promote many). Jenkins asks for approval before each promotion. PROD uses
its own cluster (`terraform/environments/prod`) and is created only for the capstone.
