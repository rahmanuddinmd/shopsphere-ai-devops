# ShopSphere Quality Gate

Create in SonarQube: **Quality Gates → Create → "ShopSphere Way"**, then *Set as Default*.
Conditions apply to **New Code** (clean-as-you-code):

| Metric | Operator | Value | Why |
|---|---|---|---|
| New bugs (Reliability rating) | worse than | A | No new bugs reach `main` |
| New vulnerabilities (Security rating) | worse than | A | No new vulnerabilities |
| Security hotspots reviewed | less than | 100% | Every hotspot gets a human decision |
| Coverage on new code | less than | 60% | Raised to 80% in Phase 23 |
| Duplicated lines on new code | greater than | 3% | Copy-paste becomes bugs later |
| Maintainability rating on new code | worse than | A | Code smells stay under control |

The Jenkins stage `waitForQualityGate abortPipeline: true` fails the build when any condition
fails. Fix the code - never lower the gate to make a build green.

Webhook (required for `waitForQualityGate`): SonarQube → Administration → Configuration →
Webhooks → Create: URL `http://jenkins:8080/sonarqube-webhook/`.
