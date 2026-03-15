# 공공기관 RAG 챗봇 운영 플랫폼 통합 설계 문서
부제: 운영사 관제 · 고객사 성과관리 · QA/지식관리 · 데이터모델 · 아키텍처 · PRD 초안 통합본

---

## 문서 목적

이 문서는 지금까지 대화에서 논의한 내용을 **빠짐없이 하나의 문서로 체계화**한 통합 설계 문서다.  
단순 요약본이 아니라, 실제로 다음 작업에 바로 이어질 수 있는 수준을 목표로 한다.

- 서비스/제품 기획 정리
- PM용 PRD 초안
- 디자이너용 Admin 구조 정리
- 개발팀용 데이터/로그/아키텍처 기준 정리
- 운영팀용 KPI 및 운영 루프 정리
- 문서화/GitHub/Notion 이전용 기준 문서

---

# 1. 제품 개요

## 1.1 제품 한 줄 정의

**법령, 조례, 민원편람, 실무편람, 홈페이지 크롤링 데이터, FAQ, 공지사항 등 다양한 공공 지식원을 기반으로 시민 질문에 자동 응답하고, 운영사와 고객사가 품질·성과·최신성·출처를 함께 관리할 수 있는 멀티기관형 RAG 챗봇 운영 플랫폼**

## 1.2 제품이 필요한 이유

공공기관 챗봇은 일반 커머스/고객센터 챗봇과 다르다.

### 일반 챗봇과 다른 점
- 답변의 자연스러움보다 **정확성·근거성·최신성**이 중요
- 법령, 조례, 민원편람 등 **제도 문서**를 기반으로 답해야 함
- 같은 질문이라도 시민용 답변과 내부 직원용 답변의 공개 범위가 다를 수 있음
- “챗봇이 편리하다”가 아니라 **민원응대 효율이 얼마나 개선됐는지**를 수치로 보여줘야 함
- 운영 중 계속 질문이 바뀌기 때문에 **QA와 문서 보강 루프**가 필수임

### 단순 LLM 챗봇의 한계
- 근거 없는 생성 답변 위험
- 문서 최신화/버전관리 부재
- 공공기관별 지식원 분리 운영 어려움
- 오답/미응답/구버전 참조를 추적하기 어려움
- 성과 대시보드와 운영 도구가 없으면 실제 운영이 불가능함

---

# 2. 제품 목표

## 2.1 운영사 목표
- 여러 공공기관 챗봇을 통합 운영할 수 있어야 함
- 기관별 문서/RAG/품질 상태를 한 번에 볼 수 있어야 함
- 장애/품질 저하를 조기에 탐지하고 대응할 수 있어야 함
- 문서 ingestion 파이프라인을 안정적으로 유지해야 함
- QA 루프를 통해 적은 인력으로도 품질을 계속 개선할 수 있어야 함
- 기관별 SLA와 비용/사용량을 관리할 수 있어야 함

## 2.2 고객사 목표
- 민원 자동응대율 향상
- 전화/창구/반복문의 감소
- 자주 묻는 질문과 실패 질문 파악
- 문서/FAQ 최신화 포인트 확인
- 내부 보고에 사용할 성과자료 확보
- 업무시간 외 민원 응대 성과 확인

## 2.3 QA/지식관리 목표
- 미응답/오답을 빠르게 발견
- 원인을 정확하게 분류
- FAQ 생성 또는 문서 보강으로 연결
- 재인덱싱 후 성과 개선 여부 추적
- 고위험/내부문서 노출 가능성 통제

---

# 3. 사용자 유형과 역할

## 3.1 운영사 관리자
예시:
- AI 운영팀
- 데이터 운영팀
- 챗봇 운영팀
- 플랫폼 관리자

주요 권한:
- 전체 기관 조회
- 전체 문서 파이프라인 관리
- 전체 RAG 품질 조회
- 전체 장애/이슈 관리
- QA/정책/배포 관리

## 3.2 고객사 관리자
예시:
- 민원실 담당자
- 디지털행정 담당자
- 챗봇 운영 담당자
- 부서 관리자

주요 권한:
- 자기 기관 성과 확인
- 미해결 질문 확인
- FAQ 관리
- 문서 수정 요청 또는 일부 문서 직접 관리
- 리포트 다운로드

## 3.3 QA / 지식관리 담당자
예시:
- 콘텐츠 운영자
- QA 담당자
- 지식관리 담당자

주요 권한:
- 미응답/오답 질문 검수
- 원인 분류
- FAQ 작성
- 문서 보강 요청
- 재인덱싱 요청
- 금칙/제한 응답 정책 관리

---

# 4. KPI 체계

이 플랫폼의 KPI는 크게 4개 묶음으로 나누는 것이 적절하다.

- 이용 KPI
- 해결/성과 KPI
- 품질 KPI
- RAG 운영 KPI

