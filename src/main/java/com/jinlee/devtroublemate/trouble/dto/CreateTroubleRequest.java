package com.jinlee.devtroublemate.trouble.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "트러블 생성 요청")
public record CreateTroubleRequest(
        @Schema(description = "제목", example = "JWT 토큰 만료 오류")
        String title,

        @Schema(description = "상세 설명", example = "로그인 후 401 Unauthorized 발생")
        String description,

        @Schema(description = "원본 로그", example = "ERROR 401 Unauthorized: Token expired")
        String rawLog,

        @Schema(description = "태그 목록", example = "[\"JWT\", \"Spring Security\"]")
        List<String> tags
) {
}
