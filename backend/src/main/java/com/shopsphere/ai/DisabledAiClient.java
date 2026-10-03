package com.shopsphere.ai;

import java.util.Optional;

/** AI_PROVIDER=disabled - every answer comes from the rule-based fallback. */
public class DisabledAiClient implements AiClient {

    @Override
    public Optional<String> complete(String systemPrompt, String userPrompt) {
        return Optional.empty();
    }

    @Override
    public String providerName() {
        return "disabled";
    }

    @Override
    public String modelName() {
        return "none";
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
