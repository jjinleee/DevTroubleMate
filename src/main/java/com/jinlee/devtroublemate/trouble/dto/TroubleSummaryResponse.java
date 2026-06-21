package com.jinlee.devtroublemate.trouble.dto;

import com.jinlee.devtroublemate.trouble.domain.Trouble;

import java.time.LocalDateTime;
import java.util.List;

public record TroubleSummaryResponse(
        Long id,
        String title,
        String status,
        LocalDateTime createdAt,
        List<String> tags
) {
}