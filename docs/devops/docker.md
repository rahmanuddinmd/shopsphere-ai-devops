# Docker concepts used in ShopSphere

| Concept | In this project |
|---|---|
| Image layers | `COPY pom.xml` / `package-lock.json` before sources so dependency layers are cached |
| Multi-stage build | Build tools stay in the `build` stage; runtime images contain only JRE/Nginx + app |
| Non-root user | Backend UID 10001, frontend UID 101 (nginx-unprivileged listens on 8080) |
| HEALTHCHECK | `wget` against `/actuator/health/liveness` and `/healthz`; `docker ps` shows `healthy` |
| Environment variables | All config via env (`.env` → Compose); no secrets baked into images |
| Networking | User-defined bridge `shopsphere-net`; containers reach each other by service name (`postgres`, `backend`) |
| Volumes | Named volumes (`pgdata`, `jenkins_home`, ...) survive `docker compose down`; `-v` deletes them |
| `.dockerignore` | Keeps `target/`, `node_modules/`, `.env` out of the build context |
| Tags | Git SHA for deployments, semver for releases, `latest`/`local` only for convenience |
| Image security | Trivy scans images in Jenkins; ECR scan-on-push; minimal Alpine bases |
