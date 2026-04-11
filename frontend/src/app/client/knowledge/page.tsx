"use client";

import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, Document, IndexStatus } from "@/lib/types";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";
import { Spinner } from "@/components/ui/Spinner";
import { useFilter } from "@/lib/filter-context";
import type { ComponentProps } from "react";

type BadgeVariant = ComponentProps<typeof Badge>["variant"];

const INDEX_LABEL: Record<IndexStatus, string> = {
  indexed: "인덱싱 완료",
  not_indexed: "미인덱싱",
  outdated: "갱신 필요",
};

const INDEX_VARIANT: Record<IndexStatus, BadgeVariant> = {
  indexed: "success",
  not_indexed: "neutral",
  outdated: "warning",
};

export default function KnowledgePage() {
  const { orgId, from, to } = useFilter();

  const params = new URLSearchParams({ page_size: "20" });
  if (orgId) params.set("organization_id", orgId);
  if (from) params.set("from_date", from);
  if (to) params.set("to_date", to);

  const { data, error, isLoading } = useSWR<PagedResponse<Document>>(
    `/api/admin/documents?${params}`,
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

  const docs = data?.items ?? [];

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-4">
        <h2 className="text-text-primary font-semibold text-lg">지식 현황</h2>
        <span className="text-text-muted text-xs">총 {data?.total ?? 0}건</span>
      </div>

      <div className="bg-bg-surface border border-bg-border rounded-xl overflow-hidden">
        <Table>
          <Thead>
            <Th>문서 ID</Th>
            <Th>제목</Th>
            <Th>인덱스 상태</Th>
            <Th>문서 유형</Th>
            <Th>마지막 인덱싱</Th>
            <Th>생성일</Th>
          </Thead>
          <Tbody>
            {docs.map((doc) => (
              <Tr key={doc.id}>
                <Td className="font-mono text-xs text-text-muted">{doc.id}</Td>
                <Td className="font-medium max-w-xs truncate">{doc.title}</Td>
                <Td>
                  <Badge variant={INDEX_VARIANT[doc.indexStatus]}>
                    {INDEX_LABEL[doc.indexStatus]}
                  </Badge>
                </Td>
                <Td className="text-xs text-text-secondary">{doc.documentType}</Td>
                <Td className="text-xs text-text-muted">
                  {doc.lastIndexedAt
                    ? new Date(doc.lastIndexedAt).toLocaleDateString("ko-KR")
                    : "-"}
                </Td>
                <Td className="text-xs text-text-muted">
                  {new Date(doc.createdAt).toLocaleDateString("ko-KR")}
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
        {docs.length === 0 && (
          <p className="text-center text-text-muted text-sm py-8">문서가 없습니다.</p>
        )}
      </div>
    </div>
  );
}
