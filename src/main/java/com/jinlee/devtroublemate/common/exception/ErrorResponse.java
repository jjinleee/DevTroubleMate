package com.jinlee.devtroublemate.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "에러 응답")
public record ErrorResponse(

        @Schema(description = "에러 발생 시간")
        LocalDateTime timestamp,

        @Schema(description = "HTTP 상태 코드", example = "404")
        int status,

        @Schema(description = "에러 코드", example = "TROUBLE_NOT_FOUND")
        String code,

        @Schema(description = "에러 메시지", example = "존재하지 않는 장애입니다.")
        String message

) {
    public static ErrorResponse of(
            int status,
            String code,
            String message
    ) {
        return new ErrorResponse(
                LocalDateTime.now(),
                status,
                code,
                message
        );
    }
}
