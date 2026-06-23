package com.jinlee.devtroublemate.trouble.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "트러블 목록 조회 응답")
public record TroubleSummaryResponse(
        @Schema(description = "트러블 ID", example = "1")
        Long id,

        @Schema(description = "제목", example = "JWT 토큰 만료 오류")
        String title,

        @Schema(description = "상태", example = "OPEN")
        String status,

        @Schema(description = "생성 일시", example = "2026-06-23T10:00:00")
        LocalDateTime createdAt,

        @Schema(description = "태그 목록", example = "[\"JWT\", \"Docker\"]")
        List<String> tags
) {
}
