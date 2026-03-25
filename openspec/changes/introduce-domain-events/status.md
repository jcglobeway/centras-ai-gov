# Status

- 상태: `planned`
- 시작일: 2026-03-22
- 마지막 업데이트: 2026-03-22

## Progress

- proposal, tasks, status 작성 완료

## Verification

- 미실행

## Risks

- `TransitionJobService`가 현재 전이 결과의 최종 status를 직접 확인하지 않으므로, `IngestionJobSummary.status`를 읽어 발행 여부를 판단하는 로직 필요
- 핸들러가 UseCase를 의존할 때 순환 빈 등록 발생 가능 — `@Lazy` 또는 `RepositoryConfiguration` 등록 순서 조정으로 해결
- `@TransactionalEventListener(phase = AFTER_COMMIT)` 사용 시 핸들러 예외가 원본 트랜잭션에 영향 없음 — 핸들러 실패 무시 여부를 명시적으로 처리 필요
