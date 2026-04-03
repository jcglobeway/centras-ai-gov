# Status: semantic-question-analysis

## 현재 상태: 구현 완료 (검증 대기)

| 아티팩트 | 상태 |
|---------|------|
| proposal.md | 완료 |
| tasks.md | 완료 |
| DB 마이그레이션 (V034, V035) | ✅ 완료 |
| eval-runner Provider 추상화 | ✅ 완료 |
| eval-runner cluster-questions CLI | ✅ 완료 |
| 백엔드 API (Spring Boot) | ✅ 완료 (기존 50개 테스트 통과) |
| 프론트엔드 수정 | ✅ 완료 |

## 구현 결과

### 신규 생성 파일
- `apps/admin-api/src/main/resources/db/migration/V034__create_question_keyword_stats.sql`
- `apps/admin-api/src/main/resources/db/migration/V035__create_question_similarity_groups.sql`
- `python/eval-runner/src/eval_runner/embedding_provider.py` — OllamaEmbeddingProvider / OpenAIEmbeddingProvider
- `python/eval-runner/src/eval_runner/llm_provider.py` — OllamaLLMProvider / OpenAILLMProvider
- `python/eval-runner/src/eval_runner/cluster_questions.py` — cluster-questions CLI

### 수정된 파일
- `python/eval-runner/pyproject.toml` — cluster-questions 엔트리포인트, openai optional dep, numpy 의존성
- `apps/admin-api/.../MetricsController.kt` — `/metrics/semantic-keywords`, `/metrics/semantic-similar-groups` 엔드포인트 추가
- `frontend/src/app/ops/statistics/page.tsx` — TOP 키워드 카드 → semantic API 교체, 의미적 유사 질문 클러스터 카드 추가

## 주요 결정 사항

- 백엔드: 헥사고날 레이어 불필요 — JDBC 템플릿 직접 쿼리 (기존 analytics 엔드포인트 패턴)
- H2 flyway.target "29" 유지 — V034·V035 PostgreSQL-only
- try/catch로 감싸 테이블 미존재 시 빈 배열 반환 (H2 테스트 안전)
- 클러스터링: Union-Find 알고리즘

## 검증 방법 (미실행)

```bash
# eval-runner 배치 실행
cd python/eval-runner && pip install -e .
cluster-questions --org-id org_acc --days 7 --dry-run  # 미리보기
cluster-questions --org-id org_acc --days 7            # 실제 적재

# API 확인
curl -H "X-Admin-Session-Id: $TOKEN" \
  "http://localhost:8080/admin/metrics/semantic-keywords?organization_id=org_acc"

# 프론트엔드: /ops/statistics 접속 확인
```
