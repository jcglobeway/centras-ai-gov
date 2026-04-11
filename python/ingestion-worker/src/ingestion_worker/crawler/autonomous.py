from __future__ import annotations

import asyncio
import hashlib
import re
from typing import Optional
from urllib.parse import urlparse

import httpx
from loguru import logger
from playwright.async_api import Browser, Page, async_playwright

from ..models import CrawledPage
from .adapters.base import BaseSiteAdapter
from .adapters.ggc_go_kr import GgcGoKrAdapter
from .adapters.gjf_or_kr import GjfOrKrAdapter
from .adapters.namgu_gwangju_kr import NamguGwangjuKrAdapter
from .extractor import ContentExtractor

# ── 어댑터 레지스트리 ──────────────────────────────────────────────────────────
# seed URL 도메인 패턴 → 어댑터 클래스 매핑.
# 순서대로 매칭하며 첫 번째 일치 항목을 사용한다.

ADAPTER_REGISTRY: list[tuple[str, type[BaseSiteAdapter]]] = [
    (r"ggc\.go\.kr",          GgcGoKrAdapter),
    (r"gjf\.or\.kr",          GjfOrKrAdapter),
    (r"job\.gg\.go\.kr",      GjfOrKrAdapter),
    (r"namgu\.gwangju\.kr",   NamguGwangjuKrAdapter),
]


def _detect_adapter(url: str) -> Optional[BaseSiteAdapter]:
    """seed URL에서 도메인 패턴으로 어댑터를 자동 감지한다. 없으면 None 반환."""
    for pattern, cls in ADAPTER_REGISTRY:
        if re.search(pattern, url):
            return cls()
    return None


def simhash(text: str, bits: int = 64) -> int:
    """SimHash fingerprint — 유사 콘텐츠(near-duplicate) 감지용."""
    v = [0] * bits
    for token in text.lower().split():
        h = int(hashlib.md5(token.encode()).hexdigest(), 16)
        for i in range(bits):
            v[i] += 1 if h & (1 << i) else -1
    fingerprint = 0
    for i in range(bits):
        if v[i] > 0:
            fingerprint |= (1 << i)
    return fingerprint


def hamming_distance(h1: int, h2: int) -> int:
    return bin(h1 ^ h2).count("1")


def is_near_duplicate(new_hash: int, seen: list[int], threshold: int = 3) -> bool:
    return any(hamming_distance(new_hash, h) <= threshold for h in seen)


class RobotsChecker:
    """robots.txt 파싱 및 허용 여부 확인."""

    def __init__(self):
        self._cache: dict[str, list[str]] = {}

    async def is_allowed(self, url: str) -> bool:
        parsed = urlparse(url)
        base = f"{parsed.scheme}://{parsed.netloc}"
        robots_url = f"{base}/robots.txt"

        if base not in self._cache:
            try:
                async with httpx.AsyncClient(timeout=5) as client:
                    r = await client.get(robots_url)
                    disallowed = []
                    current_agent = False
                    for line in r.text.splitlines():
                        line = line.strip()
                        if line.lower().startswith("user-agent:"):
                            agent = line.split(":", 1)[1].strip()
                            current_agent = agent in ("*",)
                        elif current_agent and line.lower().startswith("disallow:"):
                            path = line.split(":", 1)[1].strip()
                            if path:
                                disallowed.append(path)
                    self._cache[base] = disallowed
            except Exception:
                self._cache[base] = []

        path = parsed.path
        return not any(path.startswith(d) for d in self._cache[base])


