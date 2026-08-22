# pgvector 유사 장애 검색 인덱스

## 선택

`trouble_embedding.embedding`에는 코사인 거리용 HNSW 인덱스를 사용한다.

```sql
CREATE INDEX IF NOT EXISTS idx_trouble_embedding_embedding_hnsw_cosine
    ON trouble_embedding
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
```

- 검색 쿼리의 `<=>`는 코사인 거리 연산자이므로 `vector_cosine_ops`와 일치한다.
- HNSW는 인덱스 생성 시간이 길고 메모리를 더 사용하지만, 학습 단계 없이 생성할 수 있고 데이터가 적을 때부터 안정적인 검색 품질을 제공한다.
- IVFFlat은 인덱스가 작고 생성이 빠르지만 충분한 학습 데이터가 필요하며, 데이터 증가에 따라 `lists`와 `probes`를 다시 조정해야 한다.
- 현재처럼 데이터가 계속 추가되는 장애 지식 저장소에는 별도의 학습 시점을 운영하지 않아도 되는 HNSW가 적합하다.

`m = 16`, `ef_construction = 64`는 pgvector의 균형 잡힌 기본값을 명시한 것이다. 검색 결과 수가 최대 20개이므로 런타임 `hnsw.ef_search`는 기본값 40을 우선 사용하고, 실제 운영 데이터의 재현율을 측정한 뒤 조정한다.

## 쿼리 형태

HNSW를 사용하려면 정렬식이 거리 연산자를 직접 사용해야 한다.

```sql
ORDER BY te.embedding <=> (
    SELECT target.embedding
    FROM trouble_embedding target
    WHERE target.trouble_id = :troubleId
)
LIMIT :limit
```

기존 `CROSS JOIN` 형태는 같은 결과를 반환하지만 PostgreSQL 16과 pgvector 0.8.3 환경에서 대상 벡터가 인덱스 정렬값으로 계획되지 않아 순차 검색과 정렬이 발생했다. 기준 벡터를 스칼라 서브쿼리로 조회하면 `InitPlan` 결과가 상수처럼 사용되어 HNSW 인덱스 스캔이 가능하다.

최소 유사도는 SQL의 거리 조건으로 반복하지 않고, 정렬된 Top K 결과를 서비스에서 필터링한다. 동일한 기준 벡터 서브쿼리를 `WHERE`와 `ORDER BY`에 각각 쓰면 PostgreSQL이 서로 다른 실행 매개변수로 계획하여 HNSW 정렬을 사용하지 못하기 때문이다. Top K는 이미 유사도 내림차순이므로 임계값 미만 결과를 제거해도 반환 결과는 동일하다.

## 측정 결과

측정 환경:

- PostgreSQL 16
- pgvector 0.8.3
- 1,536차원 합성 벡터 2,000개
- Top 10 코사인 거리 검색
- `EXPLAIN (ANALYZE, BUFFERS)` 사용
- 운영 테이블을 변경하지 않는 임시 테이블에서 측정

| 방식 | 주요 실행 계획 | 실행 시간 |
|---|---|---:|
| 기존 `CROSS JOIN` | `Seq Scan` + `top-N heapsort` | 5.847ms |
| 스칼라 서브쿼리 + HNSW | `Index Scan using ..._hnsw_idx` | 0.159ms |

이 측정에서는 약 37배 빨랐고, 동일 데이터의 정확 검색 결과와 비교한 `Recall@10`은 90%였다. 합성 데이터와 로컬 환경에서 얻은 값이므로 운영 성능을 보장하는 수치는 아니며, 실제 데이터가 쌓이면 동일한 실행 계획과 재현율을 다시 측정해야 한다.

## 운영 확인

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT te.trouble_id
FROM trouble_embedding te
WHERE te.trouble_id <> :troubleId
ORDER BY te.embedding <=> (
    SELECT target.embedding
    FROM trouble_embedding target
    WHERE target.trouble_id = :troubleId
)
LIMIT 10;
```

실행 계획에 `idx_trouble_embedding_embedding_hnsw_cosine`을 사용하는 `Index Scan`이 나타나는지 확인한다. 조건 필터가 강하면 HNSW가 후보를 찾은 뒤 필터링하므로 결과 수가 부족할 수 있다. 그런 상황이 실제로 관찰될 때만 pgvector 0.8 이상의 iterative scan 또는 `hnsw.ef_search` 상향을 적용한다.
