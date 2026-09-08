package com.jinlee.devtroublemate.ai.exception;

import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

public final class AIExceptionTranslator {

    private AIExceptionTranslator() {
    }

    public static AIServiceException forAnalysis(Throwable cause) {
        AIServiceException common = translateCommon(cause);
        return common != null ? common : AIServiceException.analysisFailed(cause);
    }

    public static AIServiceException forEmbedding(Throwable cause) {
        AIServiceException common = translateCommon(cause);
        return common != null ? common : AIServiceException.embeddingFailed(cause);
    }

    private static AIServiceException translateCommon(Throwable cause) {
        for (Throwable current = cause; current != null; current = current.getCause()) {
            if (current instanceof RestClientResponseException responseException) {
                int status = responseException.getStatusCode().value();
                if (status == 429) {
                    return AIServiceException.rateLimited(cause);
                }
                if (status == 401 || status == 403) {
                    return AIServiceException.authenticationFailed(cause);
                }
            }
            if (current instanceof TransientAiException && hasHttpStatus(current, 429)) {
                return AIServiceException.rateLimited(cause);
            }
            if (current instanceof NonTransientAiException
                    && (hasHttpStatus(current, 401) || hasHttpStatus(current, 403))) {
                return AIServiceException.authenticationFailed(cause);
            }
            if (current instanceof SocketTimeoutException
                    || current instanceof TimeoutException
                    || current instanceof ResourceAccessException) {
                return AIServiceException.timeout(cause);
            }
        }
        return null;
    }

    private static boolean hasHttpStatus(Throwable cause, int status) {
        String message = cause.getMessage();
        return message != null && message.contains("HTTP " + status + " ");
    }
}
