package com.jinlee.devtroublemate.ai.client;

import com.jinlee.devtroublemate.ai.service.AIAnalysisService;
import com.jinlee.devtroublemate.ai.service.OpenAIService;
import com.jinlee.devtroublemate.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OpenAIController.class)
@Import(GlobalExceptionHandler.class)
class OpenAIControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpenAIService openAIService;

    @MockitoBean
    private AIAnalysisService aiAnalysisService;

    @ParameterizedTest
    @MethodSource("invalidAnalyzeRequests")
    void rejectInvalidAnalyzeRequest(String requestBody, String expectedMessage) throws Exception {
        mockMvc.perform(post("/api/ai/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    @ParameterizedTest
    @MethodSource("invalidAnalyzeRequests")
    void rejectInvalidAnalyzeAndSaveRequest(String requestBody, String expectedMessage) throws Exception {
        mockMvc.perform(post("/api/ai/troubles/{troubleId}/analyze", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    private static Stream<Arguments> invalidAnalyzeRequests() {
        return Stream.of(
                Arguments.of(request("", "설명", "로그", "[\"JWT\"]"), "제목은 필수입니다."),
                Arguments.of(request("제목", " ", "로그", "[\"JWT\"]"), "상세 설명은 필수입니다."),
                Arguments.of(request("제목", "설명", "", "[\"JWT\"]"), "원본 로그는 필수입니다."),
                Arguments.of(request("제목", "설명", "로그", "[]"), "태그는 하나 이상 입력해야 합니다."),
                Arguments.of(request("제목", "설명", "로그", "[\"  \"]"), "태그 이름은 공백일 수 없습니다.")
        );
    }

    private static String request(
            String title,
            String description,
            String rawLog,
            String tags
    ) {
        return """
                {
                  "title": "%s",
                  "description": "%s",
                  "rawLog": "%s",
                  "tags": %s
                }
                """.formatted(title, description, rawLog, tags);
    }
}
