package com.shopsphere.ai;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DevOpsAssistantServiceTest {

    /** Simple stub provider returning a fixed answer. */
    private static AiClient stub(String answer) {
        return new AiClient() {
            @Override
            public Optional<String> complete(String systemPrompt, String userPrompt) {
                return Optional.ofNullable(answer);
            }

            @Override
            public String providerName() {
                return "stub";
            }

            @Override
            public String modelName() {
                return "stub-model";
            }
        };
    }

    @Test
    void diagnosesCrashLoopCausedByDatabase() {
        String log = """
                NAME                       READY   STATUS             RESTARTS
                backend-7d9f6c8b5d-x2k4p   0/1     CrashLoopBackOff   5
                org.postgresql.util.PSQLException: Connection to postgres:5432 refused.
                """;
        var result = new DevOpsAssistantService(stub(null)).analyze("kubectl", log);

        assertThat(result.problem()).contains("CrashLoopBackOff");
        assertThat(result.rootCause()).contains("PostgreSQL");
        assertThat(result.evidence()).anyMatch(l -> l.contains("refused"));
        assertThat(result.aiExplanation()).isNull();
        assertThat(result.commands()).noneMatch(c -> c.destructive());
    }

    @Test
    void flagsDestructiveCommandsSuggestedByTheModel() {
        var service = new DevOpsAssistantService(stub("Problem: disk full\nCommands:\ndocker system prune -a"));
        var result = service.analyze("docker", "write /var/lib/docker: no space left on device");

        assertThat(result.aiExplanation()).startsWith("⚠");
        assertThat(result.commands()).anyMatch(c -> c.command().startsWith("docker system prune") && c.requiresConfirmation());
    }

    @Test
    void imagePullBackOffIsRecognised() {
        var result = new DevOpsAssistantService(stub(null))
                .analyze("kubectl", "Failed to pull image \"shopsphere-backend:abc\": manifest unknown\nImagePullBackOff");
        assertThat(result.problem()).contains("ImagePullBackOff");
    }

    @Test
    void secretsAreRedactedBeforeAnalysis() {
        var result = new DevOpsAssistantService(stub(null)).analyze("jenkins", "password=abc12345 ERROR build failed");
        assertThat(result.redacted()).isTrue();
    }
}
