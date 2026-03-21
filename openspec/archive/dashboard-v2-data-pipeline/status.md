# Status: dashboard-v2-data-pipeline

- 상태: `in_progress`
- 시작일: `2026-03-21`
- 마지막 업데이트: `2026-03-21`

## Progress

- OpenSpec 작성 완료 → archive 이동

## Verification

- (미실행)

## Risks

- V027에서 INSERT하는 공공 민원 Q&A는 ZIP 파일 샘플 추출 결과에 따라 내용이 달라질 수 있음
- H2 테스트 환경에서 `flyway.target` 숫자를 "27"로 올릴 때 기존 테스트 FK 제약 충돌 가능 → V027 작성 시 순서 주의
