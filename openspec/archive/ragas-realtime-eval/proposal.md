# Proposal: ragas-realtime-eval

## Problem

현재 RAGAS 평가는 eval-runner CLI(`eval-runner` 명령)를 수동으로 실행해야만 데이터가
쌓인다. 이로 인해 세 가지 문제가 발생한다.

1. **평가 지연**: 답변이 생성되고도 평가가 수 시간~수 일 뒤에야 반영된다.
2. **데이터 공백**: 수동 실행을 빠뜨린 날은 ragas_evaluations 레코드가 없어 품질
   추이를 추적할 수 없다.
3. **프론트엔드 오사용**: `ragas_evaluations`에 `organization_id`가 없어
   `quality-summary` 페이지가 `page_size=2`로 개별 레코드를 집계값처럼 사용하는
   잘못된 패턴이 고착됐다.

## Proposed Solution

다섯 개 Phase로 나눠 이벤트 드리븐 실시간 평가 파이프라인을 구축한다.

- **Phase 1 — DB 마이그레이션**: `ragas_evaluations`에 `organization_id` 컬럼 추가
  (V037). 기존 테스트(H2, flyway.target="29")는 영향 없음.
- **Phase 2 — 집계 API**: `GET /admin/ragas-evaluations/summary` 신규 엔드포인트.
  current/previous 기간 평균 지표를 반환하며 헥사고날 레이어 전체를 포함한다.
- **Phase 3 — Redis publish (admin-api)**: answer 저장 완료 후 `ragas:eval:queue`에
  평가 요청 JSON을 push한다. publish 실패는 warn 로그만 남기고 answer 트랜잭션에
  영향을 주지 않는다. `spring-boot-starter-data-redis` 의존성이 없으면 추가한다.
- **Phase 4 — Redis 구독 (eval-runner)**: `realtime_eval.py` 신규 모듈. BRPOP으로
  큐를 구독하고 기존 RAGAS 계산 로직을 재사용해 평가 후 `POST /admin/ragas-evaluations`
  를 호출한다. 에러 시 `ragas:eval:dlq`에 push하고 계속 실행한다.
- **Phase 5 — 프론트엔드**: `quality-summary/page.tsx`에서 `/summary` API를 사용해
  current/previous 집계값으로 KPI 카드와 레이더·스코어 바를 표시한다.

## Out of Scope

- 기존 `ragas_evaluations` 레코드의 `organization_id` 백필 (신규 레코드부터 적용)
- eval-runner 배치 모드(`eval-runner` CLI) 변경
- Kubernetes/docker-compose Redis 설정 변경 (기존 Redis 컨테이너 재사용)
- RAGAS 지표 종류 변경 (faithfulness / answerRelevancy / contextPrecision / contextRecall 유지)
- 평가 실패 DLQ 재처리 자동화

## Success Criteria

- [ ] answer 저장 후 5초 이내에 `ragas:eval:queue`에 메시지가 발행된다
- [ ] `realtime-eval` 데몬이 큐를 소비해 `ragas_evaluations`에 organization_id 포함
  레코드를 저장한다
- [ ] `GET /admin/ragas-evaluations/summary` 가 current/previous 기간 평균을 반환한다
- [ ] `quality-summary` 페이지가 집계 API의 current.avg* 필드를 표시한다
- [ ] 기존 50개 테스트가 계속 통과한다
