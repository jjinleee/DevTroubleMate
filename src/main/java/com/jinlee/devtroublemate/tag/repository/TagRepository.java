package com.jinlee.devtroublemate.tag.repository;

import com.jinlee.devtroublemate.tag.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
    long countByName(String name);

    @Modifying
    @Query(value = "INSERT INTO tag (name) VALUES (:name) ON CONFLICT (name) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("name") String name);
}
