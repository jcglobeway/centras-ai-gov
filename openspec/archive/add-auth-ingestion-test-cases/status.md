# Status

- 상태: `completed`
- 시작일: `2026-03-15`
- 완료일: `2026-03-15`
- 마지막 업데이트: `2026-03-15`

## Progress

- ✅ OpenSpec change 생성 완료
- ✅ 기존 테스트 분석 완료 (AdminApiApplicationTests.kt 19개 테스트)
- ✅ 6개 테스트 추가 완료
  - GET /admin/crawl-sources/{id}: 성공, 404, 권한 범위
  - GET /admin/ingestion-jobs/{id}: 성공, 404, 권한 범위
- ✅ 전체 테스트 통과 (25개)

## Verification

- `./gradlew test`: BUILD SUCCESSFUL
- 테스트 개수: 19 → 25개 (6개 추가)
- 모든 시나리오 통과 확인

## Risks

- 없음
