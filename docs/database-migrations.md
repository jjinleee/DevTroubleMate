# 데이터베이스 마이그레이션

DevTroubleMate는 Flyway가 PostgreSQL 스키마를 관리하고 Hibernate는 엔티티와 스키마의 일치 여부만 검증한다.

## 실행 방식

- 빈 DB에서는 `V1__create_initial_schema.sql`부터 순서대로 실행한다.
- 테이블은 있지만 Flyway 이력이 없는 기존 DB는 버전 1로 baseline한 뒤 V2부터 실행한다.
- 실행 이력과 체크섬은 `flyway_schema_history`에 저장된다.
- 이미 운영 환경에 적용된 마이그레이션 파일은 수정하지 않고 새 버전 파일을 추가한다.
- `schema.sql`은 더 이상 실행하지 않는다.

## 기존 데이터 백필

V2는 기존 장애의 AI 처리 상태가 비어 있을 때만 다음 규칙으로 채운다.

| 기존 데이터 | 처리 상태 | 후속 처리 |
|---|---|---|
| AI 분석과 임베딩 모두 존재 | `COMPLETED` | 없음 |
| 둘 중 하나만 존재 | `FAILED` | 재시도 가능 |
| 둘 다 없음 | `PENDING` | 최초 처리 대상 |

둘 중 하나만 존재하는 장애에는 `MIGRATION_INCOMPLETE_AI_DATA` 오류 코드를 기록한다. 이미 상태가 있는 행은 변경하지 않으므로 V2를 다시 검토하거나 다른 환경에 적용해도 기존 처리 결과를 덮어쓰지 않는다.

## 새 마이그레이션 추가

`src/main/resources/db/migration`에 다음 번호의 SQL 파일을 추가한다.

```text
V3__description.sql
V4__description.sql
```

버전과 설명 사이에는 밑줄 두 개를 사용한다. 병합되거나 배포된 파일의 내용 또는 버전 번호를 바꾸면 Flyway 체크섬 검증이 실패하므로 수정하지 않는다.

## 확인

```sql
SELECT installed_rank, version, description, type, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

애플리케이션 시작 시 Flyway가 먼저 마이그레이션을 수행하고, 이후 Hibernate `ddl-auto: validate`가 엔티티 매핑과 실제 컬럼 타입을 확인한다. 둘 중 하나라도 실패하면 애플리케이션 시작을 중단해 잘못된 스키마로 API가 실행되는 것을 방지한다.

## 도입 시 검증 결과

PostgreSQL 16과 pgvector 0.8.3 환경에서 다음 두 경로를 별도 데이터베이스로 검증했다.

- 빈 DB: V1과 V2가 순서대로 실행되고 Hibernate 검증 통과
- 기존 DB 복제본: V1 baseline 후 V2 실행 및 Hibernate 검증 통과
- 기존 장애 6개: AI 분석과 임베딩을 확인해 모두 `COMPLETED`로 백필
- 기존 trouble, AI 분석, 임베딩, 태그, 연결 데이터 개수 유지
- 전체 자동 테스트 67개 통과

검증용 데이터베이스는 실제 개발 DB와 분리해 생성했으며 확인 후 제거했다.
