# Design: admin-dashboard-renewal

> 출처: docs/stitch/design-system/ (3종) + docs/stitch/screens/ (16개 HTML 프로토타입)

---

## 1. 디자인 원칙

### No-Line Rule
레이아웃 섹션 구분에 `1px solid border` 사용 금지.
사이드바와 메인 콘텐츠 영역 경계는 `bg-bg-base`(사이드바) vs `bg-bg-base`를 CSS 변수 값 차이로만 표현.
카드 내부 리스트 구분: `border-b` 대신 `py` 간격 사용.

### Tonal Stacking
깊이는 그림자 대신 배경색 계층으로 표현.
```
bg-bg-base → bg-bg-surface → bg-bg-elevated → bg-bg-prominent
```
예: 페이지 배경(base) 위에 카드(surface), 카드 내부 코드블록(elevated).

### Dual Typeface
- **Inter**: 모든 내비게이션, 헤더, 지문 레이블
- **IBM Plex Mono**: 모든 숫자, 타임스탬프, KPI 값, 상태 코드, ID

---

## 2. 컬러 팔레트

### 다크 테마 — Sovereign AI (`docs/stitch/design-system/03-dark-sovereign.md`)

| CSS 변수 | 값 | 역할 |
|---------|----|------|
| `--bg-base` | `#0b1326` | 페이지 배경, 사이드바 |
| `--bg-surface` | `#131b2e` | 섹션·레이아웃 블록 |
| `--bg-elevated` | `#171f33` | 카드·모듈 |
| `--bg-prominent` | `#2d3449` | 팝오버, 네스트 데이터 파드 |
| `--bg-border` | `#1e2538` | 최후 수단의 테두리 (opacity 20%) |
| `--accent` | `#2563eb` | 주요 CTA, 활성 링크 |
| `--text-primary` | `#dde2ec` | 본문 텍스트 |
| `--text-secondary` | `#8b93a8` | 보조 레이블, 메타데이터 |
| `--text-muted` | `#505868` | 비활성, 플레이스홀더 |
| `--success` | `#10b981` | 정상 상태 |
| `--warning` | `#f59e0b` | 경고 상태 |
| `--error` | `#ef4444` | 위험/오류 상태 |

### 라이트 테마 — Digital Architect (`docs/stitch/design-system/01-bright-mode.md`)

| CSS 변수 | 값 | 역할 |
|---------|----|------|
| `--bg-base` | `#f8f9ff` | 페이지 배경 |
| `--bg-surface` | `#ffffff` | 카드, 사이드바 |
| `--bg-elevated` | `#eff4ff` | 서브섹션, 그루핑 컨테이너 |
| `--bg-prominent` | `#e8edff` | 팝오버, 포커스 영역 |
| `--bg-border` | `rgba(195,198,215,0.2)` | Ghost Border (가시성 최소화) |
| `--accent` | `#004ac6` | 주요 CTA |
| `--text-primary` | `#0b1c30` | 본문 텍스트 (순수 검정 사용 금지) |
| `--text-secondary` | `#434655` | 보조 레이블 |
| `--text-muted` | `#6b7280` | 비활성 |
| `--success` | `#10b981` | 동일 |
| `--warning` | `#f59e0b` | 동일 |
| `--error` | `#ef4444` | 동일 |

---

## 3. 타이포그래피 스케일

| 용도 | 폰트 | 크기 | 굵기 | 특징 |
|------|------|------|------|------|
| 페이지 타이틀 | Inter | 1.5rem | 700 | tracking-tight |
| 카드 제목 | Inter | 0.8125rem (13px) | 600 | — |
| 섹션 헤더 (사이드바) | IBM Plex Mono | 0.625rem (10px) | 700 | uppercase, tracking-widest |
| KPI 값 | IBM Plex Mono | 1.625rem (26px) | 700 | tracking-[-1px] |
| KPI 레이블 | IBM Plex Mono | 0.625rem (10px) | 500 | uppercase, tracking-[0.4px] |
| 테이블 헤더 | IBM Plex Mono | 0.75rem (12px) | 500 | uppercase |
| 배지·칩 | IBM Plex Mono | 0.6875rem (11px) | 600 | — |
| 본문 | Inter | 0.875rem (14px) | 400 | — |
| 보조 텍스트 | Inter | 0.75rem (12px) | 400 | — |

