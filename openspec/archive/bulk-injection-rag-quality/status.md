ㅓ버# Status

- 상태: `completed`
- 시작일: `2026-04-01`
- 마지막 업데이트: `2026-04-01`

## Progress

- [x] RAG 검색 파이프라인 개선 (vector + BM25 + RRF + FlashRank)
- [x] RRF → confidence 정규화 버그 수정
- [x] `citizen_query_gen` 구현 및 200건 생성
- [x] `bulk_query_runner` 구현 (멀티턴 + 날짜 분산)
- [x] 시뮬레이터 배치 200건 투입 완료
- [x] API 배치 200건 투입 완료

## 실행 결과 (org_acc)

| 지표 | 값 |
|------|-----|
| 총 질문 | 405건 (simulator 200 + api 205) |
| 답변 성공 | 404건 (99.8%) |
| 에스컬레이션 | 1건 (0.2%) |
| 평균 신뢰도 | 54.5% (개선 전 ~3%) |
| 날짜 범위 | 2026-03-03 ~ 2026-04-01 |
| RAG 검색 로그 | 404건 |
| 평균 검색 latency | 1,995ms |

## 주요 의사결정 기록

### RRF 정규화 버그
- **문제**: `distance = 1.0 - rrf_score` 계산 시 rrf_score가 0.01~0.03 범위이므로
  distance가 항상 0.97~0.99 → confidence 3% → 거의 모든 질문이 에스컬레이션
- **수정**: `distance = 1.0 - rrf_score / (2/61)` — 이론적 최댓값 대비 정규화
- **결과**: 평균 신뢰도 3% → 54.5%, 에스컬레이션 ~100% → 0.2%

### citizen_query_gen 접근 방식
- **배경**: TL 라벨링 데이터(질의응답)의 일부 질문이 분석가 관점의 메타질문
  (예: "다음 내용에서 어린이 체험관은 몇 개월부터 무료라고 했니?")
- **결정**: TS 원천데이터(상담 녹취)에서 `고객:` 발화를 추출해 Ollama로 재작성
  → 실제 시민 관점의 독립적인 민원 질문 생성
- **결과**: 200건 생성, 멀티턴 세션 73개 구성

### POST /admin/questions 타임아웃
- **문제**: admin-api의 `CreateQuestionService`가 rag-orchestrator를 **동기**로 호출하므로
  Ollama LLM 응답 대기(30~90초)가 타임아웃 유발
- **수정**: httpx timeout 15s → 120s 상향 조정
- **참고**: `POST /admin/questions` 호출 시 RAG 답변 생성까지 모두 완료됨
  (별도 `/generate` 호출 불필요)

### JsonParseException 분석
- **증상**: admin-api 로그에 `JsonParseException: Unrecognized character escape '!' (code 33)`
- **영향 없음**: 두 배치 모두 실패 0건으로 완료됨 (에러 격리됨)
- **방어 조치**: rag-orchestrator `app.py`에 answer_text sanitize 추가
  (`re.sub(r'\\([^"\\/bfnrtu\n\r\t])', r'\1', raw_content)`)
- **근본 원인**: Ollama 생성 답변에 `\!` 등 잘못된 escape 시퀀스 포함 가능성
