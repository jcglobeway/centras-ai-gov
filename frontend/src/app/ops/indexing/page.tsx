"use client";

import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, IngestionJob, IngestionJobStatus, CrawlSource } from "@/lib/types";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";
import { KpiCard } from "@/components/charts/KpiCard";
import { Spinner } from "@/components/ui/Spinner";
import { Fragment, useState, type ComponentProps } from "react";
import { useFilter } from "@/lib/filter-context";

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

const STAGE_LABEL: Record<string, string> = {
  fetch: "수집",
  extract: "추출",
  normalize: "정규화",
  chunk: "청킹",
  embed: "임베딩",
  index: "인덱싱",
  complete: "완료",
};

const TRIGGER_LABEL: Record<string, string> = {
  manual: "수동",
  scheduled: "스케줄",
  file_upload: "파일업로드",
};

const STAGE_PROGRESS: Record<string, number> = {
  fetch: 10,
  extract: 25,
  normalize: 35,
  chunk: 55,
  embed: 75,
  index: 90,
  complete: 100,
};

export default function IndexingPage() {
  const [expandedJobId, setExpandedJobId] = useState<string | null>(null);
  const { orgId, serviceId, from, to } = useFilter();

  const params = new URLSearchParams({ page_size: "20" });
  if (orgId) params.set("organization_id", orgId);
  if (serviceId) params.set("service_id", serviceId);
  if (from) params.set("from_date", from);
  if (to) params.set("to_date", to);

  const { data, error, isLoading } = useSWR<PagedResponse<IngestionJob>>(
    `/api/admin/ingestion-jobs?${params}`,
    fetcher
  );
  const { data: sourceData } = useSWR<{ items: CrawlSource[]; total: number }>(
    orgId ? `/api/admin/crawl-sources?organization_id=${orgId}` : null,
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
  const sourceItems = sourceData?.items ?? [];
  const crawlSources = serviceId
    ? sourceItems.filter((s) => s.serviceId === serviceId)
    : sourceItems;
  const sourceMap = new Map(crawlSources.map((s) => [s.id, s]));
  const succeeded = jobs.filter((j) => j.status === "succeeded").length;
  const failed = jobs.filter((j) => j.status === "failed").length;
  const running = jobs.filter((j) => j.status === "running").length;

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-4">
        <h2 className="text-text-primary font-semibold text-lg">RAG 인덱싱 현황</h2>
        <span className="text-text-muted text-xs">총 {data?.total ?? 0}건</span>
      </div>

      <div className="grid grid-cols-3 gap-4">
        <KpiCard label="완료" value={succeeded} sub="건" status="ok" />
        <KpiCard label="실패" value={failed} sub="건" status={failed > 0 ? "critical" : undefined} />
        <KpiCard label="실행 중" value={running} sub="건" />
      </div>

      <div className="bg-bg-surface border border-bg-border rounded-xl overflow-hidden">
        <Table>
          <Thead>
            <Th>컬렉션 / 소스</Th>
            <Th>상태 / 단계</Th>
            <Th>진행률</Th>
            <Th>상태</Th>
            <Th>트리거</Th>
            <Th>요청일시</Th>
            <Th>완료일시</Th>
            <Th>상세</Th>
          </Thead>
          <Tbody>
            {jobs.map((job) => {
              const source = sourceMap.get(job.crawlSourceId);
              const isExpanded = expandedJobId === job.id;
              return (
                <Fragment key={job.id}>
                  <Tr key={job.id}>
                    <Td>
                      <div className="flex flex-col gap-1">
                        {source?.collectionName && (
                          <span className="inline-flex w-fit items-center rounded border border-accent/30 bg-accent/10 px-1.5 py-0.5 text-[10px] text-accent">
                            {source.collectionName}
                          </span>
                        )}
                        <span className="text-xs text-text-primary" title={source?.name ?? job.crawlSourceId}>
                          {source?.name ?? "소스 정보 없음"}
                        </span>
                        <span className="font-mono text-[10px] text-text-muted">{job.id}</span>
                      </div>
                    </Td>
                    <Td>
                      <div className="flex flex-col gap-1">
                        <Badge variant={STATUS_VARIANT[job.status]}>
                          {STATUS_LABEL[job.status]}
                        </Badge>
                        <span className="text-[11px] text-text-secondary">
                          {STAGE_LABEL[job.jobStage] ?? job.jobStage}
                        </span>
                      </div>
                    </Td>
                    <Td>
                      <div className="w-28">
                        <div className="h-1.5 rounded bg-bg-border">
                          <div
                            className="h-1.5 rounded bg-accent"
                            style={{ width: `${STAGE_PROGRESS[job.jobStage] ?? 0}%` }}
                          />
                        </div>
                        <p className="mt-1 text-[10px] text-text-muted">{STAGE_PROGRESS[job.jobStage] ?? 0}%</p>
                      </div>
                    </Td>
                    <Td>
                      <Badge variant={STATUS_VARIANT[job.status]}>
                        {STATUS_LABEL[job.status]}
                      </Badge>
                    </Td>
                    <Td className="text-xs text-text-secondary">{TRIGGER_LABEL[job.triggerType] ?? job.triggerType}</Td>
                    <Td className="text-xs text-text-muted">
                      {new Date(job.requestedAt).toLocaleString("ko-KR")}
                    </Td>
                    <Td className="text-xs text-text-muted">
                      {job.finishedAt
                        ? new Date(job.finishedAt).toLocaleString("ko-KR")
                        : "-"}
                    </Td>
                    <Td>
                      <button
                        onClick={() => setExpandedJobId(isExpanded ? null : job.id)}
                        className="text-xs text-accent hover:underline"
                      >
                        {isExpanded ? "접기" : "상세"}
                      </button>
                    </Td>
                  </Tr>
                  {isExpanded && (
                    <Tr key={`${job.id}-detail`}>
                      <Td colSpan={8}>
                        <div className="grid grid-cols-2 gap-x-6 gap-y-2 rounded-lg bg-bg-base/40 p-3 text-xs">
                          <span className="text-text-muted">문서 ID</span>
                          <span className="font-mono text-text-secondary">{job.documentId ?? "-"}</span>
                          <span className="text-text-muted">시도 횟수</span>
                          <span className="text-text-secondary">{job.attemptCount}</span>
                          <span className="text-text-muted">에러 코드</span>
                          <span className="font-mono text-text-secondary">{job.errorCode ?? "-"}</span>
                          <span className="text-text-muted">러너</span>
                          <span className="text-text-secondary">{job.runnerType}</span>
                          <span className="text-text-muted">크롤 소스 ID</span>
                          <span className="font-mono text-text-secondary">{job.crawlSourceId}</span>
                          <span className="text-text-muted">소스 URI</span>
                          <span className="font-mono text-text-secondary break-all">{source?.sourceUri ?? "-"}</span>
                        </div>
                      </Td>
                    </Tr>
                  )}
                </Fragment>
              );
            })}
          </Tbody>
        </Table>
        {jobs.length === 0 && (
          <p className="text-center text-text-muted text-sm py-8">인제스션 잡이 없습니다.</p>
        )}
      </div>
    </div>
  );
}