## 4.1 이용 KPI
- 총 세션 수
- 총 질문 수
- 순 사용자 수(UV)
- 세션당 평균 질문 수
- 채널별 이용량
- 시간대별 이용량
- 신규 vs 재방문 비율
- 부서/서비스별 이용량

## 4.2 해결/성과 KPI
- 명시적 해결율
- 추정 해결율
- 미해결 의심률
- 재문의율
- 목표 행동 성공률
- 업무시간 외 처리 비율
- 민원 유형별 해결률
- 자동응대 완료율

## 4.3 품질 KPI
- fallback rate(미응답률)
- 오답 의심률
- 평균 만족도
- 출처 포함 답변 비율
- citation 누락 비율
- 평균 응답시간
- p95 응답시간
- 내부문서 사용 위험 건수
- 구버전/만료문서 참조 비율

## 4.4 RAG 운영 KPI
- 문서 수집 성공률
- 파싱 실패율
- 임베딩 실패율
- 인덱싱 지연 시간
- 검색 성공률
- zero result rate
- 문서 없음(DOC_MISSING) 비율
- 문서 유형별 참조 비율
- 법령 우선 질문에서 법령 참조 비율
- 민원편람 질문에서 편람 참조 비율

---

# 5. 상담사 연결이 없을 때 해결율 정의

상담사 연결 기능이 없는 경우, 해결율을 단일 확정값으로 보기 어렵다.  
따라서 **확정 해결율 + 추정 해결율 + 미해결 의심률** 구조로 보는 것이 적절하다.

## 5.1 명시적 해결율
사용자가 피드백 버튼 등으로 “해결됐다”를 명시한 경우

예시 공식:
- 해결 피드백 수 / 전체 피드백 수

## 5.2 추정 해결율
사용자 행동 로그를 기반으로 해결 가능성을 추정

대표 신호:
- 답변 후 추가 질문 없음
- 동일/유사 주제로 7일 내 재문의 없음
- 관련 링크/문서 클릭 후 종료
- 목표 행동 완료(신청 페이지 이동, 서식 다운로드 등)

## 5.3 미해결 의심률
다음과 같은 신호가 결합되면 미해결 의심으로 분류

- 부정 피드백
- fallback 발생
- 반복 질문
- 같은 주제 재문의
- 장시간 대화 후 긍정 신호 없음

## 5.4 대시보드 권장 노출
- 해결 확정
- 해결 추정
- 미해결 의심
- 판단 불가

---

# 6. Admin 전체 구조

Admin은 최소 3개 축으로 나눠야 한다.

1. 운영사 Admin
2. 고객사 Admin
3. QA / 지식관리 Admin

그리고 공통 시스템 영역이 필요하다.

- 알림센터
- 통합 검색
- 리포트 센터
- 설정
- 권한관리
- 감사로그

---

# 7. 운영사 Admin 설계

## 7.1 운영사 Admin 목적
운영사 Admin은 “예쁜 통계 화면”이 아니라 **관제실 + 운영실 + 품질센터**처럼 설계해야 한다.

핵심 목표:
- 기관 전체 상태 한눈에 파악
- 장애/품질 이상 조기 탐지
- 문서/RAG 파이프라인 관리
- 운영 효율 극대화

## 7.2 운영사 Admin IA
- 운영 대시보드
- 기관 관리
- 챗봇 운영 현황
- RAG 운영 관리
- 문서 소스 관리
- 품질 모니터링
- QA/검수 관리
- 장애/이슈 관리
- 배포/정책 관리
- 비용/사용량
- 운영 리포트
- 설정

## 7.3 운영 대시보드에서 보여야 하는 것
### 상단 KPI 카드
- 운영 중 기관 수
- 오늘 총 세션 수
- 오늘 총 질문 수
- 평균 응답시간
- fallback rate
- error rate
- citation 누락 비율
- 내부문서 참조 위험 건수

### 중앙 핵심 영역
- 기관 헬스맵
- 이상 탐지 피드
- ingestion 파이프라인 상태
- 최근 주요 장애/이슈

### 하단 영역
- 질문량 추이
- 기관별 품질 순위
- 기관별 RAG 실패율
- 기관별 비용 추이

## 7.4 기관 관리 화면
기관 목록 컬럼 예시:
- 기관명
- 상태
- 운영 시작일
- 서비스 수
- 최근 7일 질문 수
- fallback rate
- 만족도
- 마지막 문서 동기화 시각
- 담당 운영자

기관 상세에서 보여야 하는 것:
- 기관 개요
- 서비스 목록
- 문서 소스 현황
- 최근 이슈
- 품질 지표
- 검색 정책
- 리포트 다운로드

