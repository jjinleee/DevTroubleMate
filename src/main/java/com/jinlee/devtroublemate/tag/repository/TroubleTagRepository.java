package com.jinlee.devtroublemate.tag.repository;

import com.jinlee.devtroublemate.tag.domain.TroubleTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TroubleTagRepository extends JpaRepository<TroubleTag, Long> {
}