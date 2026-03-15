# MVP Screen Spec

## 1. Operations Dashboard

목적:
운영사가 현재 이상 징후와 기관별 상태를 한 번에 파악하는 첫 화면

핵심 데이터:
- 운영 중 기관 수
- 총 세션 수, 총 질문 수
- 평균 응답시간
- fallback rate, error rate
- citation 누락 비율
- 기관 헬스맵
- ingestion 상태
- 최근 장애 목록

주요 액션:
- 기관 상세 이동
- 이슈 상세 이동
- 재수집, 재인덱싱 실행
- 이상 징후 필터링

## 2. Organization Management

목적:
기관별 운영 상태와 최근 품질, 문서 최신화를 관리

핵심 데이터:
- 기관명, 상태, 운영 시작일
- 서비스 수
- 최근 7일 질문 수
- fallback rate, 만족도
- 마지막 문서 동기화 시각
- 담당 운영자

주요 액션:
- 기관 상세 조회
- 문서 소스 현황 확인
- 검색 정책 확인
- 운영 메모 관리

## 3. Document Source Management

목적:
문서의 최신성, 공개 범위, 인덱싱 상태를 관리

핵심 데이터:
- 문서명, 기관, 문서 유형
- 소스 URL 또는 업로드 경로
- 버전, 게시일, 수집일
- 파싱 상태, 인덱싱 상태
- 공개 범위, 만료 여부

주요 액션:
- 문서 등록
- 재수집 실행
- 재인덱싱 실행
- 상태 오류 확인

## 4. RAG Operations

목적:
검색 실패, zero-result, 응답 품질 이슈를 빠르게 파악

핵심 데이터:
- 검색 성공률
- zero-result rate
- 문서 없음 비율
- 상위 실패 질문
- 최근 retrieval 로그

주요 액션:
- 실패 질문 상세 보기
- 검색 로그 조회
- 인덱스 상태 점검
- 재처리 실행

## 4A. Crawl Source Management

목적:
기관별 crawl source와 수집 정책을 등록하고 증분 수집 기준을 관리

핵심 데이터:
- source 이름, 기관, service 범위
- source 유형
- base URL, sitemap URL, allow/deny pattern
- crawl 주기
- 마지막 성공 수집 시각
- 최근 실패 사유

주요 액션:
- source 등록
- source 수정
- source 비활성화
- 즉시 crawl 실행
- 실패 source 필터링

## 4B. Ingestion Job Monitor

목적:
crawl, parse, chunk, embed, index 작업의 실행 이력과 실패 구간을 추적

핵심 데이터:
- job id, 기관, source, document
- job 유형
- 현재 단계, 전체 상태
- 시도 횟수
- 시작 시각, 종료 시각, 소요 시간
- 실패 코드, 실패 메시지
- 생성/갱신된 document version 수

주요 액션:
- job 상세 조회
- 실패 job 재실행 요청
- source 화면으로 이동
- document 상세 이동

## 5. Client Dashboard

목적:
고객사가 자기 기관의 성과와 개선 포인트를 확인

핵심 데이터:
- 총 질문 수
- 해결율, 미응답률, 재문의율
- 만족도
- 상위 질문 주제
- 개선 필요 질문 수

주요 액션:
- 질문 분석 이동
- 미해결 질문 이동
- 리포트 확인

## 6. Question Analysis

목적:
반복 질문과 증가 추세를 분석해 개선 우선순위를 잡음

핵심 데이터:
- 빈출 질문
- 증가 질문
- 주제별 질문 수
- 주제별 미응답률
- 주제별 만족도

주요 액션:
- 질문 상세 이동
- 개선 대상 지정
- FAQ 또는 문서 보강 요청

## 7. Unresolved Questions

목적:
미응답, 오답 의심, 개선 필요 질문을 관리

핵심 데이터:
- 질문 원문
- 기관
- 발생 시각
- 답변 상태
- QA 상태
- 원인 코드

주요 액션:
- 검수 요청
- 문서 보강 요청
- FAQ 등록 연결

## 8. QA Review

목적:
검수자가 질문, 답변, 출처를 함께 보고 품질 원인을 분류

