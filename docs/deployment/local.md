# Run ShopSphere locally (no Docker for the app)

Best for writing code: hot reload for React and fast restarts for Spring Boot.

## 1. Prerequisites
`bash scripts/setup.sh` must show no FAIL lines (Java 21, Node 22+, Docker for PostgreSQL).

## 2. Configure
```bash
cp .env.example .env
openssl rand -base64 64 | tr -d '\n'     # paste as JWT_SECRET in .env
```
Edit `.env`: `DB_PASSWORD`, `POSTGRES_PASSWORD` (same value), `ADMIN_PASSWORD`, `GRAFANA_ADMIN_PASSWORD`.

## 3. Start PostgreSQL only
```bash
docker compose --profile app up -d postgres
docker compose ps
```
Expected: `postgres ... healthy`.

## 4. Start the backend
```bash
cd backend
set -a; source ../.env; set +a
./mvnw spring-boot:run
```
Expected log lines: `Successfully applied 2 migrations`, `Bootstrap admin account created`,
`Started ShopSphereApplication`. Verify:
```bash
curl http://localhost:8080/actuator/health          # {"status":"UP"...}
curl "http://localhost:8080/api/products?size=2"
```

## 5. Start the frontend (new terminal)
```bash
cd frontend
npm ci
npm run dev
```
Open http://localhost:5173. Sign in with `ADMIN_EMAIL` / `ADMIN_PASSWORD` → you land on `/admin`.

## 6. Optional: local AI with Ollama
```bash
docker compose --profile ai up -d ollama
docker compose exec ollama ollama pull llama3.2:3b
```
In `.env` set `AI_PROVIDER=ollama` and `OLLAMA_BASE_URL=http://localhost:11434`, restart the backend,
then open http://localhost:5173/assistant. The badge shows `Provider: ollama`.

## 7. Tests
```bash
cd backend && ./mvnw verify          # unit tests + JaCoCo report in target/site/jacoco
cd frontend && npm test              # Vitest + Testing Library
```
