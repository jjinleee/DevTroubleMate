# 환경별 설정과 Secret 관리

DevTroubleMate는 `local`, `test`, `prod` 프로필을 분리하고 비밀번호와 API 키를 환경변수로만 주입한다. Git에 추적되는 설정 파일에는 실제 Secret을 저장하지 않는다.

## 설정 파일

| 파일 | 용도 |
|---|---|
| `application.yml` | 모든 환경의 공통 Flyway, JPA 검증, AI 모델 설정 |
| `application-local.yml` | 로컬 PostgreSQL 연결과 개발 로그 |
| `application-test.yml` | 자동 테스트 DB 연결과 테스트용 OpenAI 키 |
| `application-prod.yml` | 운영 DB 연결, 커넥션 풀, 운영 로그 |

프로필을 지정하지 않으면 `local`이 사용된다. Gradle `test` 작업은 자동으로 `test` 프로필을 사용한다.

## 로컬 실행

기존 Docker 볼륨을 계속 사용하려면 해당 PostgreSQL을 만들 때 사용한 비밀번호를 `DB_PASSWORD`에 입력해야 한다.

```bash
export DB_PASSWORD='your-local-database-password'
export OPENAI_API_KEY='your-openai-api-key'
docker compose up -d
./gradlew bootRun
```

IntelliJ에서는 Spring Boot 실행 구성의 Environment variables에 다음 이름을 등록한다.

```text
DB_PASSWORD
OPENAI_API_KEY
```

호스트, 포트, DB 이름과 사용자가 기본값과 다를 때만 `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`을 추가한다.

Docker Compose는 저장소의 `.env.example`을 `.env`로 복사해 사용할 수 있다. `.env`는 Git에서 무시된다. Spring Boot는 `.env`를 자동으로 읽지 않으므로 IntelliJ 또는 셸 환경에도 필요한 값을 설정해야 한다.

## 테스트

테스트 프로필은 실제 OpenAI 호출을 하지 않도록 가짜 키를 사용한다. PostgreSQL 비밀번호는 환경변수로 전달한다.

```bash
DB_PASSWORD='your-local-database-password' ./gradlew test
```

별도 테스트 DB를 사용할 때는 `TEST_DB_HOST`, `TEST_DB_PORT`, `TEST_DB_NAME`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD`를 설정한다. 지정하지 않은 테스트 DB 변수는 동일한 이름의 로컬 DB 변수와 기본 연결 정보를 사용한다.

## 운영 실행

운영 프로필은 필수 Secret에 기본값을 제공하지 않는다. 하나라도 없으면 애플리케이션 시작이 실패한다.

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL='jdbc:postgresql://database-host:5432/devtroublemate'
export DB_USERNAME='application-user'
export DB_PASSWORD='database-password-from-secret-manager'
export OPENAI_API_KEY='openai-key-from-secret-manager'
java -jar devtroublemate.jar
```

운영 값은 배포 플랫폼의 Secret Manager 또는 암호화된 환경변수에 저장한다. PR, GitHub Issue, 로그, Docker 이미지, `application-prod.yml`에는 실제 값을 기록하지 않는다.

운영 환경에서는 SQL 출력과 Hibernate 포맷 로그를 비활성화하고 Flyway baseline 자동 생성을 허용하지 않는다. 운영 DB에는 검토된 마이그레이션 이력이 있어야 한다.

## 커밋 전 점검

```bash
git grep -n -E '(password|api[-_]?key|secret|token)'
git status --short
```

검색 결과에는 환경변수 이름, 예제 placeholder와 테스트용 값만 있어야 한다. 실제 값이 보이면 커밋하지 말고 즉시 해당 키를 폐기·재발급한다.
