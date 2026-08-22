package com.jinlee.devtroublemate.trouble.controller;

import com.jinlee.devtroublemate.common.exception.GlobalExceptionHandler;
import com.jinlee.devtroublemate.common.dto.PageResponse;
import com.jinlee.devtroublemate.embedding.service.SimilarTroubleService;
import com.jinlee.devtroublemate.trouble.dto.CreateTroubleResponse;
import com.jinlee.devtroublemate.trouble.exception.TroubleNotFoundException;
import com.jinlee.devtroublemate.trouble.service.TroubleService;
import org.junit.jupiter.api.Test;
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
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
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
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.last").value(false));
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
                Arguments.of(createRequest("제목", "설명", "로그", "[]"), "태그는 하나 이상 입력해야 합니다.")
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
