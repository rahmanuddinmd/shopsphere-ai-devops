package com.shopsphere.controller;

import com.shopsphere.ai.AiClient;
import com.shopsphere.ai.DevOpsAssistantService;
import com.shopsphere.ai.ProductAssistantService;
import com.shopsphere.dto.AiDtos.AiStatusResponse;
import com.shopsphere.dto.AiDtos.AskRequest;
import com.shopsphere.dto.AiDtos.AskResponse;
import com.shopsphere.dto.AiDtos.OpsAnalysisResponse;
import com.shopsphere.dto.AiDtos.OpsAnalyzeRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final ProductAssistantService productAssistant;
    private final DevOpsAssistantService devOpsAssistant;
    private final AiClient aiClient;

    public AiController(ProductAssistantService productAssistant, DevOpsAssistantService devOpsAssistant, AiClient aiClient) {
        this.productAssistant = productAssistant;
        this.devOpsAssistant = devOpsAssistant;
        this.aiClient = aiClient;
    }

    /** ShopSphere AI Assistant - public. */
    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return productAssistant.ask(request.question());
    }

    /** AI DevOps Assistant - ADMIN only. Analyses logs; never executes commands. */
    @PostMapping("/ops/analyze")
    public OpsAnalysisResponse analyze(@Valid @RequestBody OpsAnalyzeRequest request) {
        return devOpsAssistant.analyze(request.source(), request.log());
    }

    @GetMapping("/status")
    public AiStatusResponse status() {
        return new AiStatusResponse(aiClient.providerName(), aiClient.modelName(), aiClient.isEnabled());
    }
}
