# AWS cost guide (ap-south-1, approximate on-demand prices - confirm on the AWS pricing pages)

## What costs money
| Resource | Created by | Approx. rate | If left running 30 days | Rating |
|---|---|---|---|---|
| EKS control plane | Terraform `modules/eks` | $0.10/h | ~$73 | POTENTIALLY EXPENSIVE |
| EKS extended-support version | old `kubernetes_version` | $0.60/h | ~$438 | AVOID |
| NAT Gateway | `modules/vpc` | ~$0.056/h + $/GB | ~$40+ | POTENTIALLY EXPENSIVE |
| EC2 nodes 2 x t3.medium | node group | ~$0.045/h each | ~$66 | LOW COST per hour |
| Application Load Balancer | Ingress (LB Controller) | ~$0.02-0.03/h + LCU | ~$18+ | LOW COST, adds up |
| Public IPv4 addresses | NAT, ALB | $0.005/h each | ~$3.6 each | easy to forget |
| EBS gp3 (PostgreSQL 5 GiB, node disks) | PVC / nodes | ~$0.09/GB-month | ~$6 | LOW COST |
| RDS db.t4g.micro (optional) | `modules/rds` | ~$0.016/h + storage | ~$15 | LOW COST |
| CloudWatch Logs (control plane, Container Insights) | EKS | per GB ingested | varies | watch volume |
| ECR storage | images | $0.10/GB-month | cents | FREE/LOW |
| S3 state bucket | bootstrap | per GB | cents | FREE/LOW |
| Bedrock | AI_PROVIDER=bedrock | per 1K tokens | cents for training | LOW COST |

**Typical 4-hour EKS lab: ~$1.5-2. Forgotten for a month: ~$200.**

## How to reduce cost
- Destroy after every session; recreate next time (that's the point of IaC).
- `single_nat_gateway = true` (default); `node_capacity_type = "SPOT"` for labs.
- Keep `enable_rds = false` and `enable_container_insights = false` unless that phase needs them.
- Run Jenkins/SonarQube/Nexus locally or on one EC2 VM that you **stop** when idle (stopped = only EBS cost).
- Use a Kubernetes version in standard support.
- Run STAGING as a namespace in the DEV cluster; create PROD only for the capstone.

## What can be stopped vs. what must be destroyed
| Resource | Stop? | Must destroy to stop billing |
|---|---|---|
| EC2 training VM (Jenkins etc.) | yes (EBS still billed) | terminate when the course ends |
| EKS control plane | no | `terraform destroy` |
| NAT Gateway | no | `terraform destroy` |
| ALB | no | delete the Ingress/namespace |
| RDS | yes, max 7 days (restarts automatically) | `terraform destroy` |

## Cleanup and verification
```bash
bash scripts/aws-cleanup.sh        # namespaces -> terraform destroy -> report
```
Manual checklist (region ap-south-1): EKS clusters: none · EC2 → Load Balancers: none ·
VPC → NAT gateways: none or "deleted" · EC2 → Instances: none running (except a VM you intend to keep) ·
EC2 → Volumes: no "available" volumes · EC2 → Elastic IPs: none · RDS: none ·
Billing → Bills next day: no growing EKS/NAT lines.
