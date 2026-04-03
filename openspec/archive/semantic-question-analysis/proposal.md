# Proposal: semantic-question-analysis

## Problem

현재 `/ops/statistics` 대시보드의 질문 분석 기능이 단순 문자열 처리 기반이라 분석 품질이 낮다.

- **TOP 키워드**: 공백 기준 whitespace 분리로 조사·조동사가 포함된 의미없는 단어가 노출된다.
  예) "있습니다", "수가", "어떻게" 같은 단어가 상위 키워드로 등장
- **중복 질의 탐지**: exact text match 방식이라 같은 의도의 다른 표현을 탐지하지 못한다.
  예) "주차 요금 얼마예요?" 와 "주차비가 어떻게 되나요?"가 별개 질문으로 처리됨

두 문제 모두 운영자가 실제 민원 트렌드를 파악하는 데 방해가 된다.

## Proposed Solution

LLM 기반 키워드 추출과 임베딩 기반 유사 질문 클러스터링을 배치로 처리하고,
그 결과를 전용 테이블에 저장해 프론트엔드가 조회하는 방식으로 교체한다.

### 1. eval-runner: `cluster-questions` 배치 CLI 커맨드

일 1회 또는 수동 실행하는 배치 커맨드를 추가한다.

**Phase 1 — LLM 키워드 추출**

- 최근 N일(`--days`, 기본 7) questions 테이블에서 org별 question_text를 배치로 fetch
- Ollama LLM(설정된 모델, 기본 llama3)에 다음 프롬프트 전달:
  ```
  다음 질문 목록에서 자주 등장하는 핵심 명사 키워드 20개를 추출해줘.
  조사/부사/동사 제외. JSON 배열로만 응답.
  ```
- 추출된 키워드별로 question_text 내 실제 등장 횟수 카운트
- 결과를 `question_keyword_stats` 테이블에 run_date + organization_id 기준으로 upsert

**Phase 2 — 2-stage 유사 질문 클러스터링**

- Stage 1 (bi-encoder): bge-m3로 questions 임베딩 → 코사인 유사도 ≥ 0.75인 후보 쌍 선별
- Stage 2 (LLM 검증): 후보 쌍마다 Ollama LLM에게 판정 요청:
  ```
  다음 두 질문이 같은 의도인가? yes/no만 답해.
  질문1: ...
  질문2: ...
  ```
  yes인 쌍만 같은 클러스터로 묶음
- 클러스터별 대표 질문(가장 짧은 텍스트), 구성원 수, 평균 유사도, 샘플 텍스트(최대 5개)를
  `question_similarity_groups` 테이블에 저장

### 2. Provider 추상화 (embedding / LLM)

현재 eval-runner가 Ollama에 직접 의존하는 구조를 추상화한다.

```python
# embedding_provider.py
class EmbeddingProvider(Protocol):
    def embed(self, texts: list[str]) -> list[list[float]]: ...

class OllamaEmbeddingProvider: ...
class OpenAIEmbeddingProvider: ...
```

`.env`의 `EMBEDDING_PROVIDER=ollama|openai` 값으로 런타임 전환.
LLM도 동일 패턴으로 `LLM_PROVIDER=ollama|openai` 추가.

### 3. DB 신규 테이블 2개 (PostgreSQL only)

기존 마이그레이션이 V033까지 존재하므로 V034, V035를 신규 생성한다.

```sql
-- V034: question_keyword_stats
CREATE TABLE question_keyword_stats (
  id             TEXT      PRIMARY KEY,
  run_date       DATE      NOT NULL,
  organization_id TEXT     NOT NULL,
  keyword        TEXT      NOT NULL,
  count          INT       NOT NULL,
  created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_qks_org_date ON question_keyword_stats (organization_id, run_date);

-- V035: question_similarity_groups
CREATE TABLE question_similarity_groups (
  id                  TEXT      PRIMARY KEY,
  run_date            DATE      NOT NULL,
  organization_id     TEXT      NOT NULL,
  representative_text TEXT      NOT NULL,
  question_count      INT       NOT NULL,
  avg_similarity      FLOAT     NOT NULL,
  sample_texts        TEXT      NOT NULL,  -- JSON array, 최대 5개
  created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_qsg_org_date ON question_similarity_groups (organization_id, run_date);
```

### 4. 백엔드 API 2개 (MetricsController.kt)

`metrics-reporting` bounded context에 추가한다.

- `GET /admin/metrics/semantic-keywords?organization_id=&run_date=`
  → `question_keyword_stats` 조회, run_date 기본값 = 오늘
- `GET /admin/metrics/semantic-similar-groups?organization_id=&run_date=`
  → `question_similarity_groups` 조회, run_date 기본값 = 오늘

두 엔드포인트 모두 기존 scope 필터링 패턴 적용
(super_admin = 전체, ops_admin·client_org_admin = 자기 organization만 조회).

### 5. 프론트엔드 `/ops/statistics` 교체

- TOP 키워드 카드: whitespace 기반 현재 로직 제거 → `semantic-keywords` API로 교체
- 중복 질의 카드: exact match 기존 카드 유지 + "의미적 유사 질문 클러스터" 카드 추가
  (`semantic-similar-groups` API 사용, 대표 질문 + 구성원 수 + 샘플 tooltip)

## Out of Scope

- 형태소 분석 라이브러리(KoNLPy 등) 도입: LLM 프롬프트로 대체
- 실시간 클러스터링: 배치 결과 조회로 충분
- 클러스터 결과의 이력 관리 및 비교 뷰
- Spring Boot 테스트 신규 작성 (PostgreSQL-only 테이블이므로 H2 테스트 대상 아님)
- OpenAI Provider 실제 테스트 (Protocol 구현만 추가, 테스트는 Ollama 기준)

## Success Criteria

- `cluster-questions` CLI 실행 후 `question_keyword_stats`, `question_similarity_groups` 테이블에 데이터 적재
- `GET /admin/metrics/semantic-keywords` 응답에 `keyword`, `count` 배열 포함
- `GET /admin/metrics/semantic-similar-groups` 응답에 `representativeText`, `questionCount`, `sampleTexts` 배열 포함
- `/ops/statistics` TOP 키워드 카드가 semantic-keywords API 데이터로 렌더링
- `/ops/statistics` 의미적 유사 질문 클러스터 카드 신규 표시
- `EMBEDDING_PROVIDER=openai` 설정 시 OpenAIEmbeddingProvider가 로드됨
- 기존 Spring Boot 테스트 50개 전부 통과 (V034·V035는 H2 마이그레이션 대상 제외)
