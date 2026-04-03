"use client";

import { useState } from "react";
import Link from "next/link";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, DailyMetric, RagasEvaluation, RagSearchLogStats } from "@/lib/types";
import { KpiCard } from "@/components/charts/KpiCard";
import { MetricsLineChart } from "@/components/charts/MetricsLineChart";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Spinner } from "@/components/ui/Spinner";
import { PageFilters, getWeekFrom, getToday } from "@/components/ui/PageFilters";
import { ScoreTable } from "@/components/ui/ScoreTable";
import { PageGuide } from "@/components/ui/PageGuide";

function StatusIcon({ value, target, lowerIsBetter = false }: { value: number | null; target: number; lowerIsBetter?: boolean }) {
  if (value == null) return <span className="text-text-muted">—</span>;
  const ok = lowerIsBetter ? value <= target : value >= target;
  const warn = lowerIsBetter ? value <= target * 1.5 : value >= target * 0.8;
  if (ok) return <span className="text-success">✅</span>;
  if (warn) return <span className="text-warning">⚠️</span>;
  return <span className="text-error">❌</span>;
}

export default function QualityPage() {
  const [orgId, setOrgId] = useState("");
  const [from, setFrom] = useState(getWeekFrom);
  const [to, setTo] = useState(getToday);

  const params = new URLSearchParams({ page_size: "14" });
  if (orgId) params.set("organization_id", orgId);
  if (from) params.set("from_date", from);
  if (to) params.set("to_date", to);

  const ragasParams = new URLSearchParams({ page_size: "30" });
  if (orgId) ragasParams.set("organization_id", orgId);

  const ragSearchParams = new URLSearchParams();
  if (orgId) ragSearchParams.set("organization_id", orgId);
  if (from) ragSearchParams.set("from_date", from);
  if (to) ragSearchParams.set("to_date", to);

  const { data, error, isLoading } = useSWR<PagedResponse<DailyMetric>>(
    `/api/admin/metrics/daily?${params}`,
    fetcher
  );

  const { data: ragasListData } = useSWR<PagedResponse<RagasEvaluation>>(
    `/api/admin/ragas-evaluations?${ragasParams}`,
    fetcher
  );

  const { data: ragSearchStats } = useSWR<RagSearchLogStats>(
    `/api/admin/rag-search-logs?${ragSearchParams}`,
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
  const ragasItems = ragasListData?.items ?? [];
  const latestRagas = ragasItems[0] ?? null;

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

  const currentRagasRows = [
    { label: "Faithfulness", value: latestRagas?.faithfulness ?? null, target: 0.90 },
    { label: "Answer Relevance", value: latestRagas?.answerRelevancy ?? null, target: 0.85 },
    { label: "Context Precision", value: latestRagas?.contextPrecision ?? null, target: 0.70 },
    { label: "Context Recall", value: latestRagas?.contextRecall ?? null, target: 0.75 },
    { label: "Citation Coverage", value: latestRagas?.citationCoverage ?? null, target: 0.80 },
    { label: "Citation Correctness", value: latestRagas?.citationCorrectness ?? null, target: 0.85 },
  ];

  const belowTargetMetrics = currentRagasRows.filter(
    (r) => r.value != null && r.target != null && r.value < r.target
  );

  // RAGAS 추세 차트 데이터 (최신 30건 → 시간순)
  const ragasTrendData = [...ragasItems].reverse().map((e) => ({
    metricDate: e.evaluatedAt.slice(0, 19),
    faithfulness: e.faithfulness ?? undefined,
    answerRelevancy: e.answerRelevancy ?? undefined,
  })) as unknown as import("@/lib/types").DailyMetric[];

  // 목표 대비 현황 테이블
  const avgConfidence = latest?.autoResolutionRate; // proxy or null
  const targetRows = [
    {
      label: "Faithfulness",
      current: latestRagas?.faithfulness != null ? (latestRagas.faithfulness * 100).toFixed(1) + "%" : "-",
      target: "> 90%",
      value: latestRagas?.faithfulness,
      targetVal: 0.90,
    },
    {
      label: "Answer Relevancy",
      current: latestRagas?.answerRelevancy != null ? (latestRagas.answerRelevancy * 100).toFixed(1) + "%" : "-",
      target: "> 85%",
      value: latestRagas?.answerRelevancy,
      targetVal: 0.85,
    },
    {
      label: "평균 검색 Latency",
      current: ragSearchStats?.avgLatencyMs != null ? Math.round(ragSearchStats.avgLatencyMs).toLocaleString() + "ms" : "-",
      target: "< 3,000ms",
      value: ragSearchStats?.avgLatencyMs != null ? 3000 - ragSearchStats.avgLatencyMs : null,
      targetVal: 0,
      lowerIsBetter: false,
      isOk: ragSearchStats?.avgLatencyMs != null && ragSearchStats.avgLatencyMs < 3000,
    },
    {
      label: "P95 검색 Latency",
      current: ragSearchStats?.p95LatencyMs != null ? ragSearchStats.p95LatencyMs.toLocaleString() + "ms" : "-",
      target: "< 6,000ms",
      value: ragSearchStats?.p95LatencyMs != null ? 6000 - ragSearchStats.p95LatencyMs : null,
      targetVal: 0,
      lowerIsBetter: false,
      isOk: ragSearchStats?.p95LatencyMs != null && ragSearchStats.p95LatencyMs < 6000,
    },
    {
      label: "Citation Coverage",
      current: latestRagas?.citationCoverage != null ? (latestRagas.citationCoverage * 100).toFixed(1) + "%" : "-",
      target: "> 80%",
      value: latestRagas?.citationCoverage,
      targetVal: 0.80,
    },
    {
      label: "Citation Correctness",
      current: latestRagas?.citationCorrectness != null ? (latestRagas.citationCorrectness * 100).toFixed(1) + "%" : "-",
      target: "> 85%",
      value: latestRagas?.citationCorrectness,
      targetVal: 0.85,
    },
  ];

  return (
    <div className="space-y-6">
      <PageGuide
        description="RAGAS 4개 지표로 RAG 파이프라인의 품질을 정량 측정하는 화면입니다."
        tips={[
          "Faithfulness: 답변이 검색된 문서를 얼마나 충실히 반영하는지 (목표 ≥ 0.90).",
          "Context Precision: 검색된 문서 중 실제로 유용한 문서 비율 (목표 ≥ 0.70).",
          "지표가 낮은 단계를 찾아 시뮬레이션 룸에서 원인 케이스를 직접 확인하세요.",
        ]}
      />
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
          <CardTitle>RAGAS 평가 스코어 (최신)</CardTitle>
        </CardHeader>

        <div className="px-4 pb-4 pt-3">
          {latestRagas == null ? (
            <p className="text-text-muted text-sm text-center py-6">
              평가 데이터 없음 — eval-runner를 실행해 RAGAS 점수를 생성하세요.
            </p>
          ) : (
            <>
              <ScoreTable rows={currentRagasRows} />
              <p className="text-xs text-text-muted mt-3">
                평가 일시: {new Date(latestRagas.evaluatedAt).toLocaleString("ko-KR")}
                {ragasItems.length > 0 && ` · 총 ${ragasListData?.total ?? ragasItems.length}건`}
              </p>
            </>
          )}

          {belowTargetMetrics.length > 0 && (
            <div className="mt-4 pt-3 border-t border-bg-border space-y-1.5">
              {belowTargetMetrics.map((m) => (
                <div key={m.label} className="flex items-center gap-2">
                  <span className="text-[11px] text-warning font-mono">{m.label}</span>
                  <span className="text-[11px] text-text-muted">목표 미달 —</span>
                  <Link href="/ops/simulator" className="text-[11px] text-accent hover:underline">
                    시뮬레이션 룸에서 원인 케이스 확인 →
                  </Link>
                </div>
              ))}
            </div>
          )}
        </div>
      </Card>

      {/* RAGAS 추세 차트 */}
      {ragasTrendData.length > 1 && (
        <Card>
          <CardHeader>
            <CardTitle>RAGAS 추세 (최근 {ragasTrendData.length}건)</CardTitle>
          </CardHeader>
          <MetricsLineChart
            data={ragasTrendData}
            metrics={["faithfulness" as keyof import("@/lib/types").DailyMetric, "answerRelevancy" as keyof import("@/lib/types").DailyMetric]}
          />
        </Card>
      )}

      {/* 검색 품질 섹션 */}
      {ragSearchStats != null && ragSearchStats.total > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>RAG 검색 품질 ({ragSearchStats.total.toLocaleString()}건)</CardTitle>
          </CardHeader>
          <div className="px-4 pb-4 pt-3 grid grid-cols-2 lg:grid-cols-4 gap-4">
            <div>
              <p className="text-[11px] text-text-muted mb-1">평균 Latency</p>
              <p className="text-text-primary font-mono font-semibold">
                {ragSearchStats.avgLatencyMs != null ? Math.round(ragSearchStats.avgLatencyMs).toLocaleString() + "ms" : "-"}
              </p>
            </div>
            <div>
              <p className="text-[11px] text-text-muted mb-1">P50 Latency</p>
              <p className="text-text-primary font-mono font-semibold">
                {ragSearchStats.p50LatencyMs != null ? ragSearchStats.p50LatencyMs.toLocaleString() + "ms" : "-"}
              </p>
            </div>
            <div>
              <p className="text-[11px] text-text-muted mb-1">P95 Latency</p>
              <p className="text-text-primary font-mono font-semibold">
                {ragSearchStats.p95LatencyMs != null ? ragSearchStats.p95LatencyMs.toLocaleString() + "ms" : "-"}
              </p>
            </div>
            <div>
              <p className="text-[11px] text-text-muted mb-1">Zero-Result 비율</p>
              <p className="text-text-primary font-mono font-semibold">
                {(ragSearchStats.zeroResultRate * 100).toFixed(1)}%
              </p>
            </div>
          </div>
          {Object.keys(ragSearchStats.retrievalStatusDistribution).length > 0 && (
            <div className="px-4 pb-4 border-t border-bg-border pt-3">
              <p className="text-[11px] text-text-muted mb-2">검색 상태 분포</p>
              <div className="flex flex-wrap gap-2">
                {Object.entries(ragSearchStats.retrievalStatusDistribution).map(([status, count]) => (
                  <span key={status} className="text-[11px] bg-bg-muted text-text-secondary px-2 py-1 rounded font-mono">
                    {status}: {count.toLocaleString()}건
                  </span>
                ))}
              </div>
            </div>
          )}
        </Card>
      )}

      {/* 목표 대비 현황 테이블 */}
      <Card>
        <CardHeader>
          <CardTitle>목표 대비 현황</CardTitle>
        </CardHeader>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-bg-border">
                <th className="text-left px-4 py-2 text-[11px] text-text-muted font-medium">지표</th>
                <th className="text-right px-4 py-2 text-[11px] text-text-muted font-medium">현재값</th>
                <th className="text-right px-4 py-2 text-[11px] text-text-muted font-medium">목표</th>
                <th className="text-center px-4 py-2 text-[11px] text-text-muted font-medium">상태</th>
              </tr>
            </thead>
            <tbody>
              {targetRows.map((row) => (
                <tr key={row.label} className="border-b border-bg-border/50">
                  <td className="px-4 py-2 text-text-primary text-xs">{row.label}</td>
                  <td className="px-4 py-2 text-right font-mono text-xs text-text-secondary">{row.current}</td>
                  <td className="px-4 py-2 text-right text-xs text-text-muted">{row.target}</td>
                  <td className="px-4 py-2 text-center text-sm">
                    {"isOk" in row
                      ? row.isOk ? "✅" : "❌"
                      : <StatusIcon value={row.value} target={row.targetVal} />
                    }
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
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