## 7.5 문서 소스 관리 화면
문서 유형별 탭:
- 홈페이지 크롤링
- 법령/조례
- 민원편람
- 실무편람
- FAQ/공지
- 업로드 문서

공통 컬럼:
- 문서명
- 문서 유형
- 출처
- 버전
- 공개범위
- 최신 여부
- 마지막 수집 시각
- 마지막 인덱싱 시각
- 상태(active / deprecated / draft / archived)

## 7.6 RAG 운영 관리 화면
탭 구조 예시:
- 수집 현황
- 파싱 현황
- chunk 현황
- embedding 현황
- 인덱싱 현황
- 검색 품질
- 출처 품질

검색 품질에서 봐야 하는 지표:
- search success rate
- zero result rate
- rerank 적용률
- 문서 유형별 참조 분포
- citation 사용률

출처 품질에서 봐야 하는 지표:
- citation 없는 답변 수
- 내부문서 사용 시도 수
- 만료문서 참조 수
- 구버전 참조 수
- 비권장 문서 참조 수

## 7.7 장애/이슈 관리 화면
이슈 유형 예시:
- crawl_failure
- parse_failure
- embedding_backlog
- index_delay
- high_fallback
- latency_spike
- citation_missing_spike
- internal_source_leak_risk

---

# 8. 고객사 Admin 설계

## 8.1 고객사 Admin 목적
- 챗봇 도입 효과 확인
- 어떤 민원이 잘/안 풀리는지 확인
- 문서와 FAQ를 직접 보강
- 내부 보고용 성과 자료 확보

## 8.2 고객사 Admin IA
- 기관 대시보드
- 민원응대 성과
- 질문 분석
- 미해결/개선 필요 질문
- 문서/FAQ 관리
- 챗봇 설정
- 사용자/권한
- 리포트
- 공지/배너 관리

## 8.3 기관 대시보드 핵심 KPI
- 총 질문 수
- 총 세션 수
- 자동응대율
- 추정 해결율
- 재문의율
- 만족도
- 평균 응답시간
- 업무시간 외 응대 비율

## 8.4 질문 분석 화면
핵심 탭:
- 자주 묻는 질문
- 신규 질문
- 반복 질문
- 급증 질문
- 카테고리별 질문
- 채널별 질문

필수 기능:
- 질문 클릭 시 실제 예시 보기
- 유사 질문 묶음 보기
- 기간 비교
- 질문 증가율 보기
- FAQ 등록 바로가기

## 8.5 미해결/개선 필요 질문 화면
구분:
- 미응답 질문
- 부정 피드백 질문
- 재문의 많은 질문
- 담당부서 연결 필요 질문
- 문서 없음 의심 질문

리스트 컬럼:
- 질문
- 카테고리
- 발생 건수
- 최근 발생일
- 원인 추정
- 현재 상태
- 조치 버튼

조치 버튼 예시:
- FAQ 등록
- 문서 추가 요청
- 답변 문구 수정 요청
- 담당부서 안내 등록

## 8.6 문서/FAQ 관리 화면
- FAQ 등록
- FAQ 수정
- FAQ 승인 요청
- FAQ 사용 통계
- 문서 업로드
- 문서 버전 확인
- 공개범위 확인
- 최신화 필요 상태 확인
- 인덱싱 요청
- 민원편람 정보 수정 요청
- 처리기간/수수료/준비서류/담당부서 수정 요청

---

# 9. QA / 지식관리 Admin 설계

## 9.1 목적
- 오답/미응답 빠르게 발견
- 원인 정확히 분류
- 문서/FAQ 보강으로 연결
- 재인덱싱과 개선 이력 관리
- 고위험 답변 통제

## 9.2 IA
- 검수 대시보드
- 미응답 질문
- 오답 의심 질문
- 출처 누락 답변
- 질문 클러스터
- 문서 보강 요청
- FAQ 후보 관리
- 재인덱싱 요청
- 정책/금칙어 관리
- 개선 이력

## 9.3 검수 대시보드 KPI
- 미응답 건수
- 오답 의심 건수
- 출처 누락 건수
- 내부문서 위험 건수
- 처리 대기 건수
- 금일 처리 완료 건수

## 9.4 미응답 질문 상세화면에서 보여야 하는 것
- 원문 질문
- 정규화 질문
- 질문 카테고리
- 질문 빈도
- 발생 기관 / 채널
- 검색된 문서 후보
- 검색 점수
- fallback 이유
- 유사 질문
- 관련 FAQ

액션:
- 문서 없음으로 분류
- 검색 실패로 분류
- 질문 모호로 분류
- 정책상 제한으로 분류
- FAQ 생성
- 문서 추가 요청
- 재인덱싱 요청

## 9.5 오답 의심 질문 상세화면에서 보여야 하는 것
- 질문
- 실제 답변
- 사용된 출처
- citation 문서
- 조문/편람/페이지 위치
- 검수 의견
- 과거 유사 오류 여부

