package com.shopsphere.ai;

import com.shopsphere.dto.AiDtos.CommandSuggestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic diagnosis rules for the most common ShopSphere pipeline/runtime failures.
 * The rule result is always returned (even when the LLM is unavailable); the LLM adds explanation.
 */
public final class DevOpsKnowledgeBase {

    private DevOpsKnowledgeBase() {
    }

    public record Diagnosis(String problem, String rootCause, String recommendedFix,
                            List<CommandSuggestion> commands, String risk, List<String> verification) {
    }

    private record Rule(Pattern trigger, Diagnosis diagnosis) {
    }

    private static CommandSuggestion cmd(String command, String purpose) {
        boolean destructive = isDestructive(command);
        return new CommandSuggestion(command, purpose, destructive, destructive);
    }

    private static final Pattern DESTRUCTIVE = Pattern.compile(
            "(terraform\\s+destroy|kubectl\\s+delete|aws\\s+\\S+\\s+delete|delete-cluster|docker\\s+system\\s+prune|"
                    + "docker\\s+volume\\s+(rm|prune)|rm\\s+-rf|drop\\s+(table|database)|helm\\s+uninstall|git\\s+push\\s+--force)",
            Pattern.CASE_INSENSITIVE);

    public static boolean isDestructive(String command) {
        return command != null && DESTRUCTIVE.matcher(command).find();
    }

    /** Extracts destructive commands mentioned anywhere in free text (e.g. an LLM answer). */
    public static List<String> findDestructive(String text) {
        List<String> found = new ArrayList<>();
        if (text == null) {
            return found;
        }
        Matcher m = DESTRUCTIVE.matcher(text);
        while (m.find()) {
            found.add(m.group());
        }
        return found;
    }

