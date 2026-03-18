# Tasks

## 사전 확인
- [ ] `daily_metrics_org` 테이블 현재 컬럼 확인 (V015 마이그레이션)
- [ ] `DailyMetricsEntity`, `DailyMetrics` 도메인 모델 위치 확인
- [ ] `MetricsController` 응답 DTO 구조 확인

## 구현
- [ ] V023 마이그레이션 작성 (10개 컬럼 ALTER TABLE)
- [ ] `DailyMetricsEntity` 컬럼 필드 추가
- [ ] `DailyMetrics` 도메인 data class 필드 추가 (nullable)
- [ ] `toSummary()` / `toDomain()` 매퍼 업데이트
- [ ] API 응답 DTO 업데이트
- [ ] `application.yml` (test) `spring.flyway.target` → `"23"` 업데이트

## 테스트
- [ ] `./gradlew test` 전체 통과 확인
- [ ] 기존 시드 메트릭 데이터 NULL 호환성 확인

## 완료
- [ ] `status.md` 업데이트
- [ ] 커밋
