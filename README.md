# Public RAG Ops Platform

문서 단계에서 합의한 MVP 구조를 실제 저장소 골격으로 옮긴 초기 리포지토리다.

## Stack Direction

- Admin Web: `Next.js` 예정
- Product Core: `Spring Boot + Kotlin`
- Architecture: `DDD-lite modular monolith`
- AI Side System: `Python ingestion-worker`, `Python rag-orchestrator`
- Storage: `PostgreSQL`, `Redis`, object storage
- Retrieval: `OpenSearch` 또는 `pgvector`

## Current Layout

- `apps/admin-api`
- `modules/shared-kernel`
- `modules/identity-access`
- `modules/organization-directory`
- `modules/chat-runtime`
- `modules/document-registry`
- `modules/ingestion-ops`
- `modules/qa-review`
- `modules/metrics-reporting`
- `python/ingestion-worker`
- `python/rag-orchestrator`
- `python/common`
- `tests/unit`
- `tests/api`
- `tests/e2e`
- `tests/data`
- `mvp_docs`

## Immediate Next Steps

1. `admin-api`의 개발용 session adapter를 실제 세션 저장소 기반 구현으로 교체한다.
2. `modules/ingestion-ops`를 쓰기 계약과 상태 전이 규칙까지 확장한다.
3. `python/ingestion-worker`에 crawl source 실행 루프를 추가한다.
4. `python/rag-orchestrator`에 adapter 인터페이스와 citation 응답 계약을 추가한다.

## Bootstrap

- 환경 설치 가이드: `ENV_SETUP.md`
- 현재 상태: `JDK 21`, `Gradle 9.4.0`, `gradlew.bat` 준비 완료
- 저장소 상태: `git init -b main` 완료
- 기본 실행: `.\gradlew.bat test`

## 포털 접속 정보

### 프론트엔드 (Next.js)

```
cd frontend && npm install && npm run dev
```

| 포털 | URL | 대상 역할 |
|---|---|---|
| Ops Portal | http://localhost:3001/ops | ops_admin, super_admin |
| Client Portal | http://localhost:3001/client | client_org_admin, client_viewer |
| QA Portal | http://localhost:3001/qa | qa_manager, knowledge_editor |
| 로그인 | http://localhost:3001/login | 공통 |

### 백엔드 (Spring Boot)

```
./gradlew :apps:admin-api:bootRun
# Admin API: http://localhost:8081
# Swagger UI: http://localhost:8081/swagger-ui/index.html
```

> JAVA_HOME이 설정되지 않은 경우: `echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 25)' >> ~/.zshrc && source ~/.zshrc`

### 개발용 계정

비밀번호 공통: `pass1234`

| 이메일 | 역할 | 접근 범위 | 권장 포털 |
|---|---|---|---|
| `ops@jcg.com` | ops_admin | 전체 기관 | /ops |
| `super@jcg.com` | super_admin | 전체 기관 | /ops |
| `client@jcg.com` | client_admin | 부산시 | /client |
| `viewer@jcg.com` | client_viewer | 부산시 | /client |
| `qa@jcg.com` | qa_admin | 서울시 | /qa |
| `editor@jcg.com` | knowledge_editor | 서울시 | /qa |

> 개발 환경 전용 계정입니다. 운영 환경에서는 사용하지 마세요.

### 인프라

```bash
# PostgreSQL (Docker)
docker-compose up -d

# 접속 정보
Host: localhost:5432
DB:   ragops_dev
User: ragops_user
Pass: ragops_pass
```

## OpenSpec

- 중요한 변경은 `openspec/changes/<change-id>` 단위로 진행한다.
- 완료된 change는 `openspec/archive/<change-id>`로 이동하고 같은 단위로 커밋한다.
- 운영 규칙은 `openspec/README.md`를 기준으로 따른다.
