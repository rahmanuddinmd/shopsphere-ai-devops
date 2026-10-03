# 10 — DevSecOps Pipeline (shift-left)

```mermaid
flowchart LR
    S0["Developer<br/>.gitignore, .env, PR checklist"] --> S1["trivy fs<br/>secrets + vulnerable files"]
    S1 --> S2["SonarQube SAST<br/>bugs, vulnerabilities, hotspots"]
    S2 --> S3["OWASP Dependency-Check<br/>Java library CVEs"]
    S3 --> S4["trivy image<br/>OS + library CVEs in image"]
    S4 --> S5["trivy config<br/>Terraform + Kubernetes misconfig"]
    S5 --> S6["Runtime controls<br/>non-root, limits, Pod Identity,<br/>least-privilege IAM, private subnets"]
```

| Severity | Pipeline action (initial) |
|---|---|
| CRITICAL (fix available) | Fail |
| HIGH | Warn, report archived; tightened to fail in hardening phase |
| MEDIUM / LOW | Report only |
| Accepted risk | `.trivyignore` / suppression XML entry with reason + expiry date |
