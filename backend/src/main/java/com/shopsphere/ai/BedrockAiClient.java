package com.shopsphere.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;

import java.time.Duration;
import java.util.Optional;

/**
 * AWS Bedrock via the model-agnostic Converse API.
 * Credentials come from the AWS default credential chain:
 *   local  -> your AWS CLI profile (aws configure / aws sso login)
 *   EKS    -> EKS Pod Identity (IAM role bound to the backend ServiceAccount)
 * No access keys are ever configured in the application.
 */
public class BedrockAiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(BedrockAiClient.class);

    private final BedrockRuntimeClient client;
    private final String modelId;

    public BedrockAiClient(String region, String modelId, int timeoutSeconds) {
        this.client = BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofSeconds(timeoutSeconds))
                        .build())
                .build();
        this.modelId = modelId;
    }

    @Override
    public Optional<String> complete(String systemPrompt, String userPrompt) {
        try {
            ConverseRequest request = ConverseRequest.builder()
                    .modelId(modelId)
                    .system(SystemContentBlock.builder().text(systemPrompt).build())
                    .messages(Message.builder()
                            .role(ConversationRole.USER)
                            .content(ContentBlock.fromText(userPrompt))
                            .build())
                    .inferenceConfig(InferenceConfiguration.builder().maxTokens(800).temperature(0.2f).build())
                    .build();
            ConverseResponse response = client.converse(request);
            return response.output().message().content().stream()
                    .map(ContentBlock::text)
                    .filter(t -> t != null && !t.isBlank())
                    .findFirst()
                    .map(String::trim);
        } catch (Exception e) {
            log.warn("Bedrock call failed ({}): {}", modelId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String providerName() {
        return "bedrock";
    }

    @Override
    public String modelName() {
        return modelId;
    }
}
