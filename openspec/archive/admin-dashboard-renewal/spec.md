# Spec: admin-dashboard-renewal

---

## 1. 테마 아키텍처

### 1-1. CSS 변수 (`frontend/src/app/globals.css`)

```css
:root {
  /* Light — Digital Architect */
  --bg-base:      #f8f9ff;
  --bg-surface:   #ffffff;
  --bg-elevated:  #eff4ff;
  --bg-prominent: #e8edff;
  --bg-border:    rgba(195, 198, 215, 0.2);
  --accent:       #004ac6;
  --text-primary:   #0b1c30;
  --text-secondary: #434655;
  --text-muted:     #6b7280;
}

.dark {
  /* Dark — Sovereign AI */
  --bg-base:      #0b1326;
  --bg-surface:   #131b2e;
  --bg-elevated:  #171f33;
  --bg-prominent: #2d3449;
  --bg-border:    #1e2538;
  --accent:       #2563eb;
  --text-primary:   #dde2ec;
  --text-secondary: #8b93a8;
  --text-muted:     #505868;
}
```

### 1-2. Tailwind 설정 (`frontend/tailwind.config.ts`)

```ts
const config: Config = {
  darkMode: 'class',   // ← 추가
  theme: {
    extend: {
      colors: {
        bg: {
          base:      'var(--bg-base)',
          surface:   'var(--bg-surface)',
          elevated:  'var(--bg-elevated)',
          prominent: 'var(--bg-prominent)',
          border:    'var(--bg-border)',
        },
        accent: {
          DEFAULT: 'var(--accent)',
          // hover/muted는 고정값 유지
          hover: '#3a8ef0',
          muted: '#1d4ed8',
        },
        text: {
          primary:   'var(--text-primary)',
          secondary: 'var(--text-secondary)',
          muted:     'var(--text-muted)',
        },
        success: '#10b981',
        warning: '#f59e0b',
        error:   '#ef4444',
      },
      fontFamily: {
        sans:      ['Noto Sans KR', 'sans-serif'],
        mono:      ['IBM Plex Mono', 'monospace'],   // 교체
        inter:     ['Inter', 'sans-serif'],           // 신규
        'plex-mono': ['IBM Plex Mono', 'monospace'], // alias
      },
    },
  },
};
```

### 1-3. 테마 Hook (`frontend/src/lib/theme.ts`)

```ts
'use client';

import { useEffect, useState } from 'react';

type Theme = 'dark' | 'light';

export function useTheme() {
  const [theme, setTheme] = useState<Theme>('dark'); // 기본: 다크

  useEffect(() => {
    const stored = localStorage.getItem('theme') as Theme | null;
    const initial = stored ?? 'dark';
    applyTheme(initial);
    setTheme(initial);
  }, []);

  function toggle() {
    const next: Theme = theme === 'dark' ? 'light' : 'dark';
    applyTheme(next);
    localStorage.setItem('theme', next);
    setTheme(next);
  }

  return { theme, toggle };
}

function applyTheme(theme: Theme) {
  if (theme === 'dark') {
    document.documentElement.classList.add('dark');
  } else {
    document.documentElement.classList.remove('dark');
  }
}
```

### 1-4. 초기 깜빡임 방지 스크립트 (`frontend/src/app/layout.tsx`)

`<head>` 안에 인라인 스크립트 추가 (Next.js `Script` 컴포넌트 `strategy="beforeInteractive"`):

```ts
const themeScript = `
  (function() {
    var theme = localStorage.getItem('theme') || 'dark';
    if (theme === 'dark') document.documentElement.classList.add('dark');
  })();
`;
// <script dangerouslySetInnerHTML={{ __html: themeScript }} />
```

---

## 2. 폰트 설정 (`frontend/src/app/layout.tsx`)

Google Fonts import (Next.js `next/font/google` 사용):

