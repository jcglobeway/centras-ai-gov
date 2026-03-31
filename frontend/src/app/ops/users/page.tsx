"use client";

import useSWR from "swr";
import { fetcher } from "@/lib/api";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { PageGuide } from "@/components/ui/PageGuide";
import type { ComponentProps } from "react";

type BadgeVariant = ComponentProps<typeof Badge>["variant"];

interface AdminUserItem {
  id: string;
  email: string;
  displayName: string;
  status: string;
  lastLoginAt: string | null;
}

interface AdminUserListResponse {
  items: AdminUserItem[];
  total: number;
}

const ROLE_VARIANT: Record<string, BadgeVariant> = {
  super_admin:      "error",
  ops_admin:        "warning",
  qa_manager:       "neutral",
  client_org_admin: "success",
  client_viewer:    "neutral",
};

const MOCK_MENU_PERMISSIONS = [
  { menu: "통합 관제 (/ops)",    allowed: true },
  { menu: "서비스 통계",          allowed: true },
  { menu: "품질 모니터링",        allowed: true },
  { menu: "이상 징후 감지",       allowed: false },
  { menu: "비용 분석",           allowed: false },
  { menu: "사용자/권한 관리",     allowed: false },
];

export default function UsersPage() {
  const { data, isLoading } = useSWR<AdminUserListResponse>(
    `/api/admin/users`,
    fetcher,
  );

  return (
    <div className="space-y-6">
      <PageGuide
        description="관리자 계정 생성·삭제와 역할별 메뉴 접근 권한(RBAC)을 설정하는 화면입니다."
        tips={[
          "Customer 역할은 메뉴별로 접근 권한(O/△/X)을 세밀하게 조정할 수 있습니다.",
          "퇴직자 계정은 즉시 비활성화하고, 정기적으로 휴면 계정을 점검하세요.",
          "권한 변경 이력은 보안 감사 로그에 자동으로 기록됩니다.",
        ]}
      />
      <div className="flex items-center gap-3">
        <h2 className="text-text-primary font-semibold text-lg">사용자/권한 관리</h2>
        <Badge variant="neutral">Admin 전용</Badge>
      </div>

      {/* 계정 목록 */}
      <Card>
        <CardHeader>
          <CardTitle tag="ACCOUNTS">계정 목록</CardTitle>
        </CardHeader>
        {isLoading ? (
          <div className="flex items-center justify-center h-24">
            <Spinner />
          </div>
        ) : (
          <div className="overflow-hidden">
            <Table>
              <Thead>
                <Th>이름</Th>
                <Th>이메일</Th>
                <Th>상태</Th>
                <Th>마지막 로그인</Th>
                <Th></Th>
              </Thead>
              <Tbody>
                {(data?.items ?? []).map((u) => (
                  <Tr key={u.id}>
                    <Td className="text-sm font-medium">{u.displayName}</Td>
                    <Td className="text-sm text-text-muted">{u.email}</Td>
                    <Td>
                      <Badge variant={u.status === "active" ? "success" : "neutral"}>
                        {u.status === "active" ? "활성" : u.status === "invited" ? "초대됨" : "정지"}
                      </Badge>
                    </Td>
                    <Td className="text-xs text-text-muted">
                      {u.lastLoginAt
                        ? new Date(u.lastLoginAt).toLocaleString("ko-KR")
                        : "-"}
                    </Td>
                    <Td>
                      <div className="flex gap-2">
                        <Button variant="secondary" size="sm" disabled>편집</Button>
                        <Button variant="secondary" size="sm" disabled>삭제</Button>
                      </div>
                    </Td>
                  </Tr>
                ))}
                {(data?.items ?? []).length === 0 && !isLoading && (
                  <Tr>
                    <Td colSpan={5} className="text-center text-text-muted text-sm py-8">
                      등록된 계정이 없습니다.
                    </Td>
                  </Tr>
                )}
              </Tbody>
            </Table>
          </div>
        )}
        <div className="px-4 pb-4 pt-3">
          <Button disabled>계정 추가</Button>
        </div>
      </Card>

      {/* RBAC 설정 — 목업 유지 (저장 API 미구현) */}
      <Card>
        <CardHeader>
          <CardTitle tag="RBAC">RBAC 설정 — Customer 역할 메뉴 접근 권한</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4">
          <p className="text-xs text-text-muted mb-3">
            Customer 역할(client_org_admin / client_viewer)의 메뉴 접근 허용 설정
          </p>
          <div className="overflow-hidden rounded-lg border border-bg-border">
            <Table>
              <Thead>
                <Th>메뉴명</Th>
                <Th>접근 허용</Th>
              </Thead>
              <Tbody>
                {MOCK_MENU_PERMISSIONS.map((p) => (
                  <Tr key={p.menu}>
                    <Td className="text-sm">{p.menu}</Td>
                    <Td>
                      <div
                        className={`inline-flex items-center w-10 h-5 rounded-full transition-colors ${
                          p.allowed ? "bg-success/30" : "bg-bg-border"
                        } cursor-not-allowed`}
                      >
                        <div
                          className={`w-4 h-4 rounded-full transition-transform mx-0.5 ${
                            p.allowed ? "bg-success translate-x-5" : "bg-text-muted translate-x-0"
                          }`}
                        />
                      </div>
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          </div>
          <p className="text-[10px] text-text-muted mt-2">※ 샘플 데이터 — RBAC 저장 API 연동 예정</p>
        </div>
      </Card>
    </div>
  );
}
