"use client";

import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, Organization } from "@/lib/types";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";
import { Spinner } from "@/components/ui/Spinner";

export default function OrganizationsPage() {
  const { data, error, isLoading } = useSWR<PagedResponse<Organization>>(
    "/api/admin/organizations",
    fetcher
  );

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-48">
        <Spinner />
      </div>
    );
  }

  if (error) {
    return <p className="text-error text-sm">데이터를 불러오지 못했습니다.</p>;
  }

  const orgs = data?.items ?? [];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-text-primary font-semibold text-lg">기관 관리</h2>
        <span className="text-text-muted text-xs">총 {data?.total ?? 0}개</span>
      </div>

      <div className="bg-bg-surface border border-bg-border rounded-xl overflow-hidden">
        <Table>
          <Thead>
            <Th>기관 ID</Th>
            <Th>이름</Th>
            <Th>코드</Th>
            <Th>상태</Th>
            <Th>생성일</Th>
          </Thead>
          <Tbody>
            {orgs.map((org) => (
              <Tr key={org.organizationId}>
                <Td className="font-mono text-xs text-text-muted">{org.organizationId}</Td>
                <Td className="font-medium">{org.name}</Td>
                <Td className="font-mono text-xs">{org.code}</Td>
                <Td>
                  <Badge variant={org.status === "active" ? "success" : "neutral"}>
                    {org.status === "active" ? "활성" : "비활성"}
                  </Badge>
                </Td>
                <Td className="text-text-muted text-xs">
                  {new Date(org.createdAt).toLocaleDateString("ko-KR")}
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
        {orgs.length === 0 && (
          <p className="text-center text-text-muted text-sm py-8">기관이 없습니다.</p>
        )}
      </div>
    </div>
  );
}
