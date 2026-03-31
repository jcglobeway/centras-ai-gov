# Proposal

## Change ID

`stitch-design-alignment`

## Summary

- **변경 목적**: docs/stitch v2 HTML 샘플 및 design-system/02-dark-kinetic.md 명세에 맞게 frontend 비주얼 스타일을 정렬하고 가시성을 개선
- **변경 범위**: frontend 한정 (Tailwind 설정, globals.css, 공유 컴포넌트)
- **제외 범위**: 페이지별 레이아웃 로직, API 연동, 백엔드

## 발견된 차이점

### 1. Border Radius — Tailwind 기본값 미오버라이드
- Stitch 명세: DEFAULT `2px`, lg `4px`, xl `8px` (기술적 날카로움)
- 현재: Tailwind 기본 — lg `8px`, xl `12px` (소비자 앱 수준)
- 영향: `KpiCard`, `Card`, `Header` 드롭다운 등 모든 `rounded-*` 클래스

### 2. 카드 Border — "No-Line Rule" 위반
- Stitch 명세: 경계는 배경 색 전환으로만 표현. `outline-variant/10` (10% 불투명) "Ghost Border" 허용
- 현재: `border border-bg-border` (`#253047`, 완전 불투명) → 과도하게 시각적
- 영향: `Card`, `KpiCard`, `Header`

### 3. 폰트 — Inter 미적용
- Stitch 명세: 영문 헤드라인/레이블은 `Inter`, 숫자는 `IBM Plex Mono`
- 현재: body 기본이 `Noto Sans KR` (한국어 대응 유지 필요하나 Inter 병행 없음)
- 영향: `globals.css`, `tailwind.config.ts`

### 4. 헤더 높이
- Stitch: `h-16` (64px)
- 현재: `h-12` (48px)

### 5. 텍스트 색상 — 파란빛 부재
- Stitch `on-surface`: `#dae2fd` (cobalt-tinted)
- 현재 `--text-primary` dark: `#dde2ec` (중립)

### 6. KPI 숫자 색상 — status 색상 vs 흰색
- Stitch: 숫자 항상 흰색, 상태는 우측 pill만으로 표현
- 현재: 숫자 자체가 `text-success/error` 로 색상 변경됨

## 구현 계획

| 파일 | 변경 내용 |
|------|---------|
| `frontend/tailwind.config.ts` | `borderRadius` 오버라이드 추가 (Stitch 명세값) |
| `frontend/src/app/globals.css` | Inter 폰트 로드 추가, dark `--text-primary` → `#dae2fd`, `--bg-border` opacity 기반으로 재정의 |
| `frontend/src/components/ui/Card.tsx` | border를 `border-white/5` (5% 불투명)로 변경, `rounded-xl` → `rounded-lg` |
| `frontend/src/components/charts/KpiCard.tsx` | border 동일 처리, KPI 숫자 색상 `text-text-primary`로 통일 |
| `frontend/src/components/layout/Header.tsx` | `h-12` → `h-16`, border-b opacity 낮추기 |
| `frontend/src/app/ops/layout.tsx` (필요시) | 헤더 높이 변경에 따른 레이아웃 조정 |

## Impact

- **영향 모듈**: frontend만 (CSS/컴포넌트 변경)
- **영향 API**: 없음
- **영향 테스트**: 없음 (UI 스타일만)

## Done Definition

- [ ] Stitch v2 HTML과 나란히 놓고 보았을 때 전체적인 무드가 유사함
- [ ] 카드 border가 Ghost 수준으로 거의 보이지 않음
- [ ] KPI 카드 숫자가 흰색, 상태 pill로만 상태 표현
- [ ] Header 높이 64px
- [ ] 모든 기존 페이지가 레이아웃 깨짐 없이 정상 렌더링
