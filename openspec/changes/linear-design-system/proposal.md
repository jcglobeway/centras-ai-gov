# Proposal

## Change ID

`linear-design-system`

## Summary

- **변경 목적**: 현재 "Control Tower / Sovereign AI" 다크 테마를 Linear 디자인 시스템으로 교체. 거의 검정에 가까운 캔버스(`#08090a`), 인디고-바이올렛 단일 액센트(`#5e6ad2`), Inter Variable + OpenType cv01/ss03, 반투명 흰색 보더로 구성된 Linear의 dark-mode-native 언어를 적용한다.
- **변경 범위**: CSS 변수(색상 토큰), Tailwind 설정, Inter Variable 폰트 로드, UI 컴포넌트 5개(Card, Button, Badge, Sidebar, Header)
- **제외 범위**: 페이지별 레이아웃/비즈니스 로직, 백엔드 API, 테스트 코드. Noto Sans KR은 한국어 본문용으로 유지.

## Impact

- **영향 모듈**: `frontend/` 전체 (CSS 토큰 기반이므로 모든 페이지에 반영)
- **영향 API**: 없음
- **영향 테스트**: 없음 (프론트엔드 유닛 테스트 부재)

## Done Definition

- [ ] 다크 모드 배경이 `#08090a` (마케팅 블랙) 기준으로 렌더링됨
- [ ] 액센트 색상이 인디고-바이올렛(`#5e6ad2`)으로 적용됨
- [ ] Inter Variable 폰트가 UI 크롬(사이드바, 헤더, 버튼, 레이블)에 적용됨
- [ ] 카드 배경이 `rgba(255,255,255,0.02)`, 보더가 `rgba(255,255,255,0.08)`로 렌더링됨
- [ ] Ghost 버튼이 반투명 배경(`rgba(255,255,255,0.02)`)으로 렌더링됨
- [ ] `npm run build` 오류 없음
