package com.jinlee.devtroublemate.trouble.service;

import com.jinlee.devtroublemate.tag.domain.Tag;
import com.jinlee.devtroublemate.tag.domain.TroubleTag;
import com.jinlee.devtroublemate.tag.repository.TagRepository;
import com.jinlee.devtroublemate.tag.repository.TroubleTagRepository;
import com.jinlee.devtroublemate.trouble.dto.TroubleSearchCondition;

import com.jinlee.devtroublemate.trouble.domain.Trouble;
import com.jinlee.devtroublemate.trouble.dto.*;
import com.jinlee.devtroublemate.trouble.repository.TroubleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TroubleService {

    private final TroubleRepository troubleRepository;
    private final TagRepository tagRepository;
    private final TroubleTagRepository troubleTagRepository;

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

        return new CreateTroubleResponse(saved.getId());
    }

    @Transactional(readOnly = true)
    public TroubleDetailResponse getDetail(Long troubleId) {

        Trouble trouble = troubleRepository.findById(troubleId)
                .orElseThrow(() ->
                        new IllegalArgumentException("장애를 찾을 수 없습니다.")
                );

        List<String> tags = troubleTagRepository.findAllByTrouble(trouble)
                .stream()
                .map(troubleTag -> troubleTag.getTag().getName())
                .toList();

        return new TroubleDetailResponse(
                trouble.getId(),
                trouble.getTitle(),
                trouble.getDescription(),
                trouble.getRawLog(),
                trouble.getStatus().name(),
                trouble.getActualCause(),
                trouble.getSolution(),
                trouble.getReferenceLink(),
                tags
        );
    }

    @Transactional(readOnly = true)
    public List<TroubleSummaryResponse> getTroubles(TroubleSearchCondition condition) {

        return troubleRepository.search(condition)
                .stream()
                .map(trouble -> {

                    List<String> tags = troubleTagRepository.findAllByTrouble(trouble)
                            .stream()
                            .map(troubleTag -> troubleTag.getTag().getName())
                            .toList();

                    return new TroubleSummaryResponse(
                            trouble.getId(),
                            trouble.getTitle(),
                            trouble.getStatus().name(),
                            trouble.getCreatedAt(),
                            tags
                    );
                })
                .toList();
    }

    public void resolve(
            Long troubleId,
            ResolveTroubleRequest request
    ) {

        Trouble trouble = troubleRepository.findById(troubleId)
                .orElseThrow(() ->
                        new IllegalArgumentException("장애를 찾을 수 없습니다.")
                );

        trouble.resolve(
                request.actualCause(),
                request.solution(),
                request.referenceLink()
        );
    }
}