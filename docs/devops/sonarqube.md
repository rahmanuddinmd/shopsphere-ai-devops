# SonarQube

```bash
docker compose --profile ci up -d sonarqube      # http://localhost:9000  (admin / admin, change it)
```
1. Create the Quality Gate described in `sonarqube/quality-gate.md` and set it as default.
2. My Account → Security → generate a token → Jenkins credential `sonarqube-token`.
3. Administration → Webhooks → `http://jenkins:8080/sonarqube-webhook/` (needed by `waitForQualityGate`).
4. Run locally once: `cd backend && ./mvnw verify org.sonarsource.scanner.maven:sonar-maven-plugin:5.1.0.4751:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<token>`

| Term | Meaning |
|---|---|
| Bug | Code that is demonstrably wrong (null dereference, wrong comparison) |
| Vulnerability | Code that can be exploited (SQL injection, weak crypto) |
| Security hotspot | Security-sensitive code a human must review |
| Code smell | Maintainability issue (long method, duplicated logic) |
| Duplication | % of duplicated lines |
| Coverage | % of lines/branches executed by tests (from JaCoCo `jacoco.xml`) |
| Quality Gate | Pass/fail rules on new code; the pipeline stops when it fails |

Failure drill: add an unused private method and a duplicated block, push, watch the gate fail, fix it.
