# 장애 일반 검색 인덱스

## 검색 구조

장애 목록 조회는 다음 조건을 조합한다.

- 보관 여부
- 상태
- 생성일 역순 정렬과 페이지네이션
- 제목 또는 설명의 부분 문자열
- 태그 이름의 정확 일치

태그 조건이 없을 때도 `trouble_tag`와 `tag`를 조인하던 기존 쿼리는 중복 행 제거를 위한 `DISTINCT`가 필요했다. 태그 조건을 상관 `EXISTS` 서브쿼리로 변경해 기본 목록과 개수 쿼리는 `trouble`만 조회하고, 태그 조건이 있을 때만 연결 테이블을 탐색한다.

## 인덱스 선택

### 목록과 상태

```sql
CREATE INDEX idx_trouble_active_created_at
    ON trouble (created_at DESC)
    WHERE archived_at IS NULL;

CREATE INDEX idx_trouble_active_status_created_at
    ON trouble (status, created_at DESC)
    WHERE archived_at IS NULL;
```

기본 조회는 보관되지 않은 장애를 생성일 역순으로 반환한다. 부분 인덱스로 활성 데이터만 유지하고, 상태 조건이 있으면 복합 인덱스가 필터와 정렬을 함께 처리한다. 보관 목록에는 별도의 `created_at DESC` 부분 인덱스를 사용한다.

### 제목과 설명

`containsIgnoreCase`는 `lower(column) LIKE '%keyword%'` 형태이므로 일반 B-tree 인덱스를 사용할 수 없다. `pg_trgm` 확장과 표현식 GIN 인덱스를 사용한다.

```sql
CREATE INDEX idx_trouble_title_trgm
    ON trouble USING gin (lower(title) gin_trgm_ops);

CREATE INDEX idx_trouble_description_trgm
    ON trouble USING gin (lower(description) gin_trgm_ops);
```

두 필드의 OR 조건은 PostgreSQL이 각 GIN 인덱스의 결과를 `BitmapOr`로 결합할 수 있다. 한두 글자의 매우 짧은 검색어 또는 일치 비율이 높은 검색어는 비용 판단에 따라 순차 검색이 더 효율적일 수 있다.

`pg_trgm` 확장은 현재 `schema.sql`에서 멱등하게 활성화한다. 운영 환경에서 애플리케이션 계정에 확장 생성 권한을 주지 않는 경우에는 데이터베이스 관리자가 배포 전에 한 번 활성화해야 한다.

### 태그

`tag.name`에는 유니크 B-tree 인덱스가 이미 있다. 연결 테이블에는 양방향 탐색을 지원하는 인덱스를 추가한다.

```sql
CREATE INDEX idx_trouble_tag_trouble_id
    ON trouble_tag (trouble_id);

CREATE INDEX idx_trouble_tag_tag_id_trouble_id
    ON trouble_tag (tag_id, trouble_id);
```

`(tag_id, trouble_id)`는 태그로 장애를 찾는 `EXISTS` 쿼리를 커버하고, `trouble_id` 인덱스는 장애에서 태그를 조회하는 반대 방향과 외래 키 검사 비용을 줄인다. PostgreSQL은 외래 키 열에 인덱스를 자동 생성하지 않으므로 둘 다 명시한다.

## 운영 확인 쿼리

실제 데이터가 충분히 쌓인 환경에서 다음 실행 계획을 확인한다.

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT t.id
FROM trouble t
WHERE t.archived_at IS NULL
  AND t.status = 'OPEN'
ORDER BY t.created_at DESC
LIMIT 20;
```

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT t.id
FROM trouble t
WHERE t.archived_at IS NULL
  AND (
      lower(t.title) LIKE '%timeout%'
      OR lower(t.description) LIKE '%timeout%'
  )
ORDER BY t.created_at DESC
LIMIT 20;
```

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT t.id
FROM trouble t
WHERE t.archived_at IS NULL
  AND EXISTS (
      SELECT 1
      FROM trouble_tag tt
      JOIN tag tg ON tg.id = tt.tag_id
      WHERE tt.trouble_id = t.id
        AND tg.name = 'postgresql'
  )
ORDER BY t.created_at DESC
LIMIT 20;
```

개발 DB처럼 행이 매우 적으면 인덱스가 있어도 순차 검색이 정상적인 선택이다. 성능 검증은 운영 테이블을 오염시키지 않는 임시 합성 데이터로 수행하고, 결과는 측정 환경과 함께 기록한다.

## 측정 결과

측정 환경:

- PostgreSQL 16
- 장애 30,000개
- 태그 100개와 장애-태그 연결 90,000개
- 운영 테이블을 변경하지 않는 임시 테이블
- `EXPLAIN (ANALYZE, BUFFERS)` 사용

| 검색 | 인덱스 적용 전 | 인덱스 적용 후 | 적용 후 주요 계획 |
|---|---:|---:|---|
| 활성 상태 목록 Top 20 | 2.397ms | 0.025ms | 생성일 부분 인덱스 `Index Scan` |
| 제목·설명 `timeout` 검색 | 10.125ms | 0.323ms | 두 GIN 인덱스의 `BitmapOr` |
| `postgresql` 태그 목록 Top 20 | 6.849ms | 1.218ms | 부분 인덱스 + 연결 테이블 `Index Scan` |

상태 선택도가 높은 별도 분포에서는 `(status, created_at DESC)` 복합 부분 인덱스가 직접 선택되었고 Top 20 조회가 0.016ms에 실행됐다. 실제 개선 폭은 데이터 분포와 캐시 상태에 따라 달라지므로, 위 수치는 인덱스 선택 근거와 실행 계획 검증용으로 사용한다.
