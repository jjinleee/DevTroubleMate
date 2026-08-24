package com.jinlee.devtroublemate.trouble.controller;

import com.jinlee.devtroublemate.common.exception.GlobalExceptionHandler;
import com.jinlee.devtroublemate.common.dto.PageResponse;
import com.jinlee.devtroublemate.ai.exception.AIServiceException;
import com.jinlee.devtroublemate.ai.dto.AIProcessingResponse;
import com.jinlee.devtroublemate.ai.exception.AIRetryNotAllowedException;
import com.jinlee.devtroublemate.embedding.exception.TroubleEmbeddingNotFoundException;
import com.jinlee.devtroublemate.embedding.service.SimilarTroubleService;
import com.jinlee.devtroublemate.trouble.dto.CreateTroubleResponse;
import com.jinlee.devtroublemate.trouble.exception.TroubleNotFoundException;
import com.jinlee.devtroublemate.trouble.service.TroubleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.Pageable;

import java.util.stream.Stream;
import java.util.List;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TroubleController.class)
@Import(GlobalExceptionHandler.class)
class TroubleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TroubleService troubleService;

    @MockitoBean
    private SimilarTroubleService similarTroubleService;

    @Test
    void createTrouble() throws Exception {
        when(troubleService.create(any())).thenReturn(new CreateTroubleResponse(1L));

        mockMvc.perform(post("/api/troubles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "JWT 토큰 만료 오류",
                                  "description": "로그인 후 401 응답 발생",
                                  "rawLog": "ERROR 401 Unauthorized",
                                  "tags": ["JWT"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void returnStandardErrorWhenAIRateLimited() throws Exception {
        when(troubleService.create(any()))
                .thenThrow(AIServiceException.rateLimited(new RuntimeException()));

        mockMvc.perform(post("/api/troubles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "JWT 오류",
                                  "description": "로그인 실패",
                                  "rawLog": "ERROR 401",
                                  "tags": ["JWT"]
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.code").value("OPENAI_RATE_LIMITED"))
                .andExpect(jsonPath("$.message").value("AI 서비스 요청이 많습니다. 잠시 후 다시 시도해 주세요."));
    }

    @Test
    void retryFailedAIAnalysis() throws Exception {
        when(troubleService.retryAIAnalysis(1L)).thenReturn(new AIProcessingResponse(
                1L, "COMPLETED", null, null, LocalDateTime.now()));

        mockMvc.perform(post("/api/troubles/{troubleId}/ai-analysis/retry", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void rejectRetryWhenAnalysisHasNotFailed() throws Exception {
        when(troubleService.retryAIAnalysis(1L)).thenThrow(new AIRetryNotAllowedException(1L));

        mockMvc.perform(post("/api/troubles/{troubleId}/ai-analysis/retry", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AI_RETRY_NOT_ALLOWED"));
    }

    @ParameterizedTest
    @MethodSource("invalidCreateRequests")
    void rejectInvalidCreateRequest(String requestBody, String expectedMessage) throws Exception {
        mockMvc.perform(post("/api/troubles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    @Test
    void returnNotFoundWhenTroubleDoesNotExist() throws Exception {
        when(troubleService.getDetail(999L))
                .thenThrow(new TroubleNotFoundException(999L));

        mockMvc.perform(get("/api/troubles/{troubleId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("TROUBLE_NOT_FOUND"))
                .andExpect(jsonPath("$.message", containsString("troubleId=999")));
    }

    @Test
    void updateTrouble() throws Exception {
        mockMvc.perform(put("/api/troubles/{troubleId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest("수정 제목", "수정 설명", "수정 로그", "[\"JWT\"]")))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectInvalidUpdateRequest() throws Exception {
        mockMvc.perform(put("/api/troubles/{troubleId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest("", "수정 설명", "수정 로그", "[\"JWT\"]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("제목은 필수입니다."));
    }

    @Test
    void returnStandardErrorForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/troubles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.message").value("요청 본문을 읽을 수 없습니다. JSON 형식을 확인해 주세요."));
    }

    @Test
    void returnStandardErrorForUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/api/troubles")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("invalid"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void returnNotFoundWhenUpdatingMissingTrouble() throws Exception {
        doThrow(new TroubleNotFoundException(999L))
                .when(troubleService).update(any(), any());

        mockMvc.perform(put("/api/troubles/{troubleId}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest("수정 제목", "수정 설명", "수정 로그", "[\"JWT\"]")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TROUBLE_NOT_FOUND"));
    }

    @Test
    void getTroublesWithPagination() throws Exception {
        when(troubleService.getTroubles(any(), any()))
                .thenReturn(new PageResponse<>(List.of(), 1, 5, 12, 3, false));

        mockMvc.perform(get("/api/troubles")
                        .param("status", "OPEN")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.last").value(false));

        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(troubleService).getTroubles(any(), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort().getOrderFor("title")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("title").isAscending()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("invalidTroubleListParameters")
    void rejectInvalidTroubleListParameters(String name, String value, String expectedMessage) throws Exception {
        mockMvc.perform(get("/api/troubles").param(name, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    @Test
    void rejectNonNumericPage() throws Exception {
        mockMvc.perform(get("/api/troubles").param("page", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("page 값의 형식이 올바르지 않습니다."));
    }

    @Test
    void rejectInvalidSimilarSearchParameters() throws Exception {
        mockMvc.perform(get("/api/troubles/{troubleId}/similar", 1L)
                        .param("limit", "0")
                        .param("minSimilarity", "1.1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void returnNotFoundWhenEmbeddingDoesNotExist() throws Exception {
        when(similarTroubleService.findSimilarTroubles(1L, 5, 0.0))
                .thenThrow(new TroubleEmbeddingNotFoundException(1L));

        mockMvc.perform(get("/api/troubles/{troubleId}/similar", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TROUBLE_EMBEDDING_NOT_FOUND"));
    }

    @ParameterizedTest
    @MethodSource("invalidResolveRequests")
    void rejectInvalidResolveRequest(String requestBody, String expectedMessage) throws Exception {
        mockMvc.perform(patch("/api/troubles/{troubleId}/resolution", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    @Test
    void returnNotFoundWhenResolvingMissingTrouble() throws Exception {
        doThrow(new TroubleNotFoundException(999L))
                .when(troubleService).resolve(any(), any());

        mockMvc.perform(patch("/api/troubles/{troubleId}/resolution", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actualCause": "설정 오류",
                                  "solution": "설정 수정",
                                  "referenceLink": null
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TROUBLE_NOT_FOUND"));
    }

    @Test
    void archiveTrouble() throws Exception {
        mockMvc.perform(patch("/api/troubles/{troubleId}/archive", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void restoreTrouble() throws Exception {
        mockMvc.perform(patch("/api/troubles/{troubleId}/restore", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void returnNotFoundWhenArchivingMissingTrouble() throws Exception {
        doThrow(new TroubleNotFoundException(999L))
                .when(troubleService).archive(999L);

        mockMvc.perform(patch("/api/troubles/{troubleId}/archive", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TROUBLE_NOT_FOUND"));
    }

    private static Stream<Arguments> invalidCreateRequests() {
        return Stream.of(
                Arguments.of(createRequest("", "설명", "로그", "[\"JWT\"]"), "제목은 필수입니다."),
                Arguments.of(createRequest("제목", " ", "로그", "[\"JWT\"]"), "상세 설명은 필수입니다."),
                Arguments.of(createRequest("제목", "설명", "", "[\"JWT\"]"), "원본 로그는 필수입니다."),
                Arguments.of(createRequest("제목", "설명", "로그", "[]"), "태그는 하나 이상 입력해야 합니다."),
                Arguments.of(createRequest("제목", "설명", "로그", "[\"  \"]"), "태그 이름은 공백일 수 없습니다.")
        );
    }

    private static Stream<Arguments> invalidTroubleListParameters() {
        return Stream.of(
                Arguments.of("page", "-1", "page는 0 이상이어야 합니다."),
                Arguments.of("size", "0", "size는 1 이상이어야 합니다."),
                Arguments.of("size", "101", "size는 100 이하여야 합니다."),
                Arguments.of("sort", "id,desc", "sort는 허용된 필드와 방향으로 입력해야 합니다."),
                Arguments.of("sort", "createdAt,sideways", "sort는 허용된 필드와 방향으로 입력해야 합니다."),
                Arguments.of("status", "INVALID", "status는 OPEN 또는 RESOLVED여야 합니다.")
        );
    }

    private static Stream<Arguments> invalidResolveRequests() {
        return Stream.of(
                Arguments.of(resolveRequest("", "해결 방법"), "실제 원인은 필수입니다."),
                Arguments.of(resolveRequest("실제 원인", " "), "해결 방법은 필수입니다.")
        );
    }

    private static String createRequest(
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

    private static String resolveRequest(String actualCause, String solution) {
        return """
                {
                  "actualCause": "%s",
                  "solution": "%s"
                }
                """.formatted(actualCause, solution);
    }

    private static String updateRequest(
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
