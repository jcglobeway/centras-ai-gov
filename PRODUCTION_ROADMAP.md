# Production Roadmap

## 현재 상태 (2026-03-15)

### ✅ 완성된 것

**Backend (Spring Boot + Kotlin)**:
- ✅ 7개 모듈 전체 구현 (identity-access, organization-directory, ingestion-ops, qa-review, chat-runtime, document-registry, metrics-reporting)
- ✅ PostgreSQL + JPA + Flyway (15개 migration)
- ✅ 헥사고날 아키텍처 (ports + adapters)
- ✅ 권한 기반 멀티테넌트
- ✅ 39개 통합 테스트 (100% passing)

**Python Services**:
- ✅ ingestion-worker (Playwright 크롤링, job callback)
- ✅ rag-orchestrator (FastAPI, OpenAI LLM 답변 생성)

**Database**:
- ✅ 15개 테이블 (admin_users ~ daily_metrics_org)
- ✅ Seed 데이터 (개발/테스트용)
- ✅ FK 제약 조건 (대부분)

**MVP 핵심 루프**:
- ✅ 시민 질문 → 답변 생성 → Unresolved Queue → QA 검수 → 문서 재수집 → KPI 집계

---

## 🎯 Production 준비 로드맵

### Phase 1: 기술적 완성도 (1-2주)

#### 1.1 Vector Search 구현 (3-4일)
**Change**: `add-vector-search-retrieval`

- [ ] pgvector extension 설정
- [ ] document_chunks 테이블 구현 (embedding vector 컬럼)
- [ ] Embedding 생성 로직 (OpenAI Embeddings API)
- [ ] Vector similarity search
- [ ] rag-orchestrator에 실제 retrieval 연동
- [ ] Mock context → 실제 search results 교체

**우선순위**: HIGH
**의존성**: PostgreSQL 실행 필요

---

#### 1.2 문서 파싱 파이프라인 (3-4일)
**Change**: `add-document-parsing-pipeline`

- [ ] HTML 파싱 (Beautiful Soup)
- [ ] PDF 파싱 (pdfplumber)
- [ ] Chunk 생성 (LangChain TextSplitter)
- [ ] Embedding 생성
- [ ] document_chunks 저장
- [ ] ingestion-worker 완전 자동화

**우선순위**: HIGH
**의존성**: Vector search 완성

---

#### 1.3 로깅 및 모니터링 (2-3일)
**Change**: `add-production-logging`

- [ ] Logback 설정 (JSON 포맷)
- [ ] request_id, trace_id 자동 생성
- [ ] 모든 API 응답에 request_id 포함
- [ ] 에러 로그 구조화
- [ ] Audit log 자동 기록 강화
- [ ] Health check 개선 (DB, Redis, external service 확인)

**우선순위**: MEDIUM

---

#### 1.4 에러 처리 개선 (2일)
**Change**: `unify-error-handling`

- [ ] GlobalExceptionHandler 구현
- [ ] 통일된 에러 응답 포맷
- [ ] 에러 코드 체계 정리
- [ ] Validation 에러 메시지 개선
- [ ] 4xx/5xx 에러별 적절한 응답

**우선순위**: MEDIUM

---

### Phase 2: 운영 안정성 (1-2주)

#### 2.1 성능 최적화 (3-4일)
**Change**: `optimize-database-performance`

- [ ] DB 인덱스 최적화
- [ ] N+1 쿼리 제거
- [ ] Connection pool 설정 (HikariCP)
- [ ] 캐싱 전략 (Redis)
- [ ] API 응답 시간 목표 설정 (p95 < 500ms)

**우선순위**: MEDIUM

---

#### 2.2 보안 강화 (2-3일)
**Change**: `enhance-security`

- [ ] 비밀번호 bcrypt 해싱 (현재 개발용 평문)
- [ ] Session token 해싱 강화
- [ ] Rate limiting (Spring Security)
- [ ] CORS 설정
- [ ] SQL Injection 방어 검증
- [ ] XSS 방어 검증
- [ ] CSRF 보호

**우선순위**: HIGH (운영 전 필수)

---

#### 2.3 데이터 정합성 (2일)
**Change**: `add-data-integrity-checks`

