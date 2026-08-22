package com.jinlee.devtroublemate.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinlee.devtroublemate.ai.domain.AIAnalysis;
import com.jinlee.devtroublemate.ai.dto.AIAnalysisResponse;
import com.jinlee.devtroublemate.ai.domain.AIProcessingStatus;
import com.jinlee.devtroublemate.ai.exception.AIProcessingInProgressException;
import com.jinlee.devtroublemate.ai.exception.AIServiceException;
import com.jinlee.devtroublemate.ai.repository.AIAnalysisRepository;
import com.jinlee.devtroublemate.embedding.service.EmbeddingService;
import com.jinlee.devtroublemate.trouble.domain.Trouble;
import com.jinlee.devtroublemate.trouble.repository.TroubleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIAnalysisServiceTest {

    @Mock
    private OpenAIService openAIService;
    @Mock
    private AIAnalysisRepository aiAnalysisRepository;
    @Mock
    private TroubleRepository troubleRepository;
    @Mock
    private EmbeddingService embeddingService;

    private AIAnalysisService aiAnalysisService;

    @BeforeEach
    void setUp() {
        aiAnalysisService = new AIAnalysisService(
                openAIService,
                aiAnalysisRepository,
                troubleRepository,
                new ObjectMapper(),
                embeddingService
        );
    }

    @Test
    void updateExistingAnalysisAndEmbedding() {
        Trouble trouble = Trouble.builder()
                .title("수정 제목")
                .description("수정 설명")
                .rawLog("수정 로그")
                .build();
        AIAnalysis existing = AIAnalysis.builder()
                .trouble(trouble)
                .category("기존 카테고리")
                .summary("기존 요약")
                .possibleCauses("[]")
                .runbook("[]")
                .confidence(10)
                .build();
        AIAnalysisResponse response = new AIAnalysisResponse(
                "인증",
                "토큰 갱신 실패",
                List.of("만료 설정 오류"),
                List.of("설정 확인"),
                95
        );
        when(openAIService.analyzeTroubleLog(
                "수정 제목",
                "수정 설명",
                "수정 로그",
                List.of("JWT")
        )).thenReturn(response);
        when(aiAnalysisRepository.findTopByTroubleOrderByCreatedAtDesc(trouble))
                .thenReturn(Optional.of(existing));

        AIAnalysisResponse result = aiAnalysisService.analyzeAndSave(
                trouble,
                "수정 제목",
                "수정 설명",
                "수정 로그",
                List.of("JWT")
        );

        assertThat(result).isEqualTo(response);
        assertThat(existing.getCategory()).isEqualTo("인증");
        assertThat(existing.getSummary()).isEqualTo("토큰 갱신 실패");
        assertThat(existing.getPossibleCauses()).isEqualTo("[\"만료 설정 오류\"]");
        assertThat(existing.getRunbook()).isEqualTo("[\"설정 확인\"]");
        assertThat(existing.getConfidence()).isEqualTo(95);
        assertThat(trouble.getAiProcessingStatus()).isEqualTo(AIProcessingStatus.COMPLETED);
        assertThat(trouble.getAiProcessedAt()).isNotNull();
        verify(aiAnalysisRepository).save(existing);
        verify(embeddingService).createOrUpdate(trouble, response);
    }

    @Test
    void recordFailureStatus() {
        Trouble trouble = Trouble.builder().title("제목").description("설명").rawLog("로그").build();
        when(openAIService.analyzeTroubleLog("제목", "설명", "로그", List.of("JWT")))
                .thenThrow(AIServiceException.timeout(new RuntimeException()));

        assertThatThrownBy(() -> aiAnalysisService.analyzeAndSave(
                trouble, "제목", "설명", "로그", List.of("JWT")))
                .isInstanceOf(AIServiceException.class);

        assertThat(trouble.getAiProcessingStatus()).isEqualTo(AIProcessingStatus.FAILED);
        assertThat(trouble.getAiLastErrorCode()).isEqualTo("OPENAI_TIMEOUT");
        assertThat(trouble.getAiProcessedAt()).isNotNull();
    }

    @Test
    void rejectDuplicateProcessing() {
        Trouble trouble = Trouble.builder().title("제목").description("설명").rawLog("로그").build();
        trouble.startAIProcessing();

        assertThatThrownBy(() -> aiAnalysisService.analyzeAndSave(
                trouble, "제목", "설명", "로그", List.of()))
                .isInstanceOf(AIProcessingInProgressException.class);
    }
}
