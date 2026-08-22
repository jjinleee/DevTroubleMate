package com.jinlee.devtroublemate.embedding.service;

import com.jinlee.devtroublemate.ai.dto.AIAnalysisResponse;
import com.jinlee.devtroublemate.ai.exception.AIExceptionTranslator;
import com.jinlee.devtroublemate.embedding.domain.TroubleEmbedding;
import com.jinlee.devtroublemate.embedding.repository.TroubleEmbeddingRepository;
import com.jinlee.devtroublemate.trouble.domain.Trouble;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EmbeddingService {

    private static final String MODEL_NAME = "text-embedding-3-small";

    private final EmbeddingModel embeddingModel;
    private final TroubleEmbeddingRepository troubleEmbeddingRepository;

    public void createOrUpdate(
            Trouble trouble,
            AIAnalysisResponse aiAnalysisResponse
    ) {
        String inputText = createInputText(trouble, aiAnalysisResponse);

        float[] embedding;
        try {
            embedding = embeddingModel.embed(inputText);
        } catch (RuntimeException exception) {
            throw AIExceptionTranslator.forEmbedding(exception);
        }

        TroubleEmbedding troubleEmbedding = troubleEmbeddingRepository
                .findByTrouble(trouble)
                .orElseGet(() -> TroubleEmbedding.builder()
                        .trouble(trouble)
                        .inputText(inputText)
                        .embedding(embedding)
                        .modelName(MODEL_NAME)
                        .build());

        troubleEmbedding.update(
                inputText,
                embedding,
                MODEL_NAME
        );

        troubleEmbeddingRepository.save(troubleEmbedding);
    }

    private String createInputText(
            Trouble trouble,
            AIAnalysisResponse aiAnalysisResponse
    ) {
        return """
                제목: %s
                설명: %s
                로그: %s
                AI 요약: %s
                """.formatted(
                trouble.getTitle(),
                trouble.getDescription(),
                trouble.getRawLog(),
                aiAnalysisResponse.summary()
        );
    }
}
