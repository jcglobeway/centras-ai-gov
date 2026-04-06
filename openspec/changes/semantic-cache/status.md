# Status

- 상태: `proposed`
- 시작일: `2026-04-04`
- 마지막 업데이트: `2026-04-04`

## Progress

- Proposal 작성 완료, 구현 대기 중

## Risks

- `ivfflat` 인덱스는 행 수가 적을 때 효과 미미 (초기엔 sequential scan)
- pgvector 임베딩 생성 비용: Ollama bge-m3 호출 → exact MISS 시 항상 발생
- 임계값 0.92가 너무 엄격하면 HIT율 낮음, 너무 느슨하면 오답 반환 위험
