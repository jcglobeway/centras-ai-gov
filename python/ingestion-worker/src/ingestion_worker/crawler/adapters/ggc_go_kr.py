import os
import re
from typing import Optional

import requests
from bs4 import BeautifulSoup
from loguru import logger

from .base import BaseSiteAdapter


class GgcGoKrAdapter(BaseSiteAdapter):
    """경기도의회(ggc.go.kr) 전용 어댑터."""

    DOMAIN_PATTERN = r"ggc\.go\.kr"

    URL_ROUTES = [
        (r"/site/lwmkr/blog/",                              "member"),
        (r"/site/main/content/.*[Ff]loor",                  "floor_guide"),
        (r"admnsOfcad",                                     "floor_guide"),
        (r"actvMmbr/list",                                  "member_list"),
        (r"lwInfo|자치법규|ordinance",                        "law"),
        (r"honorGuard",                                     "honor_guard"),
        (r"council/organization|secretariat|cncl|prlmnCmpst|prlmnOprtn", "council_info"),
        (r"schedule|calendar",                              "schedule"),
        (r"committee|cmmt|stndnCmt|spclCmt|committeeactive", "committee"),
        (r"civil/petition|pttnPrcsn|residOrdinReq|civilAudit|infOthbc",  "citizen"),
    ]

    _SEARCH_NOISE = {"전체", "제목", "내용", "첨부파일", "정확도", "최신순"}

    def __init__(self):
        self._api_cache: list = []

    # ── 세션 훅 ──────────────────────────────────────────────────────────────

    def on_crawl_start(self) -> None:
        self._api_cache = self._fetch_member_api()
        logger.info(f"GGC 어댑터 초기화 완료: 의원 캐시 {len(self._api_cache)}명")

    # ── GGC 공개 API ─────────────────────────────────────────────────────────

    def _fetch_member_api(self) -> list:
        """
        ggc.go.kr 공개 API로 현직 의원 전체 목록을 수신한다.
        GGC_MEMBER_API_KEY 미설정 또는 API 오류 시 빈 리스트 반환 → HTML 폴백.
        """
        api_key = os.getenv("GGC_MEMBER_API_KEY", "").strip()
        if not api_key:
            logger.warning("GGC_MEMBER_API_KEY 미설정 — 의원 HTML 파싱 폴백 사용")
            return []

        url = (
            "https://www.ggc.go.kr/site/main/api/portaltoggc/"
            f"ggcmemrecinfoview/onclick/?key={api_key}"
        )
        headers = {
            "User-Agent": (
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/124.0.0.0 Safari/537.36"
            ),
            "Accept": "application/json, text/plain, */*",
        }
        try:
            resp = requests.get(url, headers=headers, timeout=15)
            if not resp.ok:
                logger.warning(f"GGC API 응답 오류: {resp.status_code} — HTML 폴백")
                return []

            data = resp.json()
            if not isinstance(data, list):
                logger.warning("GGC API 응답 형식 이상 — HTML 폴백")
                return []

            # 현직 의원만 (miPresent != "N")
            active = [m for m in data if m.get("miPresent", "Y") != "N"]
            logger.info(f"GGC API: {len(active)}명 수신 (전체 {len(data)}명 중)")
            return active

        except Exception as e:
            logger.warning(f"GGC API 호출 실패: {e} — HTML 폴백")
            return []

    def _find_in_cache(self, name: str) -> dict:
        """이름으로 API 캐시를 검색한다. 없으면 빈 dict."""
        if not name:
            return {}
        name_stripped = name.strip()
        for member in self._api_cache:
            if member.get("miName", "").strip() == name_stripped:
                return member
        return {}

    # ── 파서 ─────────────────────────────────────────────────────────────────

    def parse_member(self, soup: BeautifulSoup, url: str) -> dict:
        """
        의원 개인 페이지 파싱 — API 캐시 우선, HTML 폴백.
        반환 필드: name, party, district, district_detail, contact,
                   phone, floor, committee, biography, pledge, photo_url
        """
        def _text(el) -> Optional[str]:
            return el.get_text(strip=True) if el else None

        name = _text(soup.find("span", class_="name") or soup.find("h2"))
        party = _text(soup.find(class_=re.compile(r"party|정당", re.I)))
        district = _text(soup.find(class_=re.compile(r"district|선거구", re.I)))
        contact = _text(soup.find(class_=re.compile(r"contact|tel|연락", re.I)))
        floor = None
        floor_tag = soup.find(string=re.compile(r"\d+층"))
        if floor_tag:
            m = re.search(r"(\d+)층", floor_tag)
            floor = m.group(1) if m else None
        committees = [
            el.get_text(strip=True)
            for el in soup.find_all(class_=re.compile(r"committee|위원회", re.I))
        ]

        if not name:
            logger.debug(f"GGC member 파싱 실패 (name 없음): {url}")

        api = self._find_in_cache(name or "")
        biography       = api.get("miWeakForce") or None
        pledge          = api.get("miPpldgMatter") or None
        phone           = api.get("miOfficeNumber") or None
        district_detail = api.get("miDistrictDetail") or api.get("mdDistrictDetail") or None
        _photo_path     = api.get("miProfileImagePath") or api.get("rgProfileUrl") or None
        photo_url       = f"https://www.ggc.go.kr{_photo_path}" if _photo_path else None

        if not party and api:
            party = api.get("miJungdangName") or api.get("miNegotiationName")
        if not district and api:
            district = api.get("miDistrictName")

        return {
            "name": name,
            "party": party,
            "district": district,
            "district_detail": district_detail,
            "contact": contact,
            "phone": phone,
            "floor": floor,
            "committee": committees,
            "biography": biography,
            "pledge": pledge,
            "photo_url": photo_url,
        }

    def parse_floor_guide(self, soup: BeautifulSoup, url: str) -> list[dict]:
        """
        청사 층별 안내 파싱 — <table> 및 <ul class="full"> 두 형식 지원.
        검색폼 UI 노이즈(전체/제목/내용 등) 자동 제거.
        """
        result = []

        for table in soup.find_all("table"):
            for row in table.find_all("tr"):
                cells = [td.get_text(strip=True) for td in row.find_all(["td", "th"])]
                if len(cells) >= 2:
                    result.append({
                        "room_number": cells[0],
                        "name": cells[1],
                        "department": cells[2] if len(cells) > 2 else None,
                        "contact": cells[3] if len(cells) > 3 else None,
                    })

        for ul in soup.find_all("ul", class_="full"):
            for li in ul.find_all("li"):
                room_num_el = li.find("span")
                room_num = room_num_el.get_text(strip=True) if room_num_el else None
                if room_num in self._SEARCH_NOISE:
                    continue
                if room_num_el:
                    room_num_el.decompose()
                name = li.get_text(strip=True)
                if name and name not in self._SEARCH_NOISE:
                    result.append({"room_number": room_num, "name": name})

        return result

    def fetch_law_list(self) -> list[dict]:
        """
        law.go.kr 자치법규 검색 API로 경기도의회 조례 목록을 수집한다.
        LAW_API_KEY 미설정 또는 API 오류 시 빈 목록 반환.
        최대 200건 제한.
        """
        api_key = os.getenv("LAW_API_KEY", "").strip()
        if not api_key:
            logger.warning("LAW_API_KEY 미설정 — law.go.kr 조례 수집 불가")
            return []

        url = (
            "https://www.law.go.kr/DRF/lawSearch.do"
            f"?OC={api_key}&target=ordin&org=경기도의회&type=JSON&display=100"
        )
        try:
            resp = requests.get(url, timeout=15)
            if not resp.ok:
                logger.warning(f"law.go.kr API 오류: {resp.status_code}")
                return []
            data = resp.json()
            laws = data.get("LawSearch", {}).get("law", [])
            if isinstance(laws, dict):
                laws = [laws]
            logger.info(f"law.go.kr 조례 목록: {len(laws)}건 수신")
            return laws[:200]
        except Exception as e:
            logger.warning(f"law.go.kr API 호출 실패: {e}")
            return []

    def parse_law(self, soup: BeautifulSoup, url: str) -> dict:
        """조례 본문 페이지 파싱 — 조례명, 제정일, 조문 텍스트 추출."""
        title_el = (
            soup.find(class_=re.compile(r"law.*title|title.*law|조례명", re.I))
            or soup.find("h1")
            or soup.find("h2")
        )
        title = title_el.get_text(strip=True) if title_el else url

        date_el = soup.find(string=re.compile(r"제정|공포|시행"))
        enacted_date = None
        if date_el:
            m = re.search(r"\d{4}[.\-]\d{2}[.\-]\d{2}", date_el)
            enacted_date = m.group(0) if m else None

        body = soup.find(class_=re.compile(r"law.?content|cont.?wrap|article", re.I))
        raw_text = body.get_text(separator="\n") if body else soup.get_text(separator="\n")
        content = re.sub(r"\n{3,}", "\n\n", raw_text).strip()

        return {
            "title": title,
            "enacted_date": enacted_date,
            "content": content,
            "page_type": "law",
        }

    def parse_honor_guard(self, soup: BeautifulSoup, url: str) -> dict:
        """
        의장단 페이지 파싱 — 의장/부의장 카드 구조에서 이름·정당·선거구 추출.
        카드의 부모 요소가 '의장' 또는 '부의장' span을 포함하는 구조를 이용한다.
        """
        members = []
        seen = set()
        for span in soup.find_all("span"):
            role_text = span.get_text(strip=True)
            if role_text not in ("의장", "부의장"):
                continue
            parent = span.find_parent()
            if not parent:
                continue
            full_text = parent.get_text(separator=" | ", strip=True)
            if full_text in seen or len(full_text) < 10:
                continue
            seen.add(full_text)
            members.append(full_text)

        if not members:
            return {}

        lines = ["경기도의회 의장단 현황"] + members
        return {
            "title": "경기도의회 의장단",
            "content": "\n".join(lines),
            "page_type": "honor_guard",
        }

    def parse_council_info(self, soup: BeautifulSoup, url: str) -> dict:
        """사무처 조직도·의회 구성·운영 안내 페이지 파싱."""
        _NOISE_TAGS = ["nav", "header", "footer", "aside", "script", "style"]
        for tag in soup.find_all(_NOISE_TAGS):
            tag.decompose()

        title_el = soup.find("h1") or soup.find("h2") or soup.find(class_=re.compile(r"page.*title|title", re.I))
        title = title_el.get_text(strip=True) if title_el else ""

        rows = []
        for table in soup.find_all("table"):
            headers = [th.get_text(strip=True) for th in table.find_all("th")]
            for tr in table.find_all("tr"):
                cells = [td.get_text(strip=True) for td in tr.find_all("td")]
                if not cells:
                    continue
                if headers and len(cells) == len(headers):
                    rows.append(dict(zip(headers, cells)))
                elif len(cells) >= 2:
                    rows.append({"부서": cells[0], "담당자": cells[1],
                                 "전화": cells[2] if len(cells) > 2 else "",
                                 "업무": cells[3] if len(cells) > 3 else ""})

        if rows:
            lines = []
            for r in rows:
                parts = [f"{k}: {v}" for k, v in r.items() if v]
                lines.append(" | ".join(parts))
            content = "\n".join(lines)
        else:
            body = soup.find(class_=re.compile(r"cont.?wrap|content|main", re.I))
            content = body.get_text(separator="\n").strip() if body else soup.get_text(separator="\n").strip()
            content = re.sub(r"\n{3,}", "\n\n", content)

        return {"title": title, "content": content, "page_type": "council_info"}

    def parse_committee(self, soup: BeautifulSoup, url: str) -> dict:
        """위원회 상세 페이지 파싱 — 소속 의원, 위원장, 위원회 유형 추출."""
        _NOISE_TAGS = ["nav", "header", "footer", "aside", "script", "style"]
        for tag in soup.find_all(_NOISE_TAGS):
            tag.decompose()

        title_el = soup.find("h1") or soup.find("h2") or soup.find(class_=re.compile(r"page.*title|cmmt.*name", re.I))
        title = title_el.get_text(strip=True) if title_el else ""

        committee_type = "특별위원회" if re.search(r"spclCmt|특별", url) else "상임위원회"

        members = []
        chairperson = None
        for table in soup.find_all("table"):
            for tr in table.find_all("tr"):
                cells = [td.get_text(strip=True) for td in tr.find_all("td")]
                if len(cells) < 2:
                    continue
                name = cells[0]
                role = cells[1] if len(cells) > 1 else ""
                party = cells[2] if len(cells) > 2 else ""
                if name:
                    members.append({"name": name, "role": role, "party": party})
                    if "위원장" in role and not chairperson:
                        chairperson = name

        links = []
        for a in soup.find_all("a", href=re.compile(r"committee|cmmt|stndnCmt|spclCmt")):
            href = a.get("href", "")
            if href and not href.startswith("#"):
                if not href.startswith("http"):
                    href = f"https://www.ggc.go.kr{href}"
                links.append(href)

        member_names = [m["name"] for m in members if m["name"]]
        content_parts = [f"{title} ({committee_type})"]
        if chairperson:
            content_parts.append(f"위원장: {chairperson}")
        if member_names:
            content_parts.append(f"소속의원: {', '.join(member_names)}")
        content = "\n".join(content_parts)

        return {
            "title": title,
            "content": content,
            "committee_type": committee_type,
            "chairperson": chairperson,
            "members": members,
            "links": links,
            "page_type": "committee",
        }

    def parse_citizen(self, soup: BeautifulSoup, url: str) -> dict:
        """청원/진정/조례청구/감사청구/정보공개 안내 페이지 파싱."""
        _NOISE_TAGS = ["nav", "header", "footer", "aside", "script", "style"]
        for tag in soup.find_all(_NOISE_TAGS):
            tag.decompose()

        title_el = soup.find("h1") or soup.find("h2") or soup.find(class_=re.compile(r"page.*title|title", re.I))
        title = title_el.get_text(strip=True) if title_el else ""

        links = []
        if "petition/list" in url or "list" in url:
            for a in soup.find_all("a", href=re.compile(r"petition|civil|pttn")):
                href = a.get("href", "")
                if href and not href.startswith("#"):
                    if not href.startswith("http"):
                        href = f"https://www.ggc.go.kr{href}"
                    links.append(href)

        body = soup.find(class_=re.compile(r"cont.?wrap|content|main", re.I))
        raw = body.get_text(separator="\n").strip() if body else soup.get_text(separator="\n").strip()
        content = re.sub(r"\n{3,}", "\n\n", raw)

        return {"title": title, "content": content, "links": links, "page_type": "citizen"}

    def parse_schedule(self, soup: BeautifulSoup, url: str) -> dict:
        """의사일정 페이지 파싱 — 날짜/위원회/일정유형/장소 구조화."""
        _NOISE_TAGS = ["nav", "header", "footer", "aside", "script", "style"]
        for tag in soup.find_all(_NOISE_TAGS):
            tag.decompose()

        title_el = soup.find("h1") or soup.find("h2")
        title = title_el.get_text(strip=True) if title_el else "의사일정"

        events = []
        for table in soup.find_all("table"):
            for tr in table.find_all("tr"):
                cells = [td.get_text(strip=True) for td in tr.find_all("td")]
                if len(cells) >= 2:
                    events.append(" ".join(c for c in cells if c))

        content = "\n".join(events) if events else soup.get_text(separator="\n").strip()
        content = re.sub(r"\n{3,}", "\n\n", content).strip()

        return {"title": title, "content": content, "page_type": "schedule"}
