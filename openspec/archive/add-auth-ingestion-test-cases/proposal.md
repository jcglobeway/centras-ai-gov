# Proposal

## Change ID

`add-auth-ingestion-test-cases`

## Summary

### 변경 목적
- 최근 추가한 개별 조회 API에 대한 테스트 보강
- 권한 검증 시나리오 추가 (scope, forbidden)
- 회귀 방지를 위한 테스트 커버리지 확보

### 변경 범위
- `apps/admin-api/src/test` AdminApiApplicationTests.kt 확장:
  - GET /admin/crawl-sources/{id} 테스트 (성공, 404, 권한 범위)
  - GET /admin/ingestion-jobs/{id} 테스트 (성공, 404, 권한 범위)
  - 세션 기반 권한 검증 시나리오 추가
  - 존재하지 않는 리소스 404 검증

### 제외 범위
- tests/ 디렉터리 독립 테스트 (향후 별도 change로 진행)
- Python worker 테스트 (pytest, 별도 작업)
- E2E 시나리오 테스트 (Playwright 기반, 별도 작업)
- 성능 테스트

## Impact

### 영향 모듈
- `apps/admin-api`: 테스트 추가 (AdminApiApplicationTests.kt)

### 영향 API
- 테스트 대상 (기존 API, 영향 없음):
  - GET /admin/crawl-sources/{id}
  - GET /admin/ingestion-jobs/{id}

### 영향 테스트
- 기존 19개 테스트 유지
- 신규 4-6개 테스트 추가 예상

## Done Definition

- [x] 현재 테스트 구조 확인 (AdminApiApplicationTests.kt 19개 테스트 존재)
- [x] GET /admin/crawl-sources/{id} 성공 케이스 테스트
- [x] GET /admin/crawl-sources/{id} 404 케이스 테스트
- [x] GET /admin/crawl-sources/{id} 권한 범위 테스트
- [x] GET /admin/ingestion-jobs/{id} 성공 케이스 테스트
- [x] GET /admin/ingestion-jobs/{id} 404 케이스 테스트
- [x] GET /admin/ingestion-jobs/{id} 권한 범위 테스트
- [x] ./gradlew test 전체 통과
- [x] 테스트 개수 확인: 19 → 25개 (6개 추가)