---

## 4. 컴포넌트 디자인 스펙

### 4-1. Sidebar

```
너비: 240px (w-60), 고정(sticky top-0)
배경: bg-bg-base (다크: #0b1326 / 라이트: #f8f9ff)
우측 구분: 없음 — 메인 콘텐츠와 배경색 차이만으로 경계 표현
```

**브랜드 영역 (상단)**
```
패딩: px-6 py-5
로고: "C" 원형 bg-accent 8×8px
앱명: IBM Plex Mono, text-sm, font-bold, tracking-tighter
서브: 역할명, IBM Plex Mono, text-[10px], text-text-muted, uppercase, tracking-widest
```

**섹션 헤더**
```
레이블: IBM Plex Mono, text-[9px], uppercase, tracking-[0.15em], text-text-muted
패딩: px-4 pt-4 pb-1
선행 구분: mt-4 (첫 번째 섹션 제외)
```

**Nav 아이템**
```
기본: px-4 py-2.5, text-text-secondary, hover:text-text-primary hover:bg-bg-elevated
활성: bg-bg-elevated, text-white, border-l-[3px] border-accent
아이콘: Material Symbols, text-[18px], font-variation-settings 'FILL' 0
레이블: Inter, text-xs, font-medium
```

**System Health (하단 고정)**
```
배경: bg-bg-elevated, rounded-lg, mx-3 mb-4 p-3
도트: 6px 원, bg-success, box-shadow: 0 0 8px var(--success) (LED glow)
텍스트: IBM Plex Mono, text-[10px], text-text-secondary, uppercase
```

---

### 4-2. KpiCard

```
배경: bg-bg-elevated
테두리: border border-bg-border (라이트: rgba 20% opacity)
라운딩: rounded-xl
패딩: p-5
hover: -translate-y-px, border-accent/20
```

**내부 구조 (상→하)**
```
1. 레이블 행: IBM Plex Mono 10px uppercase + 상태 pill (우측)
2. 값: IBM Plex Mono 26px bold tracking-[-1px], 상태에 따른 색상
3. 서브 레이블 (선택): text-secondary 13px
4. progress bar: h-1, rounded-full, bg-bg-prominent (트랙), 상태 색 fill
   width = (현재값 / 최대값) × 100%, 애니메이션 없음
```

**상태 pill**
```
ok:       bg-success/10 text-success border border-success/30
warn:     bg-warning/10 text-warning border border-warning/30
critical: bg-error/10 text-error border border-error/30
텍스트: IBM Plex Mono 9px, OPTIMAL / WARNING / CRITICAL
```

**progress bar 기준값 (페이지별 설정)**
```
응답률:    max=100, ok≥90, warn≥80
Fallback율: max=30, ok<10, warn<15 (역방향 — 낮을수록 좋음)
무응답률:  max=20, ok<5, warn<8
응답시간:  max=5000ms
```

---

### 4-3. Card

```
배경: bg-bg-elevated
테두리: border border-bg-border
라운딩: rounded-xl
overflow: hidden
```

**CardHeader**
```
패딩: px-5 pt-5 pb-0
```

**CardTitle**
```
tag (선택): IBM Plex Mono 10px uppercase text-muted, mb-0.5
제목: Inter 13px font-semibold text-primary
```

---

### 4-4. 테마 토글 버튼 (Header)

```
위치: Header 우측, 유저 아바타 좌측
다크→라이트: 아이콘 "light_mode" (Material Symbols)
라이트→다크: 아이콘 "dark_mode" (Material Symbols)
스타일: ghost (배경 없음), text-text-secondary hover:text-text-primary
크기: 36×36px, rounded-full
전환: transition-colors 150ms
```

---

### 4-5. 파이프라인 레이턴시 바 (통합 관제)

```
컨테이너: bg-bg-surface rounded-xl p-5
제목: "파이프라인 레이턴시 (P95)", CardTitle 스타일
단계: Retrieval / Re-ranking / LLM / 후처리
```

