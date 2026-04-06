# RAG Admin Dashboard — Stitch 작업물 인덱스

> Stitch에서 작업한 디자인 시스템 명세, IA, 화면 프로토타입 전체 목록

---

## 폴더 구조

```
stitch/
├── README.md                          # 이 파일 (전체 인덱스)
├── design-system/                     # 디자인 시스템 명세 (3종)
├── ia/                                # 정보 구조 (IA) 문서
└── screens/
    ├── v1/                            # 1차 이터레이션 (5개 화면)
    ├── v2/                            # 2차 이터레이션 (4개 화면)
    └── features/                      # 기능별 개별 화면 (7개)
```

---

## 디자인 시스템 (`design-system/`)

세 가지 디자인 방향성으로 탐색한 명세서. 각각 컬러 팔레트, 타이포그래피, 컴포넌트 원칙을 담고 있다.

| 파일 | 테마명 | 모드 | 핵심 개념 |
|------|--------|------|-----------|
| [01-bright-mode.md](design-system/01-bright-mode.md) | **The Digital Architect** | 라이트 | Slate 기반 팔레트, Signal Blue 포인트. 테두리 없이 배경 톤 전환으로 레이아웃 정의 |
| [02-dark-kinetic.md](design-system/02-dark-kinetic.md) | **The Obsidian Nerve Center** | 다크 | Deep Sea 네이비+코발트. "Pulse" 그라디언트 애니메이션으로 AI 활성 상태 표현 |
| [03-dark-sovereign.md](design-system/03-dark-sovereign.md) | **The Sovereign Intelligence** | 다크 | 우주 네이비+슬레이트. 항공우주 컨트롤 인터페이스 영감. "Blink" LED 배지 컴포넌트 |

**공통 원칙:**
- **No-Line Rule**: 1px 실선 테두리 금지. 배경색 단계 전환으로 섹션 구분
- **이중 서체**: Inter(UI 레이블) + IBM Plex Mono(숫자·데이터·코드)
- **Tonal Stacking**: drop shadow 대신 surface 계층 중첩으로 깊이 표현

---

## 정보 구조 (`ia/`)

| 파일 | 설명 |
|------|------|
| [admin-ia-final.md](ia/admin-ia-final.md) | **통합 어드민 IA 최종안** — 7개 섹션 22개 메뉴의 RBAC 권한 매트릭스, 각 메뉴별 상세 기능 명세, 페이지 간 이동 흐름도 |

**RBAC 역할:** Admin / Operator / QA / Customer (4종)
**메뉴 구성:** 대시보드(3) · 지식베이스(4) · 챗봇엔진(3) · 품질관리(5) · 모니터링(3) · 보고서(2) · 시스템(2)

---

## 화면 프로토타입

### V1 — 1차 이터레이션 (`screens/v1/`)

초기 탐색 단계. 다크 테마 기반으로 여러 디자인 방향성을 시험한 화면들.

| 폴더 | 화면 | 테마 | 스크린샷 |
|------|------|------|----------|
| [01-dashboard-integrated-control](screens/v1/01-dashboard-integrated-control/) | **통합 관제** (1-1) | Kinetic Intelligence | [screen.png](screens/v1/01-dashboard-integrated-control/screen.png) |
| [02-operations-rag-traces](screens/v1/02-operations-rag-traces/) | **Operations · RAG Traces** | Obsidian AI | [screen.png](screens/v1/02-operations-rag-traces/screen.png) |
| [03-quality-security-summary](screens/v1/03-quality-security-summary/) | **품질/보안 요약** (1-3) | Obsidian Nerve Center | [screen.png](screens/v1/03-quality-security-summary/screen.png) |
| [04-data-upload](screens/v1/04-data-upload/) | **데이터 업로드** (2-1) | RAG OS | [screen.png](screens/v1/04-data-upload/screen.png) |
| [05-data-preprocessing](screens/v1/05-data-preprocessing/) | **데이터 전처리** (2-2) | RAG Admin | [screen.png](screens/v1/05-data-preprocessing/screen.png) |

---

### V2 — 2차 이터레이션 (`screens/v2/`)

**Sovereign AI** 테마로 통일. 레이아웃과 컴포넌트를 정제한 버전.

