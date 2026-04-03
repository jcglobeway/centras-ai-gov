# Spec: 대량 질의 투입 + RAG 검색 품질 개선

## 1. RAG 검색 파이프라인 (`retrieval.py`)

### 변경 전
```
질문 → get_embedding() → pgvector cosine search → top-5 반환
confidence = 1.0 - rrf_score  # 항상 ~0.97 → confidence ~3%
```

### 변경 후
```
질문
  ├─ vector_search() — pgvector cosine similarity (top_k=10)
  ├─ bm25_search()   — BM25 키워드 검색, kiwipiepy 형태소 분석 (top_k=10)
  ↓
rrf_fusion()         — Reciprocal Rank Fusion (k=60)
  ↓
rerank() [선택]      — FlashRank ms-marco-MiniLM-L-12-v2 (RERANKER_ENABLED=true)
  ↓
final_top_n=5 반환
confidence = 1.0 - rrf_score / (2/61)  # 정규화된 신뢰도 0~1
```

### 핵심 구현 세부사항

**kiwipiepy 형태소 분석**
- 설치되어 있으면 사용, 없으면 공백 분리로 폴백 (ImportError guard)
- 추출 품사: NNG, NNP(명사), VV, VA(동사/형용사 어근), XR(어근), SL(외래어)

**BM25 corpus 캐시**
- 모듈 레벨 `_bm25_cache` dict (hash, bm25, chunk_ids, corpus)
- corpus hash = `f"{청크수}:{마지막ID}"` md5 → 변경 시 자동 재빌드
- 매 요청마다 DB 재로드 없이 인메모리 캐시 사용

**RRF 정규화**
- 이론적 최댓값: 두 리스트 모두 1위일 때 `2 / (60 + 1) ≈ 0.0328`
- `distance = 1.0 - rrf_score / max_rrf` → confidence = 1 - distance

**FlashRank 싱글톤**
- `_flashrank_ranker` 전역 변수, 프로세스 내 최초 1회만 로드
- `RERANKER_ENABLED=true` 환경변수로 활성화

### 의존성 추가
```toml
# pyproject.toml
"kiwipiepy>=0.18.0"
```

### 환경변수
```
RERANKER_ENABLED=true        # FlashRank 리랭킹 활성화
HYBRID_SEARCH_TOP_K=10       # 각 검색기 후보 수
```

---

## 2. 시민 질의 생성기 (`citizen_query_gen.py`)

TS_*.zip의 `consulting_content`에서 `고객:` 발화를 추출하고
Ollama(qwen2.5:7b)로 독립적인 민원 질문으로 변환한다.

### 처리 흐름
```
TS zip → consulting_content 파싱
  → "고객:" 발화 추출 (15자 미만 제외)
  → Ollama /api/generate로 민원 질문으로 재작성
  → 짧은 응답(null 반환) 필터링
  → citizen_questions.json 저장
```

### Ollama 재작성 프롬프트 규칙
- 문맥 없이도 이해되는 독립 질문
- 존댓말
- "네", "아", 단순 확인 → null 반환
- 첫 줄만 사용, 10자 미만 제외

### 출력 스키마 (`citizen_questions.json`)
```json
[{
  "question": "...",
  "ground_truth": "",
  "org_id": "org_acc",
  "service_id": "svc_acc_chatbot",
  "task_category": "민원질의",
  "consulting_category": "문화관광",
  "source_id": "..."
}]
```

### CLI
```bash
citizen-query-gen --zip TS_*.zip --limit 200 --output citizen_questions.json
citizen-query-gen --zip TS_*.zip --limit 10 --dry-run
```

---

## 3. 대량 질의 투입기 (`bulk_query_runner.py`)

### 처리 흐름
```
입력: TL zip 또는 citizen_questions.json
  ↓
load_qa_pairs() 또는 json.load()
  ↓
group_by_source() — source_id 기준 그룹핑 (멀티턴)
  ↓
세션별 루프:
  1. POST /admin/simulator/sessions → sessionId
  2. 질문별:
     a. POST /admin/questions (RAG 오케스트레이터 내부 호출 포함)
     b. delay 대기
  3. session_delay 대기
  ↓
_backdate_questions() — DB에서 created_at을 30일 분산
```

### 멀티턴 그룹핑
- `source_id` 동일 → 하나의 세션 (자연스러운 대화 흐름)
- `--max-turns 3` 초과분은 별도 그룹으로 분할
- source_id 없는 항목 → 단일턴 세션

### 날짜 분산
- 최근 30일 date_pool 생성 (오늘 - 29일 ~ 오늘)
- 세션 인덱스 % 30으로 순환 배정
- `_backdate_questions()`: questions, answers, rag_search_logs 모두 업데이트
- 시각 무작위화: 09:00~17:59 사이 랜덤

### 중요 타임아웃
- `create_question()` timeout=120.0s — Ollama LLM 응답이 30~90초 소요되므로
- `POST /admin/questions`가 rag-orchestrator를 **동기**로 내부 호출함

### CLI
```bash
bulk-query-runner --zip TL_*.zip --limit 200 --channel simulator
bulk-query-runner --input-json citizen_questions.json --limit 200 --channel api
bulk-query-runner --zip TL_*.zip --limit 10 --dry-run
bulk-query-runner --zip TL_*.zip --skip 200 --limit 200  # 페이징
```

### 환경변수
```
ADMIN_API_BASE_URL=http://localhost:8081
RAG_ORCHESTRATOR_URL=http://localhost:8090
ADMIN_API_SESSION_TOKEN=...  # 없으면 자동 로그인
DATABASE_URL=...             # 날짜 분산에 필요 (psycopg2)
```
