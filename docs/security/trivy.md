# Trivy

```bash
trivy fs --config security/trivy/trivy.yaml .                                    # deps + secrets in the repo
trivy image --severity HIGH,CRITICAL --ignore-unfixed shopsphere-backend:<tag>   # OS + library CVEs in the image
trivy config terraform/                                                          # Terraform misconfigurations
trivy config kubernetes/                                                         # Kubernetes misconfigurations
```

| Severity | Meaning | ShopSphere policy |
|---|---|---|
| CRITICAL | Remotely exploitable, severe impact | **Fail** the pipeline when a fix exists |
| HIGH | Serious, usually needs conditions | Report and fix this sprint; fail from Phase 23 |
| MEDIUM | Limited impact or hard to exploit | Report, fix with normal updates |
| LOW / UNKNOWN | Minimal impact | Informational |

`--ignore-unfixed` hides CVEs with no released fix, so the gate only blocks things you can act on.
Fix order: update the base image (`eclipse-temurin:21-jre-alpine`, `nginx-unprivileged`), then the
library version, then rebuild. Accept a risk only in `security/trivy/.trivyignore` with a reason,
owner and review date - never by lowering the pipeline threshold.

Failure drill "Trivy HIGH vulnerability": build the frontend on an old base (`FROM nginx:1.20-alpine`),
run `trivy image`, read the report, revert, rebuild, confirm the finding is gone.
