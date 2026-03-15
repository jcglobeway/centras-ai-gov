# Proposal

## Change ID

`add-ingestion-write-contracts`

## Summary

- ingestion 영역에 `crawl source` 등록과 수동 실행 요청을 위한 쓰기 계약을 추가한다.
- ingestion job 상태 전이 규칙을 모듈 안에 고정하고 API에서 같은 규칙을 사용한다.
- `admin-api`에 개발용 쓰기 엔드포인트를 추가해 문서상 ingestion 운영 흐름을 코드로 연결한다.

제외 범위:
- 실제 DB 저장소 구현
- Python worker callback 연동
- 권한 미들웨어의 정식 도입

## Impact

- 영향 모듈: `modules/ingestion-ops`, `apps/admin-api`
- 영향 API: `POST /admin/crawl-sources`, `POST /admin/crawl-sources/{id}/run`, `POST /admin/ingestion-jobs/{id}/status`
- 영향 테스트: `apps/admin-api` MockMvc 회귀 테스트

## Done Definition

- ingestion 쓰기 계약과 상태 전이 규칙이 모듈에 추가된다.
- `admin-api`에서 source 생성, job 생성, 상태 전이 API가 동작한다.
- 허용/금지 상태 전이가 테스트로 검증된다.
