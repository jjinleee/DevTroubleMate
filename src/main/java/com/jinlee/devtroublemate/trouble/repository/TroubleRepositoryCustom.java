package com.jinlee.devtroublemate.trouble.repository;

import com.jinlee.devtroublemate.trouble.domain.Trouble;
import com.jinlee.devtroublemate.trouble.dto.TroubleSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TroubleRepositoryCustom {

    Page<Trouble> search(TroubleSearchCondition condition, Pageable pageable);
}
