package com.jinlee.devtroublemate.trouble.dto;

public record TroubleSearchCondition(
        String status,
        String tag,
        String keyword
) {
}