class AutonomousCrawler:
    """멀티페이지 재귀 크롤러.

    seed URL에서 시작해 같은 도메인 내 링크를 재귀 탐색한다.
    SimHash 중복 제거와 robots.txt 준수를 내장한다.
    Playwright(Chromium)로 JS 렌더링 페이지도 지원한다.
    """

    def __init__(
        self,
        max_depth: int = 3,
        max_pages: int = 100,
        concurrency: int = 3,
        delay: float = 1.0,
        focus_keywords: list[str] | None = None,
    ):
        self.max_depth = max_depth
        self.max_pages = max_pages
        self.concurrency = concurrency
        self.delay = delay
        self.focus_keywords = [k.lower() for k in (focus_keywords or [])]

        self._browser: Optional[Browser] = None
        self._playwright = None
        self._semaphore = asyncio.Semaphore(concurrency)
        self._robots = RobotsChecker()
        self._extractor = ContentExtractor()
        self._adapter: Optional[BaseSiteAdapter] = None

    async def __aenter__(self):
        self._playwright = await async_playwright().start()
        self._browser = await self._playwright.chromium.launch(
            headless=True,
            args=["--no-sandbox", "--disable-dev-shm-usage"],
        )
        return self

    async def __aexit__(self, *args):
        if self._browser:
            await self._browser.close()
        if self._playwright:
            await self._playwright.stop()

    async def crawl_all(self, seed_url: str) -> list[CrawledPage]:
        """seed URL에서 재귀적으로 크롤링하여 수집된 모든 페이지를 반환한다."""
        # seed URL로 사이트 전용 어댑터 자동 감지
        self._adapter = _detect_adapter(seed_url)
        if self._adapter:
            logger.info(f"사이트 어댑터 감지: {type(self._adapter).__name__}")
            self._adapter.on_crawl_start()
        else:
            logger.info("사이트 전용 어댑터 없음 — 범용 크롤러 사용")

        visited: set[str] = set()
        seen_hashes: list[int] = []
        results: list[CrawledPage] = []
        queue: list[tuple[str, int, Optional[str]]] = [(seed_url, 0, None)]

        while queue and len(results) < self.max_pages:
            url, depth, parent_url = queue.pop(0)

            if url in visited or depth > self.max_depth:
                continue
            visited.add(url)

            page = await self._crawl_page(url, depth, parent_url)
            if page is None:
                continue

            # SimHash 중복 제거
            if is_near_duplicate(page.content_hash, seen_hashes):
                logger.debug(f"중복 콘텐츠 스킵: {url}")
                continue
            seen_hashes.append(page.content_hash)
            results.append(page)

            logger.info(f"크롤 완료 [{len(results)}/{self.max_pages}] depth={depth}: {url}")

            if depth < self.max_depth:
                new_urls = self._prioritize(page.links, visited, url)
                for next_url in new_urls:
                    queue.append((next_url, depth + 1, url))

        if self._adapter:
            self._adapter.on_crawl_end()

        return results

    async def _crawl_page(
        self,
        url: str,
        depth: int,
        parent_url: Optional[str],
    ) -> Optional[CrawledPage]:
        if not await self._robots.is_allowed(url):
            logger.debug(f"robots.txt 차단: {url}")
            return None

        async with self._semaphore:
            try:
                context = await self._browser.new_context(
                    user_agent="IngestionWorker/1.0 (centras-ai-gov crawler)"
                )
                pw_page: Page = await context.new_page()
                await pw_page.goto(url, wait_until="domcontentloaded", timeout=30000)
                await asyncio.sleep(1.5)

                html = await pw_page.content()
                await context.close()

                extracted = self._extractor.extract(html, url)
                content_hash = simhash(extracted["content"])

                # 사이트 전용 어댑터가 있으면 구조화 파싱으로 메타데이터 보강
                adapter_meta: dict = {}
                if self._adapter:
                    from bs4 import BeautifulSoup
                    soup = BeautifulSoup(html, "lxml")
                    page_type = self._adapter.route(url)
                    parsed = self._adapter.parse(page_type, soup, url)
                    if parsed:
                        adapter_meta = parsed
                        logger.debug(f"어댑터 파싱 완료 [{page_type}]: {url}")

                await asyncio.sleep(self.delay)

                return CrawledPage(
                    url=url,
                    title=extracted["title"],
                    content=extracted["content"],
                    links=extracted["links"],
                    depth=depth,
                    parent_url=parent_url,
                    page_type=extracted["page_type"],
                    content_hash=content_hash,
                    metadata={
                        **extracted["metadata"],
                        "sections": extracted.get("sections", []),
                        **adapter_meta,
                    },
                )

            except Exception as e:
                logger.warning(f"크롤링 실패 {url}: {e}")
                return None

    def _prioritize(
        self,
        urls: list[str],
        visited: set[str],
        current_url: str,
    ) -> list[str]:
        new_urls = [u for u in urls if u not in visited and u != current_url]

        def score(url: str) -> float:
            s = 0.0
            url_lower = url.lower()
            for kw in self.focus_keywords:
                if kw in url_lower:
                    s += 2.0
            path_depth = len(urlparse(url).path.strip("/").split("/"))
            s -= path_depth * 0.1
            return s

        return sorted(new_urls, key=score, reverse=True)
