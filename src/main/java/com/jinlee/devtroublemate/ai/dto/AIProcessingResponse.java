package com.jinlee.devtroublemate.ai.dto;

import com.jinlee.devtroublemate.trouble.domain.Trouble;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "AI 처리 상태 응답")
public record AIProcessingResponse(
        Long troubleId,
        String status,
        String lastErrorCode,
        String lastErrorMessage,
        LocalDateTime processedAt
) {
    public static AIProcessingResponse from(Trouble trouble) {
        return new AIProcessingResponse(
                trouble.getId(),
                trouble.getAiProcessingStatus().name(),
                trouble.getAiLastErrorCode(),
                trouble.getAiLastErrorMessage(),
                trouble.getAiProcessedAt()
        );
    }
}