```ts
import { Inter, IBM_Plex_Mono, Noto_Sans_KR } from 'next/font/google';

const inter = Inter({ subsets: ['latin'], variable: '--font-inter' });
const plexMono = IBM_Plex_Mono({
  weight: ['400', '500', '600'],
  subsets: ['latin'],
  variable: '--font-plex-mono',
});
const notoSansKR = Noto_Sans_KR({
  weight: ['400', '500', '700'],
  subsets: ['latin'],
  variable: '--font-noto',
});
```

`<body>` className에 세 변수 모두 적용.

---

## 3. Sidebar 메뉴 구조

### ops 포털 최종 메뉴 (IA 기준)

```ts
const OPS_NAV = [
  // 섹션: 대시보드
  { section: '대시보드' },
  { href: '/ops',            label: '통합 관제',     icon: 'dashboard' },
  { href: '/ops/statistics', label: '서비스 통계',   icon: 'bar_chart' },  // 신규
  // 섹션: 품질 관리
  { section: '품질 관리' },
  { href: '/ops/quality',    label: '평가 지표',     icon: 'analytics' },
  { href: '/ops/unresolved', label: '미해결 질의',   icon: 'help_center' }, // 신규 (qa에도 유지)
  { href: '/ops/simulator',  label: '시뮬레이션 룸', icon: 'science' },
  // 섹션: 서비스 모니터링
  { section: '서비스 모니터링' },
  { href: '/ops/anomaly',    label: '이상 징후 감지', icon: 'warning' }, // 신규, incidents+safety 통합
  // 섹션: 운영
  { section: '운영' },
  { href: '/ops/indexing',       label: 'RAG 인덱싱',    icon: 'storage' },
  { href: '/ops/organizations',  label: '기관 관리',     icon: 'domain' },
  // 섹션: 통계 및 보고서
  { section: '통계 및 보고서' },
  { href: '/ops/cost',           label: '비용 분석',     icon: 'payments' },
];
```

> `icon` 값은 Material Symbols 아이콘명.
> 제거 메뉴: `/ops/incidents`, `/ops/safety`

### Sidebar 컴포넌트 타입 변경

```ts
interface NavItem {
  href: string;
  label: string;
  icon: string; // Material Symbols name (기존 이모지에서 변경)
}

interface NavSection {
  section: string; // 섹션 헤더
}

type NavEntry = NavItem | NavSection;
```

---

## 4. 통합 관제 페이지 (`/ops/page.tsx`)

### 4-1. KPI 카드 매핑

| 레이블 | 데이터 필드 | 변환 | ok | warn | critical | max(progress bar) |
|--------|------------|------|-----|------|----------|-------------------|
| ANSWER RATE | `resolvedRate` | — | ≥90% | ≥80% | <80% | 100 |
| ERROR RATE | `fallbackRate` | — | <10% | <15% | ≥15% | 30 |
| E2E LATENCY | `avgResponseTimeMs` | — | <1500ms | <2500ms | ≥2500ms | 5000 |
| KNOWLEDGE GAP | `zeroResultRate` | — | <5% | <8% | ≥8% | 20 |
| COST / QUERY | `llm.avgCostPerQuery` | — | <$0.008 | <$0.012 | ≥$0.012 | 가변 |

> Cost/Query는 `/api/admin/metrics/llm` 추가 호출. 기존 ops/cost/page.tsx 패턴 참조.

### 4-2. 파이프라인 레이턴시 바 (초기 하드코딩)

```ts
const PIPELINE_STAGES = [
  { label: 'Retrieval',  ms: 142, color: '#2563eb' },
  { label: 'Re-ranking', ms:  38, color: '#8b5cf6' },
  { label: 'LLM',        ms: 980, color: '#10b981' },
  { label: '후처리',      ms:  24, color: '#f59e0b' },
] as const;
// total = 1184ms
```

### 4-3. 시스템 신호등 판정 로직

