package com.jinlee.devtroublemate.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinlee.devtroublemate.ai.domain.AIAnalysis;
import com.jinlee.devtroublemate.ai.dto.AIAnalysisResponse;
import com.jinlee.devtroublemate.ai.dto.AnalyzeTroubleLogRequest;
import com.jinlee.devtroublemate.ai.repository.AIAnalysisRepository;
import com.jinlee.devtroublemate.ai.exception.AIServiceException;
import com.jinlee.devtroublemate.ai.domain.AIProcessingStatus;
import com.jinlee.devtroublemate.ai.exception.AIProcessingInProgressException;
import com.jinlee.devtroublemate.trouble.domain.Trouble;
import com.jinlee.devtroublemate.trouble.exception.TroubleNotFoundException;
import com.jinlee.devtroublemate.trouble.repository.TroubleRepository;
import com.jinlee.devtroublemate.embedding.service.EmbeddingService;
import com.jinlee.devtroublemate.tag.repository.TroubleTagRepository;
import com.jinlee.devtroublemate.ai.prompt.TroubleAnalysisPrompt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(noRollbackFor = AIServiceException.class)
public class AIAnalysisService {

    private final OpenAIService openAIService;
    private final AIAnalysisRepository aiAnalysisRepository;
    private final TroubleRepository troubleRepository;
    private final ObjectMapper objectMapper;
    private final EmbeddingService embeddingService;
    private final TroubleTagRepository troubleTagRepository;

    public AIAnalysisResponse analyzeAndSave(
            Trouble trouble,
            String title,
            String description,
            String rawLog,
            java.util.List<String> tags
    ) {
        if (trouble.getAiProcessingStatus() == AIProcessingStatus.PROCESSING) {
            throw new AIProcessingInProgressException(trouble.getId());
        }
        trouble.startAIProcessing();
        troubleRepository.save(trouble);

        try {
            AIAnalysisResponse response = openAIService.analyzeTroubleLog(
                    title,
                    description,
                    rawLog,
                    tags
            );

            String possibleCauses = toJson(response.possibleCauses());
            String runbook = toJson(response.runbook());

            AIAnalysis aiAnalysis = aiAnalysisRepository
                    .findTopByTroubleOrderByCreatedAtDesc(trouble)
                    .map(existing -> {
                        existing.update(
                                response.category(),
                                response.summary(),
                                possibleCauses,
                                runbook,
                                response.confidence(),
                                TroubleAnalysisPrompt.MODEL_NAME,
                                TroubleAnalysisPrompt.VERSION
                        );
                        return existing;
                    })
                    .orElseGet(() -> AIAnalysis.builder()
                            .trouble(trouble)
                            .category(response.category())
                            .summary(response.summary())
                            .possibleCauses(possibleCauses)
                            .runbook(runbook)
                            .confidence(response.confidence())
                            .modelName(TroubleAnalysisPrompt.MODEL_NAME)
                            .promptVersion(TroubleAnalysisPrompt.VERSION)
                            .build());

            aiAnalysisRepository.save(aiAnalysis);
            embeddingService.createOrUpdate(trouble, response);
            trouble.completeAIProcessing();
            troubleRepository.save(trouble);

            return response;
        } catch (AIServiceException exception) {
            trouble.failAIProcessing(exception.getCode(), exception.getMessage());
            troubleRepository.save(trouble);
            throw exception;
        }
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            noRollbackFor = AIServiceException.class
    )
    public AIAnalysisResponse analyzeStoredTrouble(Long troubleId) {
        Trouble trouble = troubleRepository.findById(troubleId)
                .orElseThrow(() -> new TroubleNotFoundException(troubleId));
        java.util.List<String> tags = troubleTagRepository.findAllByTrouble(trouble).stream()
                .map(troubleTag -> troubleTag.getTag().getName())
                .toList();

        return analyzeAndSave(
                trouble,
                trouble.getTitle(),
                trouble.getDescription(),
                trouble.getRawLog(),
                tags
        );
    }

    public AIAnalysisResponse analyzeAndSave(
            Long troubleId,
            AnalyzeTroubleLogRequest request
    ) {
        Trouble trouble = troubleRepository.findById(troubleId)
                .orElseThrow(() -> new TroubleNotFoundException(troubleId));

        return analyzeAndSave(
                trouble,
                request.title(),
                request.description(),
                request.rawLog(),
                request.tags()
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw AIServiceException.responseProcessingFailed(e);
        }
    }
}
