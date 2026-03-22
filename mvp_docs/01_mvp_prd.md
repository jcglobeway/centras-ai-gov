# MVP PRD — 공공기관 RAG 챗봇 운영 플랫폼

> centras-ai-gov · v2.0 · 2026-03-20

---

## 1. 제품 정의

공공기관용 멀티테넌트 RAG 챗봇을 **답변 서비스** 수준에서 끝내지 않고,
**운영 → 품질 개선 → 문서 최신화 → 성과 확인** 까지 연결하는 운영 플랫폼이다.

Spring Boot(Kotlin) 기반 모듈러 모노리스가 모든 운영 상태의 System of Record 역할을 하며,
Python 워커(FastAPI, Typer CLI)가 RAG 오케스트레이션과 문서 수집을 담당하는 하이브리드 아키텍처다.

---

## 2. MVP 목표

- 시민 질문에 대해 근거 기반 답변을 제공한다.
- 운영사·고객사·QA 담당자가 각자 필요한 운영 액션을 수행할 수 있다.
- 질문 로그·검색 로그·검수 로그를 기반으로 품질 개선 루프를 운영한다.
- LLM 호출 비용·토큰·지연시간을 실시간으로 추적해 운영 비용을 관리한다.
- RAGAS 자동 평가로 RAG 품질을 정량화하고 개선 사이클을 단축한다.

---

## 3. 사용자 (Personas)

| 역할 | 코드 | 접근 범위 | 핵심 액션 |
|---|---|---|---|
| 운영 관리자 | `ops_admin` | 전체 기관 | 시스템 상태 통합 모니터링, 수집 작업 실행, RAGAS 평가 결과 확인 |
| 기관 관리자 | `client_admin` | 자기 기관 | 성과 지표 확인, 미해결 질문 확인, 개선 포인트 파악 |
| QA 담당자 | `qa_admin` | 할당된 기관 | 미응답·오답 의심 검수, QA 리뷰 작성, 개선 작업 연결 |
| 기관 뷰어 | `client_org_viewer` | 자기 기관(읽기 전용) | 대시보드 및 지표 조회 |
| 지식 편집자 | `knowledge_editor` | 할당된 기관 | 문서 등록·수정, 수집 소스 관리 |
| QA 매니저 | `qa_manager` | 전체 QA 범위 | QA 팀 배정 관리, 검수 승인 |

---

## 4. MVP 범위

### In Scope (완료)

- **인증/세션**: 로그인·로그아웃·세션 복원·만료 처리, bcrypt 인증, 역할 기반 액션 권한
- **멀티테넌트**: 기관(organization) + 서비스(service) 이중 스코프, 전체 테이블 `org_id` 분리
- **시민 질문**: 질문 생성 → RAG 파이프라인 → 답변 자동 저장
- **Spring AI 네이티브 RAG**: OpenAI(GPT-5-latest) 또는 Ollama 선택적 사용, SSE 스트리밍 지원
- **LLM 메트릭**: 토큰(input/output/total), 비용(USD 추정), 모델명, finish_reason DB 저장
- **미해결 큐**: fallback·no_answer·confirmed_issue 상태의 질문 자동 노출
- **QA 리뷰**: pending → confirmed_issue → resolved 상태 머신, false_alarm 분기
- **피드백**: 시민 만족도(좋아요/싫어요) 및 세부 이유 수집
- **문서 수집**: Playwright 크롤링 → 파싱 → 청킹 → bge-m3 임베딩 → pgvector 저장
- **벡터 검색**: pgvector cosine similarity, 청크 Top-K 검색, 검색 로그 콜백
- **RAGAS 평가**: faithfulness·answer_relevancy·context_recall·context_precision 자동 채점
- **KPI 대시보드**: 일간 집계 지표 (사용량·해결율·품질·RAG 운영)
- **프론트엔드**: Next.js 15 기반 3개 포털 (Ops/Client/QA), 다크 테마 Control Tower UI
- **감사 로그**: 고위험 액션 자동 기록
- **통합 에러 처리**: GlobalExceptionHandler, request_id/trace_id 헤더

### Out of Scope

- 고급 승인 워크플로우 (문서 발행 전 다중 승인)
- 자동 알림·리포트 스케줄링
- 복잡한 권한 상속 구조 (그룹·팀 계층)
- 고급 retrieval 정책 엔진 (하이브리드 검색, 리랭킹 정책 UI)
- 파인튜닝 파이프라인

