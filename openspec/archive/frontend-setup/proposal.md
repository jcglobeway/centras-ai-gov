# Proposal: frontend-setup

## 목적

Next.js 15 기반 어드민 포털 프론트엔드의 기반 인프라를 구성한다.
공통 레이아웃, API 클라이언트, 인증 컨텍스트, UI 컴포넌트 등을 포함한다.

## 범위

- `frontend/` 디렉토리 기반 Next.js 15 (App Router) 프로젝트
- TypeScript, Tailwind CSS v3, SWR, Recharts
- "Control Tower" 다크 테마 (`#080D17` 배경, `#3B82F6` 어센트)
- API 프록시: `/api/admin/*` → `localhost:8080/admin/*`

## 생성 파일

- `src/lib/types.ts` — 전체 API 타입
- `src/lib/api.ts` — API 클라이언트
- `src/lib/auth.tsx` — AuthContext, useAuth hook
- `src/components/layout/Sidebar.tsx`
- `src/components/layout/Header.tsx`
- `src/components/ui/` — Button, Card, Badge, Input, Table, Spinner
- `src/components/charts/KpiCard.tsx`, `MetricsLineChart.tsx`
- `src/app/layout.tsx`, `src/app/page.tsx`
- `src/app/login/page.tsx`
- Tailwind/PostCSS/tsconfig 설정

## 영향

- Phase 2 프론트엔드 포털 3개의 기반
- 백엔드 API 계약 변경 없음
