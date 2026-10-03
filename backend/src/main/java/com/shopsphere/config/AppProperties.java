package com.shopsphere.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * All ShopSphere settings, bound from application.yml (which reads environment variables).
 * Names match docs/architecture/conventions.md.
 */
@ConfigurationProperties(prefix = "shopsphere")
public record AppProperties(Security security, Admin admin, Ai ai) {

    public record Security(String jwtSecret, long jwtExpirationMinutes, List<String> corsAllowedOrigins) {
    }

    public record Admin(String email, String password) {
    }

    /**
     * @param provider ollama | bedrock | disabled
     */
    public record Ai(String provider, String ollamaBaseUrl, String ollamaModel,
                     String awsRegion, String bedrockModelId, int timeoutSeconds) {
    }
}
