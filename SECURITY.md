# Security Policy

## Scope
ShopSphere AI is a training and portfolio project. It is not intended to process real
payments or real customer data.

## Reporting a vulnerability
Open a GitHub issue titled `SECURITY: <short description>` **without** exploit details,
or contact the maintainer (@rahmanuddinmd) privately via GitHub. Do not post credentials,
tokens or account IDs in issues.

## Secrets policy
| Rule | Enforcement |
|---|---|
| No secrets in Git (keys, passwords, tokens, kubeconfig, tfstate) | `.gitignore`, PR checklist, Trivy secret scan in Jenkins |
| Local secrets live in `.env` (copied from `.env.example`) | `.env` is git-ignored |
| CI secrets live in Jenkins Credentials | Referenced by ID only in `Jenkinsfile` |
| Cloud secrets live in AWS Secrets Manager / Kubernetes Secrets | Created out-of-band, never committed |
| AWS access from EKS pods uses IAM roles (EKS Pod Identity), not keys | Terraform IAM module |

If a secret is ever committed: **rotate it first**, then remove it from history.
Deleting the file in a new commit does not remove it from Git history.

## Security controls in this project
| Layer | Control |
|---|---|
| Code | SonarQube SAST + Quality Gate |
| Dependencies | OWASP Dependency-Check (fails on CVSS >= 9.0, configurable) |
| Filesystem / secrets | `trivy fs` |
| Container images | `trivy image` (fails on CRITICAL with a fix available) |
| IaC | `trivy config` on `terraform/` and `kubernetes/` |
| Application | JWT, BCrypt password hashing, RBAC (ADMIN / CUSTOMER), input validation, security headers |
| Runtime | Non-root containers, resource limits, least-privilege IAM, private subnets |

## Supported versions
Only the latest commit on `main` is maintained.