각 단계 행:
```
레이블: Inter text-xs text-secondary, 좌측 고정 80px
바: h-2 rounded-full bg-bg-prominent (트랙)
   fill: 단계별 색상 (Retrieval=#2563eb, Reranking=#8b5cf6, LLM=#10b981, 후처리=#f59e0b)
   width: ms 값 비례 (전체 합산 대비 %)
값: IBM Plex Mono text-xs text-primary, 우측 40px
```

> 초기 구현은 하드코딩 샘플값 사용. 추후 실측 API 연동 시 교체.

---

### 4-6. 시스템 상태 신호등 (통합 관제)

```
레이아웃: 3열 그리드
각 항목: 아이콘 + 시스템명 + 상태 pill
판정 로직: 최신 metrics 기반 — KPI 임계값 초과 여부로 자동 판정
```

| 시스템 | 판정 기준 |
|--------|---------|
| RAG 파이프라인 | `fallbackRate ≥ 15%` → 위험, `≥ 10%` → 경고 |
| 지식베이스 | `zeroResultRate ≥ 8%` → 위험, `≥ 5%` → 경고 |
| 응답 품질 | `resolvedRate < 80%` → 위험, `< 90%` → 경고 |

---

### 4-7. 시뮬레이션 룸 A/B 패널

```
레이아웃: 좌우 50%/50% 분할 (lg 이상), 소화면은 탭으로 전환
```

각 패널:
```
헤더: "Version A" / "Version B" pill (blue / purple), 프롬프트 버전 선택 드롭다운
쿼리 입력: textarea, bg-bg-base, rounded-lg, focus:border-accent
실행 버튼: Primary (A) / Secondary (B)
응답 영역: bg-bg-elevated rounded-lg p-4, IBM Plex Mono 없음 (답변 텍스트는 Inter)
```

**Retrieved Chunks 영역** (패널 하단):
```
제목: IBM Plex Mono 10px uppercase "RETRIEVED CHUNKS"
각 청크 행:
  - 순위: IBM Plex Mono 11px text-muted "#1"
  - 유사도: IBM Plex Mono 11px bg-success/10 text-success rounded px-1.5
  - 출처 파일명: text-xs text-secondary truncate
  - 텍스트 미리보기: text-xs text-muted, line-clamp-2
```

---

## 5. 이상 징후 감지 페이지 레이아웃

### KPI 상단 행 (4개)
Query Drift / Recall Deviation / Embedding Drift / 반복 질의 탐지

### 드리프트 차트
```
7일 rolling 기준선 vs 현재값 꺾은선 그래프
차트 라이브러리: recharts (기존 MetricsLineChart 재사용)
데이터: metrics/daily의 fallbackRate/zeroResultRate 시계열 (실측 기반)
```

### 임계값 설정 UI
```
각 지표별 Warning/Critical 수치 입력 필드 (현재 UI 전용, 저장 API 미구현)
버튼: "저장" (비활성 상태로 표시, 추후 연동)
```

---

## 6. 서비스 통계 페이지 레이아웃

```
상단 필터: 기간·기관 선택 (PageFilters 재사용)

Row 1 — 수치 KPI (3개)
  총 질의 수 / DAU (totalQuestions) / Knowledge Gap Rate (zeroResultRate)

Row 2 — 추세 차트
  MetricsLineChart: totalQuestions 7일 추이

Row 3 — 카테고리 분포
  도넛 차트 목업 (recharts PieChart)
  실제 데이터: /admin/faq-candidates 미구현 → 목업 표시 + "추후 연동" 안내 문구
```

---

## 7. Do & Don't

### Do
- 모든 숫자에 IBM Plex Mono 적용
- 라이트 테마에서 텍스트: `var(--text-primary)` (#0b1c30) 사용, 순수 검정 금지
- 카드 hover: `-translate-y-px` + `border-accent/20`로 미묘한 상호작용 표현
- 다크 테마 LED 도트: `box-shadow: 0 0 8px color` glow 효과

### Don't
- `border-r`, `border-b` 등 구조적 1px 실선으로 레이아웃 구분 금지
- drop-shadow (`shadow-md` 등) 사용 금지 — tonal stacking으로 대체
- 카드 라운딩 `rounded-2xl` 이상 사용 금지 — `rounded-xl` (0.75rem) 상한
- 라이트 테마에서 배경 `#ffffff` 직접 사용 금지 — CSS 변수 경유
