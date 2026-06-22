package com.jinlee.devtroublemate.trouble.repository;

import com.jinlee.devtroublemate.trouble.domain.Trouble;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TroubleRepository extends JpaRepository<Trouble, Long>, TroubleRepositoryCustom {
}