---

## 5. 핵심 문제

| 문제 | 현재 해결 방법 |
|---|---|
| 답변 근거·최신성 확인 어려움 | RAG 검색 로그 + 문서 버전 추적 |
| 오답·미응답 체계적 개선 불가 | 미해결 큐 + QA 리뷰 상태 머신 |
| 기관별 성과 분리 조회 불가 | org_id 스코프 + 일간 지표 집계 |
| LLM 운영 비용 파악 불가 | 토큰·비용·모델명 answers 테이블 저장 |
| RAG 품질 정량화 불가 | RAGAS 자동 평가 모듈 |

---

## 6. 핵심 운영 루프

```
시민 질문
  → Spring AI (OpenAI/Ollama) RAG 파이프라인
  → 답변 저장 (토큰·비용·지연시간 포함)
  → 미해결 큐 자동 분류
  → QA 담당자 검수 → 개선 액션 결정
  → 문서 보강 / 수집 재실행 / 재인덱싱
  → RAGAS 재평가 → KPI 추이 확인
```

---

## 7. 시스템 구조

### 레이어

| 레이어 | 기술 | 역할 |
|---|---|---|
| Admin API | Spring Boot 3 + Kotlin, port 8080 | System of Record, 모든 운영 상태 관리 |
| 프론트엔드 | Next.js 15 App Router, TypeScript, Tailwind v3 | Ops/Client/QA 3개 포털 |
| DB | PostgreSQL 15 + pgvector | 단일 진실 원천, 벡터 검색 통합 |
| 캐시/큐 | Redis | 세션 캐시, 트리거 |
| RAG Orchestrator | Python FastAPI, port 8090 | 질의 임베딩, pgvector 검색, 답변 합성 |
| Ingestion Worker | Python Typer CLI | 크롤링→파싱→청킹→임베딩→인덱싱 |
| LLM | OpenAI GPT-5-latest / Ollama qwen2.5 | 답변 생성 |
| 임베딩 | Ollama bge-m3 (1024차원) | 문서·쿼리 임베딩 |

### Gradle 모듈 구조

```
apps/
  admin-api/          # Spring Boot 통합 앱

modules/
  shared-kernel/      # DomainEvent 인터페이스
  identity-access/    # 인증·세션·역할·감사
  organization-directory/  # 기관·서비스 관리
  chat-runtime/       # 질문·답변·피드백·미해결 큐
  document-registry/  # 문서·버전·청크
  ingestion-ops/      # 수집 소스·작업 라이프사이클
  qa-review/          # QA 리뷰 상태 머신
  metrics-reporting/  # KPI 집계·스냅샷

python/
  rag-orchestrator/   # FastAPI: 검색·답변 합성·RAGAS 평가
  ingestion-worker/   # CLI: 크롤링→파싱→청킹→임베딩→인덱싱
  eval-runner/        # RAGAS 평가 실행기

frontend/             # Next.js 15 3개 포털
```

---

## 8. 도메인 모델 & DB 스키마

### Flyway 마이그레이션 현황 (V001 ~ V026)

| 버전 | 테이블/내용 |
|---|---|
| V001-V003 | admin_users, admin_user_roles, admin_sessions, audit_logs |
| V004-V005 | organizations, services |
| V006-V007 | crawl_sources, ingestion_jobs |
| V008 | qa_reviews |
| V009-V012 | chat_sessions, questions, answers, qa_reviews FK |
| V013-V016 | documents, document_versions, daily_metrics_org, document_chunks |
| V017 | rag_search_logs, rag_retrieved_documents |
| V018 | pgvector 확장 (PostgreSQL 전용 Kotlin migration) |
| V019 | feedbacks |
| V020-V023 | 역할 확장 (6개), 질문 모델 확장, 피드백 세부, 지표 확장 |
| V024 | 데모 시드 데이터 |
| V025 | ragas_evaluations |
| V026 | answers 테이블 LLM 메트릭 컬럼 (model_name, provider_name, input_tokens, output_tokens, total_tokens, estimated_cost_usd, finish_reason) |

### 핵심 관계

- 모든 비즈니스 테이블 → `org_id` 직접 컬럼 (조직 스코프 강제)
- `questions` → `answers` (1:1), `qa_reviews` (1:N), `rag_search_logs` (1:N), `feedbacks` (1:N)
- `documents` → `document_versions` (1:N), `document_chunks` (1:N, embedding_vector)

