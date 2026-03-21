"use client";

import { useState } from "react";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, DailyMetric, UnresolvedQuestion } from "@/lib/types";
import { KpiCard } from "@/components/charts/KpiCard";
import { MetricsLineChart } from "@/components/charts/MetricsLineChart";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { ProgressBar } from "@/components/ui/ProgressBar";
import { Spinner } from "@/components/ui/Spinner";
import { PageFilters, getWeekFrom, getToday } from "@/components/ui/PageFilters";

const RESPONSE_RATE_TARGET_MS = 80 * 10; // 80% 목표를 ms 단위로 표현 (ProgressBar 재사용)

export default function ClientDashboardPage() {
  const [orgId, setOrgId] = useState("");
  const [from, setFrom] = useState(getWeekFrom);
  const [to, setTo] = useState(getToday);

  const params = new URLSearchParams({ page_size: "14" });
  if (orgId) params.set("organization_id", orgId);
  if (from) params.set("from_date", from);
  if (to) params.set("to_date", to);

  const { data, error, isLoading } = useSWR<PagedResponse<DailyMetric>>(
    `/api/admin/metrics/daily?${params}`,
    fetcher
  );

  const { data: unresolvedData } = useSWR<PagedResponse<UnresolvedQuestion>>(
    `/api/admin/questions/unresolved?page_size=1`,
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

  const metrics = data?.items ?? [];
  const latest = metrics[metrics.length - 1];

  const resolvedRateVal = latest?.resolvedRate ?? null;
  const fallbackRateVal = latest?.fallbackRate ?? null;
  const avgResponseMsVal = latest?.avgResponseTimeMs ?? null;
  const unresolvedCount = unresolvedData?.total ?? null;

  const resolvedRate = resolvedRateVal != null ? resolvedRateVal.toFixed(1) + "%" : "-";
  const fallbackRate = fallbackRateVal != null ? fallbackRateVal.toFixed(1) + "%" : "-";
  const avgResponseMs = avgResponseMsVal != null
    ? avgResponseMsVal.toLocaleString() + "ms"
    : "-";

  // ProgressBar 재사용: resolvedRate을 ms 단위로 변환 (목표 80% → 800 → maxMs 1000)
  const progressValueMs = resolvedRateVal != null ? Math.round(resolvedRateVal * 10) : 0;
  const progressMaxMs = 1000; // 100% → 1000

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-2">
        <h2 className="text-text-primary font-semibold text-lg">기관 대시보드</h2>
        <PageFilters
          orgId={orgId} onOrgChange={setOrgId}
          from={from} onFromChange={setFrom}
          to={to} onToChange={setTo}
        />
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-5 gap-4">
        <KpiCard label="응답률" value={resolvedRate} trend="up" trendValue="목표 > 80%" help="시민 질문 중 챗봇이 정상 답변을 제공한 비율입니다." />
        <KpiCard label="Fallback율" value={fallbackRate} trend="down" trendValue="목표 < 15%" help="지식베이스의 답변 신뢰도가 낮아 일반 안내로 대체된 비율입니다. 낮을수록 서비스 품질이 높습니다." />
        <KpiCard label="평균 응답시간" value={avgResponseMs} help="챗봇이 답변을 생성하는 데 걸린 평균 시간입니다." />
        <KpiCard label="총 질문 수" value={latest?.totalQuestions ?? "-"} sub="건 (최신일)" help="최신 집계일 기준으로 챗봇에 입력된 전체 질문 건수입니다." />
        <KpiCard
          label="미해결 건수"
          value={unresolvedCount ?? "-"}
          sub="건"
          status={
            unresolvedCount == null ? undefined
            : unresolvedCount === 0 ? "ok"
            : unresolvedCount < 10 ? "warn"
            : "critical"
          }
          help="QA 검수가 필요한 미해결 질문 건수입니다."
        />
      </div>

      {/* 응답률 목표 진행 바 */}
      {resolvedRateVal != null && (
        <Card>
          <CardHeader>
            <CardTitle>응답률 목표 달성률</CardTitle>
          </CardHeader>
          <div className="px-4 pb-4">
            <ProgressBar
              label="응답률"
              valueMs={progressValueMs}
              maxMs={progressMaxMs}
              color={resolvedRateVal >= 80 ? "bg-success" : resolvedRateVal >= 60 ? "bg-warning" : "bg-error"}
            />
            <p className="text-xs text-text-muted mt-2">목표: 80% | 현재: {resolvedRate}</p>
          </div>
        </Card>
      )}

      {metrics.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>응답률 / Fallback 추세</CardTitle>
          </CardHeader>
          <MetricsLineChart
            data={metrics}
            metrics={["resolvedRate", "fallbackRate"]}
          />
        </Card>
      )}
    </div>
  );
}
