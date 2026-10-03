package com.shopsphere.ai;

import com.shopsphere.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

/** Chooses the AI provider from AI_PROVIDER at startup. */
@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean
    AiClient aiClient(AppProperties props) {
        AppProperties.Ai ai = props.ai();
        String provider = ai.provider() == null ? "disabled" : ai.provider().trim().toLowerCase(Locale.ROOT);
        int timeout = ai.timeoutSeconds() > 0 ? ai.timeoutSeconds() : 60;
        AiClient client = switch (provider) {
            case "ollama" -> new OllamaAiClient(ai.ollamaBaseUrl(), ai.ollamaModel(), timeout);
            case "bedrock" -> {
                if (ai.bedrockModelId() == null || ai.bedrockModelId().isBlank() || ai.bedrockModelId().startsWith("<")) {
                    log.warn("AI_PROVIDER=bedrock but BEDROCK_MODEL_ID is not set - AI disabled");
                    yield new DisabledAiClient();
                }
                yield new BedrockAiClient(ai.awsRegion(), ai.bedrockModelId(), timeout);
            }
            case "disabled" -> new DisabledAiClient();
            default -> {
                log.warn("Unknown AI_PROVIDER '{}' - AI disabled", provider);
                yield new DisabledAiClient();
            }
        };
        log.info("AI provider: {} (model: {})", client.providerName(), client.modelName());
        return client;
    }
}
