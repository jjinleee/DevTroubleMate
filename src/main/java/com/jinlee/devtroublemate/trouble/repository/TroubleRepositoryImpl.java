package com.jinlee.devtroublemate.trouble.repository;

import com.jinlee.devtroublemate.trouble.domain.Trouble;
import com.jinlee.devtroublemate.trouble.domain.TroubleStatus;
import com.jinlee.devtroublemate.trouble.dto.TroubleSearchCondition;
import com.jinlee.devtroublemate.trouble.exception.InvalidTroubleSortException;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.ArrayList;
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
                .orderBy(orderSpecifiers(pageable))
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

    private OrderSpecifier<?>[] orderSpecifiers(Pageable pageable) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();
        pageable.getSort().forEach(order -> orderSpecifiers.add(toOrderSpecifier(order)));
        orderSpecifiers.add(trouble.id.desc());
        return orderSpecifiers.toArray(OrderSpecifier[]::new);
    }

    private OrderSpecifier<?> toOrderSpecifier(Sort.Order order) {
        boolean ascending = order.isAscending();
        return switch (order.getProperty()) {
            case "createdAt" -> ascending ? trouble.createdAt.asc() : trouble.createdAt.desc();
            case "updatedAt" -> ascending ? trouble.updatedAt.asc() : trouble.updatedAt.desc();
            case "title" -> ascending ? trouble.title.asc() : trouble.title.desc();
            case "status" -> ascending ? trouble.status.asc() : trouble.status.desc();
            default -> throw new InvalidTroubleSortException(order.getProperty());
        };
    }
}
