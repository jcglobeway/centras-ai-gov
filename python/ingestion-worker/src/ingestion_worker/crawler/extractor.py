from __future__ import annotations

import re
from urllib.parse import urljoin, urlparse

from bs4 import BeautifulSoup
from loguru import logger


class ContentExtractor:
    """HTML에서 섹션 구조를 보존하며 의미 있는 텍스트와 링크를 추출한다."""

    NOISE_TAGS = ["script", "style", "nav", "footer", "header",
                  "aside", "iframe", "noscript", "svg"]

    TYPE_PATTERNS = {
        "blog":    [r"/blog/", r"/post/", r"/article/", r"/news/"],
        "product": [r"/product/", r"/item/", r"/shop/", r"/store/"],
        "doc":     [r"/docs/", r"/guide/", r"/manual/", r"/reference/"],
        "faq":     [r"/faq", r"/help/", r"/support/"],
        "about":   [r"/about", r"/company/", r"/team/"],
    }

    def extract(self, html: str, base_url: str) -> dict:
        soup = BeautifulSoup(html, "lxml")

        # 노이즈 제거 전 h1 캡처 — header 태그 안에 있어도 페이지 제목 보존
        h1_el = soup.find("h1")
        page_heading = h1_el.get_text(strip=True) if h1_el else ""

        for tag in self.NOISE_TAGS:
            for el in soup.find_all(tag):
                el.decompose()

        # 제목: h1 우선, 없으면 <title>
        title = ""
        if page_heading:
            title = page_heading
        elif soup.title:
            title = soup.title.string or ""
        elif soup.find("h1"):
            title = soup.find("h1").get_text(strip=True)

        # 메인 콘텐츠 영역 우선 추출
        main = (
            soup.find("main") or
            soup.find("article") or
            soup.find(id=re.compile(r"content|main|article", re.I)) or
            soup.find("body")
        )
        content = main.get_text(separator="\n", strip=True) if main else ""
        content = re.sub(r"\n{3,}", "\n\n", content)
        content = re.sub(r"[ \t]+", " ", content)

        # 헤딩이 content에서 누락된 경우 복구
        if page_heading and page_heading not in content[:len(page_heading) + 20]:
            content = page_heading + "\n" + content

        # 섹션 구조 추출 (헤딩별로 분할)
        sections = self._extract_sections(main) if main else []

        # 내부 링크만 수집
        parsed_base = urlparse(base_url)
        links = []
        for a in soup.find_all("a", href=True):
            href = a["href"].strip()
            if not href or href.startswith(("#", "mailto:", "tel:", "javascript:")):
                continue
            abs_url = urljoin(base_url, href)
            parsed = urlparse(abs_url)
            if parsed.netloc == parsed_base.netloc:
                # 외국어 버전 경로 제외
                if any(seg in parsed.path for seg in ['/jpn/', '/chn/', '/eng/', '/efk/']):
                    continue
                clean = f"{parsed.scheme}://{parsed.netloc}{parsed.path}"
                links.append(clean)

        og_title = ""
        og_desc = ""
        if og := soup.find("meta", property="og:title"):
            og_title = og.get("content", "")
        if og := soup.find("meta", property="og:description"):
            og_desc = og.get("content", "")

        page_type = "general"
        for ptype, patterns in self.TYPE_PATTERNS.items():
            if any(re.search(p, base_url) for p in patterns):
                page_type = ptype
                break

        return {
            "title": title.strip(),
            "content": content[:8000],
            "sections": sections,
            "links": list(set(links)),
            "page_type": page_type,
            "metadata": {"og_title": og_title, "og_description": og_desc},
        }

    def _extract_sections(self, container) -> list[dict]:
        """헤딩(h1~h4) 단위로 콘텐츠를 섹션별로 분리한다."""
        sections = []
        current_heading = None
        current_texts: list[str] = []

        for el in container.descendants:
            if not hasattr(el, "name") or el.name is None:
                continue
            if el.name in ("h1", "h2", "h3", "h4"):
                if current_texts:
                    text = "\n".join(t for t in current_texts if t.strip())
                    if text.strip():
                        sections.append({
                            "heading": current_heading,
                            "text": text[:2000],
                        })
                current_heading = el.get_text(strip=True)
                current_texts = []
            elif el.name in ("p", "li", "td", "dd", "blockquote"):
                txt = el.get_text(strip=True)
                if txt:
                    current_texts.append(txt)

        if current_texts:
            text = "\n".join(t for t in current_texts if t.strip())
            if text.strip():
                sections.append({"heading": current_heading, "text": text[:2000]})

        return sections