액션:
- 오답 확정
- 불완전 답변
- 환각 의심
- 문구 수정 필요
- 금칙 처리
- 정책 템플릿 수정 요청

## 9.6 질문 클러스터 화면
기능:
- 유사 질문 자동 묶음
- 대표 질문 지정
- 급증 클러스터 탐지
- FAQ 후보 추출
- 카테고리 재분류

예시:
cluster: 주민등록등본 발급
- 주민등록등본 어디서 발급하나요
- 등본 인터넷으로 떼는 법
- 무인발급기에서 등본 출력 가능한가요
- 등본 발급 수수료 얼마인가요

---

# 10. 문서 소스 전략

이 플랫폼은 단순 문서 업로드형 RAG가 아니라 **다원적 공공 지식원 기반 RAG**다.  
문서 유형별로 구조와 운영 방식이 달라야 한다.

## 10.1 홈페이지 크롤링 문서
특징:
- 자주 바뀔 수 있음
- HTML 구조가 깨질 수 있음
- 메뉴/경로 정보 중요
- 콘텐츠 hash와 변경 감지가 중요

필요 관리 항목:
- page_url
- menu_path
- page_title
- meta_description
- content_hash
- crawl_depth
- is_notice_page
- is_faq_page

## 10.2 법령/조례/규정
특징:
- 가장 높은 근거 문서
- 조문/항/호 단위 인용 필요
- 시행일/개정일 관리 중요
- 현행/구버전 분리 필요

필요 관리 항목:
- law_type
- law_code
- jurisdiction
- promulgation_date
- effective_date
- revision_date
- is_current_version
- article_no / clause_no / item_no

## 10.3 민원편람
특징:
- 실제 민원인이 궁금해하는 절차 중심
- 정형 필드와 설명형 텍스트가 혼합됨
- 공공기관 챗봇에서 핵심 소스 중 하나

구조화 권장 항목:
- service_name
- service_code
- department_name
- processing_period
- fee_text
- application_method
- visit_required_flag
- online_available_flag
- required_documents
- legal_basis_summary
- contact_info

## 10.4 실무편람
특징:
- 내부 직원용일 가능성 높음
- 시민용 답변에 직접 노출되면 위험
- 보안등급과 공개범위 관리 필수

권장 항목:
- department_name
- task_area
- target_role
- contains_internal_policy_flag
- sensitivity_level
- requires_review_flag

## 10.5 FAQ / 공지 / 보도자료
특징:
- 빠르게 반영 가능
- 최신 질문 대응에 효과적
- 반복 질문 흡수에 유리

---

# 11. 데이터 모델 / DB 설계

## 11.1 핵심 엔터티 목록
- organizations
- services
- users
- chat_sessions
- chat_messages
- questions
- answers
- feedbacks
- user_actions
- documents
- document_sources
- legal_documents
- legal_document_articles
- complaint_guides
- work_guides
- website_documents
- document_chunks
- document_ingestion_jobs
- document_ingestion_items
- rag_search_logs
- rag_retrieved_documents
- rag_generation_logs
- qa_reviews
- question_clusters
- question_cluster_map
- retrieval_policies
- daily_metrics_org
- daily_metrics_category

## 11.2 멀티테넌트 기본 구조
### organizations
- id
- name
- organization_type
- region
- status
- created_at

### services
- id
- organization_id
- name
- channel_type
- status
- created_at

## 11.3 세션 / 메시지 / 질문 / 답변 구조
### chat_sessions
- id
- organization_id
- service_id
- user_id
- session_start_at
- session_end_at
- question_count
- message_count
- resolved_flag
- suspected_unresolved_flag
- revisit_within_7d
- channel_type
- created_at

### chat_messages
- id
- session_id
- role
- message_text
- intent_label
- intent_confidence
- response_status
- created_at

### questions
- id
- session_id
- message_id
- question_text
- normalized_question
- question_category
- question_topic
- answer_status
- failure_reason_code
- needs_human_judgment_flag
- created_at

### answers
- id
- question_id
- answer_text
- answer_type
- response_time_ms
- token_input
- token_output
- has_citation
- citation_count
- used_internal_source_flag
- used_restricted_source_flag
- model_name
- model_version
- prompt_version
- created_at

## 11.4 사용자 행동 / 피드백
### user_actions
- id
- session_id
- question_id
- action_type
- action_value
- created_at

예시 action_type:
- link_click
- document_open
- feedback
- target_action
- session_end

### feedbacks
- id
- session_id
- question_id
- feedback_type
- rating
- comment
- created_at

## 11.5 문서 레지스트리
### documents
- id
- organization_id
- title
- document_type
- source_type
- source_url
- file_path
- authority_level
- audience_type
- visibility_scope
- version
- is_latest
- effective_start_date
- effective_end_date
- status
- trust_score
- priority_score
- created_at
- updated_at
- last_crawled_at
- last_indexed_at

