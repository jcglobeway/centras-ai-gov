package com.publicplatform.ragops.identityaccess

/**
 * 관리자 인증·인가를 담당하는 바운디드 컨텍스트.
 *
 * 로그인, 세션 발급/복원/만료, 역할 기반 권한 확인, 감사 로그 기록을 처리한다.
 * 세션 스냅샷(snapshot_json)을 PostgreSQL에 JSON으로 저장하여 stateless 복원을 지원한다.
 *
 * ## 주요 포트 계약
 * - [ManageAdminUserPort]: 관리자 사용자 조회 (이메일, 비밀번호 검증)
 * - [ManageAdminSessionPort]: 세션 발급, 복원, 취소
 * - [RecordAuditLogPort]: 고위험 액션 감사 로그 저장
 *
 * ## 역할 모델
 * - `ops_admin`: 전체 기관 접근
 * - `client_admin`: 소속 기관만 접근
 * - `qa_admin`: 할당된 기관 범위 접근
 *
 * ## 주요 DB 테이블
 * - `admin_users`, `admin_user_roles`, `admin_sessions`, `audit_logs`
 *
 * ## 의존 관계
 * - 다른 모듈에 의존하지 않음 (최하위 레이어)
 * - organization-directory 모듈이 조직 존재 여부 검증 시 참조
 */
class IdentityAccessModule
