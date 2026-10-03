package com.shopsphere.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Local LLM via Ollama's chat API: POST {OLLAMA_BASE_URL}/api/chat
 * Free, offline, no credentials. Good for local development and training.
 */
public class OllamaAiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaAiClient.class);

    private final RestClient restClient;
    private final String model;

    public OllamaAiClient(String baseUrl, String model, int timeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.model = model;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<String> complete(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "stream", false,
                "options", Map.of("temperature", 0.2),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));
        try {
            Map<String, Object> response = restClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (response == null || !(response.get("message") instanceof Map<?, ?> message)) {
                return Optional.empty();
            }
            Object content = message.get("content");
            return content == null ? Optional.empty() : Optional.of(content.toString().trim());
        } catch (Exception e) {
            log.warn("Ollama call failed ({}): {}", model, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String providerName() {
        return "ollama";
    }

    @Override
    public String modelName() {
        return model;
    }
}
