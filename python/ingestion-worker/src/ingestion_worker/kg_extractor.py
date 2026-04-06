from __future__ import annotations

import json
import os
import re

from langchain_core.messages import HumanMessage, SystemMessage
from langchain_ollama import ChatOllama
from loguru import logger
from tenacity import retry, stop_after_attempt, wait_exponential

from .models import CrawledPage


KG_SYSTEM_PROMPT = """당신은 웹페이지 텍스트에서 지식 그래프를 구성하는 전문가입니다.
주어진 텍스트를 분석하여 엔티티와 관계를 JSON 형식으로 추출하세요.

규칙:
- 엔티티 타입: Product, Person, Concept, Service, Organization, Feature
- 관계 타입: DESCRIBES, MENTIONS, IS_PART_OF, RELATES_TO, COMPETES_WITH, USES
- 중요하고 구체적인 엔티티만 추출 (일반 단어 제외)
- weight는 관계의 강도 (0.1~1.0)
- 반드시 순수 JSON만 반환, 설명 텍스트 없음
"""

KG_USER_TEMPLATE = """URL: {url}
제목: {title}
페이지 타입: {page_type}

본문 (최대 2000자):
{content}

다음 JSON 구조로만 응답하세요:
{{
  "entities": [
    {{"id": "짧고 고유한 영문 ID", "type": "타입", "name": "이름", "description": "1문장 설명"}}
  ],
  "relations": [
    {{"subject": "entity_id", "predicate": "관계타입", "object": "entity_id", "weight": 0.8}}
  ],
  "page_summary": "핵심 내용 2-3문장",
  "main_topics": ["토픽1", "토픽2", "토픽3"]
}}"""


class KGExtractor:
    """Ollama LLM으로 페이지에서 엔티티·토픽·요약을 추출한다.

    `KG_EXTRACTION_ENABLED=true` 환경변수가 설정된 경우에만 활성화된다.
    LLM 호출 실패 시 빈 메타데이터를 반환하며 파이프라인을 중단하지 않는다.
    """

    @staticmethod
    def is_enabled() -> bool:
        return os.getenv("KG_EXTRACTION_ENABLED", "false").lower() == "true"

    def __init__(
        self,
        model: str | None = None,
        base_url: str | None = None,
    ):
        self.llm = ChatOllama(
            model=model or os.getenv("KG_EXTRACTION_MODEL", "llama3.1:8b"),
            base_url=base_url or os.getenv("OLLAMA_URL", "http://localhost:11434"),
            temperature=0.1,
            format="json",
        )

    @retry(stop=stop_after_attempt(3), wait=wait_exponential(min=1, max=10))
    async def extract(self, page: CrawledPage) -> dict:
        """페이지에서 KG 메타데이터를 추출하여 dict로 반환한다."""
        prompt = KG_USER_TEMPLATE.format(
            url=page.url,
            title=page.title,
            page_type=page.page_type,
            content=page.content[:2000],
        )

        try:
            response = await self.llm.ainvoke([
                SystemMessage(content=KG_SYSTEM_PROMPT),
                HumanMessage(content=prompt),
            ])
            data = self._safe_parse_json(response.content)
            return {
                "kg_entities": data.get("entities", []),
                "kg_relations": data.get("relations", []),
                "page_summary": data.get("page_summary", ""),
                "main_topics": data.get("main_topics", []),
            }
        except Exception as e:
            logger.warning(f"KG 추출 실패 {page.url}: {e}")
            return {}

    def _safe_parse_json(self, raw: str) -> dict:
        raw = re.sub(r"```(?:json)?\s*", "", raw).strip()
        raw = re.sub(r"```\s*$", "", raw).strip()
        try:
            return json.loads(raw)
        except json.JSONDecodeError:
            match = re.search(r"\{.*\}", raw, re.DOTALL)
            if match:
                try:
                    return json.loads(match.group())
                except json.JSONDecodeError:
                    pass
            return {}
