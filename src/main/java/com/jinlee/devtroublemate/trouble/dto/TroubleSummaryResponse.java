package com.jinlee.devtroublemate.trouble.dto;

import com.jinlee.devtroublemate.trouble.domain.Trouble;

import java.time.LocalDateTime;

public record TroubleSummaryResponse(
        Long id,
        String title,
        String status,
        LocalDateTime createdAt
) {
    public static TroubleSummaryResponse from(Trouble trouble) {
        return new TroubleSummaryResponse(
                trouble.getId(),
                trouble.getTitle(),
                trouble.getStatus().name(),
                trouble.getCreatedAt()
        );
    }
}