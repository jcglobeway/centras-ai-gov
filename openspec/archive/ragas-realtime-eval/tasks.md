# Tasks: ragas-realtime-eval

## Phase 1 — DB 마이그레이션

- [x] P1-1: `V037__add_organization_id_to_ragas_evaluations.sql` 작성
  - `ALTER TABLE ragas_evaluations ADD COLUMN organization_id TEXT`
  - `CREATE INDEX idx_ragas_eval_org ON ragas_evaluations (organization_id)`
  - `CREATE INDEX idx_ragas_eval_org_date ON ragas_evaluations (organization_id, evaluated_at)`
- [x] P1-2: H2 flyway.target 확인 — "29" 유지, V037은 PostgreSQL에서만 동작하도록
  조건 없이 DDL 작성 (H2 호환 구문 검증)

## Phase 2 — 집계 API (백엔드)

- [x] P2-1: `evaluation/domain/RagasEvaluationPeriodSummary.kt` 신규
  - `RagasEvaluationPeriodSummary` data class
    (avgFaithfulness, avgAnswerRelevancy, avgContextPrecision, avgContextRecall,
    count, from, to: LocalDate)
- [x] P2-2: `evaluation/domain/RagasEvaluationSummary.kt`에 `organizationId: String?`
  필드 추가
- [x] P2-3: `application/port/in/GetRagasEvaluationSummaryUseCase.kt` 신규
  - `GetRagasEvaluationSummaryQuery(organizationId, fromDate, toDate)` data class
  - `GetRagasEvaluationSummaryUseCase` 인터페이스
- [x] P2-4: `application/port/out/LoadRagasEvaluationSummaryPort.kt` 신규
  - `loadSummary(organizationId: String?, from: LocalDate, to: LocalDate): RagasEvaluationPeriodSummary`
- [x] P2-5: `adapter/outbound/persistence/LoadRagasEvaluationSummaryPortAdapter.kt` 신규
  - JDBC native query로 AVG 집계
  - organization_id 필터 선택적 적용
- [x] P2-6: `application/service/GetRagasEvaluationSummaryService.kt` 신규
  - from/to 기본값: 최근 7일
  - previous 기간: current 기간 길이와 동일한 직전 기간 자동 계산
- [x] P2-7: `adapter/inbound/web/RagasEvaluationController.kt`에
  `@GetMapping("/summary")` 추가
  - Query params: `organization_id`, `from_date`, `to_date` (모두 optional)
  - Response: `RagasEvaluationSummaryResponse(current, previous, generatedAt)`
- [x] P2-8: `RagasEvaluationEntity`에 `organizationId` 컬럼 추가
- [x] P2-9: `SaveRagasEvaluationPortAdapter`에서 `organizationId` 저장 반영
- [x] P2-10: `RecordRagasEvaluationCommand`에 `organizationId: String?` 추가
- [x] P2-11: `RagasEvaluationService`에서 `organizationId` 반영
- [x] P2-12: `config/RepositoryConfiguration.kt`에 `LoadRagasEvaluationSummaryPortAdapter` Bean 등록
- [x] P2-13: `config/ServiceConfiguration.kt`에 `GetRagasEvaluationSummaryService` Bean 등록

## Phase 3 — Redis publish (admin-api)

- [x] P3-1: `apps/admin-api/build.gradle.kts`에
  `implementation("org.springframework.boot:spring-boot-starter-data-redis")` 추가
- [x] P3-2: `config/RedisConfiguration.kt` 신규
  - `RedisTemplate<String, String>` Bean 등록
  - `StringRedisSerializer` 설정
  - `@ConditionalOnProperty(name = ["ragas.eval.redis.enabled"], havingValue = "true")` 조건
- [x] P3-3: answer 저장 지점 파악
  - `CreateQuestionService`에서 answer 저장 후 `QuestionAnsweredEvent` 발행
  - `RagasEvalQueuePublisher` 이벤트 핸들러로 Redis publish 분리
- [x] P3-4: answer 저장 완료 직후 Redis publish 추가
  - `RagasEvalQueuePublisher.kt` 신규 (AFTER_COMMIT 이벤트 핸들러)
  - 실패 시 warn 로그만, 트랜잭션 영향 없음
- [x] P3-5: `application.yml`에 Redis 연결 설정 확인
  (`spring.data.redis.host`, `port`)
- [x] P3-6: 테스트 환경(`application-test.yml`)에서 Redis 비활성화
  (`ragas.eval.redis.enabled: false`)

## Phase 4 — 실시간 구독 (eval-runner)

- [x] P4-1: `python/eval-runner/pyproject.toml`에 `redis>=5.0.0` 의존성 추가
- [x] P4-2: `python/eval-runner/src/eval_runner/realtime_eval.py` 신규 작성
  - Redis BRPOP 루프 (timeout=5)
  - questionId로 admin-api `GET /admin/questions?question_id={id}` 호출해 answer 조회
  - 기존 `ragas_batch.py`의 RAGAS 계산 함수 import/재사용
  - `POST /admin/ragas-evaluations` 호출 — `organizationId` 포함
  - 에러 시 `ragas:eval:dlq` LPUSH 후 continue
- [x] P4-3: `pyproject.toml` scripts에 `realtime-eval = "eval_runner.realtime_eval:main"` 추가
- [x] P4-4: `.env` 예시에 `REDIS_URL=redis://localhost:6379` 항목 추가 (실제 .env는 gitignore)
- [x] P4-5: `RecordRagasEvaluationRequest`에 `organizationId: String?` 필드 추가
  - 백엔드 `RecordRagasEvaluationCommand` 연동 (P2-10과 연결)

## Phase 5 — 프론트엔드

- [x] P5-1: `frontend/src/lib/types.ts`에 `RagasEvaluationSummaryResponse` 타입 추가
- [x] P5-2: `quality-summary/page.tsx` 수정
  - `useSWR<PagedResponse<RagasEvaluation>>("/api/admin/ragas-evaluations?page_size=2")`
    제거
  - `useSWR<RagasEvaluationSummaryResponse>("/api/admin/ragas-evaluations/summary?...")` 추가
  - `current.avg*` 필드로 KPI 카드 (faithfulness, hallucinationRate)
  - `previous.avg*` 로 ScoreBar의 prev 값 계산
  - `current.count` 표시 (평가 건수)
  - 레이더 차트 데이터 `current.avg*` 기반으로 재구성
- [x] P5-3: `PagedResponse<RagasEvaluation>` import 및 `latestRagas`, `prevRagas`
  변수 제거 정리

## 검증

- [x] V-1: `./gradlew :apps:admin-api:test` 50개 테스트 통과 확인
- [ ] V-2: PostgreSQL에서 V037 마이그레이션 적용 확인
- [ ] V-3: `GET /admin/ragas-evaluations/summary` 수동 호출 응답 검증
- [ ] V-4: `realtime-eval` 데몬 실행 후 Redis 큐에 메시지 발행 → 자동 평가 레코드 생성 확인
- [ ] V-5: `quality-summary` 페이지에서 집계 수치 렌더링 확인
