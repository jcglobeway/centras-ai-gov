지자체 민원 데이터의 특성(정밀한 본문 추출 + 법령/편람 참조 + 멀티 테넌트)을 반영하여, 실무에서 바로 사용할 수 있는 수준으로 PRD와 구현 설계를 구체화해 드립니다.
------------------------------
[PRD] 지자체 민원 지식베이스 고도화: 통합 수집 및 하이브리드 검색 전환1. 데이터 수집 전략 (Data Acquisition)
지자체 게시판은 레이아웃이 제각각이지만, 본문 영역의 특징은 유사합니다. 이를 위해 추출 기반(Extraction-based) 크롤러를 구성합니다.
1.1 핵심 도구 및 기술

* Engine: Requests (HTTP 요청), BeautifulSoup (목록 파싱)
* Content Extractor: Trafilatura (본문 내 노이즈 제거 및 순수 텍스트 추출)
* Handling PDF/HWP: PyMuPDF (실무편람 PDF 파싱), pyhwp (한글 문서 파싱)

1.2 크롤링 파이프라인 (Pseudo Logic)

1. Seed URL: 지자체별 게시판 첫 페이지 접속.
2. Listing: BeautifulSoup으로 <a> 태그 내 상세글 ID와 제목 추출.
3. Deduplication: DB에서 content_hash 또는 article_num을 대조하여 기수집 건 스킵.
4. Extraction: 상세 페이지 접속 후 trafilatura.extract()로 본문 텍스트 + 표(Table) 추출.
5. Metadata Mapping: 지자체 코드(tenant_id), 카테고리, 원본 URL 등을 매핑.

------------------------------
2. 저장소 및 검색 설계 (Storage & Search)
   Elasticsearch의 복잡도를 낮추고 PostgreSQL의 강점을 활용한 Single-Table Multi-Tenant 구조입니다.
   2.1 데이터베이스 스키마 (PostgreSQL + pgvector)

-- pgvector 확장 활성화CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE minwon_knowledge (
id SERIAL PRIMARY KEY,
tenant_id VARCHAR(50) NOT NULL,      -- 'gyeonggido', 'common', 'seoul'
doc_type VARCHAR(20),                -- 'post', 'law', 'manual'
title TEXT NOT NULL,                 -- 제목 (BM25 검색용)
content TEXT NOT NULL,               -- 본문 (BM25 + Vector 검색용)
embedding VECTOR(3072),              -- OpenAI text-embedding-3-large
metadata JSONB,                      -- 작성일, 부서, 원본링크 등 (GIN 인덱스 적용)
content_hash VARCHAR(64) UNIQUE,     -- 중복 수집 방지용
created_at TIMESTAMP DEFAULT NOW()
);
-- 인덱스 설정: 검색 속도 최적화CREATE INDEX idx_minwon_metadata ON minwon_knowledge USING GIN (metadata);CREATE INDEX idx_minwon_embedding ON minwon_knowledge USING hnsw (embedding vector_cosine_ops);

2.2 하이브리드 검색 로직 (Hybrid Search)
키워드 매칭(정확한 용어)과 벡터 검색(의도 파악)을 결합한 RRF(Reciprocal Rank Fusion) 방식을 적용합니다.

* Step 1: 키워드 검색으로 상위 50개 추출 (예: "여권 재발급 서류")
* Step 2: 벡터 검색으로 상위 50개 추출 (예: "여권 다시 만들 때 필요한 거")
* Step 3: 두 결과의 순위를 점수화하여 최종 1~5위 도출.

------------------------------
3. 멀티 테넌트 관리 (Multi-Tenancy)
   관리 편의성을 위해 지자체별 인덱스 분리 대신 논리적 격리를 선택합니다.

* 공통 데이터 공유: 법령이나 실무편람은 tenant_id = 'common'으로 저장.
* 조회 권한 제어: 쿼리 시 항상 WHERE tenant_id IN ('target_id', 'common') 조건을 부여하여 해당 지자체 데이터와 공통 데이터만 조회.
* 리소스 관리: 특정 지자체 데이터가 급증할 경우, Postgres의 PARTITION BY LIST (tenant_id) 기능을 사용하여 물리적 파일만 분리(성능 유지).

------------------------------
4. 마이그레이션 전략 (ES → PG)
   기존에 적재된 Elasticsearch 데이터를 안전하게 옮기는 절차입니다.

    1. ES Scroll API: 대량의 데이터를 청크(Chunk) 단위로 읽어옴.
    2. Transform: ES의 _source 데이터를 Postgres 테이블 컬럼에 맞게 매핑.
    3. Batch Insert: psycopg2의 execute_values를 사용하여 초당 수만 건 단위로 삽입.
    4. Verification: 지자체별 카운트를 대조하여 유실 데이터 확인.

------------------------------
5. 기대 효과 및 로드맵

* 유지비: ES 클러스터 운영비 대비 최대 50% 절감.
* 정확도: 하이브리드 검색 도입으로 민원 키워드 매칭률 20% 향상.
* 편의성: 신규 지자체 추가 시 설정 변경 없이 데이터 적재만으로 즉시 서비스 가능.

우선 진행 과제:

1. 샘플 크롤러 구현: 특정 지자체 1곳을 정해 Trafilatura 성능 테스트.
2. DB 스키마 구축: PostgreSQL에 pgvector 인덱스 최적화 테스트.