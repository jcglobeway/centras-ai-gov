# Tasks

## 사전 확인
- [ ] 현재 `RoleCode` enum 위치 확인 (`identity-access` 모듈)
- [ ] `AdminSessionSnapshot` JSON 직렬화 구조 확인
- [ ] 기존 시드 데이터 role 값 확인 (V001 마이그레이션)

## 구현
- [ ] `RoleCode` enum에 `super_admin`, `client_viewer`, `knowledge_editor` 추가
- [ ] `qa_admin` → `qa_manager`, `client_admin` → `client_org_admin` alias/rename 처리
- [ ] `globalAccess` 판단 로직: `super_admin`, `ops_admin`만 true
- [ ] V020 SQL 마이그레이션 작성 (신규 role 시드 insert)
- [ ] `application.yml` (test) `spring.flyway.target` → `"20"` 업데이트

## 테스트
- [ ] `./gradlew test` 전체 통과 확인
- [ ] role 기반 scope 분기 동작 확인

## 완료
- [ ] `status.md` 업데이트
- [ ] 커밋
