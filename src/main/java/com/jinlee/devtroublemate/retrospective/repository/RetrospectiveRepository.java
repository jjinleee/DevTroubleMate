package com.jinlee.devtroublemate.retrospective.repository;

import com.jinlee.devtroublemate.retrospective.domain.Retrospective;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RetrospectiveRepository extends JpaRepository<Retrospective, Long> {
    Optional<Retrospective> findByTroubleId(Long troubleId);
    boolean existsByTroubleId(Long troubleId);
}
