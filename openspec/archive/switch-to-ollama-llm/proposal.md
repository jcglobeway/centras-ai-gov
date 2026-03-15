# Proposal

## Change ID

`switch-to-ollama-llm`

## Summary

### 변경 목적
- OpenAI API → Ollama로 전환
- 로컬 LLM으로 비용 절감 및 빠른 개발
- 외부 의존성 감소

### 변경 범위
- rag-orchestrator에 Ollama API 호출
- pyproject.toml에서 openai 제거
- 환경 변수: OLLAMA_URL, OLLAMA_MODEL

### 제외 범위
- Ollama 설치 가이드 (사용자가 설치)
- GPU 최적화

## Done Definition

- [ ] Ollama API 호출 구현
- [ ] OpenAI 의존성 제거
- [ ] 환경 변수 처리
- [ ] ./gradlew test 통과