### document_sources
- id
- document_id
- source_type
- source_system
- source_url
- source_hash
- crawl_job_id
- collected_at
- http_status
- change_detected_flag
- created_at

## 11.6 문서 유형별 확장 테이블
### legal_documents
- document_id
- law_type
- law_code
- jurisdiction
- promulgation_date
- effective_date
- revision_date
- is_current_version
- parent_law_id

### legal_document_articles
- id
- document_id
- article_no
- clause_no
- item_no
- article_title
- article_text
- embedding_id
- created_at

### complaint_guides
- document_id
- service_name
- service_code
- department_name
- processing_period
- fee_text
- application_method
- visit_required_flag
- online_available_flag
- required_documents
- legal_basis_summary
- contact_info
- updated_at

### work_guides
- document_id
- department_name
- task_area
- target_role
- contains_internal_policy_flag
- sensitivity_level
- requires_review_flag

### website_documents
- document_id
- page_url
- menu_path
- page_title
- meta_description
- content_hash
- crawl_depth
- is_notice_page
- is_faq_page

## 11.7 chunk 구조
### document_chunks
- id
- document_id
- chunk_index
- chunk_text
- chunk_type
- section_title
- section_path
- article_no
- clause_no
- embedding_id
- token_count
- char_count
- priority_score
- freshness_score
- is_public_answerable
- is_citation_preferred
- created_at

chunk_type 예시:
- paragraph
- faq_pair
- article
- table_row
- procedure_step
- summary

## 11.8 ingestion 구조
### document_ingestion_jobs
- id
- organization_id
- job_type
- source_type
- status
- started_at
- ended_at
- triggered_by
- error_summary
- created_at

job_type 예시:
- crawl
- upload
- sync
- reindex
- legal_update_sync

### document_ingestion_items
- id
- job_id
- document_id
- step_name
- status
- error_message
- duration_ms
- created_at

step_name 예시:
- fetch
- parse
- clean
- classify
- chunk
- embed
- index

## 11.9 RAG 런타임 로그
### rag_search_logs
- id
- question_id
- search_query
- top_k
- search_time_ms
- search_success_flag
- result_count
- created_at

### rag_retrieved_documents
- id
- question_id
- document_id
- chunk_id
- retrieval_stage
- rank_order
- retrieval_score
- rerank_score
- selected_for_answer
- citation_used
- created_at

retrieval_stage 예시:
- vector
- lexical
- rerank
- final_context

### rag_generation_logs
- id
- question_id
- model_name
- prompt_template_version
- context_chunk_ids
- generation_time_ms
- created_at

## 11.10 QA / 클러스터
### qa_reviews
- id
- question_id
- review_type
- review_status
- reviewer_id
- comment
- created_at

review_type 예시:
- wrong_answer
- incomplete
- hallucination
- policy_violation

### question_clusters
- id
- cluster_label
- cluster_description
- created_at

### question_cluster_map
- id
- question_id
- cluster_id

## 11.11 Retrieval Policy
### retrieval_policies
- id
- organization_id
- policy_name
- question_category
- priority_rules_json
- is_active
- created_at

## 11.12 집계 테이블
### daily_metrics_org
- id
- organization_id
- metric_date
- total_sessions
- total_questions
- unique_users
- explicit_resolution_rate
- estimated_resolution_rate
- revisit_rate
- avg_response_time
- p95_response_time
- fallback_rate
- error_rate
- avg_satisfaction
- created_at

### daily_metrics_category
- id
- organization_id
- metric_date
- question_category
- question_count
- resolution_rate
- fallback_rate
- avg_response_time
- target_action_success_rate

---

# 12. KPI 계산 로직

## 12.1 총 세션 수
- chat_sessions count

## 12.2 총 질문 수
- questions count

## 12.3 순 사용자 수
- distinct user_id 또는 anonymous_id

## 12.4 명시적 해결율
- feedback_type = solved / 전체 피드백 수

## 12.5 피드백 응답률
- 피드백 남긴 세션 수 / 전체 세션 수

## 12.6 추정 해결율
- resolved_flag = true / 전체 세션 수

## 12.7 미해결 의심률
- suspected_unresolved_flag = true / 전체 세션 수

## 12.8 재문의율
- revisit_within_7d = true / 전체 세션 수

## 12.9 목표 행동 성공률
- target_action completed / 해당 카테고리 질문 수

## 12.10 미응답률
- answer_status = fallback / 전체 질문 수

## 12.11 오답 의심률
- qa_reviews wrong_answer / 전체 질문 수

## 12.12 평균 만족도
- feedback rating avg

## 12.13 평균 응답시간
- answers.response_time_ms avg
- p50 / p90 / p95도 함께 관리 권장

