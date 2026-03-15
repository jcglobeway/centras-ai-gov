# Change Proposal

## Change ID

`add-auth-session-lifecycle`

## Purpose

개발용 스텁 중심이던 관리자 인증 흐름을 `로그인 -> 세션 발급 -> 세션 복원 -> 로그아웃 -> 세션 폐기` 단위로 끌어올린다.

## Scope

- `POST /admin/auth/login` 추가
- `POST /admin/auth/logout` 추가
- 세션 만료/폐기 판단을 포함한 저장소 경계 정리
- `GET /admin/auth/me`의 명시적 세션 ID 복원 시 오류 처리 추가
- MockMvc 테스트와 작업 로그 갱신

## Out Of Scope

- 실제 DB 기반 `admin_sessions` 영속화
- 비밀번호 해시/IdP/SSO 연동
- 감사로그 적재 구현

## Expected Result

- `admin-api`가 세션 라이프사이클을 API 레벨에서 다룰 수 있다.
- 명시적 세션 ID가 만료/폐기됐을 때 개발용 디버그 스텁으로 폴백하지 않는다.
- 다음 change에서 DB 저장소나 권한 미들웨어로 자연스럽게 교체할 수 있다.
