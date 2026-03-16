# Proposal: integrate-pgvector-flyway

## 목적
pgvector 설정을 수동 스크립트에서 Flyway 마이그레이션으로 이전

## 배경
`scripts/enable_pgvector.sql`을 수동으로 실행해야 했으며,
V016에서 `embedding_vector TEXT`로 생성 후 별도 ALTER가 필요했음.

## 변경
- V018__enable_pgvector.sql 생성 (PostgreSQL 전용)
- application-test.yml에 `spring.flyway.target: 17` 추가 (H2에서 V018 skip)

## H2 호환성
H2는 `CREATE EXTENSION`을 지원하지 않으므로 테스트 환경에서 V018을 적용하지 않음.
`spring.flyway.target` 설정으로 V017까지만 마이그레이션을 실행.
