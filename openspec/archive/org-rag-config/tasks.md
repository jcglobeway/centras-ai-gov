# Tasks: org-rag-config

## Phase 1 — 백엔드 DB·도메인 레이어

- [x] V040 마이그레이션 작성: `org_rag_configs` 테이블 생성
- [x] V041 마이그레이션 작성: `org_rag_config_versions` 테이블 생성
- [x] `domain/RagConfig.kt` 작성: `RagConfig`, `RagConfigSummary`, `RagConfigVersion` data class
- [x] `domain/RagConfigScope.kt` 작성 (OrganizationScope 패턴 준용)

## Phase 2 — 백엔드 포트·서비스 레이어

- [x] `application/port/in/GetRagConfigUseCase.kt` 작성
- [x] `application/port/in/SaveRagConfigUseCase.kt` 작성 (저장 + 롤백 커맨드 포함)
- [x] `application/port/out/LoadRagConfigPort.kt` 작성
- [x] `application/port/out/RecordRagConfigPort.kt` 작성
- [x] `application/service/GetRagConfigService.kt` 구현
- [x] `application/service/SaveRagConfigService.kt` 구현 (버전 이력 기록 포함)

## Phase 3 — 백엔드 어댑터 레이어

- [x] `adapter/outbound/persistence/RagConfigEntity.kt` 작성 (open class)
- [x] `adapter/outbound/persistence/RagConfigVersionEntity.kt` 작성 (open class)
- [x] `adapter/outbound/persistence/JpaRagConfigRepository.kt` 작성
- [x] `adapter/outbound/persistence/JpaRagConfigVersionRepository.kt` 작성
- [x] `adapter/outbound/persistence/LoadRagConfigPortAdapter.kt` 구현 (open class)
- [x] `adapter/outbound/persistence/RecordRagConfigPortAdapter.kt` 구현 (open class)
- [x] `RepositoryConfiguration.kt`에 신규 어댑터 @Bean 등록
- [x] `ServiceConfiguration.kt`에 신규 서비스 @Bean 등록

## Phase 4 — 백엔드 Controller

- [x] `adapter/inbound/web/RagConfigController.kt` 작성
  - `GET /admin/organizations/{orgId}/rag-config`
  - `PUT /admin/organizations/{orgId}/rag-config`
  - `GET /admin/organizations/{orgId}/rag-config/versions`
  - `POST /admin/organizations/{orgId}/rag-config/rollback/{version}`

## Phase 5 — RAG Orchestrator 연동

- [x] `python/rag-orchestrator/src/rag_orchestrator/config_client.py` 신설
  - `fetch_rag_config(org_id, admin_api_url)` 함수 구현
  - TTL 60초 인메모리 캐시 적용
  - Admin API 호출 실패 시 `DEFAULT_RAG_CONFIG` fallback 반환
- [x] `app.py` `/generate` 엔드포인트: `organization_id`로 config 조회 후 systemPrompt·model·topK 등 동적 적용
- [x] `app.py` `/generate/stream` 엔드포인트 동일하게 수정

## Phase 6 — 프론트엔드 `/ops/prompt`

- [x] 페이지 마운트 시 실제 API 데이터 로드 (useSWR)
- [x] 저장 버튼 활성화 및 PUT 호출 구현
- [x] 버전 이력 테이블: GET /versions 실제 데이터 연동
- [x] 롤백 버튼 활성화 및 POST rollback 호출 구현

## Phase 7 — 프론트엔드 `/ops/rag-params`

- [x] 페이지 마운트 시 현재 설정 조회
- [x] threshold, topK, reranker 초기값을 API 응답으로 설정
- [x] 저장 버튼 활성화 및 PUT 호출 구현

## Phase 8 — 테스트 환경

- [x] Testcontainers(pgvector/pgvector:pg16)로 전환 → V031 ON CONFLICT H2 비호환 이슈 해소
- [x] `flyway.target: "38"` 적용 후 전체 테스트 통과

## 미구현 (범위 제외)

- `/ops/model-serving` 실제 데이터 연동 → `enhance-anomaly-detection` 또는 별도 change에서 처리
- RagConfigApiTest 별도 테스트 미작성 (통합 테스트에서 간접 검증)
