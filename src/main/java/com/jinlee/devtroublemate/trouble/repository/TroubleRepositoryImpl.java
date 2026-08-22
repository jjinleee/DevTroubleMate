package com.jinlee.devtroublemate.trouble.repository;

import com.jinlee.devtroublemate.trouble.domain.Trouble;
import com.jinlee.devtroublemate.trouble.domain.TroubleStatus;
import com.jinlee.devtroublemate.trouble.dto.TroubleSearchCondition;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
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
                .selectFrom(trouble)
                .where(
                        statusEq(condition.status()),
                        tagExists(condition.tag()),
                        keywordContains(condition.keyword()),
                        archivedEq(condition.archived())
                )
                .orderBy(trouble.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        var countQuery = queryFactory
                .select(trouble.id.count())
                .from(trouble)
                .where(
                        statusEq(condition.status()),
                        tagExists(condition.tag()),
                        keywordContains(condition.keyword()),
                        archivedEq(condition.archived())
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

    private BooleanExpression tagExists(String tagName) {
        if (tagName == null || tagName.isBlank()) {
            return null;
        }

        return JPAExpressions
                .selectOne()
                .from(troubleTag)
                .join(troubleTag.tag, tag)
                .where(
                        troubleTag.trouble.eq(trouble),
                        tag.name.eq(tagName)
                )
                .exists();
    }

    private BooleanExpression keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return trouble.title.containsIgnoreCase(keyword)
                .or(trouble.description.containsIgnoreCase(keyword));
    }

    private BooleanExpression archivedEq(Boolean archived) {
        return Boolean.TRUE.equals(archived)
                ? trouble.archivedAt.isNotNull()
                : trouble.archivedAt.isNull();
    }
}
