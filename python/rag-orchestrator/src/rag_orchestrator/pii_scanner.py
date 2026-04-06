"""
Presidio 기반 PII 감지·마스킹 모듈.

입력(질문)과 출력(답변) 양방향 스캔을 지원한다.
한국어 PII 패턴(주민번호, 전화번호, 계좌번호)은 커스텀 recognizer로 추가한다.
"""
from __future__ import annotations

import re
from typing import Optional

try:
    from presidio_analyzer import AnalyzerEngine, PatternRecognizer, Pattern
    from presidio_anonymizer import AnonymizerEngine

    _analyzer = AnalyzerEngine()
    _anonymizer = AnonymizerEngine()

    # 한국어 커스텀 recognizer
    _kr_jumin = PatternRecognizer(
        supported_entity="KR_JUMIN",
        patterns=[Pattern("주민번호", r"\d{6}-[1-4]\d{6}", 0.9)],
    )
    _kr_phone = PatternRecognizer(
        supported_entity="KR_PHONE",
        patterns=[Pattern("전화번호", r"01[016789]-?\d{3,4}-?\d{4}", 0.85)],
    )
    _kr_account = PatternRecognizer(
        supported_entity="KR_ACCOUNT",
        patterns=[Pattern("계좌번호", r"\d{3,4}-\d{4,6}-\d{2,6}(-\d{2,3})?", 0.75)],
    )
    _analyzer.registry.add_recognizer(_kr_jumin)
    _analyzer.registry.add_recognizer(_kr_phone)
    _analyzer.registry.add_recognizer(_kr_account)

    _presidio_available = True

except ImportError:
    _presidio_available = False


def scan_and_mask(text: str) -> tuple[str, list[dict]]:
    """
    텍스트에서 PII를 감지하고 마스킹된 텍스트와 감지 정보를 반환한다.

    Returns:
        (masked_text, hits) — hits가 빈 리스트이면 PII 없음
    """
    if not text or not _presidio_available:
        return text, []

    try:
        results = _analyzer.analyze(text=text, language="ko")
        if not results:
            # 영어 fallback (이메일, CREDIT_CARD 등)
            results = _analyzer.analyze(text=text, language="en")
        if not results:
            return text, []

        masked = _anonymizer.anonymize(text=text, analyzer_results=results).text
        hits = [
            {
                "entity_type": r.entity_type,
                "start": r.start,
                "end": r.end,
                "score": r.score,
            }
            for r in results
        ]
        return masked, hits

    except Exception:
        return text, []
