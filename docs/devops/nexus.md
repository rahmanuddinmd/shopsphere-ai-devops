# Nexus Repository

```bash
docker compose --profile ci up -d nexus                       # http://localhost:8081 (first start ~2 min)
docker compose exec nexus cat /nexus-data/admin.password      # initial admin password
NEXUS_USER=admin NEXUS_PASSWORD='<new-password>' bash nexus/create-repositories.sh
```
Create a dedicated user `jenkins-deployer` (role with `nx-repository-view-maven2-*-*` add/edit/read)
and store it in Jenkins as `nexus-credentials`.

Flow: `Source → mvn deploy (Jenkins) → shopsphere-maven-snapshots (1.0.0-SNAPSHOT) or
shopsphere-maven-releases (1.0.0, write-once)`.

| | Nexus | Amazon ECR | Docker Hub |
|---|---|---|---|
| Stores | Maven, npm, Docker, PyPI... | Docker/OCI images only | Docker/OCI images |
| Hosting | Self-hosted (you operate it) | AWS managed | SaaS |
| Auth | Nexus users/roles | IAM (no passwords on EKS nodes) | Docker Hub accounts/tokens |
| Typical use | Enterprise artifact hub, proxy/cache of Maven Central | Images for ECS/EKS | Public images, open source |
| ShopSphere | Backend JAR (+ optional images) | Images deployed to EKS | Base images only |

Test credentials: `curl -u jenkins-deployer:<pw> http://localhost:8081/service/rest/v1/status/check`.
