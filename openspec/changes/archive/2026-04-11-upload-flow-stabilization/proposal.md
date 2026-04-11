# Proposal: upload-flow-stabilization

## Problem

`/ops/upload` 기준의 실제 운영 흐름(기관/서비스 선택 → 업로드/크롤 등록 → ingestion job 시작)이 UI와 운영 절차에서 분산되어 있다. 또한 admin-api가 Flyway 체크섬 불일치로 멈추는 상태가 발생해 E2E 검증이 막힌다.

현재 breadcrumb 전역 필터는 기관/날짜만 제공하고 서비스 필터가 없어, `/ops/upload`를 포함한 Ops 화면에서 기관-서비스 컨텍스트를 일관되게 유지하기 어렵다.

## Proposed Solution

1. admin-api 복구 절차를 확정하고 검증 가능한 runbook으로 문서화한다.
2. `/ops/upload`는 페이지 내부 기관/서비스 선택 대신 breadcrumb 전역 필터를 사용하도록 변경한다.
3. breadcrumb 필터를 기관/날짜에서 기관/서비스/날짜로 확장한다.
4. 업로드 및 리인덱싱 동작을 org+service 컨텍스트 기준으로 검증한다.

## Out of Scope

- 서비스 필터를 사용한 전 페이지 백엔드 쿼리 최적화
- 신규 백엔드 API 추가
- 업로드 페이지 외 신규 UX 디자인 리뉴얼

## Success Criteria

- admin-api가 로컬 환경에서 정상 기동되고 `/actuator/health`가 `UP`이다.
- `/ops/upload`에서 org+service 선택 후 파일 업로드 시 ingestion job이 생성된다.
- breadcrumb 필터에서 기관 선택 후 서비스를 선택할 수 있다.
- Ops의 org 필터 사용 화면에서 서비스 필터가 노출된다.
