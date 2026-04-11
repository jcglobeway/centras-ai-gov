import re
from typing import Optional

from bs4 import BeautifulSoup

from .base import BaseSiteAdapter


class NamguGwangjuKrAdapter(BaseSiteAdapter):
    """광주광역시 남구청(namgu.gwangju.kr) 전용 어댑터.

    URL 패턴에 따라 민원·복지·FAQ 등 페이지 유형을 분류하고 특화 파서로 처리한다.

    현재 구현된 파서:
    - parse_faq: 자주 묻는 질문
    - parse_civil_service: 민원 안내

    향후 추가 예정 (centras-ai-server-admin 참조):
    - parse_welfare: 복지서비스 (welfare_services.py)
    - parse_life_service: 생활정보 (life_services.py)
    - parse_application: 신청절차 안내 (application_procedures.py)
    - parse_department: 부서별 업무 소개 (department_info.py)
    - parse_namgu_info: 남구 소개 (namgu_info.py)
    - parse_sitemap: 사이트맵 URL 목록 추출 (sitemap.py)
    """

    DOMAIN_PATTERN = r"namgu\.gwangju\.kr"

    URL_ROUTES = [
        # FAQ: mid=a10000... 패턴
        (r"bid=\d+.*faq|/faq|FAQ",                         "faq"),
        # 민원서비스: a102 계열 mid
        (r"mid=a102",                                       "civil_service"),
        # 복지서비스: a104 계열 mid
        (r"mid=a104",                                       "welfare"),
        # 생활정보: a103 계열 mid
        (r"mid=a103",                                       "life_service"),
        # 신청절차 안내
        (r"application|apply|신청",                          "application"),
        # 부서 소개
        (r"department|division|부서|조직",                   "department"),
        # 남구 소개
        (r"mid=a101|intro|about|소개",                       "namgu_info"),
        # 사이트맵
        (r"sitemap",                                         "sitemap"),
    ]

    # ── 파서 ─────────────────────────────────────────────────────────────────

    def parse_faq(self, soup: BeautifulSoup, url: str) -> dict:
        """자주 묻는 질문 페이지 파싱 — Q&A 쌍 추출."""
        for tag in soup.find_all(["nav", "header", "footer", "aside", "script", "style"]):
            tag.decompose()

        title_el = soup.find("h1") or soup.find("h2")
        title = title_el.get_text(strip=True) if title_el else "자주 묻는 질문"

        # 남구청 FAQ 구조: dl/dt/dd 또는 table 형식
        qa_pairs: list[str] = []
        for dl in soup.find_all("dl"):
            dts = dl.find_all("dt")
            dds = dl.find_all("dd")
            for q, a in zip(dts, dds):
                q_text = q.get_text(strip=True)
                a_text = a.get_text(separator=" ", strip=True)
                if q_text:
                    qa_pairs.append(f"Q: {q_text}\nA: {a_text}")

        if not qa_pairs:
            # table 형식 폴백
            for table in soup.find_all("table"):
                for tr in table.find_all("tr"):
                    cells = [td.get_text(strip=True) for td in tr.find_all("td")]
                    if len(cells) >= 2:
                        qa_pairs.append(f"Q: {cells[0]}\nA: {cells[1]}")

        content = "\n\n".join(qa_pairs) if qa_pairs else soup.get_text(separator="\n").strip()
        return {"title": title, "content": content, "page_type": "faq"}

    def parse_civil_service(self, soup: BeautifulSoup, url: str) -> dict:
        """민원서비스 안내 페이지 파싱."""
        for tag in soup.find_all(["nav", "header", "footer", "aside", "script", "style"]):
            tag.decompose()

        title_el = soup.find("h1") or soup.find("h2") or soup.find(class_=re.compile(r"title|제목", re.I))
        title = title_el.get_text(strip=True) if title_el else "민원서비스"

        body = soup.find(class_=re.compile(r"cont.?wrap|content|board.?view", re.I)) or soup.find("article")
        raw = body.get_text(separator="\n").strip() if body else soup.get_text(separator="\n").strip()
        content = re.sub(r"\n{3,}", "\n\n", raw)

        # 처리 기관, 처리 기간, 수수료 등 정형 정보 추출
        metadata: dict = {}
        for row in soup.find_all("tr"):
            cells = [td.get_text(strip=True) for td in row.find_all(["th", "td"])]
            if len(cells) >= 2:
                key = cells[0]
                val = cells[1]
                if any(k in key for k in ("처리기간", "수수료", "담당부서", "전화번호")):
                    metadata[key] = val

        result: dict = {"title": title, "content": content, "page_type": "civil_service"}
        if metadata:
            result["service_info"] = metadata
        return result
