package com.jinlee.devtroublemate.retrospective.controller;

import com.jinlee.devtroublemate.common.exception.GlobalExceptionHandler;
import com.jinlee.devtroublemate.retrospective.dto.RetrospectiveResponse;
import com.jinlee.devtroublemate.retrospective.exception.RetrospectiveAlreadyExistsException;
import com.jinlee.devtroublemate.retrospective.service.RetrospectiveService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RetrospectiveController.class)
@Import(GlobalExceptionHandler.class)
class RetrospectiveControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean RetrospectiveService retrospectiveService;

    @Test
    void createRetrospective() throws Exception {
        when(retrospectiveService.create(any(), any()))
                .thenReturn(new RetrospectiveResponse(10L, 1L, "대응 과정", "교훈"));

        mockMvc.perform(post("/api/troubles/{troubleId}/retrospective", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"대응 과정","lesson":"교훈"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void rejectBlankRetrospective() throws Exception {
        mockMvc.perform(post("/api/troubles/{troubleId}/retrospective", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"","lesson":"교훈"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void returnConflictForDuplicateRetrospective() throws Exception {
        when(retrospectiveService.create(any(), any()))
                .thenThrow(new RetrospectiveAlreadyExistsException(1L));

        mockMvc.perform(post("/api/troubles/{troubleId}/retrospective", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"대응 과정","lesson":"교훈"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RETROSPECTIVE_ALREADY_EXISTS"));
    }
}
