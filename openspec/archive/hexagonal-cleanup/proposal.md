# Proposal

## Change ID

`hexagonal-cleanup`

## Summary

헥사고날 아키텍처 점검 결과 발견된 5개 비일관성을 제거한다.

- **변경 목적**: 모든 모듈이 동일한 헥사고날 패턴(의존성 방향, 포트 명명, 레이어 책임)을 따르도록 정렬
- **변경 범위**:
  - P1: `organization-directory` 레거시 포트(`OrganizationRepository`, `ServiceRepository`) 및 어댑터 제거
  - P2: `QuestionController`의 날짜 필터링 로직을 `ListQuestionsService`로 이동
  - P3: `GetOrganizationsUseCase`를 스코프 패턴으로 통일 (`getByIds` + `listAll` → `listOrganizations(scope)`)
  - P4: Auth 모듈에 UseCase 인터페이스 도입 (`AdminAuthUseCase`)
  - P5: `domain` 패키지에 있는 Command 객체를 `application/port/in/` 으로 이동
- **제외 범위**: DB 마이그레이션, 외부 API 계약, 테스트 삭제 없음

## Impact

- **영향 모듈**:
  - `modules/organization-directory`
  - `modules/chat-runtime`
  - `modules/identity-access` (auth 포트)
  - `modules/ingestion-ops` (Command 이동)
  - `modules/qa-review` (Command 이동)
  - `modules/document-registry` (Command 이동)
  - `modules/metrics-reporting` (Command 이동)
- **영향 API**: 없음 (HTTP 계약 불변)
- **영향 Bean 등록**: `RepositoryConfiguration`, `ServiceConfiguration`
- **영향 테스트**: 패키지 경로 변경으로 import 수정 필요

## Done Definition

- `OrganizationRepository`, `ServiceRepository` 인터페이스·어댑터·Bean 등록 삭제 후 컴파일 통과
- 날짜 필터링이 `ListQuestionsService`에 위치하고 Controller에서 제거됨
- `OrganizationController`가 단일 `listOrganizations(scope: OrganizationScope)` 메서드를 호출함
- `AuthCommandController`가 UseCase 인터페이스를 주입함
- Command 객체가 `application/port/in/` 하위에 위치함
- `./gradlew test` 전체 통과
