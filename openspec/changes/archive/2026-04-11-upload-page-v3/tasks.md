# Tasks: upload-page-v3

## Phase 0 — OpenSpec 아티팩트
- [x] proposal.md 작성
- [x] tasks.md 작성
- [x] status.md 작성

## Phase 1 — 게이트 조건 구현
- [x] org + service 미선택 시 파일 업로드/웹 크롤/리인덱싱 섹션에 `opacity-40 pointer-events-none` 적용
- [x] org + service 모두 선택된 시점에 섹션 활성화 (상태 변수로 제어)

## Phase 2 — 컬렉션 피커 로직 구현
- [x] org + service 선택 시 `GET /api/admin/crawl-sources?organization_id={orgId}` 호출
- [x] 응답을 `serviceId` 기준으로 클라이언트 필터링
- [x] 필터링된 결과에서 `collectionName` 유니크 값 목록 추출 (null/undefined 제외)
- [x] 컬렉션 드롭다운 상태 관리: 선택값, "새 컬렉션" 인라인 입력 표시 여부

## Phase 3 — 파일 업로드 카드 수정
- [x] 컬렉션 피커(드롭다운 + 인라인 입력)를 파일 선택 UI 위에 배치
- [x] 선택된 collectionName을 업로드 API 호출 시 전달

## Phase 4 — 웹 크롤 카드 수정
- [x] 컬렉션 피커를 소스 이름/URL 입력 필드 위에 배치
- [x] 선택된 collectionName을 crawl source 등록 API 호출 시 전달

## Phase 5 — 리인덱싱 카드 수정
- [x] 개별 crawl_source 목록 드롭다운을 컬렉션 드롭다운으로 교체
- [x] 선택한 컬렉션에 해당하는 소스 목록 및 "N개 소스 포함" 카운트 표시
- [x] 실행 버튼 클릭 시 해당 컬렉션 소스 배열을 순회하며 `ingestionApi.runSource()` 순차 호출
- [x] 진행 상황 표시 (예: 1/3 완료)

## 검증
- [x] 기관/서비스 미선택 → 섹션 비활성 확인
- [x] 기관/서비스 선택 → crawl-sources API 호출 및 컬렉션 목록 표시 확인
- [x] "새 컬렉션 추가" 선택 → 인라인 입력 표시 확인
- [x] 파일 업로드 시 선택한 collectionName 전달 확인 (Network 탭)
- [x] 리인덱싱 실행 시 컬렉션 내 소스 순차 실행 확인
- [x] `npm run build` 빌드 오류 없음 확인
