from __future__ import annotations

import asyncio
import os
from pathlib import Path
from typing import Optional

import httpx
from playwright.async_api import Browser, async_playwright


class CrawlExecutor:
    """Playwright 크롤링 + HTML 파싱 + 청킹 + Ollama 임베딩 파이프라인."""

    def __init__(self, headless: bool = True, output_dir: Optional[Path] = None):
        self.headless = headless
        self.output_dir = output_dir or Path("./crawl_output")
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self.ollama_url = os.getenv("OLLAMA_URL", "http://localhost:11434")

    async def execute_crawl(self, source_uri: str, crawl_source_id: str) -> dict:
        """
        URL을 크롤링하고 HTML 텍스트를 추출하여 반환한다.

        스크린샷은 디버깅용으로만 저장하며, 핵심 결과는 page_text.
        """
        print(f"[CrawlExecutor] Starting crawl for: {source_uri}")

        async with async_playwright() as p:
            browser: Browser = await p.chromium.launch(headless=self.headless)
            page = await browser.new_page()

            try:
                await page.goto(source_uri, wait_until="networkidle", timeout=30000)

                screenshot_path = self.output_dir / f"{crawl_source_id}_screenshot.png"
                await page.screenshot(path=str(screenshot_path))

                title = await page.title()
                html_content = await page.content()

                print(f"[CrawlExecutor] Fetched page title: {title}, html size: {len(html_content)}")

                return {
                    "status": "success",
                    "title": title,
                    "html_content": html_content,
                    "screenshot_path": str(screenshot_path),
                    "url": source_uri,
                }

            except Exception as e:
                print(f"[CrawlExecutor] Error during crawl: {e}")
                return {
                    "status": "error",
                    "error": str(e),
                    "url": source_uri,
                }

            finally:
                await browser.close()

    def execute_crawl_sync(self, source_uri: str, crawl_source_id: str) -> dict:
        """동기 wrapper for execute_crawl."""
        return asyncio.run(self.execute_crawl(source_uri, crawl_source_id))

    def extract_text(self, html_content: str) -> str:
        """
        BeautifulSoup4로 HTML에서 텍스트를 추출한다.

        script/style 태그는 제거하고 본문 텍스트만 반환.
        """
        from bs4 import BeautifulSoup

        soup = BeautifulSoup(html_content, "html.parser")

        for tag in soup(["script", "style", "nav", "footer", "header"]):
            tag.decompose()

        text = soup.get_text(separator="\n", strip=True)
        # 연속된 빈 줄 제거
        lines = [line for line in text.splitlines() if line.strip()]
        return "\n".join(lines)

    def chunk_text(self, text: str, chunk_size: int = 512, chunk_overlap: int = 64) -> list[str]:
        """
        langchain RecursiveCharacterTextSplitter로 텍스트를 청크로 분할한다.
        """
        from langchain_text_splitters import RecursiveCharacterTextSplitter

        splitter = RecursiveCharacterTextSplitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
            separators=["\n\n", "\n", "。", ".", " ", ""],
        )
        return splitter.split_text(text)

    def embed_text(self, text: str) -> Optional[list[float]]:
        """Ollama bge-m3로 텍스트 임베딩을 생성한다."""
        try:
            response = httpx.post(
                f"{self.ollama_url}/api/embeddings",
                json={"model": "bge-m3", "prompt": text},
                timeout=10.0,
            )
            if response.status_code == 200:
                return response.json().get("embedding")
            return None
        except Exception:
            return None
