# PRD vs. 현재 구현 상태 비교

> 기준일: 2026-03-22 · `platform-prd.md` 기준

---

## 어드민 포털 / UI 페이지

| PRD 항목 | PRD 경로 | 구현 상태 | 비고 |
|----------|----------|-----------|------|
| 운영 메인 대시보드 | `/ops` | 구현 | KPI 카드 5개, 파이프라인 ProgressBar, 최근 질문 테이블 |
| 기관 관리 | `/ops/organizations` | 구현 | 기관 목록 |
| 인덱싱 현황 | `/ops/indexing` | 구현 | 수집·임베딩·인덱싱 KPI 카드 |
| 품질 모니터링 | `/ops/quality` | 구현 | RAGAS ScoreTable 포함 |
| 장애/이슈 관리 | `/ops/incidents` | 구현 | 알림 로그 테이블 |
| 비용/사용량 | `/ops/cost` | 구현 | LLM 비용 + Knowledge Gap 섹션 |
| Safety 페이지 | `/ops/safety` | 구현 (PRD 외) | PRD에 없음. rag-pipeline-1pager 방향 산물 |
| 배포/버전 관리 | `/ops/deployments` | 미구현 | 페이지 없음, API 없음 |
| RAG 검색 품질 | `/ops/rag/search-quality` | 미구현 | `/ops/indexing`에 일부 통합됨 |
| 기관 헬스맵 (색상 상태) | `/ops` 내 섹션 | 부분구현 | 헬스맵 UI 없음, 이상탐지 알림 없음 |
| 고객사 대시보드 | `/client` | 구현 | 미해결 건수 KpiCard, 응답률 진행 바 |
| 민원응대 성과 | `/client/performance` | 구현 | 페이지 존재 |
| 실패/전환 분석 | `/client/failure` | 구현 | 페이지 존재 |
| 지식 현황 | `/client/knowledge` | 구현 | 페이지 존재 |
| QA 검수 대시보드 | `/qa` | 구현 | RAGAS ScoreTable + 최근 QA 리뷰 |
| 미응답/오답 관리 | `/qa/unresolved` | 구현 | 페이지 존재 |
| 문서/FAQ 관리 | `/qa/documents` | 구현 | 페이지 존재 |
| 승인 워크플로우 | `/qa/approvals` | 부분구현 | 페이지 존재, 워크플로우 로직 미완 |
| 질문 클러스터 관리 | `/qa/clusters` | 미구현 | 페이지 없음, 클러스터링 API 없음 |
| 정책/금칙어 관리 | `/qa/policies` | 미구현 | 페이지 없음 |

---

## 백엔드 API

| PRD 기능 | API | 구현 상태 | 비고 |
|----------|-----|-----------|------|
| 인증/세션 | `POST /admin/auth/login` 외 | 구현 | 6개 역할 포함 |
| 기관 관리 | `GET /admin/organizations` | 구현 | |
| 인덱싱 관리 | `/admin/crawl-sources`, `/admin/ingestion-jobs` | 구현 | 상태 전이 포함 |
| QA 리뷰 | `/admin/qa-reviews` | 구현 | 상태 머신 완성 |
| 질문/답변 | `/admin/questions`, `/admin/answers` | 구현 | |
| 미응답 큐 | `/admin/questions/unresolved` | 구현 | |
| 문서 관리 | `/admin/documents` | 구현 | |
| 사용자 피드백 | `/admin/feedbacks` | 구현 | 명시적 좋아요/싫어요 |
| RAG 검색 로그 | `/admin/rag-search-logs` | 구현 | source_doc_ids, search_score |
| RAGAS 평가 | `/admin/ragas-evaluations` | 구현 | |
| LLM 메트릭 | `/admin/metrics/llm` | 구현 | 토큰/비용/응답시간 |
| 일간 KPI 스냅샷 | `/admin/daily-metrics` | 구현 | 집계 저장 |
| 기관 헬스 집계 | — | 미구현 | 이상탐지 알림 API 없음 |
| 질문 클러스터 | — | 미구현 | 유사 질문 그룹화 API 없음 |
| 배포/버전 이력 | — | 미구현 | 모델·프롬프트 버전 추적 없음 |
| 질문 의도 자동 분류 | — | 미구현 | `question_category` 컬럼은 존재 |

