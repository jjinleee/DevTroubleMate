package com.jinlee.devtroublemate.tag.repository;

import com.jinlee.devtroublemate.tag.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {
}