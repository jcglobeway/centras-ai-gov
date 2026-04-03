# Status: admin-dashboard-renewal

## 현재 상태

```
Change: admin-dashboard-renewal
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ proposal.md    (완료)
✓ tasks.md       (완료)
✓ 구현 완료      (STEP 1~5 전체 완료)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
완료 일자: 2026-03-29
```

## STEP 1 진행 현황

| 태스크 | 상태 |
|--------|------|
| tailwind.config.ts — darkMode, CSS 변수 토큰, fontFamily | 완료 |
| globals.css — :root (라이트) + .dark (다크) 변수 정의 | 완료 |
| layout.tsx — 깜빡임 방지 스크립트, Material Symbols CDN | 완료 |
| src/lib/theme.ts — useTheme() hook 신규 | 완료 |

## STEP 2 진행 현황

| 태스크 | 상태 |
|--------|------|
| Sidebar.tsx — w-60, border-r 제거, Material Symbols, NavSection, System Health LED | 완료 |
| Header.tsx — 테마 토글 버튼, useTheme() 연결 | 완료 |
| KpiCard.tsx — stripe 제거, progress bar 추가, badge 영문화 | 완료 |
| Card.tsx — CardTitle text-[13px], shadow-sm dark:shadow-none | 완료 |

## STEP 3 진행 현황

| 태스크 | 상태 |
|--------|------|
| 통합 관제 /ops | 완료 |
| 서비스 통계 /ops/statistics | 완료 |
| 품질/보안 요약 /ops/quality-summary | 완료 |
| 데이터 업로드 /ops/upload | 완료 |
| 데이터 전처리 /ops/preprocessing | 완료 |
| 벡터 DB 관리 /ops/vector-db | 완료 |
| 동의어/금칙어 /ops/dictionary | 완료 |
| 프롬프트 /ops/prompt | 완료 |
| RAG 파라미터 /ops/rag-params | 완료 |
| 모델 서빙 /ops/model-serving | 완료 |
| 평가 지표 /ops/quality | 완료 |
| 미해결 질의 /ops/unresolved | 완료 |
| 답변 교정 /ops/correction | 완료 |
| 시뮬레이션 룸 /ops/simulator | 완료 |
| 레드팀 케이스셋 /ops/redteam | 완료 |
| 대화 이력 /ops/chat-history | 완료 |
| 보안 감사 로그 /ops/audit | 완료 |
| 이상 징후 감지 /ops/anomaly | 완료 |
| 성과 분석 리포트 /ops/reports | 완료 |
| 비용 분석 /ops/cost | 완료 |
| 사용자/권한 관리 /ops/users | 완료 |
| 연동 API 관리 /ops/api-keys | 완료 |

## STEP 4 진행 현황

| 태스크 | 상태 |
|--------|------|
| 하드코딩 색상 잔재 탐지 및 CSS 변수 교체 | 완료 |
| MetricsLineChart — 그리드/축/툴팁 CSS 변수화 | 완료 |
| Sidebar 활성 상태 text-white → text-accent font-semibold | 완료 |
| Card shadow-sm dark:shadow-none 추가 | 완료 |
| npm run build 0 errors 확인 | 완료 |

## STEP 5 진행 현황

| 태스크 | 상태 |
|--------|------|
| globals.css :root 라이트 변수값 최종 확인 | 완료 |
| theme.ts 기본값 dark 확인 | 완료 |
| 라이트 테마 전체 CSS 변수 기반 대응 확인 | 완료 |
| 텍스트 대비 확인 (primary/secondary/muted) | 완료 |
| localStorage 테마 유지 로직 확인 | 완료 |

## 이슈 / 결정 사항

- layout.tsx의 Material Symbols CDN link는 ESLint 경고 발생 (`no-page-custom-font`) — 빌드 에러 아님, 무시
- npm run build: 0 errors (경고 2개만 존재, 기존과 동일)
- recharts 인라인 style prop은 Tailwind 클래스로 표현 불가 → CSS 변수 문자열 직접 사용 (`"var(--bg-border)"` 등)
- Sidebar 활성 nav: text-white는 라이트 테마 bg-elevated(#eff4ff) 위에서 가시성 불량 → text-accent로 교체, 다크에서도 accent(#2563eb)는 충분한 대비 확보

## 다음 단계

구현 완료. `/opsx:archive admin-dashboard-renewal` 실행 가능.
