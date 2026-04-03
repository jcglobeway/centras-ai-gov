# Proposal

## Change ID

`enhance-audit-log`

## Context (왜 이 기능이 중요한가)

이 페이지의 목적은 **이미 일어난 보안 이슈를 추적하고 증거를 남기는 것**이다. 실시간 대응보다 사후 감사(audit trail) 성격이 강하다.

세 섹션 중 **PII 감지 이벤트가 핵심**이다. 이유는 세 가지다.

1. **법적 리스크 직결** — 개인정보 유출은 1건이라도 발생하면 즉각 대응이 필요하고 기록이 남아야 한다.
2. **외부 보고 의무** — 관리자 접근·금칙어 차단은 운영 참고용이지만, PII는 외부 보고 의무가 생길 수 있다.
3. **명확한 액션 체인** — 감지 → 마스킹 → 규칙 수정으로 이어지는 대응 흐름이 명확하다.

관리자 접근 이력과 금칙어 차단 로그는 있어야 하지만, 평소엔 잘 안 보는 참고용이다.

## Summary

- **목적**: PII 감지를 RAG 파이프라인(입력·출력 양방향)에 실제로 연동하고, 감사 로그 API에 필터링·export 기능을 추가해 `/ops/audit` 화면을 E2E로 완결한다.
- **배경**: `/ops/audit` 페이지는 이미 존재하지만 PII·금칙어 섹션은 MOCK 데이터이고, 실제 감지 로직이 없어 audit trail 자체가 무의미한 상태다. PRD v3 §2-9 (안전성/컴플라이언스) 요구사항 대비 미구현.
- **변경 범위**:
  - **Python (rag-orchestrator)**: Presidio 기반 PII 스캐너 — 입력(질문→LLM 전송 전) + 출력(LLM 답변→반환 전) 양방향 스캔·마스킹, 감지 시 `POST /admin/audit-logs` 기록
  - **Kotlin (admin-api)**: `GetAuditLogsUseCase` 필터 파라미터 추가, `POST /admin/audit-logs` 외부 기록용 엔드포인트 신규, CSV export 엔드포인트 신규
  - **프론트**: 날짜·액션 유형·조직 필터 UI, PII 섹션 실데이터 연동, CSV 다운로드 버튼
- **제외 범위**:
  - 금칙어 사전 관리 CRUD (별도 작업)
  - 신규 DB 테이블 없음 (기존 `audit_logs` V003 활용)

## Impact

- **영향 모듈**: `modules/identity-access` (UseCase·Service·Adapter), `apps/admin-api` (Controller), `python/rag-orchestrator` (PII 스캐너)
- **영향 API**:
  - `GET /admin/audit-logs` — 필터 파라미터 추가 (from, to, action_code, organization_id, actor_user_id)
  - `GET /admin/audit-logs/export.csv` — 신규
  - `POST /admin/audit-logs` — 신규 (rag-orchestrator → admin-api PII 이벤트 기록용)
- **영향 파일**:
  - `python/rag-orchestrator/` — `pii_scanner.py` 신규, `main.py` 또는 `retrieval.py` 수정
  - `modules/identity-access/src/.../application/port/in/GetAuditLogsUseCase.kt`
  - `modules/identity-access/src/.../application/service/GetAuditLogsService.kt`
  - `modules/identity-access/src/.../adapter/outbound/persistence/LoadAuditLogPortAdapter.kt`
  - `apps/admin-api/src/.../auth/adapter/inbound/web/AuditLogController.kt`
  - `frontend/src/app/ops/audit/page.tsx`
- **영향 테스트**: 기존 `AuthApiTests.kt` — audit-log 필터·export 테스트 케이스 추가

## Done Definition

- rag-orchestrator: 질문 입력 시 PII 감지 → 마스킹된 질문이 LLM에 전달됨
- rag-orchestrator: LLM 답변 반환 전 PII 감지 → 마스킹된 답변이 사용자에게 반환됨
- PII 감지 시 `POST /admin/audit-logs` 호출 → `audit_logs` 테이블에 `PII_DETECTED` 행 삽입
- 한국어 인식: 주민번호(000000-0000000), 전화번호(010-xxxx-xxxx), 이메일 패턴 커스텀 recognizer 동작
- `GET /admin/audit-logs?from=2026-01-01&to=2026-12-31&action_code=PII_DETECTED` 필터 정상 동작
- `GET /admin/audit-logs?actor_user_id=usr_xxx&organization_id=org_yyy` 조합 필터 동작
- `GET /admin/audit-logs/export.csv` → Content-Type: text/csv 다운로드
- 프론트 PII 감지 섹션: 실제 감지 이벤트 실데이터 표시 (MOCK 제거)
- 프론트 날짜 범위 선택 → 목록 재조회 (SWR key 변경)
- 프론트 CSV export 버튼 동작
- 통합 테스트 추가 통과 (`./gradlew test`)