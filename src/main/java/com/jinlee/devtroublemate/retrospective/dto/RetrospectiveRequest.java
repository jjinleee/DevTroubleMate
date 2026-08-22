package com.jinlee.devtroublemate.retrospective.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "장애 회고 요청")
public record RetrospectiveRequest(
        @NotBlank(message = "회고 내용은 필수입니다.")
        @Schema(description = "장애 영향과 대응 과정")
        String content,

        @NotBlank(message = "교훈 및 개선 사항은 필수입니다.")
        @Schema(description = "교훈 및 재발 방지 개선 사항")
        String lesson
) {
}
