# Tasks

## 사전 확인
- [ ] `qa_reviews.root_cause_code` 현재 타입 및 시드 값 확인
- [ ] `questions.failure_reason_code` 컬럼 존재 여부 확인 (`extend-question-model` 선행 필요)
- [ ] `shared-kernel` 모듈 구조 확인 (enum 위치 결정)

## 구현
- [ ] `FailureReasonCode` enum 작성 (A01~A10, 코드 + 설명)
  - 위치: `shared-kernel` 또는 각 모듈 domain 패키지
- [ ] `question` 생성/수정 UseCase에 `failure_reason_code` 검증 추가
- [ ] `qa-review` 생성 UseCase에 `root_cause_code` 표준 코드 허용 검증 추가
- [ ] 잘못된 코드 입력 시 400 응답 + 허용 코드 목록 반환

## 테스트
- [ ] 유효한 코드 A01~A10 입력 → 정상 처리 확인
- [ ] 잘못된 코드 입력 → 400 응답 확인
- [ ] `./gradlew test` 전체 통과 확인

## 완료
- [ ] `status.md` 업데이트
- [ ] 커밋
