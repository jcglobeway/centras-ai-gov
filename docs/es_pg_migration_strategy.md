지자체 민원 응대 최적화를 위한 데이터 수집(Crawling) 및 저장소 전환(ES → PostgreSQL) 프로젝트 PRD입니다.
------------------------------
[PRD] 지자체 민원 응대 지식베이스 구축 및 검색 엔진 고도화1. 프로젝트 개요

* 목적: 지자체별 게시판 및 실무편람 데이터를 효율적으로 수집하고, 고성능·저비용의 Hybrid 검색 환경(PostgreSQL + pgvector)으로 통합하여 민원 응대 정확도를 향상함.
* 핵심 가치: 관리 편의성 극대화, 멀티 테넌트 지원, 검색 정확도(Keyword + Semantic) 확보.

------------------------------
2. 주요 기능 및 스펙가. 지능형 데이터 수집 (Crawler)

* 대상: 지정된 지자체 홈페이지 게시판 및 공통 실무편람(PDF/법령).
* 도구: Requests + Beautiful Soup + Trafilatura.
* 핵심 로직:
* 정밀 파싱: Trafilatura를 사용하여 게시물 본문의 노이즈(HTML 태그, 메뉴, 하단 푸터)를 제거하고 순수 텍스트만 추출.
    * 증분 수집: article_num 또는 hash를 비교하여 신규 게시물만 수집.
    * 메타데이터 보존: 제목, 작성일, 담당 부서, 원본 링크 등 추출.

나. 검색 엔진 전환 (Migration: ES → PostgreSQL)

* 대상: 기존 Elasticsearch(cb-gyeonggido 등) 인덱스 데이터를 PostgreSQL로 통합.
* 데이터 구조:
* Single Table Strategy: tenant_id 컬럼으로 지자체 구분 및 파티셔닝.
    * Hybrid Storage: 고정 필드(ID, Content, Embedding) + 유연한 필드(JSONB 활용).
* 검색 방식: Hybrid Search (BM25 기반 형태소 검색 + Cosine Similarity 벡터 검색).

------------------------------
3. 기술 아키텍처

| 구분 | 변경 전 (AS-IS) | 변경 후 (TO-BE) | 기대 효과 |
|---|---|---|---|
| 수집 도구 | Selenium (추정) | Requests + Trafilatura | 속도 향상, 리소스 절감 |
| 저장소 | Elasticsearch | PostgreSQL (pgvector) | 운영비 절감, JOIN 용이 |
| 인덱싱 | Multi-index (지자체별) | Single Partitioned Table | 관리 편의성 극대화 |
| 검색 모델 | Vector Only (HNSW) | Hybrid (Keyword + Vector) | 민원 용어 검색 정확도 향상 |

------------------------------
4. 단계별 실행 계획[1단계] Trafilatura 기반 고성능 크롤러 구현
   지자체 게시판의 본문 텍스트를 정교하게 추출합니다.

import requestsfrom bs4 import BeautifulSoupfrom trafilatura import extract
def crawl_board_post(url):
response = requests.get(url, headers={'User-Agent': 'Mozilla/5.0'})
# Trafilatura로 본문만 깔끔하게 추출 (광고/메뉴 제거)
content = extract(response.text, include_tables=True, no_fallback=False)

    soup = BeautifulSoup(response.text, 'html.parser')
    title = soup.find('title').text # 예시: 실제 사이트 구조에 맞춰 조정 필요
    
    return {
        "title": title,
        "content": content,
        "url": url
    }

[2단계] ES 데이터를 PostgreSQL로 마이그레이션
기존 3072차원 벡터와 메타데이터를 Postgres로 옮깁니다.

import psycopg2from psycopg2.extras import execute_valuesimport json
# PostgreSQL 연결 및 테이블 생성 (pgvector 확장 필요)def migrate_from_es_to_pg(es_data_list):
    conn = psycopg2.connect("dbname=minwon user=admin password=pass")
    cur = conn.cursor()
    
    query = """
    INSERT INTO documents (tenant_id, content, embedding, metadata)
    VALUES %s
    """
    
    # ES 데이터를 PG 포맷으로 변환
    values = [
        (
            item['_index'].replace('cb-', ''), # tenant_id 추출
            item['_source']['content'],
            item['_source']['embedding'], # 3072 dims vector
            json.dumps(item['_source']['metadata']) # 나머지 메타데이터는 JSONB로
        )
        for item in es_data_list
    ]
    
    execute_values(cur, query, values)
    conn.commit()

------------------------------
5. 성공 지표 (KPI)

    1. 인프라 비용: Elasticsearch 노드 축소를 통해 월 서버 비용 30% 이상 절감.
    2. 검색 정확도: 단순 키워드 검색 대비 의미 기반 검색 결과의 상위 3위 이내 노출 빈도(Top-3 Recall) 측정.
    3. 운영 효율: 신규 지자체 추가 시 인덱스 생성 없이 tenant_id 추가만으로 대응 가능 여부.

------------------------------