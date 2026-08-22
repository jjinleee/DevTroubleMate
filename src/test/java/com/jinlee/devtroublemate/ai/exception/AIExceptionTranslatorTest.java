package com.jinlee.devtroublemate.ai.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class AIExceptionTranslatorTest {

    @Test
    void translateRateLimit() {
        var cause = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                HttpHeaders.EMPTY,
                new byte[0],
                null
        );

        AIServiceException exception = AIExceptionTranslator.forAnalysis(cause);

        assertThat(exception.getCode()).isEqualTo("OPENAI_RATE_LIMITED");
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void translateAuthenticationFailure() {
        var cause = HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                HttpHeaders.EMPTY,
                new byte[0],
                null
        );

        assertThat(AIExceptionTranslator.forAnalysis(cause).getCode())
                .isEqualTo("OPENAI_AUTHENTICATION_FAILED");
    }

    @Test
    void translateTimeoutFromCauseChain() {
        RuntimeException cause = new RuntimeException(new SocketTimeoutException());

        AIServiceException exception = AIExceptionTranslator.forEmbedding(cause);

        assertThat(exception.getCode()).isEqualTo("OPENAI_TIMEOUT");
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    }
}
