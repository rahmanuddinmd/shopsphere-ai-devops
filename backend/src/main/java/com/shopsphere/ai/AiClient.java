package com.shopsphere.ai;

import java.util.Optional;

/**
 * Provider-neutral AI interface. Selected by AI_PROVIDER (ollama | bedrock | disabled).
 * Callers must handle Optional.empty() (provider disabled or failed) with a rule-based fallback,
 * so the shop keeps working when the AI is down.
 */
public interface AiClient {

    Optional<String> complete(String systemPrompt, String userPrompt);

    String providerName();

    String modelName();

    default boolean isEnabled() {
        return true;
    }
}
