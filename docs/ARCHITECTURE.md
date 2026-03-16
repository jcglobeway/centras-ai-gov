# centras-ai-gov 아키텍처 문서

이 문서는 centras-ai-gov 시스템의 전체 구조, 데이터 흐름, 도메인 경계를 시각화한다.

---

## 1. 시스템 전체 구조

전체 시스템은 Spring Boot Admin API를 중심으로, Python 워커와 RAG 오케스트레이터가 외부 워커로 연결된 하이브리드 아키텍처다.

```mermaid
flowchart TB
    subgraph clients["클라이언트"]
        citizen["시민 (브라우저)"]
        admin_user["관리자"]
    end

    subgraph frontends["프론트엔드"]
        chatbot_ui["챗봇 UI"]
        admin_ui["Admin UI"]
    end

    subgraph core["코어 시스템"]
        admin_api["Admin API\n(Spring Boot, :8080)"]
        pg["PostgreSQL\n(메인 DB + pgvector)"]
        redis["Redis\n(캐시 / 큐)"]
    end

    subgraph python_services["Python 서비스"]
        rag["RAG Orchestrator\n(FastAPI, :8090)"]
        ingestion["Ingestion Worker\n(Typer CLI)"]
    end

    subgraph external["외부"]
        ollama["Ollama\n(LLM, :11434)"]
        gov_sites["공공기관 웹사이트"]
    end

    citizen --> chatbot_ui --> admin_api
    admin_user --> admin_ui --> admin_api

    admin_api <--> pg
    admin_api <--> redis
    admin_api --> rag

    rag <--> ollama
    rag <--> pg

    ingestion --> gov_sites
    ingestion --> admin_api
    admin_api --> ingestion
```

**핵심 포인트**
- Admin API(Spring Boot)가 모든 운영 상태의 System of Record
- RAG Orchestrator와 Ingestion Worker는 Python 워커로 Admin API에 콜백
- pgvector가 PostgreSQL 내에 통합되어 별도 벡터 DB 불필요

---

## 2. Gradle 모듈 의존성

`apps/admin-api`가 모든 도메인 모듈을 통합하고, 각 모듈은 `shared-kernel`에만 의존한다.

```mermaid
graph TD
    subgraph apps["apps/"]
        admin_api["admin-api\n(Spring Boot App)"]
    end

    subgraph modules["modules/"]
        shared["shared-kernel\n(DomainEvent 인터페이스)"]
        identity["identity-access\n(인증/세션/역할)"]
        org["organization-directory\n(기관/서비스 관리)"]
        ingestion["ingestion-ops\n(수집 소스/작업)"]
        qa["qa-review\n(QA 리뷰 상태 머신)"]
        chat["chat-runtime\n(질문/답변/미해결 큐)"]
        doc["document-registry\n(문서/버전/청크)"]
        metrics["metrics-reporting\n(KPI 집계)"]
    end

    admin_api --> identity
    admin_api --> org
    admin_api --> ingestion
    admin_api --> qa
    admin_api --> chat
    admin_api --> doc
    admin_api --> metrics

    identity --> shared
    org --> shared
    ingestion --> shared
    qa --> shared
    chat --> shared
    doc --> shared
    metrics --> shared
```

**핵심 포인트**
- 모듈 간 직접 의존 없음 — 순환 의존 방지
- 모듈 경계를 넘는 조회는 native SQL 또는 Admin API 경유
- `shared-kernel`은 `DomainEvent` 인터페이스만 포함

---

## 3. 헥사고날 아키텍처 패턴

모든 도메인 모듈은 포트-어댑터 패턴을 따른다. `chat-runtime` 모듈을 예시로 표현한다.

```mermaid
flowchart LR
    subgraph controller["Controller Layer (apps/admin-api)"]
        ctrl["QuestionController\nPOST /admin/questions"]
    end

    subgraph domain["Domain (modules/chat-runtime)"]
        port_r["QuestionReader\n(Port interface)"]
        port_w["QuestionWriter\n(Port interface)"]
        model["QuestionSummary\n(Domain Model)"]
    end

    subgraph adapter["Adapter (modules/chat-runtime)"]
        adapter_r["QuestionReaderAdapter\n(open class)"]
        adapter_w["QuestionWriterAdapter\n(open class)"]
        jpa_repo["JpaQuestionRepository\n(@Repository)"]
        entity["QuestionEntity\n(@Entity)"]
    end

    subgraph infra["Infrastructure"]
        db[("PostgreSQL\nquestions 테이블")]
    end

    subgraph config["Bean 등록"]
        repo_config["RepositoryConfiguration\n(@Configuration)"]
    end

    ctrl --> port_r
    ctrl --> port_w
    port_r --> adapter_r
    port_w --> adapter_w
    adapter_r --> jpa_repo
    adapter_w --> jpa_repo
    jpa_repo --> entity
    entity --> db
    adapter_r --> model
    repo_config -.->|"@Bean"| adapter_r
    repo_config -.->|"@Bean"| adapter_w
```