## 12.14 검색 성공률
- search_success_flag = true / 전체 검색 수

## 12.15 zero result rate
- result_count = 0 / 전체 검색 수

## 12.16 문서 없음 비율
- failure_reason_code = DOC_MISSING / 전체 질문 수

---

# 13. API 구조 초안

## 13.1 전체 API 구조
- /api/admin/ops
- /api/admin/client
- /api/admin/qa
- /api/chat
- /api/rag
- /api/analytics
- /api/system

## 13.2 운영사 API 예시
- GET /api/admin/ops/dashboard
- GET /api/admin/ops/org-health
- GET /api/admin/ops/anomalies

## 13.3 고객사 API 예시
- GET /api/admin/client/dashboard
- GET /api/admin/client/category-performance
- GET /api/admin/client/top-questions
- GET /api/admin/client/unresolved-questions

## 13.4 QA API 예시
- GET /api/admin/qa/unanswered
- GET /api/admin/qa/wrong-answers
- GET /api/admin/qa/question-clusters
- POST /api/admin/qa/review
- POST /api/admin/qa/create-faq

## 13.5 RAG / Analytics API 예시
- GET /api/rag/documents
- GET /api/rag/index-status
- GET /api/rag/search-metrics
- POST /api/rag/reindex
- GET /api/analytics/daily
- GET /api/analytics/category

---

# 14. 전체 시스템 아키텍처

## 14.1 레이어 구성
1. Channel Layer  
2. Gateway / App Layer  
3. Chat Orchestration Layer  
4. Retrieval / RAG Layer  
5. Knowledge Ingestion Layer  
6. Data / Analytics Layer  
7. Admin / QA Layer  

## 14.2 채널 레이어
- 기관 홈페이지 챗봇 위젯
- 모바일 웹/앱
- 내부 업무포털
- 메신저 연동
- API 채널

## 14.3 Gateway / App Layer
역할:
- 인증/인가
- 기관 식별
- Rate limiting
- 요청 validation
- 채널 metadata 부착

## 14.4 Chat Orchestration Layer
역할:
- 질문 정규화
- 의도/카테고리 분류
- 민감 질문 판별
- retrieval policy 선택
- 답변 생성 전략 선택
- 안전 정책 검사
- 로그 저장

## 14.5 Retrieval / RAG Layer
역할:
- query rewrite
- hybrid retrieval
- metadata filtering
- rerank
- citation selection
- grounding check
- response generation

## 14.6 Knowledge Ingestion Layer
입력 소스:
- 홈페이지 크롤링
- 법령/조례 동기화
- 민원편람 배치 수집
- 실무편람 업로드
- FAQ 수기 등록
- 공지/보도자료 수집

## 14.7 Data / Analytics Layer
저장소 역할:
- PostgreSQL: 업무 데이터
- Search Engine(OpenSearch): BM25 / 하이브리드 / 필터 검색
- Vector DB(Qdrant / pgvector): semantic retrieval
- Object Storage: 원문/버전/첨부 보관
- Redis: 세션/정책/검색 캐시
- Analytics Store: 대시보드용 집계

---

# 15. 서비스 구성도와 배포 구조

## 15.1 주요 서비스 단위
- Chat API
- Admin API
- Orchestrator
- Retrieval Service
- Ingestion Service
- QA Service
- Analytics Service
- Worker Cluster

## 15.2 Chat API 역할
- 세션 생성
- 질문 저장
- Orchestrator 호출
- 답변 반환

## 15.3 Orchestrator 역할
- 질문 분류
- 검색 정책 선택
- RAG 호출
- 답변 생성 전략 선택
- 안전 정책 검사

## 15.4 Retrieval Service 내부 구성
- Query Rewrite
- Hybrid Search
- Rerank
- Context Builder
- LLM Generation
- Citation Builder

## 15.5 Ingestion Service 주요 기능
- 웹 크롤링
- 법령 동기화
- 민원편람 파싱
- 문서 업로드
- chunk 생성
- embedding 생성
- 검색 인덱싱

## 15.6 Worker Layer
종류 예시:
- crawler_worker
- parser_worker
- chunk_worker
- embedding_worker
- index_worker
- analytics_worker

## 15.7 Queue / Event 구조
권장 큐:
- crawl_jobs
- parse_jobs
- chunk_jobs
- embedding_jobs
- index_jobs
- metrics_jobs

파이프라인 예시:
Crawler
→ parse_jobs
→ Parser Worker
→ chunk_jobs
→ Chunk Worker
→ embedding_jobs
→ Embedding Worker
→ index_jobs
→ Index Worker

## 15.8 Cache 설계
Redis 용도:
- 세션 캐시
- 대화 context 캐시
- 최근 검색 캐시
- 정책 캐시
- FAQ 캐시