---

## 데이터 키 (DB 컬럼)

| PRD 키 | DB 컬럼 | 구현 상태 | 비고 |
|--------|---------|-----------|------|
| `org_id` | `organizations.id` | 구현 | 모든 테이블 스코프 기준 |
| `service_id` | `services.id` | 구현 | |
| `session_id` | `chat_sessions.id` | 구현 | |
| `question_id` | `questions.id` | 구현 | |
| `user_anon_id` | — | 미구현 | 익명 사용자 식별자 없음 |
| `asked_at` | `questions.created_at` | 구현 | |
| `question_category` | `questions.question_category` (V021) | DB만 | 컬럼 있음, 분류 로직 없음 |
| `answer_result_code` | `answers.answer_status` | 구현 | `answered / no_answer / fallback / error` |
| `failure_code` (A01~A10) | `questions.failure_reason_code` (V021) | DB+도메인 | 컬럼·enum 있음, UI/API 미노출 |
| `escalated_to_human` | `questions.is_escalated` (V021) | DB만 | 컬럼 있음, 집계 미구현 |
| `satisfaction` | `feedbacks.rating` (V019) | 구현 | |
| `source_doc_ids` | `rag_retrieved_documents` (V017) | 구현 | |
| `doc_version` | `document_versions.id` | 구현 | |
| `model_version` | `answers.model_name` (V026) | 구현 | |
| `search_score` | `rag_retrieved_documents.similarity_score` | 구현 | |
| `response_time_ms` | LLM 메트릭 (V026) | 구현 | |

---

## KPI 지표 집계

| PRD KPI | 대상 | DB 컬럼 | 집계 API | UI 표시 |
|---------|------|---------|---------|---------|
| 자동응대 완료율 | 고객사 | `auto_resolution_rate` (V023) | 미구현 | 미구현 |
| 상담 전환율 | 고객사 | `escalation_rate` (V023) | 미구현 | 미구현 |
| 1회 해결률(FCR) | 고객사 | — | 미구현 | 미구현 |
| 재문의율 | 고객사 | `revisit_rate` (V023) | 미구현 | 미구현 |
| 업무시간 외 응대율 | 고객사 | `after_hours_rate` (V023) | 미구현 | 미구현 |
| 사용자 만족도 | 공통 | `feedbacks.rating` | 부분구현 | 부분구현 |
| 미응답률 | 운영사 | `unanswered_count` (V023) | 부분구현 | 있음 |
| 지식 공백 건수 | 공통 | `knowledge_gap_count` (V023) | 부분구현 | `/ops/cost`에 있음 |
| LLM 토큰/비용 | 운영사 | V026 컬럼들 | 구현 | `/ops/cost`에 있음 |
| Faithfulness (RAG) | 운영사 내부 | `ragas_evaluations` | 구현 | `/ops/quality`에 있음 |
| SLA 준수율 | 운영사 | — | 미구현 | 미구현 |
| MTTR | 운영사 | — | 미구현 | 미구현 |

---

## 요약

| 구분 | 현황 |
|------|------|
| **앞서 있는 영역** | RAG 품질 자동화 (RAGAS, LLM-as-Judge, LLM 메트릭) — Phase 2 수준 |
| **DB 있으나 미연결** | 고객사 KPI 5종 (`auto_resolution_rate` 등), 실패 코드(A01~A10), 상담 전환 여부 |
| **완전 미구현** | 기관 헬스맵·이상탐지, 배포/버전 관리, 질문 클러스터링, 정책/금칙어 |
| **PRD 범위 외 구현** | `/ops/safety` 페이지 (rag-pipeline-1pager 방향) |

**핵심 갭**: 고객사가 실제로 필요한 KPI(자동응대율, 상담 전환율, 재문의율)는 DB 컬럼이 이미 준비됐지만 집계 로직·API·UI 연결이 모두 빠진 상태. 이것이 "방향성이 틀어진" 체감의 본질이다.
