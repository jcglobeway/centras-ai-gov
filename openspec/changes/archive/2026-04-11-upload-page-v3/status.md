# Status: upload-page-v3

- 상태: `implemented`
- 시작일: `2026-04-07`
- 마지막 업데이트: `2026-04-10`

## Progress

- OpenSpec 아티팩트(proposal, tasks, status) 작성 완료
- Phase 1~5 구현 완료 (`frontend/src/app/ops/upload/page.tsx`)

## 구현 요약

### 변경 사항

1. **게이트 조건**: `isGateOpen` 플래그로 org + service 미선택 시 파일 업로드, 웹 크롤, 리인덱싱 세 카드를 `opacity-40 pointer-events-none select-none` 으로 감쌈

2. **컬렉션 피커 (파일 업로드)**: `uploadCollectionMode` / `uploadSelectedCollection` / `uploadNewCollection` 상태로 기존 컬렉션 선택 또는 "＋ 새 컬렉션 추가" 인라인 입력 처리

3. **컬렉션 피커 (웹 크롤)**: `crawlCollectionMode` / `crawlSelectedCollection` / `crawlNewCollection` 상태로 동일 패턴 적용. 기존 `crawlCollectionMode` 변수명 충돌 방지를 위해 수집 모드 변수는 `crawlCollectionModeField` 로 변경

4. **리인덱싱 카드**: `selectedSourceId` → `selectedReindexCollection` 교체. 컬렉션 단위 드롭다운 + "N개 소스 포함" 카운트 표시. `handleRunReindex` 가 `sourcesInCollection` 배열을 순회하며 `ingestionApi.runSource()` 순차 호출

5. **크롤소스 SWR**: 전체 URL 대신 `?organization_id=${selectedOrgId}` 파라미터 포함 URL로 교체. 클라이언트 측에서 `serviceId` 필터링

### 제거된 상태 변수
- `uploadCollectionName` (free text) → 피커 상태 3개로 교체
- `crawlCollectionNameField` (free text) → 피커 상태 3개로 교체
- `selectedSourceId` → `selectedReindexCollection` 으로 교체

## Verification

- 브라우저 수동 검증 완료 (2026-04-10)
  - 기관/서비스 미선택 시 게이트 안내 문구 표시 및 업로드/크롤/리인덱싱 섹션 비활성 확인
  - 기관/서비스 선택 시 `GET /api/admin/crawl-sources?organization_id=org_namgu` 호출 `200` 확인
  - 파일 업로드 컬렉션 피커에서 `＋ 새 컬렉션 추가` 선택 시 인라인 입력 렌더링 확인
  - `/ops/upload`에서 PDF 업로드 실행 후 `POST /api/admin/documents/upload` `201` 확인
  - 생성된 소스 `crawl_src_84b7869e`의 `collectionName=one-by-one-test` 저장 확인
  - 리인덱싱 실행 시 `POST /api/admin/crawl-sources/{id}/run` 3건 연속 호출 확인
- `npm run build` 통과 (2026-04-10)
  - 경고 2건만 존재 (`src/app/layout.tsx` 폰트 관련 next lint warning)

## Risks

- `collection-lifecycle` change가 완료되어야 "컬렉션 삭제" 버튼을 리인덱싱 섹션에 통합할 수 있음
- 기존 crawl_sources에 `collection_name` 이 없는 경우 드롭다운이 비어 보일 수 있음 — "새 컬렉션 추가" 선택으로 대응
