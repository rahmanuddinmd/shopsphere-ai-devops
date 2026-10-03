package com.shopsphere.ai;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Redacts secrets from logs BEFORE they are sent to any AI provider.
 * Covers AWS keys, bearer/JWT tokens, GitHub tokens, private keys and key=value passwords.
 */
public final class LogSanitizer {

    public static final int MAX_CHARS = 12_000;

    private record Rule(Pattern pattern, String replacement) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule(Pattern.compile("-----BEGIN [A-Z ]*PRIVATE KEY-----[\\s\\S]*?-----END [A-Z ]*PRIVATE KEY-----"), "[REDACTED_PRIVATE_KEY]"),
            new Rule(Pattern.compile("\\b(AKIA|ASIA)[0-9A-Z]{16}\\b"), "[REDACTED_AWS_ACCESS_KEY]"),
            new Rule(Pattern.compile("(?i)(aws_secret_access_key|aws_session_token)\\s*[=:]\\s*\\S+"), "$1=[REDACTED]"),
            new Rule(Pattern.compile("\\bgh[pousr]_[A-Za-z0-9]{20,}\\b"), "[REDACTED_GITHUB_TOKEN]"),
            new Rule(Pattern.compile("\\beyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{5,}\\b"), "[REDACTED_JWT]"),
            new Rule(Pattern.compile("(?i)(bearer)\\s+[A-Za-z0-9._~+/=-]{16,}"), "$1 [REDACTED]"),
            new Rule(Pattern.compile("(?i)\\b([a-z0-9_.-]*(?:password|passwd|pwd|secret|token|api[_-]?key)[a-z0-9_.-]*)\\s*[=:]\\s*(?!\\[REDACTED)[^\\s,;]+"), "$1=[REDACTED]"),
            new Rule(Pattern.compile("(?i)(://[^:/\\s]+):[^@\\s]+@"), "$1:[REDACTED]@"));

    private LogSanitizer() {
    }

    public record Result(String text, boolean redacted) {
    }

    public static Result sanitize(String raw) {
        if (raw == null) {
            return new Result("", false);
        }
        String text = raw;
        for (Rule rule : RULES) {
            text = rule.pattern().matcher(text).replaceAll(rule.replacement());
        }
        boolean redacted = !text.equals(raw);
        if (text.length() > MAX_CHARS) {
            // keep the tail: the actual error is usually at the end of a log
            text = "...[truncated]...\n" + text.substring(text.length() - MAX_CHARS);
        }
        return new Result(text, redacted);
    }
}
