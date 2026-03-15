# Proposal

## Change ID

`add-e2e-auth-ingestion-flow`

## Summary

### 변경 목적
- 전체 운영 플로우를 검증하는 E2E 테스트 추가
- 단위/API 테스트로는 검증하기 어려운 통합 시나리오 커버
- 회귀 방지 및 리팩토링 안정성 확보

### 변경 범위
- **E2E 테스트 시나리오**:
  1. 인증 플로우: login → 세션 복원 → logout → 만료 확인
  2. Ingestion 플로우: crawl source 생성 → job 실행 → 상태 전이 → 완료
  3. 권한 시나리오: 범위 밖 리소스 접근 → 403/404
  4. 멀티테넌트 격리: org별 데이터 격리 확인

- **테스트 구조**:
  - tests/e2e/ 디렉터리 활용
  - Kotlin + JUnit + Spring Boot Test
  - @SpringBootTest + @Transactional (롤백)

### 제외 범위
- Python worker 실제 실행 (별도 통합 테스트)
- 브라우저 기반 UI 테스트
- 성능 테스트
- 부하 테스트

## Impact

### 영향 모듈
- `tests/e2e`: 새 테스트 파일 추가

### 영향 API
- 영향 없음 (기존 API 검증)

### 영향 테스트
- 기존 25개 유지
- E2E 테스트 4-5개 추가

## Done Definition

- [x] E2E 테스트 1: 전체 인증 플로우 (login → session → logout → revoked)
- [x] E2E 테스트 2: crawl source 생성 → job 실행 → 상태 전이 → 완료
- [x] E2E 테스트 3: 권한 범위 위반 시나리오 (client_admin 제한)
- [x] E2E 테스트 4: 멀티테넌트 격리 확인 (ops vs client)
- [x] ./gradlew test 전체 통과 (29 tests)
- [x] 테스트 개수 확인: 25 → 29개 (4개 추가)
