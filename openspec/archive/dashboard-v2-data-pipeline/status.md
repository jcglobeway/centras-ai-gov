# Status: dashboard-v2-data-pipeline

- 상태: `archived`
- 시작일: `2026-03-21`
- 마지막 업데이트: `2026-03-22`

## Progress

- Phase A~C-fix 완료: KpiCard/ProgressBar/AlertBanner/ScoreTable 컴포넌트, RAGAS API, 대시보드 6개 페이지, v3 디자인 정렬
- Phase D 완료: ingestion_prep.py, query_runner.py, run_pipeline.sh, V027 SQL 생성
- Phase E 완료: 백엔드 50개 테스트 통과, V027 bootRun 확인

## Verification

- `./gradlew test` 50/50 통과 확인
- PostgreSQL V027 마이그레이션 bootRun 확인 (3.669초 기동)
- 프론트엔드 qa 포털 blank page 버그 수정 (qa_admin 역할 매핑)

## Risks

- V027에서 INSERT하는 공공 민원 Q&A는 ZIP 파일 샘플 추출 결과에 따라 내용이 달라질 수 있음
- H2 테스트 환경에서 `flyway.target` 숫자를 "27"로 올릴 때 기존 테스트 FK 제약 충돌 가능 → V027 작성 시 순서 주의
