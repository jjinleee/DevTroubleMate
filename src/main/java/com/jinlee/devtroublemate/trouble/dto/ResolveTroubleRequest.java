package com.jinlee.devtroublemate.trouble.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "트러블 해결 처리 요청")
public record ResolveTroubleRequest(
        @Schema(description = "실제 원인", example = "Access Token 만료 시간 설정 오류")
        @NotBlank(message = "실제 원인은 필수입니다.")
        String actualCause,

        @Schema(description = "해결 방법", example = "application.yml에서 expiration 값 수정")
        @NotBlank(message = "해결 방법은 필수입니다.")
        String solution,

        @Schema(description = "참고 링크", example = "https://docs.example.com/jwt")
        String referenceLink
) {
}
