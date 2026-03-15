# Tasks

## 계획 단계
- [x] mvp_docs/12_test_strategy.md 확인
- [x] 현재 AdminApiApplicationTests 패턴 확인
- [x] E2E 시나리오 설계

## E2E 테스트 추가
- [x] AdminApiApplicationTests에 E2E 시나리오 추가:
  - [x] 전체 인증 플로우 (login → me → logout → revoked 확인)
  - [x] Ingestion 전체 플로우 (source 생성 → job 실행 → 전이 → 완료 → source 상태 업데이트)
  - [x] 권한 범위 위반 (client_admin의 범위 밖 접근, 쓰기 권한 확인)
  - [x] 멀티테넌트 격리 (ops vs client, org별 데이터 분리)

## 검증
- [x] ./gradlew test 전체 통과
- [x] 테스트 개수 확인: 25 → 29개 (4개 E2E 테스트 추가)

## 마무리
- [ ] 99_worklog.md 갱신
- [ ] status.md 완료 상태로 갱신
- [ ] proposal.md Done Definition 업데이트
- [ ] change를 archive로 이동
- [ ] 커밋 (한글 메시지)
