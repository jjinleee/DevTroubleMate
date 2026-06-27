package com.jinlee.devtroublemate.ai.dto;

import java.util.List;

public record AnalyzeTroubleLogRequest(
        String title,
        String description,
        String rawLog,
        List<String> tags
) {
}