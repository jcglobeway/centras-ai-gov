# Proposal: redteam-caseset

## Problem

`/ops/redteam` 페이지는 프론트엔드에 이미 존재하지만 100% 목업 데이터 하드코딩 상태이며,
백엔드(DB, API, 도메인 모델)가 전무하다. IA 설계(4-5 레드팀 케이스셋)에 완전히 명세된
기능임에도 실제 동작하지 않아 운영에서 사용 불가하다.

현재 문제:
- 케이스 등록/편집/삭제 불가 (버튼 disabled 상태)
- 일괄 실행 버튼 disabled — RAG orchestrator 연동 없음
- 실행 이력 하드코딩 (DB 없음)
- 방어율이 항상 75%로 고정

## Proposed Solution

헥사고날 아키텍처 패턴(qa-review 모듈 기준)을 따라 `redteam` bounded context를
신규 Spring Boot 모듈로 구현하고, 프론트엔드의 목업을 실제 API 호출로 교체한다.

### 핵심 설계 결정

1. **신규 Gradle 모듈**: `modules/redteam/` — qa-review와 동일한 헥사고날 패키지 구조
2. **판정 방식**: rule-based (LLM 없음). 카테고리별 키워드/패턴 매칭으로 응답 분류
   - `pii_induction`: 개인정보 관련 응답 여부 감지
   - `out_of_domain`: `answer_status = 'no_answer'` 또는 fallback 감지
   - `prompt_injection`: 시스템 프롬프트 노출 여부 감지
   - `harmful_content`: 금칙어 목록 매칭
3. **일괄 실행**: RAG orchestrator에 케이스별 질의 투입 → 응답 수신 → rule 판정 → batch 결과 저장
4. **Flyway**: V050 (redteam_cases), V051 (redteam_batch_runs, redteam_case_results)

## Out of Scope

- CI/CD 실제 연동 (GitHub Actions webhook)
- LLM 기반 자동 판정 (GPT-4 judge 등)
- 케이스 임포트/익스포트 (CSV 일괄 업로드)
- 실시간 실행 진행 상황 SSE 스트리밍

## Success Criteria

- [ ] `/admin/redteam/cases` CRUD API 동작
- [ ] `/admin/redteam/batch-runs` POST → RAG orchestrator 질의 → 결과 저장
- [ ] `/admin/redteam/batch-runs` GET → 이력 조회
- [ ] 프론트 `/ops/redteam` 페이지: 목업 배지 제거, 실제 API 연동
- [ ] ArchUnit 8개 규칙 모두 통과
- [ ] 기존 50개 통합 테스트 회귀 없음
