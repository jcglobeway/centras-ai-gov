# Status: ragas-realtime-eval

## 현재 상태: IMPLEMENTED

| Phase | 상태 | 완료 조건 |
|-------|------|-----------|
| Phase 1 — DB 마이그레이션 | 완료 | V037 마이그레이션 파일 생성 + H2 호환 확인 |
| Phase 2 — 집계 API | 완료 | /summary 엔드포인트 동작 + 헥사고날 레이어 전체 |
| Phase 3 — Redis publish | 완료 | answer 저장 후 ragas:eval:queue 메시지 발행 |
| Phase 4 — Redis 구독 | 완료 | realtime-eval 데몬 큐 소비 + ragas_evaluations 저장 |
| Phase 5 — 프론트엔드 | 완료 | quality-summary 집계 API 연동 |

## 이력

- 2026-04-03: proposal.md, tasks.md, status.md 작성 완료
- 2026-04-02: Phase 1~5 전체 구현 완료, 50개 테스트 통과

## 의존 관계

```
Phase 1 (V037)
    └── Phase 2 (organization_id 컬럼 필요)
            └── Phase 5 (summary API 필요)

Phase 3 (Redis publish)
    └── Phase 4 (Redis 큐 소비, POST /ragas-evaluations organizationId 필요)
            └── Phase 2 (organizationId 저장 지원 필요)
```

## 주요 결정 사항

- H2 flyway.target은 "29" 유지 (V037은 PostgreSQL 전용, H2는 적용 안 함)
- Redis publish 실패는 warn 로그만 — answer 저장 트랜잭션에 롤백 없음
- 기존 ragas_evaluations 레코드 백필 없음 (신규부터 organization_id 적용)
- summary API의 previous 기간은 current 기간 길이와 동일한 직전 기간으로 자동 계산
- `spring-boot-starter-data-redis` 미존재 시 이번 Phase 3에서 추가
