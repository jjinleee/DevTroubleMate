package com.jinlee.devtroublemate.embedding.repository;

import com.jinlee.devtroublemate.embedding.domain.TroubleEmbedding;
import com.jinlee.devtroublemate.trouble.domain.Trouble;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TroubleEmbeddingRepository
        extends JpaRepository<TroubleEmbedding, Long> {

    Optional<TroubleEmbedding> findByTrouble(Trouble trouble);
}