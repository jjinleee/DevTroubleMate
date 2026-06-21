package com.jinlee.devtroublemate.retrospective.repository;

import com.jinlee.devtroublemate.retrospective.domain.Retrospective;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetrospectiveRepository extends JpaRepository<Retrospective, Long> {
}