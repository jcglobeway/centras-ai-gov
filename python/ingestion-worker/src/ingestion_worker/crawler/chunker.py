from __future__ import annotations

from ..models import TextChunk


class HierarchicalChunker:
    """섹션 구조를 보존하는 계층형 청커.

    섹션 단위로 먼저 분할하고, 섹션이 max_chunk_size를 초과하면 다시 분할한다.
    섹션 정보가 없으면 RecursiveCharacterTextSplitter 방식으로 폴백한다.
    """

    def __init__(
        self,
        max_chunk_size: int = 512,
        chunk_overlap: int = 64,
    ):
        self.max_chunk_size = max_chunk_size
        self.chunk_overlap = chunk_overlap

    def chunk(
        self,
        sections: list[dict],
        full_text: str,
        source_url: str,
    ) -> list[TextChunk]:
        if sections:
            return self._chunk_by_sections(sections, source_url)
        return self._chunk_flat(full_text, source_url)

    def _chunk_by_sections(self, sections: list[dict], source_url: str) -> list[TextChunk]:
        chunks: list[TextChunk] = []
        order = 0

        for section in sections:
            heading = section.get("heading")
            text = section.get("text", "").strip()
            if not text:
                continue

            # 섹션 텍스트에 헤딩 prefix 추가 — 검색 시 맥락 보존
            prefixed = f"{heading}\n{text}" if heading else text

            if len(prefixed) <= self.max_chunk_size:
                chunks.append(TextChunk(
                    chunk_key=f"chunk_{order}",
                    chunk_text=prefixed,
                    chunk_order=order,
                    token_count=len(prefixed.split()),
                    source_url=source_url,
                    heading=heading,
                ))
                order += 1
            else:
                # 섹션이 너무 크면 슬라이딩 윈도우로 재분할
                sub_chunks = self._sliding_split(prefixed, heading, source_url, start_order=order)
                chunks.extend(sub_chunks)
                order += len(sub_chunks)

        return chunks

    def _chunk_flat(self, text: str, source_url: str) -> list[TextChunk]:
        from langchain_text_splitters import RecursiveCharacterTextSplitter

        splitter = RecursiveCharacterTextSplitter(
            chunk_size=self.max_chunk_size,
            chunk_overlap=self.chunk_overlap,
            separators=["\n\n", "\n", "。", ".", " ", ""],
        )
        parts = splitter.split_text(text)
        return [
            TextChunk(
                chunk_key=f"chunk_{i}",
                chunk_text=part,
                chunk_order=i,
                token_count=len(part.split()),
                source_url=source_url,
            )
            for i, part in enumerate(parts)
        ]

    def _sliding_split(
        self,
        text: str,
        heading: str | None,
        source_url: str,
        start_order: int,
    ) -> list[TextChunk]:
        chunks: list[TextChunk] = []
        start = 0
        idx = start_order

        while start < len(text):
            end = start + self.max_chunk_size
            chunk_text = text[start:end]
            chunks.append(TextChunk(
                chunk_key=f"chunk_{idx}",
                chunk_text=chunk_text,
                chunk_order=idx,
                token_count=len(chunk_text.split()),
                source_url=source_url,
                heading=heading,
            ))
            idx += 1
            start += self.max_chunk_size - self.chunk_overlap

        return chunks
