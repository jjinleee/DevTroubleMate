package com.jinlee.devtroublemate.tag.repository;

import com.jinlee.devtroublemate.tag.domain.TroubleTag;
import com.jinlee.devtroublemate.trouble.domain.Trouble;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TroubleTagRepository extends JpaRepository<TroubleTag, Long> {

    List<TroubleTag> findAllByTrouble(Trouble trouble);

    @Query("SELECT tt FROM TroubleTag tt JOIN FETCH tt.tag WHERE tt.trouble.id IN :troubleIds")
    List<TroubleTag> findAllWithTagByTroubleIdIn(@Param("troubleIds") Collection<Long> troubleIds);
}