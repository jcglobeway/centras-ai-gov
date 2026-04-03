# Status: testcontainers-migration

## 상태: ✅ 완료 (2026-04-03)

## 완료 항목

- [x] `build.gradle.kts`: H2 제거, testcontainers 3종 추가, `tasks.withType<Test>` Colima 환경 설정
- [x] `BaseApiTest.kt`: `pgvector/pgvector:pg16` 싱글턴 컨테이너 패턴 (`@Testcontainers` + `@ServiceConnection`)
- [x] `application-test.yml`: H2 datasource/dialect 설정 제거, flyway target `"43"` 으로 갱신
- [x] `V018__EnablePgVector.kt`: H2 early return 제거
- [x] `V029__add_question_embedding.sql`: `TEXT` → `vector(1024)` 직접 사용
- [x] `V030__QuestionEmbeddingVector.kt`: H2 early return + ALTER 제거, HNSW 인덱스만 유지
- [x] `V031__reseed_missing_demo_data.sql`: 모든 INSERT에 `ON CONFLICT (id) DO NOTHING` 추가
- [x] `LoadCrawlSourcePortAdapter.kt` / `LoadIngestionJobPortAdapter.kt`: `Sort.by("id")` 추가 (PostgreSQL 비결정 순서 대응)
- [x] 54개 테스트 전부 통과 확인

## 이슈 및 해결

| 이슈 | 원인 | 해결 |
|------|------|------|
| V031 duplicate key | `qa_reviews` INSERT에 ON CONFLICT 누락 | ON CONFLICT (id) DO NOTHING 추가 |
| citation_correctness 컬럼 없음 | flyway target이 V38로 낮아 V042 미적용 | target을 "43"으로 상향 |
| PostgreSQL 정렬 비결정성 | `findAll()` 순서 보장 없음 | Sort.by("id") 추가 |
| Colima Docker 소켓 미인식 | DOCKER_HOST 환경변수 미설정 | tasks.withType<Test> 블록에 명시 |
| Ryuk 마운트 실패 | Colima 소켓 경로 제약 | TESTCONTAINERS_RYUK_DISABLED=true |

## 진행 이력

| 날짜 | 내용 |
|------|------|
| 2026-04-03 | proposal.md, tasks.md 초안 작성 |
| 2026-04-03 | 전체 구현 완료, 54개 테스트 통과 |
