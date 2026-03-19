# Proposal

## Change ID

`extend-roles-6`

## Summary

- **변경 목적**: 현재 3개 역할(ops_admin, client_admin, qa_admin)을 planning_draft 기준 6개 역할로 확장
- **변경 범위**:
  - DB: V020 마이그레이션 — 신규 role 시드 추가, `client_admin` → `client_org_admin` rename
  - 도메인: `RoleCode` enum에 `super_admin`, `client_viewer`, `knowledge_editor` 추가
  - 권한 체계: 각 역할별 globalAccess/scope 적용 로직 업데이트
- **제외 범위**: 프론트엔드 메뉴 필터링, UI 역할 선택 화면

## Roles 정의

| 기존 | 신규 | globalAccess | 설명 |
|---|---|---|---|
| — | `super_admin` | true | 최상위 전체 접근 |
| `ops_admin` | `ops_admin` | true | 운영사 관리자 (유지) |
| `qa_admin` | `qa_manager` | false | 품질 관리자 (rename) |
| `client_admin` | `client_org_admin` | false | 고객사 어드민 (rename) |
| — | `client_viewer` | false | 고객사 조회 전용 |
| — | `knowledge_editor` | false | 지식 편집 (문서 편집만 가능) |

## Impact

- **영향 모듈**: `identity-access`, `apps/admin-api`
- **영향 API**: 세션 restore 시 role_code 역직렬화 → 신규 enum 값 인식
- **영향 테스트**: `AuthApiTests` — 시드 데이터 기반, role 검증 케이스

## Done Definition

- [ ] `RoleCode` enum 6개 역할 정의 (기존 3개 포함)
- [ ] V020 마이그레이션 작성 (신규 시드 + alias 처리)
- [ ] `AdminSessionSnapshot` 역직렬화 호환성 유지
- [ ] 권한 scope 분기 로직 업데이트 (globalAccess 판단)
- [ ] `./gradlew test` 전체 통과
