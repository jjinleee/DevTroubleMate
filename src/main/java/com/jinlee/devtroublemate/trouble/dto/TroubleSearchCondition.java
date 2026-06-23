package com.jinlee.devtroublemate.trouble.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "트러블 검색 조건")
public record TroubleSearchCondition(
        @Schema(description = "상태 필터", example = "OPEN")
        String status,

        @Schema(description = "태그 필터", example = "Docker")
        String tag,

        @Schema(description = "키워드 (제목/설명 검색)", example = "JWT")
        String keyword
) {
}
