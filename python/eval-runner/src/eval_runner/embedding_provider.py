"""
임베딩 공급자 추상화.

EMBEDDING_PROVIDER=ollama(기본) 또는 openai 환경변수로 런타임 전환.
프로덕션 전환 시 OpenAIEmbeddingProvider로 교체.
"""
from __future__ import annotations
from typing import Protocol, runtime_checkable
import os

@runtime_checkable
class EmbeddingProvider(Protocol):
    def embed(self, texts: list[str]) -> list[list[float]]: ...

class OllamaEmbeddingProvider:
    """Ollama /api/embed 호출. bge-m3 기본 모델."""
    def __init__(self, base_url: str, model: str = "bge-m3") -> None:
        self.base_url = base_url.rstrip("/")
        self.model = model

    def embed(self, texts: list[str]) -> list[list[float]]:
        import httpx
        results = []
        for text in texts:
            resp = httpx.post(
                f"{self.base_url}/api/embed",
                json={"model": self.model, "input": text},
                timeout=60.0,
            )
            resp.raise_for_status()
            data = resp.json()
            embeddings = data.get("embeddings") or data.get("embedding")
            if isinstance(embeddings, list) and embeddings:
                emb = embeddings[0] if isinstance(embeddings[0], list) else embeddings
                results.append(emb)
        return results

class OpenAIEmbeddingProvider:
    """OpenAI text-embedding-3-small. OPENAI_API_KEY 필요."""
    def __init__(self, model: str = "text-embedding-3-small") -> None:
        self.model = model

    def embed(self, texts: list[str]) -> list[list[float]]:
        from openai import OpenAI  # lazy import — openai optional dependency
        client = OpenAI()
        response = client.embeddings.create(input=texts, model=self.model)
        return [item.embedding for item in response.data]

def get_embedding_provider(provider: str | None = None, **kwargs) -> EmbeddingProvider:
    p = (provider or os.getenv("EMBEDDING_PROVIDER", "ollama")).lower()
    if p == "openai":
        return OpenAIEmbeddingProvider(**kwargs)
    ollama_url = kwargs.get("base_url") or os.getenv("OLLAMA_URL", "http://jcg-office.tailedf4dc.ts.net:11434")
    model = kwargs.get("model", "bge-m3")
    return OllamaEmbeddingProvider(base_url=ollama_url, model=model)