## 15.9 LLM Adapter 구조
- OpenAI
- Azure OpenAI
- Local LLM
- Fallback model

장점:
- 모델 교체 용이
- 비용 관리
- fallback 가능
- A/B 테스트 가능

## 15.10 권장 배포 단위
- api-service
- orchestrator-service
- retrieval-service
- ingestion-service
- worker-cluster
- admin-service

데이터 레이어:
- postgres cluster
- opensearch cluster
- vector db
- redis
- object storage

---

# 16. 문서 수집/동기화 파이프라인

## 16.1 홈페이지 크롤링 파이프라인
- 기관별 크롤링 잡 생성
- fetch
- HTML 본문 추출
- 메뉴 경로 추출
- boilerplate 제거
- 문서 분류
- hash 비교
- 변경 시 새 버전 생성
- chunk / embed / index

핵심:
- 본문 추출 품질
- 중복 제거
- 페이지 변경 감지
- 깨진 페이지/404 감시

## 16.2 법령 동기화 파이프라인
- source sync
- 문서 메타 수집
- 조문 단위 파싱
- 개정 여부 비교
- 현행/구버전 분리
- article chunk 생성
- embed / index

핵심:
- 조문 단위 관리
- 시행일 기준 버전 관리
- 현행 법령 우선 사용

## 16.3 민원편람 파이프라인
- 편람 수집
- 템플릿 구조 인식
- 처리기간/수수료/서류/부서 추출
- 구조화 필드 저장
- 절차형 chunk 생성
- index

핵심:
- 정형 데이터화
- 민원명 표준화
- 설명형 텍스트와 정형 필드 결합

## 16.4 실무편람 / 내부문서 파이프라인
- 업로드
- 보안등급 태깅
- 공개범위 판정
- internal / restricted 설정
- chunk / embed / index
- 시민 채널 차단 규칙 반영

---

# 17. Retrieval 전략

공공기관 질문은 질문 유형별 검색 전략이 달라야 한다.

## 17.1 법적 기준 질문
예:
- 과태료 얼마인가요
- 어떤 법 조항이 근거인가요

우선순위:
- law
- ordinance
- regulation
- complaint_guide

제한:
- internal / restricted 제외
- 최신 효력 문서 우선

## 17.2 민원 신청 절차 질문
예:
- 어디서 신청하나요
- 준비서류가 뭔가요

우선순위:
- complaint_guide
- website_page
- faq

제한:
- internal 제외
- 최근 업데이트 페이지 가중치 부여 가능

## 17.3 내부 업무 질문
예:
- 담당 공무원이 어떤 순서로 처리하나요

우선순위:
- work_guide
- internal_manual
- law

제한:
- 내부 사용자에게만 허용

## 17.4 시민 채널 공통 규칙
- internal / restricted / draft / deprecated / expired 차단
- citation 가능한 chunk 우선
- 최신 문서 우선
- 민감 질문은 안전 응답 또는 담당부서 안내

---

# 18. 답변 생성 전략

공공기관 챗봇은 질문 유형별 답변 전략도 달라야 한다.

## 18.1 정형 우선 답변
민원편람에서 바로 뽑을 수 있는 항목:
- 처리기간
- 수수료
- 준비서류
- 접수처

이 경우 템플릿형 답변이 안정적이다.

## 18.2 인용형 답변
법령 질문의 경우:
- 짧은 요약
- 조문 근거
- 문서명/조문 표시
- 유의문구

## 18.3 절차형 답변
민원 신청/처리 안내의 경우:
- 1단계
- 2단계
- 3단계
형식으로 주는 것이 좋다.

## 18.4 제한 응답
판단/심사/예외 적용 질문의 경우:
- 일반 원칙 안내
- 담당부서 문의 유도
- 법적 판단 아님 고지

---

# 19. 운영 워크플로우

## 19.1 품질 개선 루프
질문 발생  
→ 미응답/오답 감지  
→ QA 검수  
→ 원인 분류  
→ FAQ 생성 또는 문서 추가 요청  
→ 재인덱싱  
→ 개선 효과 측정

## 19.2 운영사 대응 루프
이상 탐지  
→ 기관/문서/검색 품질 문제 확인  
→ 파이프라인/정책/문서 상태 점검  
→ 조치(재크롤링/재인덱싱/정책 수정)  
→ 재모니터링

## 19.3 고객사 개선 루프
미해결 질문 확인  
→ FAQ 등록 또는 문서 수정 요청  
→ 운영사/QA 승인  
→ 반영 후 성과 비교

---

# 20. 보안 및 거버넌스

## 20.1 필수 보안 통제
- tenant isolation
- 문서 visibility filter
- 내부문서 차단
- prompt injection 방어
- 금칙 응답 정책
- PII 마스킹
- audit logging
- category별 citation mandatory

