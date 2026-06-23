package com.jinlee.devtroublemate.trouble.repository;

import com.jinlee.devtroublemate.trouble.domain.Trouble;
import com.jinlee.devtroublemate.trouble.dto.TroubleSearchCondition;

import java.util.List;

public interface TroubleRepositoryCustom {

    List<Trouble> search(TroubleSearchCondition condition);
}