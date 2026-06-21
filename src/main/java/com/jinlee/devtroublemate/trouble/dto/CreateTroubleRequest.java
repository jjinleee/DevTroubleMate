package com.jinlee.devtroublemate.trouble.dto;

import java.util.List;

public record CreateTroubleRequest(
        String title,
        String description,
        String rawLog,
        List<String> tags
) {
}