핵심 데이터:
- 질문 원문
- 생성 답변
- 검색 문서 목록
- 점수
- 출처 문서 상태
- 기존 검수 이력

주요 액션:
- 원인 코드 저장
- FAQ 등록
- 문서 수정 요청
- 재인덱싱 요청
- 운영 이슈 등록

## 9. MVP Priority

우선 개발 대상:
- Operations Dashboard
- Client Dashboard
- Unresolved Questions
- QA Review
- Document Source Management

후속 개발 대상:
- Organization Management 상세 고도화
- Question Analysis 심화 기능
- RAG Operations 상세 튜닝 기능

## 10. Shared Filter Rule

공통 필터:
- `organization_id`
- `service_id`
- `from`
- `to`

목록형 화면 공통 규칙:
- 기본 정렬은 `created_at desc` 또는 `updated_at desc`
- 기본 페이지 크기는 `20`
- 고객사 사용자는 자기 기관 범위로 자동 고정

## 11. Screen States And Filters

### Operations Dashboard

상태값:
- `healthy`
- `warning`
- `critical`

권장 필터:
- `organization_id`
- `health_status`
- `has_open_incident`
- `index_status`
- `from`
- `to`

### Organization Management

상태값:
- `active`
- `inactive`
- `onboarding`
- `suspended`

권장 필터:
- `status`
- `owner_user_id`
- `last_document_sync_from`
- `last_document_sync_to`

### Document Source Management

상태값:
- `ingestion_status`: `pending`, `running`, `success`, `failed`
- `index_status`: `pending`, `indexed`, `stale`, `failed`
- `visibility_scope`: `public`, `internal`, `restricted`

권장 필터:
- `organization_id`
- `document_type`
- `ingestion_status`
- `index_status`
- `visibility_scope`
- `published_from`
- `published_to`

### RAG Operations

상태값:
- `retrieval_status`: `success`, `zero_result`, `timeout`, `error`
- `quality_flag`: `normal`, `watch`, `issue`

권장 필터:
- `organization_id`
- `service_id`
- `retrieval_status`
- `zero_result_only`
- `latency_over_ms`
- `from`
- `to`

### Crawl Source Management

상태값:
- `source_status`: `active`, `paused`, `disabled`
- `source_type`: `website`, `sitemap`, `rss`, `file_feed`, `manual_seed`
- `crawl_mode`: `scheduled`, `manual_only`

권장 필터:
- `organization_id`
- `service_id`
- `source_status`
- `source_type`
- `has_recent_failure`
- `last_success_from`
- `last_success_to`

### Ingestion Job Monitor

상태값:
- `job_type`: `crawl`, `parse`, `chunk`, `embed`, `index`, `reindex`
- `job_status`: `queued`, `running`, `success`, `partial_success`, `failed`, `cancelled`
- `job_stage`: `fetch`, `extract`, `normalize`, `chunk`, `embed`, `index`, `complete`

권장 필터:
- `organization_id`
- `service_id`
- `crawl_source_id`
- `job_type`
- `job_status`
- `job_stage`
- `document_id`
- `started_from`
- `started_to`

### Client Dashboard

상태값:
- `trend_status`: `improving`, `flat`, `degrading`

권장 필터:
- `service_id`
- `from`
- `to`

### Question Analysis

상태값:
- `topic_status`: `normal`, `rising`, `critical`

권장 필터:
- `organization_id`
- `service_id`
- `topic`
- `min_question_count`
- `min_fallback_rate`
- `from`
- `to`

### Unresolved Questions

상태값:
- `answer_status`: `fallback`, `no_answer`, `error`
- `review_status`: `pending`, `confirmed_issue`, `false_alarm`, `resolved`

권장 필터:
- `organization_id`
- `service_id`
- `answer_status`
- `review_status`
- `root_cause_code`
- `channel`
- `from`
- `to`

### QA Review

상태값:
- `review_status`: `pending`, `confirmed_issue`, `false_alarm`, `resolved`
- `action_type`: `faq_create`, `document_fix_request`, `reindex_request`, `ops_issue`, `no_action`

권장 필터:
- `organization_id`
- `review_status`
- `root_cause_code`
- `action_type`
- `reviewer_id`
- `reviewed_from`
- `reviewed_to`
