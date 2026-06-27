package com.jinlee.devtroublemate.ai.repository;

import com.jinlee.devtroublemate.ai.domain.AIAnalysis;
import com.jinlee.devtroublemate.trouble.domain.Trouble;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AIAnalysisRepository extends JpaRepository<AIAnalysis, Long> {

    Optional<AIAnalysis> findTopByTroubleOrderByCreatedAtDesc(Trouble trouble);
}