# Proposal

## Change ID

`refactor-hexagonal-packages`

## Summary

현재 7개 모듈의 코드가 모두 flat 패키지(예: `com.publicplatform.ragops.chatruntime.*`)에 혼재되어 있다.
CLAUDE.md에 선언한 헥사고날 아키텍처 가이드라인에 맞춰 각 모듈의 패키지를 레이어별로 분리한다.

**변경 범위**:
- 7개 Kotlin 모듈 전체 (identity-access, organization-directory, ingestion-ops, qa-review, chat-runtime, document-registry, metrics-reporting)
- `*Contracts.kt` → `domain/` + `port/out/` 서브패키지로 분리
- `*Entity.kt`, `Jpa*Repository.kt`, `*Adapter.kt` → `adapter/persistence/` 서브패키지로 이동
- `*StateMachine.kt` → `domain/` 서브패키지로 이동
- `AdminApiApplication.kt` — `@EnableJpaRepositories`, `@EntityScan` basePackages 업데이트
- `ArchitectureTest.kt` — 명명 규칙 기반 → 패키지 경로 기반 규칙으로 업그레이드

**제외 범위**:
- `apps/admin-api/…/*Controller.kt` — 이미 바운디드 컨텍스트별 서브패키지(`chatruntime/`, `ingestion/` 등)로 분리되어 있음. 변경 없음.
- Python 서비스, Flyway 마이그레이션 SQL

## 목표 패키지 구조

모든 모듈에 동일한 3-레이어 구조 적용:

```
com.publicplatform.ragops.<module>/
  domain/             ← data class, enum, StateMachine
  port/
    out/              ← Reader/Writer 인터페이스 (아웃바운드 포트)
  adapter/
    persistence/      ← *Entity, Jpa*Repository, *Adapter
```

### chat-runtime 예시 (가장 복잡한 모듈)

| 현재 파일 | 이동 후 패키지 |
|---|---|
| `ChatRuntimeContracts.kt` | 분리: data class → `domain/`, interface → `port/out/` |
| `ChatRuntimeModule.kt` | 그대로 (루트 패키지) |
| `QuestionEntity.kt` | `adapter/persistence/` |
| `JpaQuestionRepository.kt` | `adapter/persistence/` |
| `QuestionReaderAdapter.kt` | `adapter/persistence/` |
| `QuestionWriterAdapter.kt` | `adapter/persistence/` |
| `AnswerEntity.kt` | `adapter/persistence/` |
| `JpaAnswerRepository.kt` | `adapter/persistence/` |
| `AnswerReaderAdapter.kt` | `adapter/persistence/` |
| `AnswerWriterAdapter.kt` | `adapter/persistence/` |
| `ChatSessionEntity.kt` | `adapter/persistence/` |
| `FeedbackEntity.kt` | `adapter/persistence/` |
| `JpaFeedbackRepository.kt` | `adapter/persistence/` |
| `FeedbackReaderAdapter.kt` | `adapter/persistence/` |
| `FeedbackWriterAdapter.kt` | `adapter/persistence/` |
| `RagSearchLogEntity.kt` | `adapter/persistence/` |
| `RagRetrievedDocumentEntity.kt` | `adapter/persistence/` |
| `JpaRagSearchLogRepository.kt` | `adapter/persistence/` |
| `RagSearchLogWriterAdapter.kt` | `adapter/persistence/` |

### IngestionJobStateMachine / QAReviewStateMachine

State machine은 비즈니스 규칙이므로 `domain/` 패키지로 이동.

## ArchUnit 규칙 업그레이드

패키지 분리 완료 후 `ArchitectureTest.kt`의 명명 규칙 기반 규칙을 패키지 경로 기반으로 교체:

| 규칙 | 현재 (명명 기반) | 변경 후 (패키지 기반) |
|---|---|---|
| Rule 1 | `*Summary`, `*Command` → JPA 금지 | `..domain..` → `jakarta.persistence..` 접근 금지 |
| Rule 2 | `*Controller` → `*Entity` 금지 | `adapter.inbound.web..` → `adapter.persistence..` 직접 의존 금지 |
| Rule 3 | `Jpa*Repository` → Adapter/Config만 허용 | `..adapter.persistence..` 패키지 내부만 허용 |
| Rule 4 | 모듈 간 순환 금지 | 그대로 유지 |

## Impact

- **영향 모듈**: 7개 모듈 전체, `apps/admin-api` (AdminApiApplication, RepositoryConfiguration)
- **영향 파일 수**: 약 70개 Kotlin 파일 (package 선언 + import 변경)
- **영향 API**: 없음 (HTTP API 계약 불변)
- **영향 테스트**: `ArchitectureTest.kt` 규칙 업그레이드 필요, 기존 39개 API 테스트는 import만 변경

## Done Definition

- [ ] 7개 모듈 모두 `domain/`, `port/out/`, `adapter/persistence/` 서브패키지로 분리 완료
- [ ] `AdminApiApplication.kt` basePackages가 새 서브패키지 경로 반영
- [ ] `ArchitectureTest.kt` 패키지 경로 기반 규칙으로 업그레이드
- [ ] `./gradlew :apps:admin-api:test` 전체 통과 (43개 테스트)
- [ ] 컴파일 오류 없음

## 구현 전략

파일당 변경 절차:
1. 새 서브패키지 디렉토리에 파일 이동 (`package` 선언 변경)
2. 해당 파일을 참조하는 모든 파일의 `import` 업데이트
3. 모듈 완료 시마다 컴파일 확인 (`./gradlew compileKotlin`)
4. 전체 테스트 통과 확인

**진행 순서** (의존성 역순):
1. `shared-kernel` (변경 없음)
2. `organization-directory`
3. `identity-access`
4. `ingestion-ops`
5. `qa-review`
6. `document-registry`
7. `chat-runtime` (가장 많은 파일)
8. `metrics-reporting`
9. `AdminApiApplication.kt`, `RepositoryConfiguration.kt` import 업데이트
10. `ArchitectureTest.kt` 규칙 업그레이드
