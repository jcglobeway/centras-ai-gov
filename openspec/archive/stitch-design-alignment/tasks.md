# Tasks — stitch-design-alignment

## 1. tailwind.config.ts — border radius 오버라이드
- [x] `borderRadius` 추가: DEFAULT `0.125rem`, sm `0.125rem`, md `0.25rem`, lg `0.25rem`, xl `0.5rem`, `2xl` `0.75rem`

## 2. globals.css — 색상 및 폰트 조정
- [x] Inter 폰트 Google Fonts import 추가 (wght 800 포함)
- [x] dark `--text-primary`: `#dde2ec` → `#dae2fd`
- [x] dark `--bg-border`: `#253047` → `rgba(255,255,255,0.06)` (Ghost 수준)

## 3. Card.tsx — border & radius
- [x] `border border-bg-border` → `border border-white/5`
- [x] `rounded-xl` → `rounded-lg`
- [x] hover border 제거 (hover는 배경색 전환으로 대체)

## 4. KpiCard.tsx — border, radius, 숫자 색상
- [x] `border border-bg-border` → `border border-white/5`
- [x] hover: translate 제거 → `hover:bg-bg-prominent` (배경 전환)
- [x] KPI 숫자 색상: 항상 `text-text-primary`로 통일

## 5. Header.tsx — 높이 & border
- [x] `h-12` → `h-16`
- [x] `border-b border-bg-border` → `border-b border-white/5`
- [x] 패딩 `px-4` → `px-6`

## 6. 레이아웃 조정
- [x] flex-col 구조로 자동 적응 — 별도 수정 불필요

## 7. 가시성 개선 — 폰트 크기 전체 상향
- [x] Table.tsx: Th `10px`→`11px`, Td `11px`→`text-xs`(12px)
- [x] Badge.tsx: `10px`→`11px`, 패딩 증가
- [x] ProgressBar.tsx: `10px`→`11px`
- [x] Card.tsx: tag 레이블 `10px`→`11px`
- [x] KpiCard.tsx: label/sub/pill 크기 상향, trend/tooltip `text-xs`
- [x] Sidebar.tsx: section/System Health/Operational 크기 상향
- [x] Header.tsx: role 텍스트 `10px`→`11px`

## 8. 커밋
- [x] 단일 커밋, 한국어 메시지