---

## 9. API 계약 (핵심 엔드포인트)

### 인증

| Method | Path | 설명 |
|---|---|---|
| POST | /admin/auth/login | 로그인 |
| POST | /admin/auth/logout | 로그아웃 |
| GET | /admin/auth/me | 세션 복원 |

### Chat Runtime

| Method | Path | 설명 |
|---|---|---|
| POST | /admin/questions | 질문 생성 + RAG 답변 자동 생성 |
| GET | /admin/questions | 질문 목록 (필터: org, service, from, to) |
| GET | /admin/questions/unresolved | 미해결 큐 |
| GET | /admin/questions/stream | SSE 스트리밍 답변 |
| POST | /admin/feedbacks | 시민 피드백 제출 |

### QA 리뷰

| Method | Path | 설명 |
|---|---|---|
| POST | /admin/qa-reviews | QA 리뷰 생성 |
| GET | /admin/qa-reviews | 리뷰 목록 (questionId 필터) |

### 문서 & 수집

| Method | Path | 설명 |
|---|---|---|
| GET | /admin/documents | 문서 목록 |
| GET | /admin/documents/{id}/versions | 버전 이력 |
| POST | /admin/document-chunks | 청크 인덱싱 (Worker 콜백) |
| GET | /admin/crawl-sources | 수집 소스 목록 |
| POST | /admin/crawl-sources | 수집 소스 생성 |
| GET | /admin/ingestion-jobs | 수집 작업 목록 |
| POST | /admin/ingestion-jobs | 수집 작업 생성 |
| POST | /admin/ingestion-jobs/{id}/status | 상태 전이 (Worker 콜백) |

### 평가 & 지표

| Method | Path | 설명 |
|---|---|---|
| POST | /admin/ragas-evaluations | RAGAS 평가 결과 저장 |
| POST | /admin/rag-search-logs | RAG 검색 로그 저장 (Orchestrator 콜백) |
| GET | /admin/metrics/daily | 일간 KPI 집계 |
| GET | /admin/organizations | 기관 목록 |

### 공통 응답 규칙

- 모든 응답: `request_id`, `generated_at` 포함
- 에러 형식: `{ "error": { "code": "...", "message": "...", "request_id": "..." } }`
- 401: 미인증 / 403: 권한 없음 / 404: 스코프 밖 리소스

---

## 10. 핵심 상태 머신

### QA 리뷰 상태

```
pending
  → confirmed_issue (root_cause_code + action_type 필수)
      → resolved (review_comment 필수)
  → false_alarm (action_type = no_action 강제)
      ↛ resolved (금지)
```

### 수집 작업 상태

```
pending → queued → running → succeeded
                            → failed
                            → cancelled
```

### 미해결 큐 가시성 규칙

표시 조건 (OR):
- `answer_status IN ('fallback', 'no_answer', 'error')`
- `answer_status = 'answered'` AND 최신 `qa_review.review_status = 'confirmed_issue'`

제외 조건:
- 최신 리뷰가 `resolved` 또는 `false_alarm`

---

## 11. LLM 메트릭 추적

Spring AI `ChatResponse`에서 추출해 `answers` 테이블에 저장:

| 필드 | 출처 | 설명 |
|---|---|---|
| model_name | `response.metadata.model` | gpt-5-latest, llama3 등 |
| provider_name | 설정값 | openai, ollama |
| input_tokens | `usage.promptTokens` | 입력 토큰 수 |
| output_tokens | `usage.completionTokens` | 출력 토큰 수 |
| total_tokens | `usage.totalTokens` | 합계 |
| estimated_cost_usd | 내부 계산 | 모델별 단가 × 토큰 수 |
| finish_reason | `result.metadata.finishReason` | stop, length, content_filter |
| response_time_ms | 측정값 | 전체 응답 지연시간 |

**모델별 단가 (USD/1M 토큰)**:
- gpt-5-latest: input $1.25 / output $7.50
- gpt-4o: input $2.50 / output $10.00
- gpt-4o-mini: input $0.15 / output $0.60

---

## 12. RAG 품질 KPI

| 지표 | 목표값 | 측정 방법 |
|---|---|---|
| Faithfulness | > 0.90 | RAGAS |
| Answer Relevancy | > 0.85 | RAGAS |
| Context Recall | > 0.85 | RAGAS |
| Context Precision | > 0.70 | RAGAS |
| E2E Latency (P95) | < 3s | response_time_ms |
| TTFT | < 800ms | SSE 첫 청크 시간 |
| Hallucination Rate | < 5% | LLM-as-Judge |

