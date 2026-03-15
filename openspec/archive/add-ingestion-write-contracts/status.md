# Status

- 상태: `completed`
- 시작일: `2026-03-15`
- 마지막 업데이트: `2026-03-15`

## Progress

- ingestion 조회 계약과 문서 요구를 다시 확인했다.
- 이번 change 범위를 `쓰기 계약`, `상태 전이`, `admin-api 개발용 엔드포인트`, `MockMvc 테스트`로 고정했다.
- `modules/ingestion-ops`에 crawl source 생성, job 생성, job 상태 전이 계약을 추가했다.
- `admin-api`에 source 생성, 수동 실행, job 상태 전이 엔드포인트를 추가했다.
- 개발용 저장소를 읽기/쓰기 공용 인메모리 store 로 재구성하고 조직 범위 검증을 연결했다.

## Verification

- `.\gradlew.bat test`

## Risks

- 현재는 인메모리 저장소라 프로세스 재시작 시 데이터가 유지되지 않는다.
- 권한 미들웨어는 아직 도입 전이라 세션 범위 검증만 우선 적용한다.
