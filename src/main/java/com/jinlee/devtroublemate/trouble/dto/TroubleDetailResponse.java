package com.jinlee.devtroublemate.trouble.dto;

import com.jinlee.devtroublemate.ai.dto.AIAnalysisSummaryResponse;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "트러블 상세 조회 응답")
public record TroubleDetailResponse(
        @Schema(description = "트러블 ID", example = "1")
        Long id,

        @Schema(description = "제목", example = "JWT 토큰 만료 오류")
        String title,

        @Schema(description = "상세 설명")
        String description,

        @Schema(description = "원본 로그")
        String rawLog,

        @Schema(description = "상태", example = "OPEN")
        String status,

        @Schema(description = "실제 원인")
        String actualCause,

        @Schema(description = "해결 방법")
        String solution,

        @Schema(description = "참고 링크")
        String referenceLink,

        @Schema(description = "태그 목록", example = "[\"JWT\", \"Docker\"]")
        List<String> tags,

        @Schema(description = "AI 장애 분석 결과")
        AIAnalysisSummaryResponse aiAnalysis,

        @Schema(description = "보관 일시", nullable = true)
        LocalDateTime archivedAt,

        @Schema(description = "AI 처리 상태", example = "COMPLETED")
        String aiProcessingStatus,

        @Schema(description = "마지막 AI 오류 코드", nullable = true)
        String aiLastErrorCode,

        @Schema(description = "마지막 AI 오류 메시지", nullable = true)
        String aiLastErrorMessage,

        @Schema(description = "마지막 AI 처리 완료 시각", nullable = true)
        LocalDateTime aiProcessedAt
) {
}
