# Proposal

## Change ID

`naming-structure-cleanup`

## Summary

파일명·폴더 구조 점검에서 발견된 5개 비일관성을 제거한다.

- **변경 목적**: 파일명 = 클래스명 원칙, port.in/out 대칭, 레이어 구조 일관성 확보
- **변경 범위**:
  - P1: 어댑터 파일명을 내부 클래스명과 일치시킴 (20개 rename)
  - P2: `port/out/` 단일 묶음 파일을 포트별 개별 파일로 분리
  - P3: `apps/admin-api/auth/` 하위에 `adapter/inbound/web/` 계층 구조 적용
  - P4: `RagOrchestratorClient`, `MetricsAggregationScheduler`, `HealthController` 적절한 위치로 이동
  - P5: `*Domain.kt` 단일 파일을 타입별 개별 파일로 분리
- **제외 범위**: 비즈니스 로직 변경 없음, DB 마이그레이션 없음, API 계약 불변

## Impact

- **영향 모듈**: 전 모듈 (파일명/패키지 경로 변경)
- **영향 API**: 없음 (HTTP 계약 불변)
- **영향 Bean 등록**: `RepositoryConfiguration` — 클래스명 참조 확인 필요
- **영향 테스트**: import 경로 수정 필요 가능성 있음
- **ArchUnit**: 패키지 경로가 바뀌지 않으므로 규칙 영향 없음

## Done Definition

- 모든 어댑터 파일명이 내부 클래스명과 일치함
- `port/out/` 파일이 포트별 개별 파일로 분리됨
- `auth/` 컨트롤러가 `adapter/inbound/web/` 하위에 위치함
- `RagOrchestratorClient` → `chatruntime/adapter/outbound/http/` 위치
- `MetricsAggregationScheduler` → `metrics/adapter/inbound/scheduler/` 위치
- `*Domain.kt` 가 타입별 개별 파일로 분리됨
- `./gradlew test` 전체 통과
