# Status

- 상태: `completed`
- 시작일: `2026-03-15`
- 마지막 업데이트: `2026-03-15`

## Progress

- `identity-access` 모듈에 액션 기반 권한 정책을 추가했다.
- ingestion 조회/쓰기 엔드포인트가 동일한 정책과 조직 범위 검증을 사용하도록 정리했다.
- 권한 부족 케이스를 MockMvc 테스트로 보강했다.

## Verification

- `admin-api` ingestion 컨트롤러와 현재 세션 모델 확인 완료
- `.\gradlew.bat test` 통과

## Risks

- 이후 `auth` 전반으로 정책을 확장할 때 공통 예외 응답 포맷을 함께 정리해야 한다.
