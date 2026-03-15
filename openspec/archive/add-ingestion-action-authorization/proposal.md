# Proposal

## Change ID

`add-ingestion-action-authorization`

## Summary

- ingestion API가 역할 코드 분기 대신 액션 기반 권한 정책을 사용하도록 바꾼다.
- `identity-access` 모듈에 재사용 가능한 권한 정책 클래스를 추가한다.
- `admin-api` ingestion 조회/쓰기 엔드포인트와 테스트를 새 정책 기준으로 정리한다.

## Impact

- 영향 모듈: `modules/identity-access`, `apps/admin-api`
- 영향 API: `GET /admin/crawl-sources`, `GET /admin/ingestion-jobs`, `POST /admin/crawl-sources`, `POST /admin/crawl-sources/{id}/run`, `POST /admin/ingestion-jobs/{id}/status`
- 영향 테스트: `apps/admin-api` MockMvc 테스트

## Done Definition

- ingestion API가 액션 코드와 조직 범위 모두를 검증한다.
- 권한 정책이 `identity-access` 모듈 경계 안에 위치한다.
- 권한 부족 케이스 테스트가 추가된다.
