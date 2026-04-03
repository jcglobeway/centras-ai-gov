# Proposal: org-rag-config

## Change ID

`org-rag-config`

## Summary

- **목적**: 기관별 RAG 설정(시스템 프롬프트, RAG 파라미터, 모델 설정)을 DB에 저장하고 API로 관리한다.
- **범위**: 백엔드 신규 테이블·API + RAG Orchestrator 연동 + 프론트엔드 3페이지 실제 동작
- **제외 범위**: 프롬프트 A/B 테스트, 기관별 임베딩 모델 선택, 실시간 GPU 메트릭 수집, Rate Limit 실시간 모니터링, 시뮬레이션 룸 연동

## Problem

현재 `/ops/prompt`, `/ops/rag-params`, `/ops/model-serving` 세 페이지는 모두 목업 데이터다.
저장·롤백 버튼이 비활성화되어 있어 실제 설정 변경이 불가능하다.

RAG Orchestrator(`python/rag-orchestrator/app.py`)는 시스템 프롬프트와 RAG 파라미터를
환경변수(`OLLAMA_MODEL`, `HYBRID_SEARCH_TOP_K`, `RERANKER_ENABLED`)와 하드코딩 값으로 제어한다.
기관마다 다른 설정을 적용할 수 없으며, 운영 중 변경하려면 서버를 재시작해야 한다.

구체적인 하드코딩 현황:
- `system_prompt`: `app.py` 내 문자열 리터럴 (2곳 — `/generate`, `/generate/stream`)
- `temperature`: `0.3` 하드코딩
- `num_predict`: `500` 하드코딩
- `top_k`: `HYBRID_SEARCH_TOP_K` env (기관 구분 없음)
- `reranker_enabled`: `RERANKER_ENABLED` env (기관 구분 없음)
- `model`: `OLLAMA_MODEL` env (기관 구분 없음)

## Proposed Solution

### 1. 백엔드 — 기관별 RAG 설정 저장소

**Flyway V038**: `org_rag_configs` 테이블 신설

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | VARCHAR PK | `rag_cfg_` + UUID 8자 |
| organization_id | VARCHAR FK → organizations | 기관 ID |
| system_prompt | TEXT | 시스템 프롬프트 전문 |
| tone | VARCHAR(20) | formal / friendly / neutral |
| top_k | INTEGER | 최대 검색 청크 수 (1~20) |
| similarity_threshold | NUMERIC(4,3) | 유사도 임계값 (0.0~1.0) |
| reranker_enabled | BOOLEAN | Reranker 활성화 여부 |
| llm_model | VARCHAR(100) | 사용 모델명 (예: qwen2.5:7b) |
| llm_temperature | NUMERIC(3,2) | LLM temperature (0.0~1.0) |
| llm_max_tokens | INTEGER | 최대 출력 토큰 수 |
| version | INTEGER | 현재 버전 번호 (1~) |
| created_at | TIMESTAMPTZ | 생성일 |
| updated_at | TIMESTAMPTZ | 최종 수정일 |

**Flyway V039**: `org_rag_config_versions` 테이블 신설 (버전 이력)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | VARCHAR PK | |
| organization_id | VARCHAR | 기관 ID |
| version | INTEGER | 버전 번호 |
| system_prompt | TEXT | 저장 시점 프롬프트 |
| tone | VARCHAR(20) | |
| top_k | INTEGER | |
| similarity_threshold | NUMERIC(4,3) | |
| reranker_enabled | BOOLEAN | |
| llm_model | VARCHAR(100) | |
| llm_temperature | NUMERIC(3,2) | |
| llm_max_tokens | INTEGER | |
| change_note | VARCHAR(500) | 변경 메모 |
| changed_by | VARCHAR | admin_user_id |
| created_at | TIMESTAMPTZ | |

**API 엔드포인트** (`/admin/organizations/{orgId}/rag-config`):

| Method | Path | 설명 |
|--------|------|------|
| GET | `/admin/organizations/{orgId}/rag-config` | 현재 RAG 설정 조회 |
| PUT | `/admin/organizations/{orgId}/rag-config` | RAG 설정 저장 (버전 증가) |
| GET | `/admin/organizations/{orgId}/rag-config/versions` | 버전 이력 목록 |
| POST | `/admin/organizations/{orgId}/rag-config/rollback/{version}` | 특정 버전으로 롤백 |

