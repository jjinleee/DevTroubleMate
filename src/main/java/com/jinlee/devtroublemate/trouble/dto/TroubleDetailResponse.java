package com.jinlee.devtroublemate.trouble.dto;

import java.util.List;

public record TroubleDetailResponse(
        Long id,
        String title,
        String description,
        String rawLog,
        String status,
        String actualCause,
        String solution,
        String referenceLink,
        List<String> tags
) {
}