```ts
type SystemStatus = 'ok' | 'warn' | 'critical';

function calcSystemStatus(latest: DailyMetric | undefined): {
  pipeline: SystemStatus;
  knowledge: SystemStatus;
  quality: SystemStatus;
} {
  const fallback = latest?.fallbackRate ?? 0;
  const zero    = latest?.zeroResultRate ?? 0;
  const resolved = latest?.resolvedRate ?? 100;

  return {
    pipeline:  fallback >= 15 ? 'critical' : fallback >= 10 ? 'warn' : 'ok',
    knowledge: zero >= 8     ? 'critical' : zero >= 5      ? 'warn' : 'ok',
    quality:   resolved < 80 ? 'critical' : resolved < 90  ? 'warn' : 'ok',
  };
}
```

---

## 5. 서비스 통계 페이지 (`/ops/statistics/page.tsx`)

### API 호출

```ts
// 기존 API 재사용
GET /api/admin/metrics/daily?page_size=14&organization_id=&from_date=&to_date=
```

### 카테고리 분포 도넛 차트 (목업)

```ts
const MOCK_CATEGORIES = [
  { name: '복지/급여', value: 34 },
  { name: '민원/신청', value: 28 },
  { name: '교육/취업', value: 18 },
  { name: '교통/주차', value: 12 },
  { name: '기타',      value:  8 },
];
```

recharts `PieChart` + `Cell` 사용. 상단에 `"※ 카테고리 분류 API 연동 전 샘플 데이터"` 안내 문구.

### Knowledge Gap Rate 클릭 → 미해결 질의 이동

```tsx
<Link href="/ops/unresolved" className="text-accent text-xs hover:underline">
  미해결 질의 바로가기 →
</Link>
```

---

## 6. 이상 징후 감지 페이지 (`/ops/anomaly/page.tsx`)

### API 호출

```ts
// 기존 API 재사용
GET /api/admin/metrics/daily?page_size=14   // 7일 drift 계산
GET /api/admin/questions?page_size=100      // 반복 질의 탐지 (기존 safety 로직)
```

### Query Drift 계산

```ts
// 7일 평균 대비 최신값 이탈률
function calcDrift(metrics: DailyMetric[], field: keyof DailyMetric): number | null {
  if (metrics.length < 2) return null;
  const values = metrics.map(m => Number(m[field]) ?? 0);
  const baseline = values.slice(0, -1).reduce((a, b) => a + b, 0) / (values.length - 1);
  const latest = values[values.length - 1];
  return baseline > 0 ? ((latest - baseline) / baseline) * 100 : null;
}
```

### 임계값 설정 UI (저장 미연동)

```ts
interface ThresholdConfig {
  metric: string;
  label: string;
  warnValue: number;
  criticalValue: number;
  unit: string;
}

const DEFAULT_THRESHOLDS: ThresholdConfig[] = [
  { metric: 'fallbackRate',      label: 'Fallback율',   warnValue: 10, criticalValue: 15, unit: '%' },
  { metric: 'zeroResultRate',    label: '무응답률',     warnValue:  5, criticalValue:  8, unit: '%' },
  { metric: 'avgResponseTimeMs', label: '평균 응답시간', warnValue: 1500, criticalValue: 2500, unit: 'ms' },
];
```

저장 버튼: `disabled` 상태 + tooltip "API 연동 후 활성화 예정".

---

## 7. 시뮬레이션 룸 업그레이드 (`/ops/simulator/page.tsx`)

### 상태 구조

```ts
interface PanelState {
  version: 'A' | 'B';
  query: string;
  response: string;
  chunks: RetrievedChunk[];
  isLoading: boolean;
}

interface RetrievedChunk {
  rank: number;
  score: number;
  source: string;   // 파일명
  text: string;     // 미리보기 (100자)
}
```

### rag-orchestrator 응답 포맷 (`/generate/stream`)

**변경 전**: NDJSON 텍스트 스트림
**변경 후**: 첫 번째 청크에 metadata 포함

