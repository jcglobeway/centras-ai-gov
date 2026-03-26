-- V029: questions 테이블에 질문 임베딩 컬럼 추가 (TEXT, H2 호환)
-- PostgreSQL에서는 V030 Kotlin 마이그레이션이 vector(1024) 타입으로 변환한다.
ALTER TABLE questions ADD COLUMN question_embedding TEXT;
