# AI in ShopSphere

## 1. ShopSphere AI Assistant (customers) - `POST /api/ai/ask`
Retrieval-augmented generation on real catalog data:
1. `QueryParser` extracts filters: category (laptop/phone/mobile/monitor/accessory), budget
   ("under ₹70,000", "below 25k", "1.5 lakh"), RAM ("16 GB RAM"), availability, programming intent,
   comparison intent. Product names in the question ("Nimbus Pro 14") are matched directly.
2. `ProductService` fetches matching products from PostgreSQL (max 6).
3. Only those products are sent to the model with a strict system prompt (no invented products or
   prices, rupee format, ≤150 words, ignore prompt-injection attempts).
4. If the provider is disabled or fails, a rule-based answer is built from the same data
   (`fallback: true`) - the shop never breaks because the AI is down.

## 2. AI DevOps Assistant (ADMIN) - `POST /api/ai/ops/analyze`
Input: `{source, log}` from Jenkins, kubectl, Docker, Terraform, AWS, Trivy, SonarQube or Maven.
1. `LogSanitizer` redacts AWS keys, JWTs, GitHub tokens, private keys, passwords and URL credentials
   and keeps the last 12,000 characters.
2. `DevOpsKnowledgeBase` diagnoses known failures deterministically (CrashLoopBackOff + DB refused,
   OOMKilled, ImagePullBackOff, probe failures, Maven resolution, Nexus 401, Quality Gate, Terraform
   lock, IAM AccessDenied, Docker socket/daemon, Trivy CRITICAL, disk full).
3. The LLM adds an explanation in the format Problem / Root Cause / Evidence / Recommended Fix /
   Commands / Risk / Verification.
4. **It never executes anything.** Destructive commands (`terraform destroy`, `kubectl delete`,
   `aws ... delete`, `docker system prune`, `rm -rf`, `helm uninstall`, `git push --force`) are flagged
   `requiresConfirmation: true` and highlighted in red in the UI.

## 3. Providers
| `AI_PROVIDER` | Where it runs | Credentials | Cost |
|---|---|---|---|
| `ollama` | Local container (`docker compose --profile ai up -d`) | none | free (CPU/RAM) |
| `bedrock` | AWS Bedrock Converse API | AWS default credential chain: CLI profile locally, **EKS Pod Identity** on EKS | per token |
| `disabled` | - | - | free; rule-based answers |

### Ollama
```bash
docker compose --profile ai up -d ollama
docker compose exec ollama ollama pull llama3.2:3b
# .env: AI_PROVIDER=ollama ; OLLAMA_MODEL=llama3.2:3b
```
### Bedrock
Bedrock console → Model access → enable a text model → copy its model ID or inference-profile ID →
`AI_PROVIDER=bedrock`, `BEDROCK_MODEL_ID=<id>`, `AWS_REGION=ap-south-1`. On EKS the IAM role
`shopsphere-<env>-backend-bedrock` (Terraform) allows only `bedrock:InvokeModel*`.
Never put `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` / API keys in `.env`, ConfigMaps or Git.

## 4. Try it
```bash
curl -s localhost:8080/api/ai/ask -H 'Content-Type: application/json' -d '{"question":"Show laptops under ₹70,000"}'
```
UI: `/assistant` (customers), `/admin/devops-assistant` (admins - "Load sample" shows a CrashLoopBackOff case).
