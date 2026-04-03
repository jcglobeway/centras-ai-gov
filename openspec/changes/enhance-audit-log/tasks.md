# Tasks — enhance-audit-log

## Python (rag-orchestrator)

- [ ] P1: `pii_scanner.py` 신규 작성
  - `pip install presidio-analyzer presidio-anonymizer` 의존성 추가
  - 한국어 커스텀 recognizer: 주민번호(`\d{6}-[1-4]\d{6}`), 전화번호(`01[016789]-?\d{3,4}-?\d{4}`), 계좌번호
  - `scan_and_mask(text: str) -> tuple[str, list[dict]]` — 마스킹된 텍스트 + 감지 정보 반환

- [ ] P2: `retrieval.py` (또는 `main.py`) 수정 — 입력 PII 차단
  - 질문 수신 후, pgvector 검색 전에 `scan_and_mask(question)` 실행
  - 마스킹된 질문으로 LLM 호출
  - PII 감지 시 `POST {ADMIN_API_BASE_URL}/admin/audit-logs` 호출 (action_code=`PII_DETECTED`, resource_type=`question_input`)

- [ ] P3: `app.py generate_answer()` 수정 — 출력 PII 차단
  - `answer_text = llm_result["content"]` 직후 `scan_and_mask(answer_text)` 실행
  - 마스킹된 답변 반환
  - PII 감지 시 `_log_pii_event()` 호출 (resource_type=`answer_output`)
  - Redis 캐시 저장(line 192) 시 **마스킹된 텍스트**로 저장 (원본 아님)

- [ ] P4: `app.py generate_answer_stream()` 수정 — 스트리밍 경로도 동일 적용
  - 입력: `hybrid_search()` 호출 전 질문 마스킹
  - 출력: 스트리밍 완료 후 전체 토큰 조합본에 `scan_and_mask` 적용은 어려우므로 **입력 스캔만** 적용

- [ ] P5: `_log_pii_event()` 헬퍼 함수 추가 (`app.py`)
  - `_log_search_result()`와 동일한 fire-and-forget 패턴 (httpx.post + timeout=3.0 + except pass)
  - `POST {ADMIN_API_BASE_URL}/admin/audit-logs` 호출
  - body: `{actionCode: "PII_DETECTED", organizationId, resourceType, resultCode: "masked"}`

## 백엔드 (admin-api)

- [ ] T1: `AuditLogFilter` 데이터 클래스 추가
  - 위치: `identity-access/application/port/in/`
  - 필드: `from: LocalDate?`, `to: LocalDate?`, `actionCode: String?`, `organizationId: String?`, `actorUserId: String?`

- [ ] T2: `GetAuditLogsUseCase` 시그니처 변경
  - `fun list(filter: AuditLogFilter, page: Int, pageSize: Int): GetAuditLogsResult`

- [ ] T3: `GetAuditLogsService` 필터 전달 구현

- [ ] T4: `LoadAuditLogPortAdapter` 쿼리 확장
  - native SQL WHERE 조건 동적 추가 (from/to/action_code/organization_id/actor_user_id)

- [ ] T5: `AuditLogController` 수정
  - `GET /admin/audit-logs` — 필터 파라미터 바인딩
  - `POST /admin/audit-logs` — 외부 서비스(rag-orchestrator)가 이벤트 기록용으로 호출하는 엔드포인트 신규
  - `GET /admin/audit-logs/export.csv` — CSV 스트리밍 응답 신규 추가

- [ ] T6: `RecordAuditLogUseCase` + `RecordAuditLogService` 신규
  - `POST /admin/audit-logs` body → `audit_logs` 테이블 삽입
  - `RecordAuditLogPortAdapter` 기존 어댑터 확장 또는 신규

- [ ] T7: `V032__audit_log_seed_security_events.sql` — 시드 삭제
  - 실제 Presidio 감지 이벤트가 쌓이므로 시드 불필요
  - (H2 테스트용 최소 시드 1~2건만 유지)

## 프론트엔드

- [ ] T8: `audit/page.tsx` — 필터 UI 추가
  - 날짜 범위 (from/to `input[type=date]`), action_code Select, organization_id Select
  - 필터 변경 시 SWR key 업데이트

- [ ] T9: `audit/page.tsx` — PII 섹션 실데이터 교체
  - `MOCK_PII_EVENTS` 제거
  - `useSWR("/api/admin/audit-logs?action_code=PII_DETECTED&page_size=10")` 연동

- [ ] T10: `audit/page.tsx` — 금칙어 섹션 실데이터 교체
  - `useSWR("/api/admin/audit-logs?action_code=BLOCKLIST_HIT&page_size=10")` 연동

- [ ] T11: `audit/page.tsx` — CSV export 버튼 추가
  - 현재 필터 파라미터를 `/api/admin/audit-logs/export.csv?...`에 그대로 전달

## 테스트

- [ ] T12: 통합 테스트 추가
  - `POST /admin/audit-logs` → DB 삽입 확인
  - `action_code=PII_DETECTED` 필터 → 해당 행만 반환 확인
  - 날짜 범위 필터 동작 확인
  - `/export.csv` → 200 + `text/csv` Content-Type 확인
  - `./gradlew test` 전체 통과 확인

- [ ] T13: Presidio 한국어 recognizer 검증
  - 주민번호·전화번호·이메일 패턴 단위 테스트 (pytest)
  - 마스킹 후 원본 패턴이 남지 않는지 확인
