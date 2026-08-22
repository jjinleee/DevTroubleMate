package com.jinlee.devtroublemate.trouble.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "트러블 수정 요청")
public record UpdateTroubleRequest(
        @Schema(description = "제목", example = "JWT 토큰 갱신 오류")
        @NotBlank(message = "제목은 필수입니다.")
        String title,

        @Schema(description = "상세 설명", example = "Access Token 갱신 중 401 응답 발생")
        @NotBlank(message = "상세 설명은 필수입니다.")
        String description,

        @Schema(description = "원본 로그", example = "ERROR 401 Unauthorized: Refresh failed")
        @NotBlank(message = "원본 로그는 필수입니다.")
        String rawLog,

        @Schema(description = "태그 목록", example = "[\"JWT\", \"Spring Security\"]")
        @NotEmpty(message = "태그는 하나 이상 입력해야 합니다.")
        List<@NotBlank(message = "태그 이름은 공백일 수 없습니다.") String> tags
) {
}
