package com.jinlee.devtroublemate.tag.repository;

import com.jinlee.devtroublemate.tag.domain.TroubleTag;
import com.jinlee.devtroublemate.trouble.domain.Trouble;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TroubleTagRepository extends JpaRepository<TroubleTag, Long> {
    List<TroubleTag> findAllByTrouble(Trouble trouble);
}