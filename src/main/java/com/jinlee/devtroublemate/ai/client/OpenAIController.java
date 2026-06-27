package com.jinlee.devtroublemate.ai.client;

import com.jinlee.devtroublemate.ai.dto.AnalyzeTroubleLogRequest;
import com.jinlee.devtroublemate.ai.dto.AIAnalysisResponse;
import com.jinlee.devtroublemate.ai.service.AIAnalysisService;
import com.jinlee.devtroublemate.ai.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class OpenAIController {

    private final OpenAIService openAIService;
    private final AIAnalysisService aiAnalysisService;

    @PostMapping("/analyze")
    public AIAnalysisResponse analyze(
            @RequestBody AnalyzeTroubleLogRequest request
    ) {
        return openAIService.analyzeTroubleLog(
                request.title(),
                request.description(),
                request.rawLog(),
                request.tags()
        );
    }
    @PostMapping("/troubles/{troubleId}/analyze")
    public AIAnalysisResponse analyzeAndSave(
            @PathVariable Long troubleId,
            @RequestBody AnalyzeTroubleLogRequest request
    ) {
        return aiAnalysisService.analyzeAndSave(troubleId, request);
    }
}