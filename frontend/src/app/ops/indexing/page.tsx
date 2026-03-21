"use client";

import { useState } from "react";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, IngestionJob, IngestionJobStatus } from "@/lib/types";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";
import { KpiCard } from "@/components/charts/KpiCard";
import { Spinner } from "@/components/ui/Spinner";
import { PageFilters, getWeekFrom, getToday } from "@/components/ui/PageFilters";
import type { ComponentProps } from "react";

type BadgeVariant = ComponentProps<typeof Badge>["variant"];

const STATUS_LABEL: Record<IngestionJobStatus, string> = {
  queued: "큐",
  running: "실행 중",
  succeeded: "완료",
  partial_success: "부분완료",
  failed: "실패",
  cancelled: "취소",
};

const STATUS_VARIANT: Record<IngestionJobStatus, BadgeVariant> = {
  queued: "neutral",
  running: "info",
  succeeded: "success",
  partial_success: "warning",
  failed: "error",
  cancelled: "neutral",
};

export default function IndexingPage() {
  const [orgId, setOrgId] = useState("");
  const [from, setFrom] = useState(getWeekFrom);
  const [to, setTo] = useState(getToday);

  const params = new URLSearchParams({ page_size: "20" });
  if (orgId) params.set("organization_id", orgId);
  if (from) params.set("from_date", from);
  if (to) params.set("to_date", to);

  const { data, error, isLoading } = useSWR<PagedResponse<IngestionJob>>(
    `/api/admin/ingestion-jobs?${params}`,
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

  const jobs = data?.items ?? [];
  const succeeded = jobs.filter((j) => j.status === "succeeded").length;
  const failed = jobs.filter((j) => j.status === "failed").length;
  const running = jobs.filter((j) => j.status === "running").length;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between flex-wrap gap-2">
        <div className="flex items-center gap-4">
          <h2 className="text-text-primary font-semibold text-lg">RAG 인덱싱 현황</h2>
          <span className="text-text-muted text-xs">총 {data?.total ?? 0}건</span>
        </div>
        <PageFilters
          orgId={orgId} onOrgChange={setOrgId}
          from={from} onFromChange={setFrom}
          to={to} onToChange={setTo}
        />
      </div>

      <div className="grid grid-cols-3 gap-4">
        <KpiCard label="완료" value={succeeded} sub="건" status="ok" />
        <KpiCard label="실패" value={failed} sub="건" status={failed > 0 ? "critical" : undefined} />
        <KpiCard label="실행 중" value={running} sub="건" />
      </div>

      <div className="bg-bg-surface border border-bg-border rounded-xl overflow-hidden">
        <Table>
          <Thead>
            <Th>잡 ID</Th>
            <Th>크롤 소스 ID</Th>
            <Th>상태</Th>
            <Th>트리거</Th>
            <Th>시작일시</Th>
            <Th>완료일시</Th>
          </Thead>
          <Tbody>
            {jobs.map((job) => (
              <Tr key={job.id}>
                <Td className="font-mono text-xs text-text-muted">{job.id}</Td>
                <Td className="font-mono text-xs">{job.crawlSourceId}</Td>
                <Td>
                  <Badge variant={STATUS_VARIANT[job.status]}>
                    {STATUS_LABEL[job.status]}
                  </Badge>
                </Td>
                <Td className="text-xs text-text-secondary">{job.triggerType}</Td>
                <Td className="text-xs text-text-muted">
                  {job.startedAt
                    ? new Date(job.startedAt).toLocaleString("ko-KR")
                    : "-"}
                </Td>
                <Td className="text-xs text-text-muted">
                  {job.finishedAt
                    ? new Date(job.finishedAt).toLocaleString("ko-KR")
                    : "-"}
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
        {jobs.length === 0 && (
          <p className="text-center text-text-muted text-sm py-8">인제스션 잡이 없습니다.</p>
        )}
      </div>
    </div>
  );
}
