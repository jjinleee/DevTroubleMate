package com.jinlee.devtroublemate.retrospective.service;

import com.jinlee.devtroublemate.retrospective.domain.Retrospective;
import com.jinlee.devtroublemate.retrospective.dto.RetrospectiveRequest;
import com.jinlee.devtroublemate.retrospective.dto.RetrospectiveResponse;
import com.jinlee.devtroublemate.retrospective.exception.RetrospectiveAlreadyExistsException;
import com.jinlee.devtroublemate.retrospective.exception.RetrospectiveNotFoundException;
import com.jinlee.devtroublemate.retrospective.repository.RetrospectiveRepository;
import com.jinlee.devtroublemate.trouble.domain.Trouble;
import com.jinlee.devtroublemate.trouble.exception.TroubleNotFoundException;
import com.jinlee.devtroublemate.trouble.repository.TroubleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RetrospectiveService {

    private final RetrospectiveRepository retrospectiveRepository;
    private final TroubleRepository troubleRepository;

    public RetrospectiveResponse create(Long troubleId, RetrospectiveRequest request) {
        Trouble trouble = findTrouble(troubleId);
        if (retrospectiveRepository.existsByTroubleId(troubleId)) {
            throw new RetrospectiveAlreadyExistsException(troubleId);
        }

        Retrospective retrospective = Retrospective.builder()
                .trouble(trouble)
                .content(request.content())
                .lesson(request.lesson())
                .build();

        try {
            return RetrospectiveResponse.from(retrospectiveRepository.saveAndFlush(retrospective));
        } catch (DataIntegrityViolationException exception) {
            throw new RetrospectiveAlreadyExistsException(troubleId);
        }
    }

    @Transactional(readOnly = true)
    public RetrospectiveResponse get(Long troubleId) {
        findTrouble(troubleId);
        return retrospectiveRepository.findByTroubleId(troubleId)
                .map(RetrospectiveResponse::from)
                .orElseThrow(() -> new RetrospectiveNotFoundException(troubleId));
    }

    public RetrospectiveResponse update(Long troubleId, RetrospectiveRequest request) {
        findTrouble(troubleId);
        Retrospective retrospective = retrospectiveRepository.findByTroubleId(troubleId)
                .orElseThrow(() -> new RetrospectiveNotFoundException(troubleId));
        retrospective.update(request.content(), request.lesson());
        return RetrospectiveResponse.from(retrospective);
    }

    private Trouble findTrouble(Long troubleId) {
        return troubleRepository.findById(troubleId)
                .orElseThrow(() -> new TroubleNotFoundException(troubleId));
    }
}
