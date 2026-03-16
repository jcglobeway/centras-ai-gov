# Proposal: refactor-test-structure

## 목적
887줄 단일 테스트 파일을 논리적 단위로 분리하여 빌드 속도 개선 및 유지보수성 향상

## 배경
`AdminApiApplicationTests.kt`에 39개 테스트가 집중되어 있고,
`@DirtiesContext(AFTER_EACH_TEST_METHOD)`로 테스트마다 Spring Context를 재시작함.

## 변경 후 파일 구조
```
auth/AuthApiTests.kt          # 8개 테스트
ingestion/IngestionApiTests.kt # 11개 테스트
qareview/QAReviewApiTests.kt   # 5개 테스트
chatruntime/ChatRuntimeApiTests.kt # 5개 테스트
e2e/FullFlowE2ETests.kt        # 4개 테스트
BaseApiTest.kt                 # 공통 헬퍼
```

## @DirtiesContext 전략
- 클래스 레벨 `AFTER_CLASS`로 변경 (클래스당 1회 재시작)
- 헬퍼 함수는 `BaseApiTest`로 추출
