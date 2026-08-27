package com.jinlee.devtroublemate.trouble.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinlee.devtroublemate.ai.dto.AIAnalysisSummaryResponse;
import com.jinlee.devtroublemate.ai.dto.AIProcessingResponse;
import com.jinlee.devtroublemate.ai.domain.AIProcessingStatus;
import com.jinlee.devtroublemate.ai.exception.AIRetryNotAllowedException;
import com.jinlee.devtroublemate.ai.exception.AIServiceException;
import com.jinlee.devtroublemate.ai.event.AIAnalysisRequestedEvent;
import com.jinlee.devtroublemate.ai.repository.AIAnalysisRepository;
import com.jinlee.devtroublemate.ai.service.AIAnalysisService;
import com.jinlee.devtroublemate.common.dto.PageResponse;
import com.jinlee.devtroublemate.tag.domain.Tag;
import com.jinlee.devtroublemate.tag.domain.TroubleTag;
import com.jinlee.devtroublemate.tag.repository.TagRepository;
import com.jinlee.devtroublemate.tag.repository.TroubleTagRepository;
import com.jinlee.devtroublemate.trouble.dto.TroubleSearchCondition;

import com.jinlee.devtroublemate.trouble.domain.Trouble;
import com.jinlee.devtroublemate.trouble.dto.*;
import com.jinlee.devtroublemate.trouble.exception.TroubleNotFoundException;
import com.jinlee.devtroublemate.trouble.repository.TroubleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TroubleService {

    private final TroubleRepository troubleRepository;
    private final TagRepository tagRepository;
    private final TroubleTagRepository troubleTagRepository;
    private final AIAnalysisService aiAnalysisService;
    private final AIAnalysisRepository aiAnalysisRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public CreateTroubleResponse create(CreateTroubleRequest request) {

        Trouble trouble = Trouble.builder()
                .title(request.title())
                .description(request.description())
                .rawLog(request.rawLog())
                .build();

        Trouble saved = troubleRepository.save(trouble);

        saveTags(saved, request.tags());
        eventPublisher.publishEvent(new AIAnalysisRequestedEvent(saved.getId()));

        return new CreateTroubleResponse(saved.getId());
    }

    public void update(Long troubleId, UpdateTroubleRequest request) {
        Trouble trouble = troubleRepository.findById(troubleId)
                .orElseThrow(() -> new TroubleNotFoundException(troubleId));

        trouble.update(
                request.title(),
                request.description(),
                request.rawLog()
        );

        List<TroubleTag> existingTags = troubleTagRepository.findAllByTrouble(trouble);
        List<String> requestedTags = normalizeTags(request.tags());
        Set<String> requestedTagSet = Set.copyOf(requestedTags);
        Set<String> existingTagSet = existingTags.stream()
                .map(troubleTag -> troubleTag.getTag().getName())
                .collect(Collectors.toSet());

        List<TroubleTag> removedTags = existingTags.stream()
                .filter(troubleTag -> !requestedTagSet.contains(troubleTag.getTag().getName()))
                .toList();
        troubleTagRepository.deleteAll(removedTags);
        saveTags(
                trouble,
                requestedTags.stream()
                        .filter(tagName -> !existingTagSet.contains(tagName))
                        .toList()
        );
        trouble.resetAIProcessing();
        eventPublisher.publishEvent(new AIAnalysisRequestedEvent(trouble.getId()));
    }

    @Transactional(readOnly = true)
    public TroubleDetailResponse getDetail(Long troubleId) {

        Trouble trouble = troubleRepository.findById(troubleId)
                .orElseThrow(() ->
                        new TroubleNotFoundException(troubleId)
                );

        List<String> tags = troubleTagRepository
                .findAllWithTagByTroubleIdIn(List.of(troubleId))
                .stream()
                .map(troubleTag -> troubleTag.getTag().getName())
                .toList();

        AIAnalysisSummaryResponse aiAnalysis = aiAnalysisRepository
                .findTopByTroubleOrderByCreatedAtDesc(trouble)
                .map(analysis -> AIAnalysisSummaryResponse.from(analysis, objectMapper))
                .orElse(null);

        return new TroubleDetailResponse(
                trouble.getId(),
                trouble.getTitle(),
                trouble.getDescription(),
                trouble.getRawLog(),
                trouble.getStatus().name(),
                trouble.getActualCause(),
                trouble.getSolution(),
                trouble.getReferenceLink(),
                tags,
                aiAnalysis,
                trouble.getArchivedAt(),
                trouble.getAiProcessingStatus().name(),
                trouble.getAiLastErrorCode(),
                trouble.getAiLastErrorMessage(),
                trouble.getAiProcessedAt()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<TroubleSummaryResponse> getTroubles(
            TroubleSearchCondition condition,
            Pageable pageable
    ) {

        Page<Trouble> troubles = troubleRepository.search(condition, pageable);

        if (!troubles.hasContent()) {
            return PageResponse.from(troubles.map(this::toSummaryResponse));
        }

        List<Long> troubleIds = troubles.getContent().stream()
                .map(Trouble::getId)
                .toList();

        Map<Long, List<String>> tagsByTroubleId = troubleTagRepository
                .findAllWithTagByTroubleIdIn(troubleIds)
                .stream()
                .collect(Collectors.groupingBy(
                        troubleTag -> troubleTag.getTrouble().getId(),
                        Collectors.mapping(troubleTag -> troubleTag.getTag().getName(), Collectors.toList())
                ));

        Page<TroubleSummaryResponse> response = troubles.map(trouble ->
                toSummaryResponse(
                        trouble,
                        tagsByTroubleId.getOrDefault(trouble.getId(), List.of())
                )
        );

        return PageResponse.from(response);
    }

    private TroubleSummaryResponse toSummaryResponse(Trouble trouble) {
        return toSummaryResponse(trouble, List.of());
    }

    private TroubleSummaryResponse toSummaryResponse(Trouble trouble, List<String> tags) {
        return new TroubleSummaryResponse(
                trouble.getId(),
                trouble.getTitle(),
                trouble.getStatus().name(),
                trouble.getCreatedAt(),
                tags
        );
    }

    private void saveTags(Trouble trouble, List<String> tagNames) {
        normalizeTags(tagNames).forEach(tagName -> {
            tagRepository.insertIfAbsent(tagName);
            Tag tag = tagRepository.findByName(tagName)
                    .orElseThrow(() -> new IllegalStateException("태그 저장 후 조회에 실패했습니다. tagName=" + tagName));

            TroubleTag troubleTag = TroubleTag.builder()
                    .trouble(trouble)
                    .tag(tag)
                    .build();

            troubleTagRepository.save(troubleTag);
        });
    }

    private List<String> normalizeTags(List<String> tagNames) {
        return tagNames.stream()
                .map(String::trim)
                .distinct()
                .toList();
    }

    public AIProcessingResponse retryAIAnalysis(Long troubleId) {
        Trouble trouble = troubleRepository.findByIdForUpdate(troubleId)
                .orElseThrow(() -> new TroubleNotFoundException(troubleId));
        if (trouble.getAiProcessingStatus() != AIProcessingStatus.FAILED
                && trouble.getAiProcessingStatus() != AIProcessingStatus.PENDING) {
            throw new AIRetryNotAllowedException(troubleId);
        }

        List<String> tags = troubleTagRepository.findAllByTrouble(trouble).stream()
                .map(troubleTag -> troubleTag.getTag().getName())
                .toList();
        tryAnalyze(
                trouble,
                trouble.getTitle(),
                trouble.getDescription(),
                trouble.getRawLog(),
                tags
        );
        return AIProcessingResponse.from(trouble);
    }

    private void tryAnalyze(
            Trouble trouble,
            String title,
            String description,
            String rawLog,
            List<String> tags
    ) {
        try {
            aiAnalysisService.analyzeAndSave(trouble, title, description, rawLog, tags);
        } catch (AIServiceException ignored) {
            // 장애 데이터와 AI 실패 상태는 유지하고 재시도 API를 통해 복구한다.
        }
    }

    public void resolve(
            Long troubleId,
            ResolveTroubleRequest request
    ) {

        Trouble trouble = troubleRepository.findById(troubleId)
                .orElseThrow(() ->
                        new TroubleNotFoundException(troubleId)
                );

        trouble.resolve(
                request.actualCause(),
                request.solution(),
                request.referenceLink()
        );
    }

    public void archive(Long troubleId) {
        Trouble trouble = troubleRepository.findById(troubleId)
                .orElseThrow(() -> new TroubleNotFoundException(troubleId));
        trouble.archive();
    }

    public void restore(Long troubleId) {
        Trouble trouble = troubleRepository.findById(troubleId)
                .orElseThrow(() -> new TroubleNotFoundException(troubleId));
        trouble.restore();
    }
}
