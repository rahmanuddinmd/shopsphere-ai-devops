# Run ShopSphere with Docker Compose

## Images
| Image | Build stages | Runtime | User | Port | Health check |
|---|---|---|---|---|---|
| `shopsphere-backend` | Maven/JDK 21 → JAR | `eclipse-temurin:21-jre-alpine` | 10001 | 8080 | `/actuator/health/liveness` |
| `shopsphere-frontend` | Node 22 → `vite build` | `nginx-unprivileged:1.27-alpine` | 101 | 8080 | `/healthz` |

Multi-stage builds keep compilers out of the runtime image (smaller, fewer CVEs). Dependency
layers are copied before the source code so `docker build` reuses cached layers when only code changes.

## Start the application
```bash
cp .env.example .env                                  # fill in the <CHANGE_ME> values
docker compose --profile app up -d --build
docker compose ps
bash scripts/health-check.sh local
```
Open http://localhost:3000 (shop) · http://localhost:8080/actuator/health (API).

## Add AI, monitoring, CI tools
```bash
docker compose --profile app --profile ai up -d
docker compose exec ollama ollama pull llama3.2:3b     # then AI_PROVIDER=ollama in .env and:
docker compose --profile app up -d backend              # recreate backend with new env
docker compose --profile app --profile monitoring up -d # Prometheus :9090, Grafana :3001, Alertmanager :9093, Mailpit :8025
docker compose --profile ci up -d                       # Jenkins :8085, SonarQube :9000, Nexus :8081 (needs ~6 GB RAM)
```

## Useful commands
```bash
docker compose logs -f backend               # follow logs (JSON/ECS format)
docker compose exec postgres psql -U shopsphere -d shopsphere -c '\dt'
docker image ls shopsphere-*                 # image sizes
docker compose --profile app down            # stop (data kept)
docker compose --profile app down -v         # stop AND delete volumes (database reset)
```

## Tagging (why not `latest`)
`scripts/build.sh` tags images with the 12-character Git SHA, e.g. `shopsphere-backend:3f9c2a1b7d4e`.
A SHA tag points to exactly one tested build, so deployments are reproducible and rollbacks are exact.
`latest` moves every build - two clusters running "latest" may run different code.

## Manual build / run / tag / push
```bash
docker build -t shopsphere-backend:1.0.0 ./backend
docker run --rm -p 8080:8080 --env-file .env -e DB_HOST=host.docker.internal shopsphere-backend:1.0.0
docker tag shopsphere-backend:1.0.0 <YOUR_AWS_ACCOUNT_ID>.dkr.ecr.<YOUR_AWS_REGION>.amazonaws.com/shopsphere-backend:1.0.0
docker push <YOUR_AWS_ACCOUNT_ID>.dkr.ecr.<YOUR_AWS_REGION>.amazonaws.com/shopsphere-backend:1.0.0
```
