package com.jinlee.devtroublemate.ai.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AIServiceException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    private AIServiceException(HttpStatus status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    public static AIServiceException rateLimited(Throwable cause) {
        return new AIServiceException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "OPENAI_RATE_LIMITED",
                "AI 서비스 요청이 많습니다. 잠시 후 다시 시도해 주세요.",
                cause
        );
    }

    public static AIServiceException timeout(Throwable cause) {
        return new AIServiceException(
                HttpStatus.GATEWAY_TIMEOUT,
                "OPENAI_TIMEOUT",
                "AI 서비스 응답 시간이 초과되었습니다.",
                cause
        );
    }

    public static AIServiceException authenticationFailed(Throwable cause) {
        return new AIServiceException(
                HttpStatus.BAD_GATEWAY,
                "OPENAI_AUTHENTICATION_FAILED",
                "AI 서비스 인증에 실패했습니다.",
                cause
        );
    }

    public static AIServiceException analysisFailed(Throwable cause) {
        return new AIServiceException(
                HttpStatus.BAD_GATEWAY,
                "AI_ANALYSIS_FAILED",
                "AI 장애 분석에 실패했습니다.",
                cause
        );
    }

    public static AIServiceException invalidResponse(Throwable cause) {
        return new AIServiceException(
                HttpStatus.BAD_GATEWAY,
                "AI_RESPONSE_INVALID",
                "AI 분석 응답 형식이 올바르지 않습니다.",
                cause
        );
    }

    public static AIServiceException embeddingFailed(Throwable cause) {
        return new AIServiceException(
                HttpStatus.BAD_GATEWAY,
                "EMBEDDING_FAILED",
                "Embedding 생성에 실패했습니다.",
                cause
        );
    }

    public static AIServiceException responseProcessingFailed(Throwable cause) {
        return new AIServiceException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "AI_RESPONSE_PROCESSING_FAILED",
                "AI 분석 결과 처리에 실패했습니다.",
                cause
        );
    }
}
