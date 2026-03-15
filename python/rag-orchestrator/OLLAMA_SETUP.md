# Ollama Setup Guide

## Ollama 설치

### Windows
```powershell
# Ollama 다운로드 및 설치
# https://ollama.com/download

# 설치 후 자동 실행됨 (백그라운드)
```

### 모델 다운로드

```bash
# 추천 모델: llama3.2 (가볍고 빠름)
ollama pull llama3.2

# 또는 더 큰 모델
ollama pull llama3.1
ollama pull gemma2
```

## rag-orchestrator 실행

### 환경 변수

```bash
# Ollama URL (기본값: http://localhost:11434)
export OLLAMA_URL=http://localhost:11434

# 사용할 모델 (기본값: llama3.2)
export OLLAMA_MODEL=llama3.2
```

### 실행

```bash
cd python/rag-orchestrator
pip install -e .
rag-orchestrator
```

서버 실행 후:
- http://localhost:8090/healthz (health check)
- POST http://localhost:8090/generate (답변 생성)

## 테스트

```bash
curl -X POST http://localhost:8090/generate \
  -H "Content-Type: application/json" \
  -d '{
    "question_id": "test_001",
    "question_text": "복지 혜택은 어떻게 신청하나요?",
    "organization_id": "org_seoul_120",
    "service_id": "svc_welfare"
  }'
```

## Ollama 모델 추천

| 모델 | 크기 | 속도 | 품질 | 용도 |
|------|------|------|------|------|
| llama3.2 | 2GB | 빠름 | 중간 | 개발/테스트 |
| llama3.1 | 5GB | 보통 | 높음 | Production |
| gemma2 | 2GB | 빠름 | 중간 | 개발/테스트 |
| qwen2.5 | 5GB | 보통 | 높음 | 한국어 특화 |

## 문제 해결

### Ollama가 실행되지 않을 때
```bash
# Windows: 작업 관리자에서 Ollama 확인
# 또는 수동 실행
ollama serve
```

### 모델이 없을 때
```bash
ollama list  # 설치된 모델 확인
ollama pull llama3.2  # 모델 다운로드
```

### 느린 응답
```bash
# GPU 사용 확인
ollama run llama3.2 --verbose

# 더 작은 모델 사용
export OLLAMA_MODEL=llama3.2:1b
```
