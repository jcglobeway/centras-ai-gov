# Status

- 상태: `completed`
- 시작일: `2026-03-19`
- 마지막 업데이트: `2026-03-19`

## Progress

- P1: OrganizationRepository·ServiceRepository 인터페이스·어댑터·Bean 등록 제거 완료
- P2: 날짜 필터링 → ListQuestionsService 이동, Controller에서 제거 완료
- P3: OrganizationScope 도입, GetOrganizationsUseCase.listOrganizations(scope) 통일 완료
- P4: AdminAuthUseCase 인터페이스 생성, AuthCommandController 의존성 교체 완료
- P5: Commands는 port.out에서도 참조되므로 이동 생략 (domain 위치 유지가 적절)

## Verification

- ./gradlew compileKotlin → BUILD SUCCESSFUL
- ./gradlew test → BUILD SUCCESSFUL (44 tests)

## Risks

- 없음
