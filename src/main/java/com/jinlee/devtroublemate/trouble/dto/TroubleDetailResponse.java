package com.jinlee.devtroublemate.trouble.dto;

import com.jinlee.devtroublemate.trouble.domain.Trouble;

public record TroubleDetailResponse(
        Long id,
        String title,
        String description,
        String rawLog,
        String status,
        String actualCause,
        String solution,
        String referenceLink
) {
    public static TroubleDetailResponse from(Trouble trouble) {
        return new TroubleDetailResponse(
                trouble.getId(),
                trouble.getTitle(),
                trouble.getDescription(),
                trouble.getRawLog(),
                trouble.getStatus().name(),
                trouble.getActualCause(),
                trouble.getSolution(),
                trouble.getReferenceLink()
        );
    }
}