package com.jinlee.devtroublemate.trouble.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "트러블 해결 처리 요청")
public record ResolveTroubleRequest(
        @Schema(description = "실제 원인", example = "Access Token 만료 시간 설정 오류")
        String actualCause,

        @Schema(description = "해결 방법", example = "application.yml에서 expiration 값 수정")
        String solution,

        @Schema(description = "참고 링크", example = "https://docs.example.com/jwt")
        String referenceLink
) {
}
