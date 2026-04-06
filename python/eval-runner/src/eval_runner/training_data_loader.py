"""
훈련 데이터 로더.

원천데이터(TS_*.zip)와 라벨링데이터(TL_*_질의응답.zip)에서
eval_questions.json 형식의 Q&A 쌍과 문서 임베딩 대상 텍스트를 추출한다.

데이터 구조:
  TS zip: consulting_content(str) — 상담 대화 원문 → pgvector 임베딩 대상
  TL zip: instructions[].data[].instruction → 질문
          instructions[].data[].output      → ground truth 답변
          consulting_content(str)           → 컨텍스트 (TL도 동일 원문 포함)
"""
from __future__ import annotations

import json
import zipfile
from pathlib import Path
from typing import Iterator


# ── Q&A 추출 (라벨링데이터 TL) ──────────────────────────────────────────────

def iter_qa_pairs(
    zip_path: Path,
    org_id: str,
    service_id: str,
    limit: int = 0,
) -> Iterator[dict]:
    """
    TL_*_질의응답.zip에서 Q&A 쌍을 추출한다.

    각 JSON 파일 → 1개 레코드 → instructions[0].data[] → 질문/답변 쌍.
    limit=0이면 전체 추출.
    """
    count = 0
    with zipfile.ZipFile(zip_path) as zf:
        json_files = sorted(n for n in zf.namelist() if n.endswith(".json"))
        for name in json_files:
            if limit and count >= limit:
                return
            try:
                with zf.open(name) as f:
                    records = json.loads(f.read().decode("utf-8"))
            except Exception:
                continue

            for record in records:
                context = record.get("consulting_content", "")
                category = record.get("consulting_category", "")
                source_id = record.get("source_id", "")
                date_str = record.get("consulting_date", "")

                instructions = record.get("instructions", [])
                if not instructions:
                    continue

                inst_block = instructions[0]
                data_items = inst_block.get("data", [])
                if isinstance(data_items, str):
                    # 간혹 JSON 문자열로 들어오는 경우
                    try:
                        data_items = json.loads(data_items.replace("'", '"'))
                    except Exception:
                        continue

                for item in data_items:
                    if limit and count >= limit:
                        return
                    question = item.get("instruction", "").strip()
                    answer = item.get("output", "").strip()
                    task_cat = item.get("task_category", "")
                    if not question or not answer:
                        continue
                    # 상담 분석형 질문 제외 (민원인 관점이 아닌 상담 내용 분석)
                    if _is_analysis_question(question):
                        continue

                    yield {
                        "question": question,
                        "ground_truth": answer,
                        "context": context,
                        "org_id": org_id,
                        "service_id": service_id,
                        "task_category": task_cat,
                        "consulting_category": category,
                        "source_id": source_id,
                        "consulting_date": date_str,
                    }
                    count += 1


# ── 문서 청크 추출 (원천데이터 TS) ──────────────────────────────────────────

def iter_source_docs(
    zip_path: Path,
    org_id: str,
    service_id: str,
    chunk_size: int = 800,
    limit: int = 0,
) -> Iterator[dict]:
    """
    TS_*.zip에서 상담 대화 원문을 청크로 분할해 반환한다.

    chunk_size: 문자 단위 최대 청크 길이 (bge-m3 권장 512 토큰 ≈ 800자).
    """
    count = 0
    with zipfile.ZipFile(zip_path) as zf:
        json_files = sorted(n for n in zf.namelist() if n.endswith(".json"))
        for name in json_files:
            if limit and count >= limit:
                return
            try:
                with zf.open(name) as f:
                    records = json.loads(f.read().decode("utf-8"))
            except Exception:
                continue

            for record in records:
                content = record.get("consulting_content", "")
                if not content or not isinstance(content, str):
                    continue

                source_id = record.get("source_id", "")
                category = record.get("consulting_category", "")
                date_str = record.get("consulting_date", "")

                # 대화를 chunk_size 문자씩 분할 (문장 경계 보존 시도)
                chunks = _split_text(content, chunk_size)
                for chunk_idx, chunk_text in enumerate(chunks):
                    if limit and count >= limit:
                        return
                    if len(chunk_text.strip()) < 30:
                        continue
                    yield {
                        "source_id": source_id,
                        "chunk_idx": chunk_idx,
                        "chunk_text": chunk_text.strip(),
                        "org_id": org_id,
                        "service_id": service_id,
                        "consulting_category": category,
                        "consulting_date": date_str,
                        "file_name": name,
                    }
                    count += 1


def _split_text(text: str, chunk_size: int) -> list[str]:
    """텍스트를 chunk_size 문자 이하로 분할한다 (줄바꿈 경계 우선)."""
    if len(text) <= chunk_size:
        return [text]
    chunks = []
    start = 0
    while start < len(text):
        end = start + chunk_size
        if end >= len(text):
            chunks.append(text[start:])
            break
        # 청크 끝에서 가장 가까운 줄바꿈 위치 탐색
        nl = text.rfind("\n", start, end)
        if nl > start:
            end = nl + 1
        chunks.append(text[start:end])
        start = end
    return chunks


# ── 분석형 질문 필터 ─────────────────────────────────────────────────────────

_ANALYSIS_SUFFIXES = ("니?", "냐?", "니까?", "했니?", "이니?", "였니?", "하니?", "누구야?", "누구니?", "누구냐?")
_ANALYSIS_PREFIXES = (
    "고객은 ", "고객이 ", "상담사는 ", "상담원은 ", "상담사가 ",
    "민원인은 ", "민원인이 ", "민원인의 ", "다음 내용에서",
)


def _is_analysis_question(question: str) -> bool:
    """
    상담 내용을 분석하는 형식의 질문인지 판별한다.

    민원인이 직접 묻는 질문이 아니라, 상담 내용을 제3자 관점에서
    분석하는 질문(예: "고객이 예매한 공연은 연극 아트이니?")을 걸러낸다.
    """
    for suffix in _ANALYSIS_SUFFIXES:
        if question.endswith(suffix):
            return True
    for prefix in _ANALYSIS_PREFIXES:
        if question.startswith(prefix):
            return True
    return False


# ── 편의 함수 ────────────────────────────────────────────────────────────────

def load_qa_pairs(
    zip_path: Path,
    org_id: str,
    service_id: str,
    limit: int = 0,
) -> list[dict]:
    """TL zip → eval_questions.json 형식 리스트 반환."""
    return list(iter_qa_pairs(zip_path, org_id, service_id, limit))