- [ ] 트랜잭션 경계 검증
- [ ] Optimistic locking (버전 충돌 방지)
- [ ] Idempotency 보장 (재시도 안전성)
- [ ] Cascade delete 정책
- [ ] Orphan data cleanup

**우선순위**: MEDIUM

---

### Phase 3: 배포 자동화 (1주)

#### 3.1 Docker 이미지 빌드 (2일)
**Change**: `add-docker-build`

- [ ] admin-api Dockerfile (멀티스테이지 빌드)
- [ ] ingestion-worker Dockerfile
- [ ] rag-orchestrator Dockerfile
- [ ] docker-compose.yml 확장 (전체 스택)
- [ ] 이미지 최적화 (layer 캐싱)

**우선순위**: HIGH

---

#### 3.2 CI/CD 파이프라인 (2-3일)
**Change**: `add-ci-cd-pipeline`

- [ ] GitHub Actions workflow
- [ ] 자동 테스트 실행 (PR 시)
- [ ] Docker 이미지 빌드 (main 커밋 시)
- [ ] 이미지 레지스트리 푸시 (Docker Hub/ECR)
- [ ] 배포 스크립트 (dev/staging/prod)

**우선순위**: MEDIUM

---

#### 3.3 환경 설정 관리 (1일)
**Change**: `add-environment-config`

- [ ] 환경별 설정 분리 (dev/staging/prod)
- [ ] Secret 관리 (환경 변수, Vault)
- [ ] 설정 검증 (@ConfigurationProperties)
- [ ] Feature flag 시스템

**우선순위**: MEDIUM

---

### Phase 4: 운영 기능 (1-2주)

#### 4.1 배치 작업 (2-3일)
**Change**: `add-batch-jobs`

- [ ] KPI 일배치 집계 (daily_metrics_org 자동 생성)
- [ ] 세션 만료 정리
- [ ] Audit log 아카이빙
- [ ] 문서 재수집 스케줄러
- [ ] Spring Batch 또는 Quartz 설정

**우선순위**: MEDIUM

---

#### 4.2 알림 시스템 (2일)
**Change**: `add-notification-system`

- [ ] Ingestion job 실패 알림
- [ ] QA review 대기 알림
- [ ] 시스템 장애 알림
- [ ] Slack/Email 연동

**우선순위**: LOW

---

#### 4.3 관리자 기능 보강 (2-3일)
**Change**: `enhance-admin-features`

- [ ] 사용자 관리 UI API (CRUD)
- [ ] 역할 할당 API
- [ ] 조직 설정 API
- [ ] 시스템 설정 API
- [ ] Audit log 조회 API

**우선순위**: MEDIUM

---

### Phase 5: 스케일링 준비 (선택적)

#### 5.1 Read Replica (선택)
- [ ] PostgreSQL read replica 설정
- [ ] Read/Write 분리
- [ ] @Transactional(readOnly=true) 최적화

**우선순위**: LOW (트래픽 증가 시)

---

#### 5.2 캐싱 계층 (선택)
- [ ] Redis 연동
- [ ] 세션 캐싱
- [ ] API 응답 캐싱
- [ ] Document 메타데이터 캐싱

**우선순위**: LOW (성능 이슈 시)

---

#### 5.3 비동기 처리 (선택)
- [ ] Message Queue (RabbitMQ/Kafka)
- [ ] 비동기 답변 생성
- [ ] Webhook callback
- [ ] Event-driven architecture

**우선순위**: LOW (스케일링 필요 시)

---

## 🎯 추천 우선순위 (다음 2주)

### Week 1: 핵심 기능 완성
```
1. Vector Search 구현 (3-4일)
   → 실제 검색 기반 답변

2. 문서 파싱 파이프라인 (3-4일)
   → 실제 크롤링/chunk/embed
```

### Week 2: 운영 준비
```
3. 보안 강화 (2-3일)
   → 비밀번호 해싱, rate limiting

4. 로깅/모니터링 (2-3일)
   → request_id, 구조화된 로그

5. Docker 빌드 (2일)
   → 배포 가능한 이미지
```

---

## 📊 Production Readiness Checklist

