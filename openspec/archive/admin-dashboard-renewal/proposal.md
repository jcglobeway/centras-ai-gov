# Proposal: admin-dashboard-renewal

## Problem

현재 프론트엔드 어드민 대시보드는 초기 구축 시점의 "Control Tower" 다크 테마를 사용하지만,
다음 네 가지 문제가 존재한다.

1. **IA 불일치**: 사이드바 메뉴 구조가 IA 최종안(docs/stitch/ia/admin-ia-final.md)과 다르다.
   incidents/safety 페이지가 별도로 존재하며, 서비스 통계(1-2)·이상 징후 감지(5-3) 페이지가 없다.

2. **디자인 시스템 미반영**: Sovereign AI 디자인 시스템(03-dark-sovereign.md)의 팔레트·
   타이포그래피·No-Line Rule이 현재 tailwind.config.ts에 반영되어 있지 않다.

3. **단일 테마**: 다크 테마만 존재. Stitch에 밝은 테마(Digital Architect, 01-bright-mode.md)가
   설계되어 있으나 구현되지 않았다. 사용자별 환경·선호에 따라 테마 전환이 필요하다.

4. **시뮬레이션 룸 미완성**: chatbot-simulator change에서 A/B 2패널 비교와
   Retrieved Chunks 표시가 구현되지 않은 상태다.

## Proposed Solution

두 개 Phase로 나누어 리뉴얼한다.

**Phase 1 — 디자인 시스템 리뉴얼 (다크/라이트 듀얼 테마 포함)**

CSS 변수 기반 테마 아키텍처를 구축하고 다크·라이트 두 테마를 동시에 지원한다.

- **다크 테마 (Sovereign AI)**: docs/stitch/design-system/03-dark-sovereign.md 기준
  - surface: #0b1326, container-low: #131b2e, container: #171f33, prominent: #2d3449
  - primary: #2563eb
- **라이트 테마 (Digital Architect)**: docs/stitch/design-system/01-bright-mode.md 기준
  - surface: #f8f9ff, container-low: #eff4ff, container: #ffffff, on_surface_variant: #434655
  - primary: #004ac6

Tailwind의 `class` 전략(`dark:` 변형자)으로 구현. `<html>` 태그에 `dark` 클래스 토글.
Header에 테마 토글 버튼 추가. 선택값 localStorage 유지.

사이드바 No-Line Rule, IBM Plex Mono 데이터 엔진, 활성 상태 left-border 방식 도입.

**Phase 2 — IA 구조 정렬**
ops 메뉴를 IA 최종안 섹션 헤더 구조로 재편하고,
미존재 페이지(서비스 통계, 이상 징후 감지)를 신규 생성한다.
incidents + safety 페이지를 anomaly 페이지로 통합한다.
시뮬레이션 룸을 A/B 2패널 + Retrieved Chunks 표시로 업그레이드한다.

## 테마 팔레트 대응표

| 토큰 | 다크 (Sovereign) | 라이트 (Digital Architect) |
|------|-----------------|--------------------------|
| `--bg-base` | #0b1326 | #f8f9ff |
| `--bg-surface` | #131b2e | #ffffff |
| `--bg-elevated` | #171f33 | #eff4ff |
| `--bg-prominent` | #2d3449 | #e8edff |
| `--bg-border` | #1e2538 | #c3c6d7 (20% opacity) |
| `--accent` | #2563eb | #004ac6 |
| `--text-primary` | #dde2ec | #0b1c30 |
| `--text-secondary` | #8b93a8 | #434655 |
| `--text-muted` | #505868 | #6b7280 |

## Out of Scope

- 백엔드 API 신규 추가 (시뮬레이터 스트림 파싱 제외)
- client 포털·qa 포털 리뉴얼
- 2-1~2-4, 3-1~3-3, 4-1~4-5, 5-1~5-2, 6-1~6-2, 7-1~7-2 신규 기능 페이지 구현
- 실제 데이터 연동 (신규 페이지는 목업 데이터로 구성)
- 반응형 모바일 레이아웃

## Success Criteria

- `tailwind.config.ts`가 CSS 변수 기반 듀얼 테마를 지원한다.
- Header 토글로 다크/라이트 전환 시 전체 UI가 올바르게 변경된다.
- localStorage에 테마 선택이 유지된다.
- 사이드바가 No-Line Rule을 준수하며 섹션 헤더 그루핑을 갖춘다.
- IBM Plex Mono가 숫자·레이블에 일관 적용된다.
- `/ops/statistics` 페이지가 DAU·Knowledge Gap Rate를 표시한다.
- `/ops/anomaly` 페이지가 이상 징후 감지 지표를 표시한다.
- `/ops/simulator` 페이지가 A/B 2패널 비교와 Retrieved Chunks를 표시한다.
- 기존 Spring Boot 테스트 50개 전부 통과(백엔드 변경 없음).
