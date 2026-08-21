package com.jinlee.devtroublemate.trouble.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinlee.devtroublemate.ai.dto.AIAnalysisSummaryResponse;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
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

    public CreateTroubleResponse create(CreateTroubleRequest request) {

        Trouble trouble = Trouble.builder()
                .title(request.title())
                .description(request.description())
                .rawLog(request.rawLog())
                .build();

        Trouble saved = troubleRepository.save(trouble);

        if (request.tags() != null) {
            request.tags().forEach(tagName -> {
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(
                                Tag.builder()
                                        .name(tagName)
                                        .build()
                        ));

                TroubleTag troubleTag = TroubleTag.builder()
                        .trouble(saved)
                        .tag(tag)
                        .build();

                troubleTagRepository.save(troubleTag);
            });
        }
        aiAnalysisService.analyzeAndSave(
                saved,
                request.title(),
                request.description(),
                request.rawLog(),
                request.tags()
        );

        return new CreateTroubleResponse(saved.getId());
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
                aiAnalysis
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
}
