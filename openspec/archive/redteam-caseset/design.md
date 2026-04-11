# Design: redteam-caseset

## Architecture

qa-review 모듈의 헥사고날 패키지 구조를 그대로 따른다.
신규 Gradle 모듈 `modules/redteam/`을 생성하고, admin-api에서 의존성을 추가한다.
배치 실행은 동기 방식으로 처리한다 (케이스 수가 적고 실시간 SSE는 Out of Scope).

## Components

### 신규: modules/redteam/

```
modules/redteam/
  src/main/kotlin/com/publicplatform/ragops/redteam/
    RedteamModule.kt

    domain/
      RedteamCase.kt            # 도메인 모델 (data class)
      RedteamBatchRun.kt
      RedteamCaseResult.kt
      RedteamJudge.kt           # rule-based 판정 로직 (순수 함수)

    application/
      port/
        in/
          ManageRedteamCaseUseCase.kt   # 케이스 CRUD
          RunRedteamBatchUseCase.kt     # 일괄 실행
          ListRedteamBatchRunsUseCase.kt
        out/
          LoadRedteamCasePort.kt
          SaveRedteamCasePort.kt
          DeleteRedteamCasePort.kt
          LoadRedteamBatchRunPort.kt
          SaveRedteamBatchRunPort.kt
          SaveRedteamCaseResultPort.kt
      service/
          ManageRedteamCaseService.kt
          RunRedteamBatchService.kt
          ListRedteamBatchRunsService.kt

    adapter/
      outbound/
        persistence/
          JpaRedteamCaseRepository.kt
          JpaRedteamBatchRunRepository.kt
          JpaRedteamCaseResultRepository.kt
          RedteamCaseEntity.kt
          RedteamBatchRunEntity.kt
          RedteamCaseResultEntity.kt
          RedteamCasePortAdapter.kt       # Load + Save + Delete 통합
          RedteamBatchRunPortAdapter.kt   # Load + Save 통합
          RedteamCaseResultPortAdapter.kt # Save 전용
```

### 수정: apps/admin-api/

```
apps/admin-api/
  src/main/kotlin/.../adminapi/
    redteam/
      adapter/inbound/web/
        RedteamCaseController.kt
        RedteamBatchRunController.kt
    config/
      RepositoryConfiguration.kt    # RedteamCase/BatchRun/Result 어댑터 Bean 추가
      ServiceConfiguration.kt       # Redteam Service Bean 추가
  src/main/resources/db/migration/
    V050__create_redteam_cases.sql
    V051__create_redteam_batch_runs.sql
  build.gradle.kts                  # implementation(project(":modules:redteam")) 추가
```

### 수정: frontend/

```
frontend/src/app/ops/redteam/
  page.tsx                          # 목업 제거, API 훅 연결
  _components/
    CaseFormModal.tsx               # 케이스 등록/수정 모달
    BatchRunResultModal.tsx         # 배치런 상세 결과 모달
```

## Data Flow

### 케이스 등록 흐름

```
RedteamCaseController
  → ManageRedteamCaseUseCase.createCase(command)
  → ManageRedteamCaseService
  → SaveRedteamCasePort
  → RedteamCasePortAdapter
  → JpaRedteamCaseRepository.save(entity)
```

### 일괄 실행 흐름

```
RedteamBatchRunController (POST /admin/redteam/batch-runs)
  → RunRedteamBatchUseCase.runBatch(organizationId, triggeredBy)
  → RunRedteamBatchService
    1. isActive=true 케이스 조회 (LoadRedteamCasePort)
    2. BatchRun 생성 (status=running, SaveRedteamBatchRunPort)
    3. 케이스별 루프:
       a. RagOrchestrationPort.generateAnswer(queryText, organizationId)
       b. RedteamJudge.judge(case, response) → RedteamJudgment
       c. RedteamCaseResult 저장 (SaveRedteamCaseResultPort)
    4. passCount/failCount 집계 → BatchRun 갱신 (status=completed)
  → BatchRun 요약 반환
```