## 20.2 시민 채널에서 차단해야 하는 문서 상태
- internal
- restricted
- draft
- deprecated
- expired

## 20.3 감사로그 대상
- 문서 업로드/수정
- FAQ 수정
- 공개범위 변경
- 정책 변경
- 재인덱싱 요청
- 권한 변경

---

# 21. PRD 초안 관점 정리

## 21.1 제품명(가칭)
CENTRAS AI GovChat Platform

## 21.2 제품 목적
공공기관이 보유한 다양한 지식자원(법령, 민원편람, 홈페이지, 실무편람 등)을 기반으로 시민 질문에 자동 응답하고, 운영사가 품질을 지속적으로 개선할 수 있는 RAG 기반 챗봇 운영 플랫폼 제공

## 21.3 핵심 기능 영역
1. 챗봇 서비스
2. RAG 데이터 관리
3. 분석/대시보드
4. QA/지식관리
5. 운영/관제

## 21.4 챗봇 서비스 기능
- 세션 관리
- 질문 로그 저장
- 질문 정규화
- 의도/카테고리 분류
- RAG 검색
- 답변 생성
- 출처 표시

## 21.5 RAG 데이터 관리 기능
- 문서 소스 관리
- 문서 버전 관리
- 크롤링 스케줄
- 변경 감지
- ingestion 단계 상태 조회
- 재인덱싱

## 21.6 분석/대시보드 기능
- 기관 성과 대시보드
- 민원 유형 분석
- 질문 분석
- 미해결 질문 분석
- 운영 대시보드

## 21.7 QA 기능
- 미응답/오답 검수
- FAQ 생성
- 문서 보강 요청
- 원인 분류
- 개선 이력

## 21.8 운영/관제 기능
- 기관 헬스맵
- 장애/이슈 관리
- RAG 파이프라인 관제
- 비용/사용량
- 리포트

---

# 22. 단계별 구축 로드맵

## 22.1 1단계: MVP
- 웹 챗봇
- 문서 업로드
- 기본 RAG
- 기본 citation
- 운영 대시보드 최소셋
- 고객사 대시보드 최소셋
- QA 검수 기본셋

## 22.2 2단계: 공공기관형 고도화
- 홈페이지 크롤링
- 민원편람 구조화
- 법령 동기화
- 출처/근거 강화
- 문서 최신성 관리
- 고위험 질문 관리

## 22.3 3단계: 멀티기관 SaaS
- 기관별 정책
- 기관별 검색 우선순위
- 통합 운영 관제
- 자동 리포트
- 비용/사용량 관리

## 22.4 4단계: 운영 자동화
- 변경 감지 자동 반영
- 이상탐지
- 재인덱싱 자동화
- prompt / retrieval A/B 테스트
- 품질 저하 알림

---

# 23. MVP 범위 제안

## 23.1 MVP에 반드시 포함할 기능
- 웹 채널 챗봇
- 문서 업로드 + FAQ
- 기본 하이브리드 RAG
- citation 출력
- 운영 대시보드
- 고객사 대시보드
- 미해결 질문 리스트
- QA 검수
- 재인덱싱 요청
- 기본 문서 메타데이터
- KPI 집계 테이블

## 23.2 후순위 가능 기능
- 다채널 확장
- 음성 챗봇
- 고급 이상탐지
- 자동 정책 추천
- 내부 공무원 전용 모드 세분화
- 고급 A/B 테스트

---

# 24. 남아 있는 상세 설계 과제

- 질문 taxonomy 표준화
- retrieval policy 세부 규칙
- 법령/편람/홈페이지 간 충돌 시 우선순위
- 문서 유형별 chunking 최적화
- 해결율 추정 모델 상세화
- 리포트 템플릿 표준화
- 권한/승인 플로우 상세 규정
- 화면별 API 응답 스키마 구체화

---

# 25. 최종 정리

이번에 논의한 플랫폼은 단순 챗봇이 아니다.

**“공공기관의 다양한 지식원을 기반으로 시민 민원 응대를 자동화하고, 운영사와 기관 담당자가 성과·품질·최신성·근거성을 함께 관리하는 RAG 운영 플랫폼”**

핵심 성공 조건은 4가지다.

## 25.1 문서 소스 관리
홈페이지, 법령, 민원편람, 실무편람, FAQ, 공지 등 소스를 구분해 관리해야 한다.

## 25.2 운영 중심 Admin
운영사 / 고객사 / QA 화면을 분리해 역할에 맞는 액션이 가능해야 한다.

## 25.3 품질 개선 루프
질문 → 검수 → FAQ/문서 보강 → 재인덱싱 → 재측정 구조가 있어야 한다.

## 25.4 근거와 최신성 보장
공공기관 챗봇은 생성 품질보다 출처, 최신성, 공개범위 통제가 더 중요하다.
