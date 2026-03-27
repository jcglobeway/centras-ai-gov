"use client";

import { useState } from "react";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, DailyMetric, LlmMetrics, UnresolvedQuestion } from "@/lib/types";
import { KpiCard } from "@/components/charts/KpiCard";
import { MetricsLineChart } from "@/components/charts/MetricsLineChart";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Spinner } from "@/components/ui/Spinner";
import { PageFilters, getWeekFrom, getToday } from "@/components/ui/PageFilters";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";

function getKpiStatus(
  value: number | null,
  thresholds: { ok: (v: number) => boolean; warn: (v: number) => boolean }
): "ok" | "warn" | "critical" | undefined {
  if (value == null) return undefined;
  if (thresholds.ok(value)) return "ok";
  if (thresholds.warn(value)) return "warn";
  return "critical";
}

export default function CostHealthPage() {
  const [orgId, setOrgId] = useState("");
  const [from, setFrom] = useState(getWeekFrom);
  const [to, setTo] = useState(getToday);

  const metricsParams = new URLSearchParams({ page_size: "14" });
  if (orgId) metricsParams.set("organization_id", orgId);
  if (from) metricsParams.set("from_date", from);
  if (to) metricsParams.set("to_date", to);

  const llmParams = new URLSearchParams();
  if (orgId) llmParams.set("organization_id", orgId);
  if (from) llmParams.set("from_date", from);
  if (to) llmParams.set("to_date", to);

  const { data: metricsData, isLoading } = useSWR<PagedResponse<DailyMetric>>(
    `/api/admin/metrics/daily?${metricsParams}`,
    fetcher
  );

  const { data: llmData } = useSWR<LlmMetrics>(
    `/api/admin/metrics/llm?${llmParams}`,
    fetcher
  );

  const { data: unresolvedData } = useSWR<PagedResponse<UnresolvedQuestion>>(
    `/api/admin/questions/unresolved?page_size=5`,
    fetcher
  );

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-48">
        <Spinner />
      </div>
    );
  }

  const metrics = metricsData?.items ?? [];
  const latest = metrics[metrics.length - 1];

  const avgCostPerQuery = llmData?.avgCostPerQuery ?? null;
  const zeroResultRate = latest?.zeroResultRate ?? null;
  const avgInputTokens = llmData?.avgInputTokens ?? null;
  const avgOutputTokens = llmData?.avgOutputTokens ?? null;
  const tokenEfficiency =
    avgInputTokens != null && avgOutputTokens != null && avgInputTokens > 0
      ? avgOutputTokens / avgInputTokens
      : null;

  const unresolvedItems = unresolvedData?.items ?? [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-2">
        <h2 className="text-text-primary font-semibold text-lg">비용 & 건강도</h2>
        <PageFilters
          orgId={orgId} onOrgChange={setOrgId}
          from={from} onFromChange={setFrom}
          to={to} onToChange={setTo}
        />
      </div>

      {/* KPI 그리드 */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <KpiCard
          label="COST / QUERY"
          value={avgCostPerQuery != null ? `$${avgCostPerQuery.toFixed(4)}` : "-"}
          status={getKpiStatus(avgCostPerQuery, {
            ok: (v) => v < 0.008,
            warn: (v) => v < 0.012,
          })}
          help="질문 1건당 평균 LLM 비용입니다. $0.008 이하 유지 권장."
        />
        <KpiCard
          label="KNOWLEDGE GAP RATE"
          value={zeroResultRate != null ? zeroResultRate.toFixed(1) + "%" : "-"}
          status={getKpiStatus(zeroResultRate, {
            ok: (v) => v < 8,
            warn: (v) => v < 12,
          })}
          help="검색 결과 없음 비율. 지식 베이스 공백을 의미합니다."
        />
        <KpiCard
          label="AVG INPUT TOKENS"
          value={avgInputTokens != null ? avgInputTokens.toFixed(0) : "-"}
          help="평균 입력 토큰 수 (컨텍스트 + 질문)."
        />
        <KpiCard
          label="TOKEN EFFICIENCY"
          value={tokenEfficiency != null ? tokenEfficiency.toFixed(2) : "-"}
          sub="out/in"
          help="출력 토큰 / 입력 토큰 비율. 값이 높을수록 컨텍스트 대비 응답이 길어짐을 의미합니다."
        />
      </div>

      {/* LLM 비용 요약 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <Card>
          <CardHeader>
            <CardTitle tag="LLM COST">총 비용 요약</CardTitle>
          </CardHeader>
          <div className="px-4 pb-4 space-y-3">
            <div className="grid grid-cols-2 gap-3">
              {[
                { label: "총 비용 (USD)", value: llmData?.totalCostUsd != null ? `$${llmData.totalCostUsd.toFixed(4)}` : "-" },
                { label: "처리 건수", value: llmData?.answerCount != null ? llmData.answerCount.toLocaleString() + " 건" : "-" },
                { label: "평균 입력 토큰", value: avgInputTokens != null ? avgInputTokens.toFixed(0) : "-" },
                { label: "평균 출력 토큰", value: avgOutputTokens != null ? avgOutputTokens.toFixed(0) : "-" },
              ].map((item) => (
                <div key={item.label} className="bg-bg-elevated rounded p-3">
                  <p className="font-mono text-[10px] uppercase tracking-[0.4px] text-text-muted mb-1">{item.label}</p>
                  <p className="font-mono text-[18px] font-bold text-text-primary">{item.value}</p>
                </div>
              ))}
            </div>
            <p className="text-[10px] font-mono text-text-muted">
              * answers 테이블 집계값. V026 컬럼 기준.
            </p>
          </div>
        </Card>

        {/* Knowledge Gap */}
        <Card>
          <CardHeader>
            <CardTitle tag="KNOWLEDGE GAP">미해결 질문 Top 5</CardTitle>
          </CardHeader>
          <div className="overflow-hidden">
            <Table>
              <Thead>
                <Th>질문</Th>
                <Th>상태</Th>
              </Thead>
              <Tbody>
                {unresolvedItems.map((q) => (
                  <Tr key={q.questionId}>
                    <Td className="max-w-[200px] truncate">{q.questionText}</Td>
                    <Td>
                      <Badge variant={q.answerStatus === "no_answer" ? "neutral" : "warning"}>
                        {q.answerStatus === "no_answer" ? "무응답" : "Fallback"}
                      </Badge>
                    </Td>
                  </Tr>
                ))}
                {unresolvedItems.length === 0 && (
                  <Tr>
                    <Td colSpan={2} className="text-center text-text-muted py-6">
                      미해결 질문이 없습니다.
                    </Td>
                  </Tr>
                )}
              </Tbody>
            </Table>
          </div>
        </Card>
      </div>

      {/* 추세 차트 */}
      {metrics.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle tag="TREND">무응답률 추세</CardTitle>
          </CardHeader>
          <MetricsLineChart
            data={metrics}
            metrics={["zeroResultRate", "fallbackRate"]}
          />
        </Card>
      )}

      {/* 문서 건강도 */}
      <Card>
        <CardHeader>
          <CardTitle tag="DOC HEALTH">문서 건강도</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4">
          <div className="grid grid-cols-2 lg:grid-cols-3 gap-3">
            {[
              { label: "Stale Doc Rate", value: "—", note: "90일 이상 미갱신 문서 비율. 계산 예정." },
              { label: "중복 문서율", value: "—", note: "의미 유사 문서 비율 (OTel 연동 후)." },
              { label: "Cache Hit Rate", value: "—", note: "쿼리 캐시 적중률 (미추적)." },
            ].map((item) => (
              <div key={item.label} className="bg-bg-elevated rounded p-3">
                <p className="font-mono text-[10px] uppercase tracking-[0.4px] text-text-muted mb-1">{item.label}</p>
                <p className="font-mono text-[18px] font-bold text-text-primary mb-1">{item.value}</p>
                <p className="text-[10px] text-text-muted">{item.note}</p>
              </div>
            ))}
          </div>
        </div>
      </Card>
    </div>
  );
}
