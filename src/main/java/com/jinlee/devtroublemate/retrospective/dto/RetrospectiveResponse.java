package com.jinlee.devtroublemate.retrospective.dto;

import com.jinlee.devtroublemate.retrospective.domain.Retrospective;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "장애 회고 응답")
public record RetrospectiveResponse(
        Long id,
        Long troubleId,
        String content,
        String lesson
) {
    public static RetrospectiveResponse from(Retrospective retrospective) {
        return new RetrospectiveResponse(
                retrospective.getId(),
                retrospective.getTrouble().getId(),
                retrospective.getContent(),
                retrospective.getLesson()
        );
    }
}
