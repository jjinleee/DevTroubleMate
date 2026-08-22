package com.jinlee.devtroublemate.embedding.service;

import com.jinlee.devtroublemate.ai.dto.AIAnalysisResponse;
import com.jinlee.devtroublemate.ai.exception.AIServiceException;
import com.jinlee.devtroublemate.embedding.repository.TroubleEmbeddingRepository;
import com.jinlee.devtroublemate.trouble.domain.Trouble;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock EmbeddingModel embeddingModel;
    @Mock TroubleEmbeddingRepository troubleEmbeddingRepository;
    @InjectMocks EmbeddingService embeddingService;

    @Test
    void translateEmbeddingFailure() {
        Trouble trouble = Trouble.builder().title("제목").description("설명").rawLog("로그").build();
        AIAnalysisResponse analysis = new AIAnalysisResponse(
                "분류", "요약", List.of("원인"), List.of("점검"), 90);
        when(embeddingModel.embed(anyString())).thenThrow(new RuntimeException("provider failure"));

        assertThatThrownBy(() -> embeddingService.createOrUpdate(trouble, analysis))
                .isInstanceOfSatisfying(AIServiceException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("EMBEDDING_FAILED"));
    }
}
