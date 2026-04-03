# Status

## linear-design-system

**상태**: completed
**완료일**: 2026-04-03

### 변경 요약

| 파일 | 내용 |
|------|------|
| `frontend/src/app/globals.css` | Linear 색상 토큰 교체, Inter Variable @import, font-feature-settings cv01/ss03, --card-bg/--card-border 변수 추가 |
| `frontend/tailwind.config.ts` | borderRadius 스케일 업데이트 (6px default), accent-interactive/text-subtle/bg-border-subtle 토큰 추가 |
| `frontend/src/components/ui/Card.tsx` | CSS 변수 기반 반투명 배경/보더, 8px radius |
| `frontend/src/components/ui/Button.tsx` | ghost/secondary Linear 스타일, rounded-md(6px), font-inter weight 510 |
| `frontend/src/components/ui/Badge.tsx` | neutral 반투명 보더, 2px radius, font-inter weight 510 |
| `frontend/src/components/layout/Sidebar.tsx` | Inter UI 폰트, 인디고-바이올렛 활성 accent, 반투명 보더 |
| `frontend/src/components/layout/Header.tsx` | Linear 보더, 드롭다운 8px radius, Inter 폰트 |

### Linear 디자인 매핑 결과

- 배경: `#08090a` (다크) / `#f7f8f8` (라이트)
- 액센트: `#5e6ad2` (primary) / `#7170ff` (interactive)
- 텍스트: `#f7f8f8` → `#d0d6e0` → `#8a8f98` → `#62666d` 4단계
- 보더: `rgba(255,255,255,0.08)` / `rgba(255,255,255,0.05)`
- 폰트: Inter Variable (UI) + Noto Sans KR (본문) + IBM Plex Mono (코드)
