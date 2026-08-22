package com.jinlee.devtroublemate.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinlee.devtroublemate.ai.exception.AIServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAIServiceTest {

    private ChatClient chatClient;
    private OpenAIService openAIService;

    @BeforeEach
    void setUp() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(builder.build()).thenReturn(chatClient);
        openAIService = new OpenAIService(builder, new ObjectMapper());
    }

    @Test
    void parseValidResponse() {
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("""
                {
                  "category": "인증",
                  "summary": "토큰 만료",
                  "possibleCauses": ["만료 시간 오류"],
                  "runbook": ["설정 확인"],
                  "confidence": 90
                }
                """);

        var response = openAIService.analyzeTroubleLog("제목", "설명", "로그", List.of("JWT"));

        assertThat(response.category()).isEqualTo("인증");
        assertThat(response.confidence()).isEqualTo(90);
    }

    @Test
    void rejectMalformedJson() {
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("not-json");

        assertThatThrownBy(() -> openAIService.analyzeTroubleLog("제목", "설명", "로그", List.of()))
                .isInstanceOfSatisfying(AIServiceException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("AI_RESPONSE_INVALID"));
    }

    @Test
    void rejectMissingRequiredFields() {
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("""
                {"category":"인증","summary":"","possibleCauses":[],"runbook":[],"confidence":101}
                """);

        assertThatThrownBy(() -> openAIService.analyzeTroubleLog("제목", "설명", "로그", List.of()))
                .isInstanceOfSatisfying(AIServiceException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("AI_RESPONSE_INVALID"));
    }

    @Test
    void rejectTooManyOrBlankItems() {
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("""
                {
                  "category":"인증",
                  "summary":"요약",
                  "possibleCauses":["원인1","원인2","원인3","원인4"],
                  "runbook":["점검1"," "],
                  "confidence":80
                }
                """);

        assertThatThrownBy(() -> openAIService.analyzeTroubleLog("제목", "설명", "로그", List.of()))
                .isInstanceOfSatisfying(AIServiceException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("AI_RESPONSE_INVALID"));
    }

    @Test
    void translateGeneralCallFailure() {
        when(chatClient.prompt().user(anyString()).call().content())
                .thenThrow(new RuntimeException("provider failure"));

        assertThatThrownBy(() -> openAIService.analyzeTroubleLog("제목", "설명", "로그", List.of()))
                .isInstanceOfSatisfying(AIServiceException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("AI_ANALYSIS_FAILED"));
    }
}
