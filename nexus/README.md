# Nexus Repository

| Repository | Format | Policy | Used by |
|---|---|---|---|
| `shopsphere-maven-releases` | maven2 hosted | RELEASE, write-once | `mvn deploy` of tagged versions (`1.0.0`) |
| `shopsphere-maven-snapshots` | maven2 hosted | SNAPSHOT, redeploy allowed | `mvn deploy` from `develop` (`1.0.0-SNAPSHOT`) |
| `shopsphere-docker` | docker hosted, HTTP :8082 | write-once | Optional local image registry |

Setup: `bash nexus/create-repositories.sh` (details in `docs/devops/nexus.md`).
Jenkins publishes with `jenkins/settings.xml` + credential `nexus-credentials`.
