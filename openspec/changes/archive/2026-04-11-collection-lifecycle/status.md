# Status: collection-lifecycle

- 상태: `validated`
- 시작일: `2026-04-07`
- 마지막 업데이트: `2026-04-10`

## Progress

- OpenSpec 아티팩트(proposal, tasks, status) 작성 완료
- Phase 1: V048 마이그레이션 생성 완료
- Phase 2: 도메인/퍼시스턴스 확장 완료
- Phase 3: DeleteCollectionChunks 포트/서비스/어댑터 구현 완료
- Phase 4: CollectionController, DocumentUploadController 수정 완료
- Phase 5: Bean 등록 완료
- Phase 6: 프론트엔드 upload/page.tsx 수동 재인덱싱 섹션에 버튼 추가 완료

## Verification

- `./gradlew :apps:admin-api:compileKotlin` 성공
- 기존 53개 테스트 통과 (QAReviewApiTests.qa_review_list_can_be_filtered 1건 pre-existing 실패, 무관)
- ArchUnit 8개 규칙 통과
- `./gradlew :apps:admin-api:test --tests '*FullFlowE2ETests.e2e delete collection chunks resets indexed documents*'` 성공
- 신규 E2E에서 `DELETE /admin/collections/chunks` 호출 후 `deletedChunks=2`, `resetDocuments=1`, 문서 상태 `pending/not_indexed` 검증

## Risks

- `documents` 테이블에 crawl_source_id를 backfill하지 않으면 기존 청크는 컬렉션 삭제 API로 제거할 수 없음 — 신규 업로드분부터만 적용됨을 사용자에게 고지 필요
- native DELETE 쿼리 사용 시 JPA 1차 캐시와 동기화 문제 발생 가능 — `@Modifying(clearAutomatically = true)` 필수
- ArchUnit Rule 4: `CollectionController`가 persistence 어댑터를 직접 참조하지 않도록 주의