    private static final List<Rule> RULES = List.of(
            new Rule(Pattern.compile("(?i)crashloopbackoff[\\s\\S]*(connection refused|5432|could not connect|connection to .* refused)|"
                    + "(connection refused|5432)[\\s\\S]*crashloopbackoff"),
                    new Diagnosis("Pod is in CrashLoopBackOff",
                            "The backend cannot reach PostgreSQL (connection refused on port 5432), so Spring Boot exits at startup.",
                            "Check that the postgres pod/Service is running and that DB_HOST, DB_PORT, DB_USERNAME and DB_PASSWORD in the ConfigMap/Secret are correct.",
                            List.of(cmd("kubectl get pods -n shopsphere-dev", "See which pods are failing"),
                                    cmd("kubectl logs deploy/backend -n shopsphere-dev --previous", "Read the crash log of the last container"),
                                    cmd("kubectl get svc postgres -n shopsphere-dev", "Confirm the database Service exists"),
                                    cmd("kubectl get configmap shopsphere-config -n shopsphere-dev -o yaml", "Check DB_HOST/DB_PORT")),
                            "Low - read-only diagnosis commands.",
                            List.of("kubectl get pods -n shopsphere-dev shows backend Running 1/1", "curl /actuator/health returns UP"))),
            new Rule(Pattern.compile("(?i)oomkilled|out of memory|java\\.lang\\.outofmemoryerror"),
                    new Diagnosis("Container killed for using too much memory (OOMKilled)",
                            "The JVM used more memory than the container limit.",
                            "Raise resources.limits.memory or lower -XX:MaxRAMPercentage; check for memory leaks with the JVM heap dashboard in Grafana.",
                            List.of(cmd("kubectl describe pod -l app.kubernetes.io/name=backend -n shopsphere-dev", "Look for 'Last State: Terminated, Reason: OOMKilled'"),
                                    cmd("kubectl top pod -n shopsphere-dev", "Current memory usage")),
                            "Medium - raising limits increases node resource usage.",
                            List.of("No new OOMKilled events in kubectl describe pod", "JVM heap stays below 80% in Grafana"))),
            new Rule(Pattern.compile("(?i)crashloopbackoff"),
                    new Diagnosis("Pod is in CrashLoopBackOff",
                            "The container starts and exits repeatedly. The reason is in the previous container's logs.",
                            "Read the previous logs, check environment variables and secrets, then check probes.",
                            List.of(cmd("kubectl logs <pod> -n shopsphere-dev --previous", "Crash reason"),
                                    cmd("kubectl describe pod <pod> -n shopsphere-dev", "Events, exit code, probe failures")),
                            "Low - read-only diagnosis commands.",
                            List.of("Pod restarts stop increasing", "kubectl get pods shows Running"))),
            new Rule(Pattern.compile("(?i)imagepullbackoff|errimagepull|manifest unknown|not found: manifest|pull access denied"),
                    new Diagnosis("Kubernetes cannot pull the container image (ImagePullBackOff)",
                            "The image name or tag in the Deployment does not exist in ECR, or the nodes lack permission to pull it.",
                            "Compare the image in the Deployment with the tags in ECR; make sure Jenkins pushed the same Git SHA tag.",
                            List.of(cmd("kubectl describe pod <pod> -n shopsphere-dev", "Exact image reference and pull error"),
                                    cmd("aws ecr describe-images --repository-name shopsphere-backend --query 'imageDetails[].imageTags'", "Tags that really exist"),
                                    cmd("kubectl get deploy backend -n shopsphere-dev -o jsonpath='{.spec.template.spec.containers[0].image}'", "Image the Deployment asks for")),
                            "Low - read-only diagnosis commands.",
                            List.of("Pod status changes to Running", "kubectl rollout status deploy/backend succeeds"))),
            new Rule(Pattern.compile("(?i)readiness probe failed|liveness probe failed|startup probe failed"),
                    new Diagnosis("Health probe failing",
                            "The probe path/port does not respond in time - often a wrong port, wrong path, or the app is still starting.",
                            "Confirm the probes use port 8080 and /actuator/health/readiness|liveness; increase startupProbe failureThreshold if startup is slow.",
                            List.of(cmd("kubectl describe pod <pod> -n shopsphere-dev", "Probe failure events"),
                                    cmd("kubectl port-forward deploy/backend 8080:8080 -n shopsphere-dev", "Then curl localhost:8080/actuator/health/readiness")),
                            "Low.",
                            List.of("READY column shows 1/1", "No probe failure events"))),
            new Rule(Pattern.compile("(?i)could not resolve dependencies|could not transfer artifact|failed to read artifact descriptor"),
                    new Diagnosis("Maven dependency resolution failed",
                            "Maven cannot download a dependency - network/proxy problem, wrong Nexus URL, or a version that does not exist.",
                            "Check the dependency version, Nexus availability and credentials in settings.xml; retry with -U to refresh.",
                            List.of(cmd("./mvnw -U -B dependency:resolve", "Force re-resolution"),
                                    cmd("curl -I http://<nexus-host>:8081/service/rest/v1/status", "Is Nexus up?")),
                            "Low.",
                            List.of("./mvnw verify completes", "Jenkins build stage turns green"))),
            new Rule(Pattern.compile("(?i)(401|unauthorized)[\\s\\S]*nexus|nexus[\\s\\S]*(401|unauthorized)|return code is: 401"),
                    new Diagnosis("Nexus authentication failure",
                            "The username/password Jenkins uses for Nexus is wrong, or the server id does not match distributionManagement.",
                            "Update the nexus-credentials entry in Jenkins and make sure settings.xml server ids are nexus-releases / nexus-snapshots.",
                            List.of(cmd("curl -u <user> http://<nexus-host>:8081/service/rest/v1/status/check", "Test credentials")),
                            "Low.",
                            List.of("mvn deploy uploads the artifact", "Artifact visible in shopsphere-maven-snapshots"))),
            new Rule(Pattern.compile("(?i)quality gate (status: )?(failed|error)|qualitygate.*error"),
                    new Diagnosis("SonarQube Quality Gate failed",
                            "New code does not meet the Quality Gate (coverage, bugs, vulnerabilities, duplication).",
                            "Open the SonarQube project, fix the failing conditions on new code, then push again. Do not lower the gate to make it pass.",
                            List.of(cmd("./mvnw verify", "Re-run tests locally to check coverage")),
                            "Low.",
                            List.of("Quality Gate shows Passed in SonarQube", "Jenkins Quality Gate stage is green"))),
            new Rule(Pattern.compile("(?i)error acquiring the state lock|state lock|conditionalcheckfailed|lock info"),
                    new Diagnosis("Terraform state is locked",
                            "Another terraform run holds the lock, or a previous run crashed and left the lock behind.",
                            "Make sure no other apply is running. Only then release the lock with terraform force-unlock <LOCK_ID>.",
                            List.of(cmd("terraform force-unlock <LOCK_ID>", "Release a stale lock - only when no other run is active")),
                            "Medium - force-unlocking while another apply runs can corrupt state.",
                            List.of("terraform plan runs without lock errors"))),
            new Rule(Pattern.compile("(?i)accessdenied|not authorized to perform|unauthorizedoperation|is not authorized"),
                    new Diagnosis("AWS IAM permission denied",
                            "The IAM identity used does not have the permission named in the error.",
                            "Identify the caller and the missing action, then add that single action to the role's policy (least privilege).",
                            List.of(cmd("aws sts get-caller-identity", "Which identity is making the call"),
                                    cmd("aws iam simulate-principal-policy --policy-source-arn <role-arn> --action-names <action>", "Test the permission")),
                            "Low.",
                            List.of("The failing AWS call succeeds"))),
            new Rule(Pattern.compile("(?i)permission denied.*docker\\.sock|got permission denied while trying to connect to the docker daemon"),
                    new Diagnosis("Docker permission denied",
                            "The user is not allowed to use the Docker socket.",
                            "Add the user to the docker group and log in again.",
                            List.of(cmd("sudo usermod -aG docker $USER", "Grant Docker access"), cmd("newgrp docker", "Apply group without logout")),
                            "Low.",
                            List.of("docker ps works without sudo"))),
            new Rule(Pattern.compile("(?i)cannot connect to the docker daemon|is the docker daemon running"),
                    new Diagnosis("Docker daemon unavailable",
                            "The Docker engine is not running.",
                            "Start Docker Desktop, or on Linux: sudo systemctl start docker.",
                            List.of(cmd("sudo systemctl start docker", "Start the engine"), cmd("docker info", "Verify")),
                            "Low.",
                            List.of("docker info prints server details"))),
            new Rule(Pattern.compile("(?i)total:\\s*\\d+\\s*\\(.*critical:\\s*[1-9]|critical\\s+vulnerab"),
                    new Diagnosis("Trivy found CRITICAL vulnerabilities",
                            "The image or dependencies contain packages with known critical CVEs.",
                            "Update the base image or the vulnerable library to the fixed version shown by Trivy. Only add a .trivyignore entry with a reason and expiry if no fix exists.",
                            List.of(cmd("trivy image --severity CRITICAL,HIGH --ignore-unfixed shopsphere-backend:<tag>", "Show fixable issues")),
                            "Low.",
                            List.of("Trivy stage passes with no fixable CRITICAL findings"))),
            new Rule(Pattern.compile("(?i)no space left on device"),
                    new Diagnosis("Disk full",
                            "The build agent or node ran out of disk (usually old Docker images).",
                            "Check disk usage first; remove unused images only after confirming nothing needed is lost.",
                            List.of(cmd("df -h", "Disk usage"), cmd("docker system df", "Docker space usage"),
                                    cmd("docker system prune -a", "Remove unused images/containers - DESTRUCTIVE")),
                            "High for the prune command - it deletes all unused images and stopped containers.",
                            List.of("df -h shows free space", "Build runs again"))));

    public static Diagnosis diagnose(String log) {
        String text = log == null ? "" : log;
        for (Rule rule : RULES) {
            if (rule.trigger().matcher(text).find()) {
                return rule.diagnosis();
            }
        }
        return new Diagnosis("Unrecognised failure",
                "No known pattern matched. Look for the first ERROR/FAILED line and the lines just above it.",
                "Share the full error block (not just the last line) and the command that produced it.",
                List.of(), "Unknown.", List.of("Re-run the failing step and confirm it succeeds"));
    }

    /** First lines that look like errors - used as "evidence". */
    public static List<String> evidence(String log) {
        List<String> lines = new ArrayList<>();
        if (log == null) {
            return lines;
        }
        for (String line : log.split("\\R")) {
            String l = line.toLowerCase(Locale.ROOT);
            if (l.contains("error") || l.contains("failed") || l.contains("exception") || l.contains("backoff")
                    || l.contains("denied") || l.contains("refused") || l.contains("critical") || l.contains("oomkilled")) {
                lines.add(line.strip().length() > 300 ? line.strip().substring(0, 300) + "..." : line.strip());
                if (lines.size() == 6) {
                    break;
                }
            }
        }
        return lines;
    }
}
