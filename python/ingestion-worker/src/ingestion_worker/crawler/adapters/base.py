import re
from abc import ABC

from bs4 import BeautifulSoup


class BaseSiteAdapter(ABC):
    """
    사이트별 크롤러 어댑터 기반 클래스.
    새 사이트 추가 시 이 클래스를 상속하고 ADAPTER_REGISTRY에 등록한다.
    """

    DOMAIN_PATTERN: str = ""
    URL_ROUTES: list[tuple[str, str]] = []

    def route(self, url: str) -> str:
        """URL을 page_type으로 분류. 매칭 없으면 'general' 반환."""
        for pattern, page_type in self.URL_ROUTES:
            if re.search(pattern, url):
                return page_type
        return "general"

    def parse(self, page_type: str, soup: BeautifulSoup, url: str) -> dict:
        """
        page_type에 맞는 parse_{page_type} 메서드를 디스패치한다.
        메서드가 없으면 빈 dict 반환 → 범용 ContentExtractor로 폴백.
        """
        handler = getattr(self, f"parse_{page_type}", None)
        if handler:
            return handler(soup, url)
        return {}

    def on_crawl_start(self) -> None:
        """크롤 세션 시작 시 1회 호출. 하위 클래스에서 API 캐시 초기화 등에 활용."""
        pass

    def on_crawl_end(self) -> None:
        """크롤 세션 종료 시 호출. 필요 시 하위 클래스에서 정리 작업."""
        pass
