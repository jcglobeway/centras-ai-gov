# Tasks

> IA 기준: `docs/stitch/ia/admin-ia-final.md`
>
> **범례**: ✅ 실API 연동 완료 | 🔶 Mock 배지 유지 (백엔드 없음) | ❌ IA 항목 미구현 | ⚠️ 부분 구현

---

## P0 — 프론트만 수정 (백엔드 API 이미 존재)

- [x] P0-A: `frontend/src/lib/api.ts` — `reviewNote` → `reviewComment` 필드명 수정
- [x] P0-B: `frontend/src/app/qa/unresolved/page.tsx` — 리뷰 작성 모달 구현 (QA 상태 머신 적용)
- [x] P0-C: `frontend/src/app/qa/documents/page.tsx` — 버전 이력 모달 구현
- [x] P0-D: `frontend/src/app/ops/statistics/page.tsx` — Knowledge Gap Rate → `/ops/unresolved` Link 연결 (IA 1-2 드릴다운)

---

## P1 — 백엔드 집계 로직 + 프론트 연동

### P1-A: V023 필드 실집계 (metrics-reporting)
> IA 1-5 기관 대시보드 (Customer 포털)

- [ ] `MetricsDailyAggregationService` 에 집계 구현
  - `autoResolutionRate` = is_escalated=false 질문 수 / total
  - `escalationRate` = is_escalated=true 질문 수 / total
  - `revisitRate` = 동일 sessionId에서 2회 이상 질문한 세션 수 / 전체 세션 수
  - `afterHoursRate` = 18시 이후 또는 주말 질문 수 / total
- [ ] `/client/page.tsx` 실집계 값 연동 확인

### P1-B: lowSatisfactionCount 집계 (metrics-reporting)
> IA 1-3 품질/보안 요약 "사용자 피드백" 카드 기반 데이터

- [ ] `lowSatisfactionCount` = feedbacks.rating ≤ 2 건수 집계
- [ ] `/qa/page.tsx` 실집계 값 연동 확인

### P1-C: 카테고리 분포 엔드포인트 (ops/statistics)
> IA 1-2 서비스 통계 "질의 카테고리 분포 도넛 차트"

- [x] `GET /admin/metrics/category-distribution` 신규 (question_category groupBy)
- [x] `/ops/statistics/page.tsx` MOCK_CATEGORIES → 실API 교체

### P1-D: 피드백 추이 엔드포인트 (ops/quality-summary)
> IA 1-3 품질/보안 요약 "사용자 피드백 주간 추이"

- [x] `GET /admin/metrics/feedback-trend` 신규 (feedbacks createdAt 기준 7일 groupBy)
- [x] `/ops/quality-summary/page.tsx` MOCK_FEEDBACK_TREND → 실API 교체

### P1-E: 반복 질의 집계 엔드포인트 (ops/anomaly)
> IA 5-3 이상 징후 감지 "비정상 반복 질의 (DDoS 의심 패턴)"

- [x] `GET /admin/metrics/duplicate-questions` 신규 (questions 중복 questionText 집계)
- [x] `/ops/anomaly/page.tsx` 반복 질의 카드 복구 (실API 연동)

### P1-F: 감사 로그 연동 (ops/audit)
> IA 5-2 보안 감사 로그 — audit_logs 테이블 V003 존재

- [x] `GET /admin/audit-logs` 구현 (LoadAuditLogPort → GetAuditLogsService → AuditLogController)
- [x] `/ops/audit/page.tsx` 관리자 접근 이력 → 실API 교체 (PII·금칙어는 샘플 유지)
  - PII 이벤트·금칙어 차단은 샘플 데이터 유지 (분류기 미구현)

### P1-G: 사용자 목록 연동 (ops/users)
> IA 7-1 사용자/권한 관리 — admin_users 테이블 V001 존재

- [x] `GET /admin/users` 구현 (LoadAdminUsersPort → GetAdminUsersService → AdminUserController)
- [x] `/ops/users/page.tsx` MOCK_USERS → 실API 교체 (RBAC 섹션은 샘플 유지)

### P1-H: 대화 이력 연동 (ops/chat-history)
> IA 5-1 대화 이력 조회 — questions 테이블 연동

- [x] `GET /admin/questions?page_size=30` 연동으로 MOCK_SESSIONS 교체
  - 세션 단위 대신 질문 단위 표시 (sessions API 미구현 → P4-H로 이관)

### P1-I: PII 감지 건수 카드 (ops/quality-summary)
> IA 1-3 품질/보안 요약 "PII 감지 건수 — 이번 달 누적, 마지막 감지 시각 → 5-2 보안 감사 로그 링크"

- [x] audit_logs에서 PII_DETECTED 이벤트 월별 집계 (`GET /admin/metrics/pii-count`)
- [x] `/ops/quality-summary/page.tsx` PII 감지 건수 KpiCard 추가 + /ops/audit 링크 연결

---

## P2 — UI 제거 (실측 불가 허위 수치)

- [x] P2-A: `/ops/cost` — CACHE HIT RATE `status="warn"` 제거, "샘플 데이터" 명시 (구현 유보)
- [x] P2-B: `/ops/anomaly` — EMBEDDING DRIFT KpiCard 제거
- [x] P2-C: `/ops/anomaly` — 안전성 지표 섹션 전체 제거 (PII, OOD, Adversarial, Safety Score, 독성점수)
- [x] P2-D: `/ops/quality` — 이전 버전(v2.3.9) 비교 탭 제거
- [x] P2-E: `/ops/redteam` — KPI 카드 4개 제거 (PII 100%, OOD 89.2%, Adversarial 96.7%, Safety 96.4)

---

## P3 — 레이블 수정

- [x] P3-A: `/client` — `revisitRate` 레이블 "피드백 완료율" → "재방문율" + help 텍스트 교체

---

## P4 — IA 정의 항목 중 인프라 미구비 (이번 태스크 제외, 향후 별도 change)

> IA에 명시된 항목이나 현재 인프라/외부 시스템이 없어 구현 불가. Mock 배지 유지.

| ID | 화면 | IA 항목 | 필요 인프라 |
|----|------|---------|-------------|
| P4-A | 1-1 통합 관제 | 활성 알림 배너 (미해소 알림 실시간 노출) | alerting 메시지 큐 / WebSocket |
| P4-B | 1-1 통합 관제 | 파이프라인 레이턴시 실측 | rag-orchestrator 단계별 latency 계측 |
| P4-C | 4-1 평가 지표 | 버전 비교 탭 복구 | ragas_evaluations.model_version 컬럼 추가 |
| P4-D | 4-4 시뮬레이션 룸 | A/B 비교 패널 (프롬프트/파라미터 버전 좌우) | 세션 다중 지원 |
| P4-E | 4-4 시뮬레이션 룸 | 결과 저장 및 공유 링크 | 저장 API + 링크 생성 |
| P4-F | 4-5 레드팀 | CI/CD 연동 상태 (GitHub Actions 통과율 차단 배너) | GitHub Actions webhook |
| P4-G | 5-3 이상 징후 | Embedding Drift 실측 | pgvector 임베딩 분포 배치 통계 |
| P4-H | 6-2 비용 분석 | 예산 대비 실적 (고객사별 비용 분리) | billing 테이블/API |
| P4-I | 6-1 성과 분석 | PDF/PPT 리포트 자동 생성 | 렌더링 서비스 (Puppeteer 등) |

---

## 마무리

- [x] 전체 변경 커밋 (한국어 메시지)
- [ ] status.md 완료 업데이트
