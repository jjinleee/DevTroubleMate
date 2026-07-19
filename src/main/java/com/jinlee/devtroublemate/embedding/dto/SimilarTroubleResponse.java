package com.jinlee.devtroublemate.embedding.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "유사 장애 검색 결과")
public record SimilarTroubleResponse(

        @Schema(description = "장애 ID", example = "3")
        Long troubleId,

        @Schema(description = "장애 제목", example = "JWT 인증 오류")
        String title,

        @Schema(description = "장애 상태", example = "RESOLVED")
        String status,

        @Schema(description = "유사도", example = "0.9315")
        Double similarity
) {
}