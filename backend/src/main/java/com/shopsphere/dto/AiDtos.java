package com.shopsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public final class AiDtos {

    private AiDtos() {
    }

    public record AskRequest(@NotBlank @Size(max = 500) String question) {
    }

    public record ProductSuggestion(Long id, String name, String imageUrl, BigDecimal price,
                                    Integer ramGb, String processor, boolean inStock) {
    }

    public record AskResponse(String answer, List<ProductSuggestion> products, String provider, boolean fallback) {
    }

    public record OpsAnalyzeRequest(
            @NotBlank @Size(max = 40) String source,
            @NotBlank @Size(max = 60000) String log) {
    }

    public record CommandSuggestion(String command, String purpose, boolean destructive, boolean requiresConfirmation) {
    }

    public record OpsAnalysisResponse(
            String problem, String rootCause, List<String> evidence, String recommendedFix,
            List<CommandSuggestion> commands, String risk, List<String> verification,
            String aiExplanation, String provider, boolean redacted) {
    }

    public record AiStatusResponse(String provider, String model, boolean enabled) {
    }
}