**GET /admin/model-serving/status**: RAG Orchestrator health + 연결된 모델 목록 반환

**헥사고날 레이어**: `organization-directory` 모듈에 `ragconfig` 서브 패키지 추가
- `domain/RagConfig.kt`
- `application/port/in/GetRagConfigUseCase.kt`, `SaveRagConfigUseCase.kt`
- `application/port/out/LoadRagConfigPort.kt`, `RecordRagConfigPort.kt`
- `application/service/GetRagConfigService.kt`, `SaveRagConfigService.kt`
- `adapter/outbound/persistence/RagConfigEntity.kt`, `JpaRagConfigRepository.kt`, `LoadRagConfigPortAdapter.kt`, `RecordRagConfigPortAdapter.kt`
- `adapter/inbound/web/RagConfigController.kt` (admin-api)

### 2. RAG Orchestrator — 기관별 설정 동적 조회

`/generate` 및 `/generate/stream` 엔드포인트 호출 시:
1. `organization_id`를 키로 Admin API `GET /admin/organizations/{orgId}/rag-config` 호출
2. 반환된 설정으로 `system_prompt`, `top_k`, `similarity_threshold`, `reranker_enabled`, `llm_model`, `llm_temperature`, `llm_max_tokens` 적용
3. Admin API 호출 실패 시 env 변수 기반 fallback 값 사용

캐싱 전략: 프로세스 내 딕셔너리에 TTL 60초 캐싱 (간단한 in-memory).
org당 1분에 1회 Admin API 호출로 제한.

### 3. 프론트엔드 — 3페이지 실제 연동

**`/ops/prompt`**:
- 페이지 마운트 시 `GET /api/admin/organizations/{orgId}/rag-config` 조회
- 저장 버튼: `PUT /api/admin/organizations/{orgId}/rag-config` 호출 후 성공 토스트
- 버전 이력 테이블: `GET /api/admin/organizations/{orgId}/rag-config/versions`로 실제 데이터
- 롤백 버튼: `POST /api/admin/organizations/{orgId}/rag-config/rollback/{version}` 호출

**`/ops/rag-params`**:
- 페이지 마운트 시 현재 설정 조회 (동일 엔드포인트)
- 저장 버튼: PUT 호출
- "현재 설정" 패널에 실제 값 표시 (목업 0.75/5 제거)
- "시뮬레이션" 패널은 이번 change 제외 (Out of Scope)

**`/ops/model-serving`**:
- LLM API 연동 상태: `GET /api/admin/model-serving/status` 조회
- 인스턴스 리소스(CPU/MEMORY/GPU): 이번 change 제외 — "준비 중" 안내 문구로 교체
- Rate Limit: 이번 change 제외 — "준비 중" 안내 문구로 교체
- MockBadge 제거 (실제 데이터 표시 시)

## Impact

- **영향 모듈**: `organization-directory` (신규 ragconfig 레이어), `admin-api` (신규 Controller + Config), `python/rag-orchestrator`
- **영향 API**: 신규 5개 엔드포인트
- **영향 테스트**: 신규 통합 테스트 추가 필요 (기존 50개 유지)
- **DB 마이그레이션**: V038 (org_rag_configs), V039 (org_rag_config_versions)

## Done Definition

- `GET /admin/organizations/{orgId}/rag-config` → 기관별 설정 반환
- `PUT /admin/organizations/{orgId}/rag-config` → 저장 후 version 증가
- `GET /admin/organizations/{orgId}/rag-config/versions` → 이력 목록 반환
- `POST /admin/organizations/{orgId}/rag-config/rollback/{version}` → 롤백 후 현재 버전으로 복원
- RAG Orchestrator가 기관별 system_prompt, top_k, similarity_threshold, reranker_enabled, llm_model을 동적으로 적용
- `/ops/prompt` 저장·롤백 실제 동작
- `/ops/rag-params` 저장 실제 동작
- `/ops/model-serving` LLM 연결 상태 실제 표시
- 기존 Spring Boot 테스트 50개 전부 통과
- 신규 통합 테스트 최소 5개 추가
- ArchUnit 8개 규칙 전부 통과