**핵심 포인트**
- 어댑터는 `@Component` 없이 `RepositoryConfiguration`에서 명시적 `@Bean` 등록
- 어댑터는 반드시 `open class` — Spring CGLIB 프록시 지원
- Controller는 도메인 모델(Port)만 알고, JPA 엔티티를 직접 참조하지 않음

---

## 4. 시민 질문 → 답변 생성 흐름

시민이 질문을 입력하면 RAG 파이프라인을 거쳐 답변이 생성되고 DB에 저장되는 전체 흐름이다.

```mermaid
sequenceDiagram
    actor 시민
    participant API as Admin API<br/>(Spring Boot)
    participant QW as QuestionWriter
    participant ROC as RagOrchestratorClient
    participant RAG as RAG Orchestrator<br/>(FastAPI)
    participant Embed as Ollama bge-m3<br/>(임베딩)
    participant VDB as PostgreSQL<br/>(pgvector)
    participant LLM as Ollama qwen2.5<br/>(답변 합성)
    participant AW as AnswerWriter

    시민->>API: POST /admin/questions
    API->>QW: 질문 저장 (status: pending)
    API->>ROC: generateAnswer(questionId, text)
    ROC->>RAG: POST /generate
    RAG->>Embed: 질문 임베딩 생성
    Embed-->>RAG: embedding vector (1024차원)
    RAG->>VDB: vector similarity search<br/>(pgvector cosine)
    VDB-->>RAG: 관련 청크 Top-K 반환
    RAG->>LLM: 컨텍스트 + 질문으로 답변 합성
    LLM-->>RAG: 생성된 답변
    RAG->>API: POST /admin/rag-search-logs (검색 로그 콜백)
    RAG-->>ROC: { answer, sources }
    ROC-->>API: AnswerResult
    API->>AW: 답변 저장 (status: answered)
    API-->>시민: 200 OK { answer }
```

**핵심 포인트**
- 답변 합성은 RAG Orchestrator에서 완결 — Admin API는 결과만 수신
- 검색 로그(`rag_search_logs`, `rag_retrieved_documents`)는 콜백으로 Admin API에 저장
- 답변 생성 실패 시 `answer_status = fallback` 또는 `no_answer`로 저장

---

## 5. 문서 수집 파이프라인 (Ingestion Worker)

관리자가 수집을 트리거하면 Python 워커가 단계별로 문서를 처리하고 Admin API에 콜백한다.

```mermaid
flowchart LR
    subgraph trigger["트리거"]
        manual["수동 실행\n(관리자 UI)"]
        schedule["스케줄\n(자동)"]
    end

    subgraph api["Admin API"]
        job_api["ingestion_jobs\n(status: queued)"]
        cb["POST /ingestion-jobs/{id}/status\n(단계별 콜백)"]
        idx["POST /admin/document-chunks\n(청크 인덱싱)"]
    end

    subgraph worker["Ingestion Worker (Python)"]
        fetch["FETCH\n(Playwright 크롤링)"]
        extract["EXTRACT\n(BeautifulSoup 파싱)"]
        chunk["CHUNK\n(LangChain 분할)"]
        embed["EMBED\n(Ollama bge-m3)"]
        index["INDEX\n(Admin API 전송)"]
        complete["COMPLETE\n(작업 완료)"]
    end

    subgraph source["외부"]
        gov["공공기관 웹사이트"]
    end

    manual --> job_api
    schedule --> job_api
    job_api --> fetch
    fetch --> gov
    fetch -->|"running"| cb
    fetch --> extract
    extract -->|"extracting"| cb
    extract --> chunk
    chunk -->|"chunking"| cb
    chunk --> embed
    embed -->|"embedding"| cb
    embed --> index
    index --> idx
    index -->|"indexing"| cb
    index --> complete
    complete -->|"succeeded"| cb
```

**핵심 포인트**
- 각 단계마다 `POST /ingestion-jobs/{id}/status` 콜백으로 진행 상태 기록
- Worker는 Playwright로 JavaScript 렌더링 페이지까지 크롤링
- 임베딩 벡터(1024차원)는 `document_chunks.embedding_vector`에 저장

---

## 6. DB 스키마 관계

15개 핵심 테이블의 관계를 bounded context 단위로 표현한다.

