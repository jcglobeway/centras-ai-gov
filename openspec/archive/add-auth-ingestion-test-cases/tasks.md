# Tasks

## 계획 단계
- [x] 요구 범위 재확인
- [x] 현재 테스트 구조 확인 (AdminApiApplicationTests.kt)
- [x] 기존 19개 테스트 분석
- [x] 새 API endpoint 확인 (개별 조회)

## 테스트 추가
- [x] GET /admin/crawl-sources/{id} 성공 케이스
  - ops admin으로 조회 성공
  - 응답 필드 검증 (id, name, sourceType, sourceUri, renderMode 등)
- [x] GET /admin/crawl-sources/{id} 404 케이스
  - 존재하지 않는 ID 조회
- [x] GET /admin/crawl-sources/{id} 권한 범위 케이스
  - client_admin이 범위 밖 조회 시 404

- [x] GET /admin/ingestion-jobs/{id} 성공 케이스
  - ops admin으로 조회 성공
  - 응답 필드 검증 (id, crawlSourceId, jobType, jobStatus, jobStage 등)
- [x] GET /admin/ingestion-jobs/{id} 404 케이스
  - 존재하지 않는 ID 조회
- [x] GET /admin/ingestion-jobs/{id} 권한 범위 케이스
  - qa_admin이 범위 밖 조회 시 404

## 검증
- [x] ./gradlew test 실행
- [x] 전체 테스트 통과 확인
- [x] 테스트 개수 확인: 19 → 25개 (6개 추가)

## 마무리
- [ ] 99_worklog.md 갱신
- [ ] status.md 완료 상태로 갱신
- [ ] proposal.md Done Definition 업데이트
- [ ] change를 archive로 이동
- [ ] 커밋 (한글 메시지)
