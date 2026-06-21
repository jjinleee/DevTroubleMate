package com.jinlee.devtroublemate.trouble.dto;

public record ResolveTroubleRequest(
        String actualCause,
        String solution,
        String referenceLink
) {
}