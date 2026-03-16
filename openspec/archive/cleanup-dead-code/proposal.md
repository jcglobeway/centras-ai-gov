# Proposal: cleanup-dead-code

## 목적
InMemory 어댑터 5개 제거 및 RagOrchestratorClient의 RestTemplate 생성자 주입 개선

## 배경
JPA 어댑터로 마이그레이션 완료 후 InMemory 구현체들이 코드베이스에 잔존하여 혼란을 주고 있음.
RagOrchestratorClient는 `new RestTemplate()`으로 직접 생성하여 Spring DI 패턴을 따르지 않음.

## 범위

### 삭제 대상
- `modules/identity-access/.../InMemoryAdminSessionRepository.kt`
- `modules/identity-access/.../InMemoryAdminUserRepository.kt`
- `modules/identity-access/.../InMemoryAuditLogRepository.kt`
- `modules/organization-directory/.../InMemoryOrganizationRepository.kt`
- `modules/organization-directory/.../InMemoryServiceRepository.kt`

### 수정 대상
- `apps/admin-api/.../chatruntime/RagOrchestratorClient.kt`
  - `RestTemplateBuilder` 생성자 주입으로 변경

## 영향
- RepositoryConfiguration은 이미 JPA 어댑터를 사용하므로 영향 없음
- 테스트는 JPA 기반으로 동작 중이므로 영향 없음
