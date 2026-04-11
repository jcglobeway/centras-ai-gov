import re
from typing import Optional

from bs4 import BeautifulSoup
from loguru import logger

from .base import BaseSiteAdapter


class GjfOrKrAdapter(BaseSiteAdapter):
    """경기도일자리재단(gjf.or.kr / job.gg.go.kr) 전용 어댑터.

    경기도일자리재단 공식 사이트와 Jobaba(job.gg.go.kr) 취업 지원 사이트를
    함께 처리한다. URL 패턴에 따라 페이지 유형을 분류하고 특화 파서로 처리한다.

    현재 구현된 파서:
    - parse_notice: 공지사항 목록/상세
    - parse_main_biz: 주요사업 목록/상세

    향후 추가 예정 (centras-ai-server-admin 참조):
    - parse_job_support: 구직자 취업지원 (jobaba_job_support.py)
    - parse_enterprise_support: 기업지원 (jobaba_enterprise_support.py)
    - parse_employment_education: 취업교육 (jobaba_employment_education.py)
    - parse_apply_support: 신청지원 (jobaba_apply_support.py)
    - parse_youth_worker: 청년노동자 지원 (jobaba_youth_worker.py)
    - parse_youth_point: 청년포인트 (jobaba_youth_point.py)
    - parse_youth_annuity: 청년연금 (jobaba_youth_annuity.py)
    """

    DOMAIN_PATTERN = r"gjf\.or\.kr|job\.gg\.go\.kr"

    URL_ROUTES = [
        (r"/main/pst/",                          "notice"),
        (r"/main/main_biz/",                     "main_biz"),
        (r"/jobSprt/|/jobaba/.*job",              "job_support"),
        (r"/entSprt/",                            "enterprise_support"),
        (r"/empmEdu/",                            "employment_education"),
        (r"/applySprt/",                          "apply_support"),
        (r"/youth/worker|youth_worker_support",   "youth_worker"),
        (r"/youth/point|youth_point",             "youth_point"),
        (r"/youth/annuity|youth_annuity",         "youth_annuity"),
    ]

    # ── 파서 ─────────────────────────────────────────────────────────────────

    def parse_notice(self, soup: BeautifulSoup, url: str) -> dict:
        """공지사항 목록·상세 페이지 파싱."""
        for tag in soup.find_all(["nav", "header", "footer", "aside", "script", "style"]):
            tag.decompose()

        title_el = soup.find("h1") or soup.find("h2") or soup.find(class_=re.compile(r"title|제목", re.I))
        title = title_el.get_text(strip=True) if title_el else "공지사항"

        date_el = soup.find(string=re.compile(r"\d{4}[.\-]\d{2}[.\-]\d{2}"))
        date_str: Optional[str] = None
        if date_el:
            m = re.search(r"\d{4}[.\-]\d{2}[.\-]\d{2}", str(date_el))
            date_str = m.group(0) if m else None

        body = (
            soup.find(class_=re.compile(r"cont.?wrap|content.?body|board.?view", re.I))
            or soup.find("article")
        )
        raw = body.get_text(separator="\n").strip() if body else soup.get_text(separator="\n").strip()
        content = re.sub(r"\n{3,}", "\n\n", raw)

        result = {"title": title, "content": content, "page_type": "notice"}
        if date_str:
            result["published_date"] = date_str
        return result

    def parse_main_biz(self, soup: BeautifulSoup, url: str) -> dict:
        """주요사업 목록·상세 페이지 파싱.

        gjf.or.kr은 주요사업 목록을 API로 제공하지만 HTML 페이지에서도
        기본 정보를 추출할 수 있다.
        """
        for tag in soup.find_all(["nav", "header", "footer", "aside", "script", "style"]):
            tag.decompose()

        title_el = soup.find("h1") or soup.find("h2") or soup.find(class_=re.compile(r"biz.?title|title", re.I))
        title = title_el.get_text(strip=True) if title_el else "주요사업"

        body = (
            soup.find(class_=re.compile(r"biz.?content|cont.?wrap|content", re.I))
            or soup.find("article")
        )
        raw = body.get_text(separator="\n").strip() if body else soup.get_text(separator="\n").strip()
        content = re.sub(r"\n{3,}", "\n\n", raw)

        return {"title": title, "content": content, "page_type": "main_biz"}
