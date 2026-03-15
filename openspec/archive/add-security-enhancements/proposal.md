# Proposal

## Change ID

`add-security-enhancements`

## Summary

### 변경 목적
- Production 운영을 위한 보안 강화
- 비밀번호 bcrypt 해싱 (현재 개발용 평문)
- Rate limiting으로 API 남용 방지
- CORS 설정으로 허용된 origin만 접근

### 변경 범위
1. 비밀번호 bcrypt 해싱
   - DevelopmentAdminCredentialAuthenticator에 bcrypt 적용
   - Migration에서 seed 비밀번호 해싱
2. Rate limiting
   - Spring Security + Bucket4j (또는 간단한 Filter)
   - API endpoint별 rate limit 설정
3. CORS 설정
   - WebMvcConfigurer
   - 허용 origin, method, header 설정

### 제외 범위
- JWT token (현재 session 기반 유지)
- OAuth2 외부 인증
- API key 인증
- HTTPS 설정 (인프라 레벨)

## Done Definition

- [ ] bcrypt 의존성 추가
- [ ] 비밀번호 해싱 구현
- [ ] CORS 설정
- [ ] Rate limiting (간단한 구현)
- [ ] 테스트 통과
