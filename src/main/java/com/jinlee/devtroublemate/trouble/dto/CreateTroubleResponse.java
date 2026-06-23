package com.jinlee.devtroublemate.trouble.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "트러블 생성 응답")
public record CreateTroubleResponse(
        @Schema(description = "생성된 트러블 ID", example = "1")
        Long id
) {
}
