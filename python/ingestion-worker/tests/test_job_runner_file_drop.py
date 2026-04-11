from __future__ import annotations

from pathlib import Path

from ingestion_worker.job_runner import IngestionJobRunner


class DummyAdminApiClient:
    pass


def test_load_file_drop_pages_with_text_file(tmp_path: Path):
    file_path = tmp_path / "sample.txt"
    file_path.write_text("hello\nworld", encoding="utf-8")

    runner = IngestionJobRunner(admin_api_client=DummyAdminApiClient())
    pages = runner._load_file_drop_pages(str(file_path))

    assert len(pages) == 1
    assert pages[0].title == "sample.txt"
    assert pages[0].content == "hello\nworld"
    assert pages[0].metadata["source_type"] == "file_drop"


def test_load_file_drop_pages_supports_file_uri(tmp_path: Path):
    file_path = tmp_path / "uri.txt"
    file_path.write_text("uri content", encoding="utf-8")

    runner = IngestionJobRunner(admin_api_client=DummyAdminApiClient())
    pages = runner._load_file_drop_pages(file_path.resolve().as_uri())

    assert len(pages) == 1
    assert pages[0].content == "uri content"


def test_load_file_drop_pages_pdf_uses_pdf_extractor(monkeypatch, tmp_path: Path):
    file_path = tmp_path / "dummy.pdf"
    file_path.write_bytes(b"%PDF-1.4\n")

    runner = IngestionJobRunner(admin_api_client=DummyAdminApiClient())
    monkeypatch.setattr(
        runner,
        "_extract_pdf_text",
        lambda _path: "pdf extracted text",
    )

    pages = runner._load_file_drop_pages(str(file_path))

    assert len(pages) == 1
    assert pages[0].content == "pdf extracted text"


def test_load_file_drop_pages_returns_empty_when_pdf_extraction_fails(monkeypatch, tmp_path: Path):
    file_path = tmp_path / "broken.pdf"
    file_path.write_bytes(b"%PDF-1.4\n")

    runner = IngestionJobRunner(admin_api_client=DummyAdminApiClient())

    def _raise(_path: Path) -> str:
        raise ValueError("PDF 추출 실패")

    monkeypatch.setattr(runner, "_extract_pdf_text", _raise)
    pages = runner._load_file_drop_pages(str(file_path))

    assert pages == []


def test_post_process_pdf_text_normalizes_whitespace():
    runner = IngestionJobRunner(admin_api_client=DummyAdminApiClient())
    text = "line1   line1b\r\n\r\n\r\nline2\t\tline2b"

    processed = runner._post_process_pdf_text(text)

    assert processed == "line1 line1b\n\nline2 line2b"
