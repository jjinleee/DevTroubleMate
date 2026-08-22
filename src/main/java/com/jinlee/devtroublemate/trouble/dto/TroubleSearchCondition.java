package com.jinlee.devtroublemate.trouble.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(description = "트러블 검색 조건")
public record TroubleSearchCondition(
        @Schema(description = "상태 필터", example = "OPEN")
        @Pattern(regexp = "^(OPEN|RESOLVED)$", message = "status는 OPEN 또는 RESOLVED여야 합니다.")
        String status,

        @Schema(description = "태그 필터", example = "Docker")
        String tag,

        @Schema(description = "키워드 (제목/설명 검색)", example = "JWT")
        String keyword,

        @Schema(description = "보관된 트러블 조회 여부", example = "false", defaultValue = "false")
        Boolean archived
) {

    public TroubleSearchCondition(String status, String tag, String keyword) {
        this(status, tag, keyword, false);
    }
}