```json
// 청크 1: retrieved docs metadata
{"type": "chunks", "data": [
  {"rank": 1, "score": 0.923, "source": "welfare_guide_2026.pdf", "text": "..."},
  {"rank": 2, "score": 0.887, "source": "benefit_table.xlsx", "text": "..."}
]}

// 청크 2~N: 텍스트 스트림
{"type": "content", "text": "국민기초생활보장제도는..."}

// 마지막 청크
{"type": "done"}
```

### Next.js API Route (`/api/simulator/chat/route.ts`) 변경

```ts
// rag-orchestrator 스트림에서 chunks 청크 분리
// chunks → response header 'X-Retrieved-Chunks' (JSON) 또는 첫 번째 SSE 이벤트로 전달
// content → 기존 텍스트 스트림 그대로
```

### rag-orchestrator 변경 (`app.py`)

```python
# /generate/stream 엔드포인트 (기존 로직 재사용)
async def stream_generate(request):
    ...
    chunks = hybrid_search(query, org_id)  # 기존 함수

    # 1. retrieved chunks 먼저 emit
    yield json.dumps({
        "type": "chunks",
        "data": [
            {"rank": i+1, "score": float(c["score"]), "source": c["filename"], "text": c["text"][:100]}
            for i, c in enumerate(chunks)
        ]
    }) + "\n"

    # 2. LLM 스트림
    async for token in llm_stream(query, chunks):
        yield json.dumps({"type": "content", "text": token}) + "\n"

    yield json.dumps({"type": "done"}) + "\n"
```

---

## 8. 삭제 파일

| 파일 | 이유 |
|------|------|
| `frontend/src/app/ops/incidents/page.tsx` | `/ops/anomaly`로 통합 |
| `frontend/src/app/ops/safety/page.tsx` | `/ops/anomaly`로 통합 |

Sidebar `OPS_NAV`에서 `/ops/incidents`, `/ops/safety` 항목도 제거.

---

## 9. 기존 API 사용 현황 (신규 없음)

| 엔드포인트 | 사용 페이지 |
|-----------|-----------|
| `GET /admin/metrics/daily` | 통합 관제, 서비스 통계, 이상 징후 감지 |
| `GET /admin/metrics/llm` | 통합 관제 (Cost/Query KPI) |
| `GET /admin/questions/unresolved` | 미해결 질의 |
| `GET /admin/questions` | 이상 징후 감지 (반복 질의) |
| `POST /admin/simulator/sessions` | 시뮬레이션 룸 |
| `POST /api/simulator/chat` (Next.js route) | 시뮬레이션 룸 |

---

## 10. 검증 체크리스트

| 체크 | 항목 |
|------|------|
| [ ] | 라이트 테마 전환 시 `<html>` 태그에 `dark` 클래스 제거 확인 |
| [ ] | 다크↔라이트 전환 후 localStorage `theme` 키 확인 |
| [ ] | 페이지 새로고침 후 테마 유지 확인 (깜빡임 없음) |
| [ ] | IBM Plex Mono가 KPI 값·레이블·배지에 적용 확인 |
| [ ] | Sidebar border-r 제거 확인 (dev tools에서 computed border 없음) |
| [ ] | 활성 nav 항목에 `border-l-[3px]` 적용 확인 |
| [ ] | `/ops/statistics` 렌더링 확인 (메트릭 API 데이터 표시) |
| [ ] | `/ops/anomaly` 렌더링 확인 (incidents + safety 내용 포함) |
| [ ] | `/ops/incidents`, `/ops/safety` 404 반환 확인 |
| [ ] | 시뮬레이터 A 패널 전송 시 B 패널 비영향 확인 |
| [ ] | Retrieved Chunks 영역 표시 확인 (rag-orchestrator 연동 시) |
| [ ] | `npm run build` 0 errors |
| [ ] | `JAVA_HOME=... ./gradlew test` 50개 전부 통과 (백엔드 무변경 확인) |
