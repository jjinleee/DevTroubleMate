package com.jinlee.devtroublemate.embedding.repository;

import com.jinlee.devtroublemate.embedding.domain.TroubleEmbedding;
import com.jinlee.devtroublemate.trouble.domain.Trouble;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TroubleEmbeddingRepository
        extends JpaRepository<TroubleEmbedding, Long> {

    Optional<TroubleEmbedding> findByTrouble(Trouble trouble);
    boolean existsByTrouble(Trouble trouble);

    @Query(value = """
        SELECT
            t.id,
            t.title,
            t.status,
            1 - (te.embedding <=> (
                SELECT target.embedding
                FROM trouble_embedding target
                WHERE target.trouble_id = :troubleId
            )) AS similarity
        FROM trouble_embedding te
        JOIN trouble t
            ON te.trouble_id = t.id
        WHERE te.trouble_id <> :troubleId
          AND t.archived_at IS NULL
        ORDER BY te.embedding <=> (
            SELECT target.embedding
            FROM trouble_embedding target
            WHERE target.trouble_id = :troubleId
        )
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarTroubles(
            @Param("troubleId") Long troubleId,
            @Param("limit") int limit
    );
}