| 폴더 | 화면 | 비고 | 스크린샷 |
|------|------|------|----------|
| [01-dashboard-integrated-control](screens/v2/01-dashboard-integrated-control/) | **통합 관제** (1-1) | v1 대비 Critical Alert 배너 추가 | [screen.png](screens/v2/01-dashboard-integrated-control/screen.png) |
| [02-cost-analysis](screens/v2/02-cost-analysis/) | **비용 분석** (6-2) | Cost/Query 추이, Token Usage | [screen.png](screens/v2/02-cost-analysis/screen.png) |
| [03-service-statistics](screens/v2/03-service-statistics/) | **서비스 통계** (1-2) | DAU · 세션 성공률 · 카테고리 분포 | [screen.png](screens/v2/03-service-statistics/screen.png) |
| [04-evaluation-metrics](screens/v2/04-evaluation-metrics/) | **평가 지표** (4-1) | Faithfulness · Recall@K 등 6개 지표 | [screen.png](screens/v2/04-evaluation-metrics/screen.png) |

---

### Feature Designs — 기능별 개별 화면 (`screens/features/`)

IA에 정의된 특정 메뉴를 집중 설계한 화면들.

| 폴더 | 화면 | IA 메뉴 | 테마 | 스크린샷 |
|------|------|---------|------|----------|
| [anomaly-detection](screens/features/anomaly-detection/) | **이상 징후 감지** | 5-3 | Kinetic AI | [screen.png](screens/features/anomaly-detection/screen.png) |
| [cost-analysis](screens/features/cost-analysis/) | **비용 분석** | 6-2 | Kinetic Intelligence | [screen.png](screens/features/cost-analysis/screen.png) |
| [evaluation-metrics](screens/features/evaluation-metrics/) | **평가 지표** | 4-1 | RAG Ops | [screen.png](screens/features/evaluation-metrics/screen.png) |
| [red-team-case-set](screens/features/red-team-case-set/) | **레드팀 케이스셋** | 4-5 | Obsidian AI | [screen.png](screens/features/red-team-case-set/screen.png) |
| [simulation-room](screens/features/simulation-room/) | **시뮬레이션 룸** | 4-4 | — | [screen.png](screens/features/simulation-room/screen.png) |
| [unresolved-queries](screens/features/unresolved-queries/) | **미해결 질의** | 4-2 | Kinetic AI | [screen.png](screens/features/unresolved-queries/screen.png) |
| [user-permission-management](screens/features/user-permission-management/) | **사용자/권한 관리** | 7-1 | — | [screen.png](screens/features/user-permission-management/screen.png) |

---

## IA 메뉴 커버리지

| IA 섹션 | 메뉴 | 화면 |
|---------|------|------|
| **1. 대시보드** | 1-1 통합 관제 | v1/01, v2/01 |
| | 1-2 서비스 통계 | v2/03 |
| | 1-3 품질/보안 요약 | v1/03 |
| **2. 지식베이스** | 2-1 데이터 업로드 | v1/04 |
| | 2-2 데이터 전처리 | v1/05 |
| | 2-3 벡터 DB 관리 | — |
| | 2-4 동의어/금칙어 | — |
| **3. 챗봇 엔진** | 3-1 프롬프트 | — |
| | 3-2 RAG 파라미터 | — |
| | 3-3 모델 서빙 | v1/02 (Operations) |
| **4. 품질 관리** | 4-1 평가 지표 | v2/04, features/evaluation-metrics |
| | 4-2 미해결 질의 | features/unresolved-queries |
| | 4-3 답변 교정 | — |
| | 4-4 시뮬레이션 룸 | features/simulation-room |
| | 4-5 레드팀 케이스셋 | features/red-team-case-set |
| **5. 모니터링** | 5-1 대화 이력 | — |
| | 5-2 보안 감사 로그 | — |
| | 5-3 이상 징후 감지 | features/anomaly-detection |
| **6. 보고서** | 6-1 성과 분석 리포트 | — |
| | 6-2 비용 분석 | v2/02, features/cost-analysis |
| **7. 시스템** | 7-1 사용자/권한 관리 | features/user-permission-management |
| | 7-2 연동 API 관리 | — |

---

## 참고

- 각 화면 폴더에는 `code.html` (소스 코드)과 `screen.png` (스크린샷)이 있다.
- `DESIGN_DARK_MODE.md`와 `vector_deep_technical/DESIGN.md`는 동일 파일이었음 → `03-dark-sovereign.md`로 통합.
- `vector_deep/DESIGN.md` → `02-dark-kinetic.md`로 이동 (별도 디자인 방향성).
