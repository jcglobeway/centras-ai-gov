# Status

- 상태: `planned`
- 시작일: `2026-04-03`
- 마지막 업데이트: `2026-04-03`

## Progress

- proposal 작성 완료

## Verification

- 미실행

## Risks

- **Presidio 한국어 인식률**: 영어 기준으로 설계되어 한국어 주민번호·계좌번호는 커스텀 recognizer 없이 미탐지. 패턴 커버리지 사전 검증 필요.
- **rag-orchestrator → admin-api 호출 지연**: PII 감지 이벤트 기록이 동기 호출이면 응답 레이턴시 증가 가능 → 비동기(fire-and-forget) 처리 검토
- **POST /admin/audit-logs 인증**: ~~세션 기반 인증 불가~~ → `POST /admin/rag-search-logs`가 세션 검증 없이 동작하는 패턴 확인. 동일하게 적용 가능. **해소됨.**
- CSV export 응답이 대용량일 경우 스트리밍 처리 필요
