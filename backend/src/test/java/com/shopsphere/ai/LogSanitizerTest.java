package com.shopsphere.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogSanitizerTest {

    @Test
    void redactsAwsKeysPasswordsAndTokens() {
        String raw = """
                AWS_ACCESS_KEY_ID=AKIAABCDEFGHIJKLMNOP
                aws_secret_access_key = wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
                DB_PASSWORD=SuperSecret123
                Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiJ9.c2lnbmF0dXJlLXZhbHVl
                jdbc:postgresql://shop:hunter2@db:5432/shopsphere
                """;
        var result = LogSanitizer.sanitize(raw);
        assertThat(result.redacted()).isTrue();
        assertThat(result.text())
                .doesNotContain("AKIAABCDEFGHIJKLMNOP")
                .doesNotContain("wJalrXUtnFEMI")
                .doesNotContain("SuperSecret123")
                .doesNotContain("eyJhbGciOiJIUzI1NiJ9")
                .doesNotContain("hunter2");
    }

    @Test
    void leavesCleanLogsUntouchedAndKeepsTail() {
        String clean = "ERROR Connection refused: postgres:5432";
        assertThat(LogSanitizer.sanitize(clean).redacted()).isFalse();

        String huge = "x".repeat(LogSanitizer.MAX_CHARS + 500) + "FINAL ERROR LINE";
        assertThat(LogSanitizer.sanitize(huge).text()).endsWith("FINAL ERROR LINE").startsWith("...[truncated]...");
    }
}
