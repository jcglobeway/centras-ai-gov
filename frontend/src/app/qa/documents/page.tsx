"use client";

import { useState } from "react";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, Document, DocumentVersion, IndexStatus } from "@/lib/types";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
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

export default function QaDocumentsPage() {
  const { orgId, from, to } = useFilter();

  const [versionsDocId, setVersionsDocId]   = useState<string | null>(null);
  const [versionsTitle, setVersionsTitle]   = useState("");

  const params = new URLSearchParams({ page_size: "20" });
  if (orgId) params.set("organization_id", orgId);
  if (from)  params.set("from_date", from);
  if (to)    params.set("to_date", to);

  const { data, error, isLoading } = useSWR<PagedResponse<Document>>(
    `/api/admin/documents?${params}`,
    fetcher
  );

  const { data: versionsData, isLoading: versionsLoading } =
    useSWR<{ items: DocumentVersion[]; total: number }>(
      versionsDocId ? `/api/admin/documents/${versionsDocId}/versions` : null,
      fetcher
    );

  function openVersions(doc: Document) {
    setVersionsDocId(doc.id);
    setVersionsTitle(doc.title);
  }

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
        <h2 className="text-text-primary font-semibold text-lg">문서 관리</h2>
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
            <Th></Th>
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
                <Td>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => openVersions(doc)}
                  >
                    버전 이력
                  </Button>
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
        {docs.length === 0 && (
          <p className="text-center text-text-muted text-sm py-8">문서가 없습니다.</p>
        )}
      </div>

      {/* 버전 이력 모달 */}
      {versionsDocId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
          <div className="bg-bg-elevated border border-bg-border rounded-xl shadow-2xl w-full max-w-lg mx-4">
            <div className="flex items-center justify-between px-5 py-4 border-b border-bg-border">
              <div>
                <h3 className="text-text-primary font-semibold text-sm">버전 이력</h3>
                <p className="text-text-muted text-xs mt-0.5 truncate max-w-xs">{versionsTitle}</p>
              </div>
              <button
                onClick={() => setVersionsDocId(null)}
                className="text-text-muted hover:text-text-primary transition-colors"
              >
                <span className="material-symbols-outlined text-[20px]">close</span>
              </button>
            </div>

            <div className="px-5 py-4">
              {versionsLoading ? (
                <div className="flex justify-center py-6">
                  <Spinner />
                </div>
              ) : (versionsData?.items ?? []).length === 0 ? (
                <p className="text-center text-text-muted text-sm py-6">버전 이력이 없습니다.</p>
              ) : (
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-bg-border">
                      <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted w-28">버전</th>
                      <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted w-24">변경 여부</th>
                      <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted">생성일</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-bg-border">
                    {(versionsData?.items ?? []).map((v) => (
                      <tr key={v.id}>
                        <td className="py-2.5 font-mono text-xs text-accent">{v.versionLabel}</td>
                        <td className="py-2.5">
                          <span className={`text-xs font-mono ${v.changeDetected ? "text-warning" : "text-text-muted"}`}>
                            {v.changeDetected ? "변경 있음" : "변경 없음"}
                          </span>
                        </td>
                        <td className="py-2.5 text-xs text-text-muted">
                          {new Date(v.createdAt).toLocaleDateString("ko-KR")}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>

            <div className="flex justify-end px-5 py-4 border-t border-bg-border">
              <Button variant="ghost" size="sm" onClick={() => setVersionsDocId(null)}>
                닫기
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
