package com.jinlee.devtroublemate.trouble.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinlee.devtroublemate.ai.dto.AIAnalysisResponse;
import com.jinlee.devtroublemate.ai.exception.AIServiceException;
import com.jinlee.devtroublemate.ai.repository.AIAnalysisRepository;
import com.jinlee.devtroublemate.ai.service.OpenAIService;
import com.jinlee.devtroublemate.embedding.domain.TroubleEmbedding;
import com.jinlee.devtroublemate.embedding.repository.TroubleEmbeddingRepository;
import com.jinlee.devtroublemate.embedding.service.EmbeddingService;
import com.jinlee.devtroublemate.retrospective.repository.RetrospectiveRepository;
import com.jinlee.devtroublemate.tag.repository.TagRepository;
import com.jinlee.devtroublemate.tag.repository.TroubleTagRepository;
import com.jinlee.devtroublemate.trouble.domain.Trouble;
import com.jinlee.devtroublemate.trouble.repository.TroubleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TroubleApiFlowIntegrationTest {

    private static final int EMBEDDING_DIMENSIONS = 1536;
    private static final DockerImageName PGVECTOR_IMAGE = DockerImageName
            .parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(PGVECTOR_IMAGE)
            .withDatabaseName("devtroublemate_api_flow_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configurePostgreSQL(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TroubleRepository troubleRepository;

    @Autowired
    private TroubleEmbeddingRepository troubleEmbeddingRepository;

    @Autowired
    private AIAnalysisRepository aiAnalysisRepository;

    @Autowired
    private RetrospectiveRepository retrospectiveRepository;

    @Autowired
    private TroubleTagRepository troubleTagRepository;

    @Autowired
    private TagRepository tagRepository;

    @MockitoBean
    private OpenAIService openAIService;

    @MockitoBean
    private EmbeddingService embeddingService;

    @AfterEach
    void cleanUp() {
        retrospectiveRepository.deleteAllInBatch();
        troubleEmbeddingRepository.deleteAllInBatch();
        aiAnalysisRepository.deleteAllInBatch();
        troubleTagRepository.deleteAllInBatch();
        tagRepository.deleteAllInBatch();
        troubleRepository.deleteAllInBatch();
    }

    @Test
    void completeCoreUserFlowFromCreationToRestore() throws Exception {
        AIAnalysisResponse analysis = analysisResponse();
        when(openAIService.analyzeTroubleLog(anyString(), anyString(), anyString(), anyList()))
                .thenThrow(AIServiceException.timeout(new RuntimeException("stub timeout")))
                .thenReturn(analysis);

        long targetId = createTrouble(
                "JWT 인증 실패",
                "로그인 요청에서 인증 오류 발생",
                "401 token expired",
                List.of("JWT", "Security")
        );

        mockMvc.perform(get("/api/troubles/{troubleId}", targetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiProcessingStatus").value("FAILED"))
                .andExpect(jsonPath("$.aiLastErrorCode").value("OPENAI_TIMEOUT"));

        mockMvc.perform(post("/api/troubles/{troubleId}/ai-analysis/retry", targetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.troubleId").value(targetId))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.lastErrorCode").doesNotExist());

        mockMvc.perform(put("/api/troubles/{troubleId}", targetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"JWT 토큰 갱신 실패",
                                  "description":"Refresh Token 처리 중 인증 오류 발생",
                                  "rawLog":"401 refresh token expired",
                                  "tags":["JWT","Backend"]
                                }
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/troubles/{troubleId}", targetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("JWT 토큰 갱신 실패"))
                .andExpect(jsonPath("$.tags.length()").value(2))
                .andExpect(jsonPath("$.tags").isArray())
                .andExpect(jsonPath("$.aiProcessingStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.aiAnalysis.summary").value("JWT 토큰 검증 실패"));

        long similarId = createTrouble(
                "JWT 만료 오류",
                "만료된 토큰으로 요청",
                "401 expired jwt",
                List.of("JWT")
        );
        saveEmbedding(targetId, vector(1.0f, 0.0f));
        saveEmbedding(similarId, vector(0.9f, 0.1f));

        mockMvc.perform(get("/api/troubles")
                        .param("status", "OPEN")
                        .param("tag", "JWT")
                        .param("keyword", "토큰")
                        .param("sort", "createdAt,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/troubles/{troubleId}/similar", targetId)
                        .param("limit", "5")
                        .param("minSimilarity", "0.9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].troubleId").value(similarId))
                .andExpect(jsonPath("$[0].title").value("JWT 만료 오류"))
                .andExpect(jsonPath("$[0].similarity").value(org.hamcrest.Matchers.greaterThan(0.99)));

        mockMvc.perform(patch("/api/troubles/{troubleId}/resolution", targetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actualCause":"토큰 만료 시간 설정 오류",
                                  "solution":"만료 시간 설정 수정",
                                  "referenceLink":"https://example.com/runbook"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/troubles/{troubleId}/retrospective", targetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"인증 장애 대응 과정","lesson":"만료 알림을 추가한다"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.troubleId").value(targetId));

        mockMvc.perform(get("/api/troubles/{troubleId}/retrospective", targetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("인증 장애 대응 과정"));

        mockMvc.perform(put("/api/troubles/{troubleId}/retrospective", targetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"수정된 대응 과정","lesson":"토큰 만료 모니터링을 강화한다"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lesson").value("토큰 만료 모니터링을 강화한다"));

        mockMvc.perform(get("/api/troubles/{troubleId}", targetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.actualCause").value("토큰 만료 시간 설정 오류"));

        mockMvc.perform(patch("/api/troubles/{troubleId}/archive", targetId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/troubles").param("keyword", "갱신"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/troubles").param("keyword", "갱신").param("archived", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(targetId));

        mockMvc.perform(patch("/api/troubles/{troubleId}/restore", targetId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/troubles").param("keyword", "갱신"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(targetId));
    }

    @Test
    void returnStandardErrorResponsesForInvalidRequests() throws Exception {
        mockMvc.perform(get("/api/troubles/{troubleId}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("TROUBLE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        mockMvc.perform(post("/api/troubles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","description":"","rawLog":"","tags":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        mockMvc.perform(post("/api/troubles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"));

        mockMvc.perform(get("/api/troubles/{troubleId}/similar", 1L)
                        .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private long createTrouble(
            String title,
            String description,
            String rawLog,
            List<String> tags
    ) throws Exception {
        String requestBody = objectMapper.writeValueAsString(new CreateRequest(
                title,
                description,
                rawLog,
                tags
        ));
        String responseBody = mockMvc.perform(post("/api/troubles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        return response.get("id").asLong();
    }

    private void saveEmbedding(long troubleId, float[] vector) {
        Trouble trouble = troubleRepository.findById(troubleId).orElseThrow();
        troubleEmbeddingRepository.saveAndFlush(TroubleEmbedding.builder()
                .trouble(trouble)
                .inputText(trouble.getTitle())
                .embedding(vector)
                .modelName("test-embedding-model")
                .build());
    }

    private float[] vector(float first, float second) {
        float[] vector = new float[EMBEDDING_DIMENSIONS];
        vector[0] = first;
        vector[1] = second;
        return vector;
    }

    private AIAnalysisResponse analysisResponse() {
        return new AIAnalysisResponse(
                "인증",
                "JWT 토큰 검증 실패",
                List.of("토큰 만료"),
                List.of("토큰 만료 시간 확인"),
                92
        );
    }

    private record CreateRequest(
            String title,
            String description,
            String rawLog,
            List<String> tags
    ) {
    }
}
