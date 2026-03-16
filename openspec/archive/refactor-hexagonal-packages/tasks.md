# Tasks

## refactor-hexagonal-packages

### Phase 1: 모듈 내부 패키지 분리 (7개 모듈)

- [ ] organization-directory: domain/ + port/out/ + adapter/persistence/ 분리
- [ ] identity-access: domain/ + port/out/ + adapter/persistence/ 분리
- [ ] ingestion-ops: domain/ + port/out/ + adapter/persistence/ 분리
- [ ] qa-review: domain/ + port/out/ + adapter/persistence/ 분리
- [ ] chat-runtime: domain/ + port/out/ + adapter/persistence/ 분리
- [ ] document-registry: domain/ + port/out/ + adapter/persistence/ 분리
- [ ] metrics-reporting: domain/ + port/out/ + adapter/persistence/ 분리

### Phase 2: 크로스 모듈 파일 업데이트

- [ ] RepositoryConfiguration.kt — 와일드카드 import를 서브패키지 경로로 교체
- [ ] AdminApiApplication.kt — @EnableJpaRepositories, @EntityScan basePackages 업데이트 (재귀 스캔이므로 선택사항)
- [ ] Controller 파일들 — import 경로 업데이트

### Phase 3: ArchUnit 규칙 업그레이드

- [ ] ArchitectureTest.kt — 명명 규칙 기반 → 패키지 경로 기반 규칙으로 교체

### Phase 4: 검증

- [ ] ./gradlew compileKotlin — 컴파일 오류 없음
- [ ] ./gradlew :apps:admin-api:test — 43개 테스트 전체 통과
