package com.jinlee.devtroublemate.trouble.repository;

import com.jinlee.devtroublemate.trouble.domain.Trouble;
import com.jinlee.devtroublemate.trouble.domain.TroubleStatus;
import com.jinlee.devtroublemate.trouble.dto.TroubleSearchCondition;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static com.jinlee.devtroublemate.tag.domain.QTag.tag;
import static com.jinlee.devtroublemate.tag.domain.QTroubleTag.troubleTag;
import static com.jinlee.devtroublemate.trouble.domain.QTrouble.trouble;

@RequiredArgsConstructor
public class TroubleRepositoryImpl implements TroubleRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Trouble> search(TroubleSearchCondition condition, Pageable pageable) {
        List<Trouble> content = queryFactory
                .selectDistinct(trouble)
                .from(trouble)
                .leftJoin(troubleTag).on(troubleTag.trouble.eq(trouble))
                .leftJoin(troubleTag.tag, tag)
                .where(
                        statusEq(condition.status()),
                        tagEq(condition.tag()),
                        keywordContains(condition.keyword())
                )
                .orderBy(trouble.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        var countQuery = queryFactory
                .select(trouble.id.countDistinct())
                .from(trouble)
                .leftJoin(troubleTag).on(troubleTag.trouble.eq(trouble))
                .leftJoin(troubleTag.tag, tag)
                .where(
                        statusEq(condition.status()),
                        tagEq(condition.tag()),
                        keywordContains(condition.keyword())
                );

        return PageableExecutionUtils.getPage(
                content,
                pageable,
                () -> countQuery.fetchOne()
        );
    }

    private BooleanExpression statusEq(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        return trouble.status.eq(TroubleStatus.valueOf(status));
    }

    private BooleanExpression tagEq(String tagName) {
        if (tagName == null || tagName.isBlank()) {
            return null;
        }

        return tag.name.eq(tagName);
    }

    private BooleanExpression keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return trouble.title.containsIgnoreCase(keyword)
                .or(trouble.description.containsIgnoreCase(keyword));
    }
}
