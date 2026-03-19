# Status

- 상태: `planned`
- 시작일: `2026-03-19`
- 마지막 업데이트: `2026-03-19`

## Progress

- proposal, tasks, status 작성 완료
- 사용자 승인 대기 중

## Verification

- 미실행

## Risks

- `naming-structure-cleanup` 미완료 시 패키지 경로 기준 불일치 발생 → 선행 완료 후 구현
- `spring-boot-starter-webflux` 추가 시 MVC 자동 설정 충돌 가능 → `spring.main.web-application-type: servlet` 명시로 해결
- RAGAS BackgroundTask는 best-effort (실패 시 무시) → 평가 누락 발생 가능, 추후 재시도 큐 도입 고려
- H2에서 `DECIMAL(4,3)` 타입 호환 확인 필요
- `ragas>=0.1.0` 패키지가 Ollama LLM과의 연동 시 버전별 API 차이 존재 가능
- Context Recall은 `web2rag-poc` GT 파이프라인 완료 전까지 미활성 — eval-runner에 플래그로 분기 처리
