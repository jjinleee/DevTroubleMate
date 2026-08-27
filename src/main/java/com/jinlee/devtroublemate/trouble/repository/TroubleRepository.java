package com.jinlee.devtroublemate.trouble.repository;

import com.jinlee.devtroublemate.trouble.domain.Trouble;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TroubleRepository extends JpaRepository<Trouble, Long>, TroubleRepositoryCustom {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Trouble t WHERE t.id = :troubleId")
    Optional<Trouble> findByIdForUpdate(@Param("troubleId") Long troubleId);
}
