package com.jinlee.devtroublemate.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinlee.devtroublemate.ai.domain.AIAnalysis;
import com.jinlee.devtroublemate.ai.dto.AIAnalysisResponse;
import com.jinlee.devtroublemate.ai.dto.AnalyzeTroubleLogRequest;
import com.jinlee.devtroublemate.ai.repository.AIAnalysisRepository;
import com.jinlee.devtroublemate.trouble.domain.Trouble;
import com.jinlee.devtroublemate.trouble.repository.TroubleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AIAnalysisService {

    private final OpenAIService openAIService;
    private final AIAnalysisRepository aiAnalysisRepository;
    private final TroubleRepository troubleRepository;
    private final ObjectMapper objectMapper;

    public AIAnalysisResponse analyzeAndSave(
            Trouble trouble,
            String title,
            String description,
            String rawLog,
            java.util.List<String> tags
    ) {
        AIAnalysisResponse response = openAIService.analyzeTroubleLog(
                title,
                description,
                rawLog,
                tags
        );

        AIAnalysis aiAnalysis = AIAnalysis.builder()
                .trouble(trouble)
                .category(response.category())
                .summary(response.summary())
                .possibleCauses(toJson(response.possibleCauses()))
                .runbook(toJson(response.runbook()))
                .confidence(response.confidence())
                .build();

        aiAnalysisRepository.save(aiAnalysis);

        return response;
    }

    public AIAnalysisResponse analyzeAndSave(
            Long troubleId,
            AnalyzeTroubleLogRequest request
    ) {
        Trouble trouble = troubleRepository.findById(troubleId)
                .orElseThrow(() -> new IllegalArgumentException("장애를 찾을 수 없습니다."));

        return analyzeAndSave(
                trouble,
                request.title(),
                request.description(),
                request.rawLog(),
                request.tags()
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("AI 분석 결과 JSON 변환 실패", e);
        }
    }
}