```mermaid
erDiagram
    organizations {
        string org_id PK
        string name
        string status
    }
    services {
        string service_id PK
        string org_id FK
        string name
    }
    crawl_sources {
        string source_id PK
        string org_id FK
        string url
    }
    ingestion_jobs {
        string job_id PK
        string org_id FK
        string source_id FK
        string status
    }
    chat_sessions {
        string session_id PK
        string org_id FK
    }
    questions {
        string question_id PK
        string session_id FK
        string text
        string answer_status
    }
    answers {
        string answer_id PK
        string question_id FK
        string content
    }
    qa_reviews {
        string review_id PK
        string question_id FK
        string review_status
    }
    rag_search_logs {
        string log_id PK
        string question_id FK
    }
    rag_retrieved_documents {
        string doc_id PK
        string log_id FK
        float score
    }
    documents {
        string document_id PK
        string org_id FK
        string title
    }
    document_versions {
        string version_id PK
        string document_id FK
    }
    document_chunks {
        string chunk_id PK
        string document_id FK
        vector embedding_vector
    }
    daily_metrics_org {
        string metric_id PK
        string org_id FK
        date metric_date
    }

    organizations ||--o{ services : "has"
    organizations ||--o{ crawl_sources : "has"
    organizations ||--o{ ingestion_jobs : "has"
    organizations ||--o{ chat_sessions : "has"
    organizations ||--o{ documents : "has"
    organizations ||--o{ daily_metrics_org : "has"
    crawl_sources ||--o{ ingestion_jobs : "triggers"
    chat_sessions ||--o{ questions : "has"
    questions ||--o| answers : "has"
    questions ||--o{ qa_reviews : "has"
    questions ||--o{ rag_search_logs : "has"
    rag_search_logs ||--o{ rag_retrieved_documents : "has"
    documents ||--o{ document_versions : "has"
    documents ||--o{ document_chunks : "has"
```

**핵심 포인트**
- 모든 비즈니스 테이블은 `org_id`로 조직 스코프 적용
- `document_chunks.embedding_vector`는 H2(테스트)에서 TEXT, PostgreSQL에서 `vector(1024)`
- V018 Flyway 마이그레이션은 PostgreSQL 전용 (H2 테스트는 `flyway.target=17`로 스킵)

---

## 7. QA 리뷰 상태 머신

미해결 답변에 대해 운영자가 수행하는 QA 리뷰의 상태 전이를 나타낸다.

```mermaid
stateDiagram-v2
    [*] --> pending : QA 리뷰 생성

    pending --> confirmed_issue : 실제 문제 확인\n(root_cause_code, action_type 필수)
    pending --> false_alarm : 오탐 판정\n(action_type = no_action 강제)

    confirmed_issue --> resolved : 조치 완료

    false_alarm --> resolved : 전환 금지
    note right of false_alarm
        false_alarm → resolved 전환 불가
        (시스템에서 거부)
    end note

    resolved --> [*]
```

**핵심 포인트**
- `confirmed_issue` 전환 시 `root_cause_code`와 `action_type` 필드 필수
- `false_alarm`은 반드시 `action_type = no_action`으로 저장
- `false_alarm → resolved` 전환은 도메인 규칙으로 금지

---

## 8. 역할별 접근 권한

세 가지 관리자 역할이 접근할 수 있는 범위와 기능을 정의한다.

```mermaid
flowchart LR
    subgraph roles["관리자 역할"]
        ops["ops_admin\n(운영 관리자)"]
        client["client_admin\n(기관 관리자)"]
        qa_r["qa_admin\n(QA 담당자)"]
    end

    subgraph permissions["접근 권한"]
        all_orgs["전체 기관 조회/관리"]
        own_org["자기 기관 데이터 조회"]
        ingestion_exec["수집 작업 실행"]
        qa_write["QA 리뷰 작성"]
        assigned_org["할당된 기관 조회"]
        audit["감사 로그 기록"]
    end

    ops --> all_orgs
    ops --> ingestion_exec
    ops --> qa_write
    ops --> audit

    client --> own_org

    qa_r --> assigned_org
    qa_r --> qa_write
```

**핵심 포인트**
- 권한은 화면 단위가 아닌 **액션 단위**로 적용
- 모든 요청은 세션에서 `user_id`, `role_code`, `organization_scope`를 복원
- 스코프 밖 리소스 접근: 권한 없음 → `403`, 스코프 밖 리소스 → `404`

---

## 참고 문서

| 문서 | 내용 |
|---|---|
| `mvp_docs/04_data_api.md` | 전체 테이블 스키마, API 계약 |
| `mvp_docs/05_architecture_openrag.md` | 모듈러 모노리스 아키텍처 결정 |
| `mvp_docs/06_access_policy.md` | 역할별 화면 접근 정책 |
| `mvp_docs/09_unresolved_qa_state_machine.md` | 미해결 큐 가시성 규칙 |
| `mvp_docs/10_auth_authz_api.md` | 인증 API, 세션 라이프사이클 |