### RedteamJudge 판정 (순수 함수, domain 레이어)

```kotlin
object RedteamJudge {
    fun judge(case: RedteamCase, response: RagResponse): RedteamJudgment
}
```

외부 의존 없이 도메인 레이어에서 완결. 테스트 용이성 극대화.

## DB Schema

### V050: redteam_cases

```sql
CREATE TABLE redteam_cases (
    id               VARCHAR(32)  PRIMARY KEY,
    organization_id  VARCHAR(32)  NOT NULL,
    category         VARCHAR(32)  NOT NULL,  -- pii_induction / out_of_domain / ...
    title            VARCHAR(200) NOT NULL,
    query_text       TEXT         NOT NULL,
    expected_behavior VARCHAR(16) NOT NULL,  -- defend / detect
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by       VARCHAR(64)  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

### V051: redteam_batch_runs + redteam_case_results

```sql
CREATE TABLE redteam_batch_runs (
    id               VARCHAR(32)  PRIMARY KEY,
    organization_id  VARCHAR(32)  NOT NULL,
    triggered_by     VARCHAR(64)  NOT NULL,
    status           VARCHAR(16)  NOT NULL DEFAULT 'running',
    total_cases      INTEGER      NOT NULL DEFAULT 0,
    pass_count       INTEGER      NOT NULL DEFAULT 0,
    fail_count       INTEGER      NOT NULL DEFAULT 0,
    pass_rate        NUMERIC(5,2) NOT NULL DEFAULT 0.0,
    started_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at     TIMESTAMPTZ
);

CREATE TABLE redteam_case_results (
    id               VARCHAR(32)  PRIMARY KEY,
    batch_run_id     VARCHAR(32)  NOT NULL REFERENCES redteam_batch_runs(id),
    case_id          VARCHAR(32)  NOT NULL,  -- 참조만, FK 없음 (케이스 삭제 허용)
    query_text       TEXT         NOT NULL,
    response_text    TEXT,
    answer_status    VARCHAR(32),
    judgment         VARCHAR(8)   NOT NULL,  -- pass / fail
    judgment_detail  TEXT,
    executed_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

> case_id에 FK를 걸지 않는 이유: 케이스 삭제 후에도 과거 실행 결과를 보존해야 함.
> 실행 시점의 query_text 스냅샷을 저장하므로 케이스가 삭제되어도 결과는 유효.

## Considerations

### 동기 실행 vs 비동기 큐

케이스 수가 통상 10~50개 수준이고, RAG orchestrator 응답 시간이 케이스당 2~5초라 가정하면
전체 실행 50케이스 기준 최대 250초. 이는 HTTP 타임아웃 범위 내에 들어온다.
비동기 큐(Redis) 도입은 케이스가 100개를 초과하는 시점에 재검토한다.

### RagOrchestrationPort 재사용

기존 `chatruntime` 모듈의 `RagOrchestrationPort`를 직접 의존하면 모듈 간 순환 의존이 발생한다.
대신 RunRedteamBatchService가 `RagOrchestrationPort` 인터페이스를 직접 파라미터로 받는다.
ServiceConfiguration에서 기존 `ragOrchestrationPort` Bean을 주입한다.

### ArchUnit 준수

- `RedteamCasePortAdapter`는 `open class` (CGLIB 프록시 요건)
- Controller는 UseCase 인터페이스만 참조 (JPA Entity/Repository 직접 참조 금지)
- domain 레이어는 JPA/Spring 어노테이션 금지
- 새 Bean은 모두 `RepositoryConfiguration` / `ServiceConfiguration`에 명시 등록

### 프론트엔드 API 연동

기존 `/api/admin/*` → `localhost:8080/admin/*` rewrite 설정 재사용.
CaseFormModal: category 셀렉트(4종) + title/queryText 텍스트 입력 + expectedBehavior 라디오.
배치 실행 버튼 클릭 시 로딩 스피너 표시 → 완료 후 이력 목록 갱신.
