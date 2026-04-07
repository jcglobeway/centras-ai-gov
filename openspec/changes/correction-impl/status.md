# Status

- 상태: `planned`
- 시작일: `2026-04-07`
- 마지막 업데이트: `2026-04-07`

## Progress

- proposal.md, tasks.md, status.md 초안 작성 완료
- 구현 미시작

## Verification

- 아직 없음

## Risks

- `answer_corrections` 테이블이 V050으로 추가됨. 테스트 환경 `flyway.target`이 "46"으로 고정되어 있으므로, 신규 마이그레이션을 테스트가 인식하도록 `application-test.yml`의 `flyway.target` 값을 "50"으로 올려야 함
- `ManageFeedbackUseCase`와 동일 패턴으로 구현하므로 ArchUnit 충돌 위험은 낮음
- 프론트 `Feedback` 인터페이스 수정 시 다른 페이지에서 해당 타입을 재사용하는지 사전 확인 필요
