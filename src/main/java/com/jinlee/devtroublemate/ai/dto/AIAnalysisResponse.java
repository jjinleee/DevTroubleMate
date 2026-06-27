package com.jinlee.devtroublemate.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "AI 장애 분석 결과")
public record AIAnalysisResponse(

        @Schema(description = "장애 카테고리", example = "서버 오류")
        String category,

        @Schema(description = "장애 요약")
        String summary,

        @Schema(description = "가능한 원인")
        List<String> possibleCauses,

        @Schema(description = "점검 순서")
        List<String> runbook,

        @Schema(description = "AI 신뢰도", example = "92")
        Integer confidence

) {
}