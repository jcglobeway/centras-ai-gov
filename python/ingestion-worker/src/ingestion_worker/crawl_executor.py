from __future__ import annotations

import asyncio
from pathlib import Path
from typing import Optional

from playwright.async_api import Browser, async_playwright


class CrawlExecutor:
    """Playwright를 사용한 웹 크롤링 실행기 (스텁)."""

    def __init__(self, headless: bool = True, output_dir: Optional[Path] = None):
        self.headless = headless
        self.output_dir = output_dir or Path("./crawl_output")
        self.output_dir.mkdir(parents=True, exist_ok=True)

    async def execute_crawl(self, source_uri: str, crawl_source_id: str) -> dict[str, str]:
        """
        URL을 크롤링하고 스크린샷을 저장한다 (스텁 구현).

        실제 파싱/chunk/embed는 이후 단계에서 구현.
        """
        print(f"[CrawlExecutor] Starting crawl for: {source_uri}")

        async with async_playwright() as p:
            browser: Browser = await p.chromium.launch(headless=self.headless)
            page = await browser.new_page()

            try:
                # URL fetch
                print(f"[CrawlExecutor] Navigating to: {source_uri}")
                await page.goto(source_uri, wait_until="networkidle", timeout=30000)

                # 스크린샷 저장
                screenshot_path = self.output_dir / f"{crawl_source_id}_screenshot.png"
                await page.screenshot(path=str(screenshot_path))
                print(f"[CrawlExecutor] Screenshot saved: {screenshot_path}")

                # 페이지 title 추출 (간단한 메타데이터)
                title = await page.title()
                print(f"[CrawlExecutor] Page title: {title}")

                return {
                    "status": "success",
                    "title": title,
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

    def execute_crawl_sync(self, source_uri: str, crawl_source_id: str) -> dict[str, str]:
        """동기 wrapper for execute_crawl."""
        return asyncio.run(self.execute_crawl(source_uri, crawl_source_id))