---

## 13. 운영 KPI

### 사용량

- 총 세션 수, 총 질문 수, 순 사용자 수

### 해결율

- 명시적 해결율 (좋아요 피드백 비율)
- 추정 해결율 (답변 후 세션 종료)
- 재문의율

### 품질

- 미응답률 (`answer_status = no_answer`)
- 오답 의심률 (`confirmed_issue` 비율)
- 시민 만족도 평균

### RAG 운영

- 검색 성공률, zero-result rate
- 수집/인덱싱 성공률
- LLM 일간 비용 합계 (USD)
- 모델별 토큰 사용량 추이

---

## 14. 프론트엔드 포털

| 포털 | 경로 | 대상 역할 | 핵심 페이지 |
|---|---|---|---|
| Ops Portal | /ops/* | ops_admin | 대시보드, 수집 관리, 문서 관리, QA 리뷰, 기관 관리 |
| Client Portal | /client/* | client_admin, client_org_viewer | 성과 대시보드, 질문 로그, 미해결 큐, 피드백 현황 |
| QA Portal | /qa/* | qa_admin, qa_manager | QA 대기열, 리뷰 작성, RAGAS 평가 결과 |

**기술 스택**: Next.js 15 App Router, TypeScript, Tailwind CSS v3
**테마**: "Control Tower" 다크 (#080D17 base, #0D1424 surface, #3B82F6 accent)
**인증**: `X-Admin-Session-Id` 헤더, localStorage 세션 ID
**API 프록시**: `/api/admin/*` → `localhost:8080/admin/*`

---

## 15. 보안

- **인증**: bcrypt 비밀번호 해싱 (Spring Security PasswordEncoder)
- **세션**: UUID 기반 세션 ID, Redis 캐시
- **CORS**: localhost:3000, localhost:8080 허용
- **권한 검증**: 액션 기반 (`grantedActions + organization_scope`)
- **감사**: 고위험 액션 `audit_logs` 자동 기록
- **에러**: 스코프 밖 리소스 → 403 아닌 404 반환 (존재 노출 방지)

---

## 16. 테스트 현황

| 구분 | 수량 | 내용 |
|---|---|---|
| 통합 테스트 | 50개 | Auth 9, Ingestion 16, QA 5, Chat 5, E2E 4, RAGAS 3, ArchUnit 8 |
| ArchUnit 규칙 | 8개 | domain JPA 금지, port→adapter 역방향 금지, Controller→persistence 직접 접근 금지, 순환의존 금지 등 |
| 테스트 환경 | H2 in-memory | MODE=PostgreSQL, Flyway V001-V026 자동 적용 |

---

## 17. 남은 과제

### 단기 (즉시 구현 가능)

| 항목 | 우선순위 |
|---|---|
| Docker 멀티스테이지 빌드 (admin-api, rag-orchestrator, ingestion-worker) | HIGH |
| PostgreSQL 연결 (현재 테스트는 H2) | HIGH |
| Ollama 로컬 실행 문서화 | MEDIUM |
| CI/CD 파이프라인 (GitHub Actions) | MEDIUM |

### 중기 (R&D 완료 후)

| 항목 | 선행 조건 |
|---|---|
| 하이브리드 검색 (BM25 + 벡터) | 검색 품질 실험 |
| 청크 전략 최적화 (문서 유형별) | 파싱 실험 완료 |
| 프롬프트 자동 최적화 | RAGAS 회귀 테스트 자동화 |
| 실시간 알림 (Slack/Email) | 운영 요구사항 확정 |

---

## 18. 성공 기준

- 기관 담당자가 미해결 질문과 개선 포인트를 주기적으로 확인할 수 있다.
- 운영사가 LLM 비용·품질·장애 상태를 한 화면에서 파악할 수 있다.
- QA 담당자가 검수 결과를 실제 개선 작업으로 연결할 수 있다.
- RAGAS 점수로 RAG 품질 변화를 정량적으로 추적할 수 있다.
- 핵심 KPI를 기관·서비스·기간별로 집계할 수 있다.
- 모든 LLM 호출의 토큰·비용이 DB에 기록되어 월간 비용 분석이 가능하다.
