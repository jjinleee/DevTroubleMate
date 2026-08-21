package com.jinlee.devtroublemate.embedding.service;

import com.jinlee.devtroublemate.embedding.dto.SimilarTroubleResponse;
import com.jinlee.devtroublemate.embedding.repository.TroubleEmbeddingRepository;
import com.jinlee.devtroublemate.trouble.domain.Trouble;
import com.jinlee.devtroublemate.trouble.exception.TroubleNotFoundException;
import com.jinlee.devtroublemate.trouble.repository.TroubleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SimilarTroubleService {

    private final TroubleRepository troubleRepository;
    private final TroubleEmbeddingRepository troubleEmbeddingRepository;

    public List<SimilarTroubleResponse> findSimilarTroubles(Long troubleId, int limit) {

        Trouble trouble = troubleRepository.findById(troubleId)
                .orElseThrow(() -> new TroubleNotFoundException(troubleId));

        List<Object[]> results =
                troubleEmbeddingRepository.findSimilarTroubles(
                        trouble.getId(),
                        limit
                );

        return results.stream()
                .map(this::toResponse)
                .toList();
    }

    private SimilarTroubleResponse toResponse(Object[] row) {

        return new SimilarTroubleResponse(
                ((Number) row[0]).longValue(),
                (String) row[1],
                row[2].toString(),
                ((Number) row[3]).doubleValue()
        );
    }
}