# 07 — Jenkins Architecture

```mermaid
flowchart TB
    GH["GitHub"] -->|"webhook POST /github-webhook/"| JC["Jenkins controller<br/>custom image: JDK 21, Maven,<br/>Docker CLI, Trivy, AWS CLI,<br/>kubectl, Terraform"]
    JC --> CREDS["Credentials store<br/>github-credentials, sonarqube-token,<br/>nexus-credentials, aws-jenkins, nvd-api-key"]
    JC --> WS["Workspace<br/>jenkins_home volume"]
    JC -->|"docker.sock - training only"| DKR["Docker Engine"]
    JC --> SQ["SonarQube :9000"]
    SQ -->|"webhook: Quality Gate result"| JC
    JC --> NX["Nexus :8081 / :8082"]
    JC --> ECR["Amazon ECR"]
    JC --> EKS["Amazon EKS"]
```

Trade-off: mounting `/var/run/docker.sock` gives Jenkins root-equivalent access to the host.
Acceptable on a single-user training VM; in production use dedicated build agents,
Kaniko/BuildKit, or ephemeral Kubernetes agents.
