"use client";

import { useState } from "react";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, DailyMetric, RagasEvaluation } from "@/lib/types";
import { KpiCard } from "@/components/charts/KpiCard";
import { MetricsLineChart } from "@/components/charts/MetricsLineChart";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Spinner } from "@/components/ui/Spinner";
import { PageFilters, getWeekFrom, getToday } from "@/components/ui/PageFilters";
import { ScoreTable } from "@/components/ui/ScoreTable";

export default function QualityPage() {
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

  const { data: ragasData } = useSWR<PagedResponse<RagasEvaluation>>(
    `/api/admin/ragas-evaluations?page_size=1`,
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
  const latestRagas = ragasData?.items?.[0] ?? null;

  const fallbackRate = latest?.fallbackRate != null
    ? latest.fallbackRate.toFixed(1) + "%"
    : "-";
  const zeroResultRate = latest?.zeroResultRate != null
    ? latest.zeroResultRate.toFixed(1) + "%"
    : "-";
  const resolvedRate = latest?.resolvedRate != null
    ? latest.resolvedRate.toFixed(1) + "%"
    : "-";
  const avgResponseMs = latest?.avgResponseTimeMs != null
    ? latest.avgResponseTimeMs.toLocaleString() + "ms"
    : "-";

  const ragasRows = [
    { label: "Faithfulness", value: latestRagas?.faithfulness ?? null, target: 0.90 },
    { label: "Answer Relevance", value: latestRagas?.answerRelevancy ?? null, target: 0.85 },
    { label: "Context Precision", value: latestRagas?.contextPrecision ?? null, target: 0.70 },
    { label: "Context Recall", value: latestRagas?.contextRecall ?? null, target: 0.75 },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-2">
        <h2 className="text-text-primary font-semibold text-lg">품질 모니터링</h2>
        <PageFilters
          orgId={orgId} onOrgChange={setOrgId}
          from={from} onFromChange={setFrom}
          to={to} onToChange={setTo}
        />
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <KpiCard label="응답률" value={resolvedRate} trend="up" trendValue="목표 > 85%" help="전체 질문 중 정상 답변이 제공된 비율입니다. 85% 이상 유지를 권장합니다." />
        <KpiCard label="Fallback율" value={fallbackRate} trend="down" trendValue="목표 < 10%" help="신뢰도 부족으로 일반 안내문으로 대체된 비율입니다. 문서 품질 개선으로 낮출 수 있습니다." />
        <KpiCard label="무응답율" value={zeroResultRate} trend="down" trendValue="목표 < 5%" help="검색 결과 자체가 없는 질문의 비율입니다. 해당 주제의 문서 추가가 필요합니다." />
        <KpiCard label="평균 응답시간" value={avgResponseMs} help="RAG 파이프라인이 답변을 생성하는 데 걸린 평균 시간입니다. 1,500ms 이하를 권장합니다." />
      </div>

      {/* RAGAS 스코어카드 */}
      <Card>
        <CardHeader>
          <CardTitle>RAGAS 평가 스코어</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4">
          {latestRagas == null ? (
            <p className="text-text-muted text-sm text-center py-6">
              평가 데이터 없음 — eval-runner를 실행해 RAGAS 점수를 생성하세요.
            </p>
          ) : (
            <>
              <ScoreTable rows={ragasRows} />
              <p className="text-xs text-text-muted mt-3">
                평가 일시: {new Date(latestRagas.evaluatedAt).toLocaleString("ko-KR")}
              </p>
            </>
          )}
        </div>
      </Card>

      {metrics.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Fallback / 무응답 추세</CardTitle>
          </CardHeader>
          <MetricsLineChart
            data={metrics}
            metrics={["fallbackRate", "zeroResultRate", "resolvedRate"]}
          />
        </Card>
      )}
    </div>
  );
}
