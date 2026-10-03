# OWASP Dependency-Check (SCA)

Scans every Java dependency (including transitive ones) against the NVD CVE database.

```bash
export NVD_API_KEY=<your free key>     # without a key the first run takes a very long time
cd backend && ./mvnw -Psecurity -DskipTests verify
open target/dependency-check-report.html
```
Pipeline decision: `failBuildOnCVSS = 9` (critical). Reports (HTML/JSON/XML) are archived by Jenkins.

**False positives** happen because matching is done by CPE names (e.g. the PostgreSQL *server* CVEs
matched to the JDBC *driver*). Verify the CVE really affects the library and version you use; if not,
add a suppression to `security/dependency-check/suppressions.xml` with `<notes>` and an `until`
date. **Real findings**: upgrade the library (or the Spring Boot version that manages it) and rerun.

SonarQube (code), Dependency-Check (Java libraries) and Trivy (image OS packages, secrets, IaC) cover
different layers - together they implement shift-left security.