### 기능 완성도
- ✅ MVP 핵심 루프 (100%)
- ⏸️ Vector search (0% - mock)
- ⏸️ 문서 파싱 (20% - 크롤링만)
- ⏸️ Embedding (0%)

### 운영 안정성
- ⏸️ 로깅 (30% - 기본만)
- ⏸️ 에러 처리 (50% - 부분적)
- ⏸️ Health check (40% - 기본만)
- ❌ 모니터링 (0%)

### 보안
- ❌ 비밀번호 해싱 (0% - 개발용 평문)
- ⏸️ 인증/권한 (80% - 핵심만)
- ❌ Rate limiting (0%)
- ❌ CORS (0%)

### 배포
- ✅ Docker Compose (100% - PostgreSQL)
- ❌ Docker 이미지 (0%)
- ❌ CI/CD (0%)
- ❌ 환경 설정 (20% - 기본만)

### 데이터
- ✅ 스키마 설계 (100%)
- ✅ Migration (100%)
- ⏸️ Backup 전략 (0%)
- ⏸️ 성능 최적화 (30%)

---

## 🚀 Production 출시를 위한 최소 요구사항

### Must-Have (필수)
```
1. ✅ Vector search 구현
2. ✅ 문서 파싱 파이프라인
3. ✅ 보안 강화 (비밀번호 해싱, rate limiting)
4. ✅ 로깅 개선 (request_id, trace_id)
5. ✅ Docker 이미지 빌드
6. ✅ Health check 강화
```

### Should-Have (권장)
```
7. ⭕ 에러 처리 통일
8. ⭕ KPI 배치 작업
9. ⭕ CI/CD 파이프라인
10. ⭕ 알림 시스템
```

### Nice-to-Have (선택)
```
11. 🔵 Read replica
12. 🔵 Redis 캐싱
13. 🔵 Message queue
```

---

## 📅 타임라인 추정

### 최소 출시 (4-6주)
```
Week 1-2: Vector search + 문서 파싱
Week 3:   보안 + 로깅
Week 4:   Docker + 배포 준비
Week 5-6: 통합 테스트 + 버그 수정
```

### 안정적 출시 (8-10주)
```
Week 1-4: 최소 출시 범위
Week 5-6: CI/CD + 배치 작업
Week 7-8: 성능 최적화
Week 9-10: 부하 테스트 + 최종 점검
```

---

## 🎯 즉시 시작 가능한 작업 (우선순위순)

1. **Vector Search 구현** (3-4일)
   - pgvector extension
   - Embedding 생성
   - Similarity search

2. **문서 파싱** (3-4일)
   - HTML/PDF 파싱
   - Chunk 생성
   - Embedding

3. **보안 강화** (2-3일)
   - bcrypt 비밀번호
   - Rate limiting
   - CORS

4. **로깅 개선** (2일)
   - request_id
   - JSON 로그
   - 구조화

5. **Docker 빌드** (2일)
   - Dockerfile 작성
   - 이미지 최적화

---

## 📝 참고 사항

### OpenRAG 관련
- OpenRAG는 별도 PoC 트랙으로 유지
- 현재 자체 구현 우선 (Vector search + LLM)
- OpenRAG 검토는 자체 구현 완성 후 비교 분석

### 기술 스택 결정
- Vector DB: **pgvector** (PostgreSQL extension) 권장
  - 이유: 이미 PostgreSQL 사용 중, 관리 포인트 감소
  - 대안: OpenSearch (별도 클러스터 필요)

- LLM: **OpenAI gpt-4o-mini** 현재 사용
  - 대안: Claude, 로컬 모델

- Embedding: **OpenAI text-embedding-3-small**
  - 대안: 로컬 embedding 모델

### 비용 고려
- OpenAI API 비용 (embedding + LLM)
- PostgreSQL 인스턴스 크기
- Vector index 메모리 사용량

---

## 🎊 현재 완성도: MVP 100%

**Production 준비도: ~40%**

남은 작업은 주로:
- 실제 검색 기능 (vector search)
- 운영 안정성 (로깅, 모니터링, 보안)
- 배포 자동화

**MVP는 완성, Production은 4-6주 소요 예상**
