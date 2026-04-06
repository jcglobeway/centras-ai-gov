"""
LLM 공급자 추상화.

LLM_PROVIDER=ollama(기본) 또는 openai 환경변수로 런타임 전환.
"""
from __future__ import annotations
from typing import Protocol, runtime_checkable
import os

@runtime_checkable
class LLMProvider(Protocol):
    def generate(self, prompt: str) -> str: ...

class OllamaLLMProvider:
    """Ollama /api/generate 호출."""
    def __init__(self, base_url: str, model: str) -> None:
        self.base_url = base_url.rstrip("/")
        self.model = model

    def generate(self, prompt: str) -> str:
        import httpx
        resp = httpx.post(
            f"{self.base_url}/api/generate",
            json={"model": self.model, "prompt": prompt, "stream": False},
            timeout=120.0,
        )
        resp.raise_for_status()
        return resp.json().get("response", "").strip()

class OpenAILLMProvider:
    """OpenAI gpt-4o-mini. OPENAI_API_KEY 필요."""
    def __init__(self, model: str = "gpt-4o-mini") -> None:
        self.model = model

    def generate(self, prompt: str) -> str:
        from openai import OpenAI  # lazy import
        client = OpenAI()
        resp = client.chat.completions.create(
            model=self.model,
            messages=[{"role": "user", "content": prompt}],
            temperature=0.0,
        )
        return resp.choices[0].message.content.strip()

def get_llm_provider(provider: str | None = None, **kwargs) -> LLMProvider:
    p = (provider or os.getenv("LLM_PROVIDER", "ollama")).lower()
    if p == "openai":
        return OpenAILLMProvider(**kwargs)
    ollama_url = kwargs.get("base_url") or os.getenv("OLLAMA_URL", "http://jcg-office.tailedf4dc.ts.net:11434")
    model = kwargs.get("model") or os.getenv("OLLAMA_MODEL", "qwen3:8b")
    return OllamaLLMProvider(base_url=ollama_url, model=model)
