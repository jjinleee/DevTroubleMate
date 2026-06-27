package com.jinlee.devtroublemate.ai.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinlee.devtroublemate.ai.domain.AIAnalysis;

import java.util.List;

public record AIAnalysisSummaryResponse(
        String category,
        String summary,
        List<String> possibleCauses,
        List<String> runbook,
        Integer confidence
) {

    public static AIAnalysisSummaryResponse from(
            AIAnalysis aiAnalysis,
            ObjectMapper objectMapper
    ) {
        return new AIAnalysisSummaryResponse(
                aiAnalysis.getCategory(),
                aiAnalysis.getSummary(),
                toList(aiAnalysis.getPossibleCauses(), objectMapper),
                toList(aiAnalysis.getRunbook(), objectMapper),
                aiAnalysis.getConfidence()
        );
    }

    private static List<String> toList(
            String json,
            ObjectMapper objectMapper
    ) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("AI 분석 결과 JSON 파싱 실패", e);
        }
    }
}