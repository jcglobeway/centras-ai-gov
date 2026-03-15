# Status

## State

completed

## Progress

- change 생성 완료
- `POST /admin/auth/login`, `POST /admin/auth/logout` 추가
- `GET /admin/auth/me`가 명시적 세션 ID의 만료/폐기를 `401`로 반환하도록 정리
- 개발용 인메모리 세션 저장소에 발급/폐기/만료 데이터와 자격 증명 검증 경계 추가
- `.\gradlew.bat test` 통과

## Risks

- 현재는 개발용 인메모리 저장소라 프로세스 재시작 시 세션이 유지되지 않는다.
- 감사로그는 이번 change 범위에서 제외된다.
- 비밀번호 해시와 세션 토큰 해시는 실제 저장소 구현 change에서 대체해야 한다.
