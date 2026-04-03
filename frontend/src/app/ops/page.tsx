"use client";

import { useState } from "react";
import Link from "next/link";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, DailyMetric, UnresolvedQuestion, LlmMetrics, Organization, RagasEvaluation, InfraMetrics, RateLimitMetrics } from "@/lib/types";
import { KpiCard } from "@/components/charts/KpiCard";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Spinner } from "@/components/ui/Spinner";
import { PageFilters, getWeekFrom, getToday } from "@/components/ui/PageFilters";
import { AlertBanner } from "@/components/ui/AlertBanner";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";
import { PageGuide } from "@/components/ui/PageGuide";
import { MetricsLineChart } from "@/components/charts/MetricsLineChart";
import clsx from "clsx";

// ── 타입 ──────────────────────────────────────────────────────────────────────

type KpiStatus = "ok" | "warn" | "critical";
type SystemStatus = "ok" | "warn" | "critical";

function getKpiStatus(
  value: number | null,
  thresholds: { ok: (v: number) => boolean; warn: (v: number) => boolean }
): KpiStatus | undefined {
  if (value == null) return undefined;
  if (thresholds.ok(value)) return "ok";
  if (thresholds.warn(value)) return "warn";
  return "critical";
}

// ── 파이프라인 단계 색상 (실측 데이터 없을 때 fallback 비율) ────────────────

const PIPELINE_COLORS = {
  retrieval:    "#5e6ad2",
  llm:          "#10b981",
  postprocess:  "#f59e0b",
};
const PIPELINE_SAMPLE = { retrievalMs: 180, llmMs: 980, postprocessMs: 24 };

interface PipelineLatencyData {
  avgRetrievalMs: number | null;
  avgLlmMs: number | null;
  avgPostprocessMs: number | null;
  avgTotalMs: number | null;
  sampleCount: number;
}

// ── 전일 대비 트렌드 ──────────────────────────────────────────────────────────

function calcTrend(
  curr: number | null,
  prev: number | null,
  higherIsBetter = true
): { trend: "up" | "down" | "neutral"; trendValue: string } | undefined {
  if (curr == null || prev == null || prev === 0) return undefined;
  const delta = curr - prev;
  const pct = (delta / Math.abs(prev)) * 100;
  if (Math.abs(pct) < 0.05) return { trend: "neutral", trendValue: "전일 동일" };
  const isGood = delta > 0 ? higherIsBetter : !higherIsBetter;
  const trend = isGood ? "up" : "down";
  const sign = delta > 0 ? "+" : "";
  return { trend, trendValue: `${sign}${pct.toFixed(1)}% 전일 대비` };
}

// ── 시스템 상태 신호등 ─────────────────────────────────────────────────────────

function calcSystemStatus(latest?: DailyMetric): Record<string, SystemStatus> {
  const fallback  = latest?.fallbackRate     ?? 0;
  const zero      = latest?.zeroResultRate   ?? 0;
  const resolved  = latest?.resolvedRate     ?? 100;
  return {
    pipeline:  fallback  >= 15 ? "critical" : fallback  >= 10 ? "warn" : "ok",
    knowledge: zero      >= 8  ? "critical" : zero      >= 5  ? "warn" : "ok",
    quality:   resolved  < 80  ? "critical" : resolved  < 90  ? "warn" : "ok",
  };
}

const STATUS_DOT: Record<SystemStatus, string> = {
  ok:       "bg-success",
  warn:     "bg-warning",
  critical: "bg-error animate-pulse",
};
const STATUS_BG: Record<SystemStatus, string> = {
  ok:       "bg-success/10 text-success",
  warn:     "bg-warning/10 text-warning",
  critical: "bg-error/10 text-error",
};
const STATUS_LABEL: Record<SystemStatus, string> = {
  ok:       "정상",
  warn:     "주의",
  critical: "위험",
};

// ── 품질/보안 요약 인라인 컴포넌트 ───────────────────────────────────────────

interface PiiCountData {
  count: number;
  lastDetectedAt: string | null;
}

interface FeedbackTrendPoint {
  date: string;
  positive: number;
  negative: number;
}

interface FeedbackItem {
  satisfactionScore: number | null;
}

function MiniSparkline({ values }: { values: number[] }) {
  if (values.length === 0) return null;
  const max = Math.max(...values);
  return (
    <div className="flex items-end gap-0.5 h-8 mt-2">
      {values.map((v, i) => (
        <div
          key={i}
          className={clsx(
            "flex-1 rounded-t-sm",
            i === values.length - 1 ? "bg-accent" : "bg-accent/40"
          )}
          style={{ height: `${max > 0 ? (v / max) * 100 : 0}%`, minHeight: 2 }}
        />
      ))}
    </div>
  );
}

function FeedbackBar({ positive, negative }: { positive: number; negative: number }) {
  const total = positive + negative;
  const posPct = total > 0 ? (positive / total) * 100 : 0;
  return (
    <div className="mt-2">
      <div className="flex items-center gap-2 text-xs text-text-secondary mb-1">
        <span className="text-success font-mono">{positive.toLocaleString()} 긍정</span>
        <span className="text-text-muted">/</span>
        <span className="text-error font-mono">{negative.toLocaleString()} 부정</span>
        {total > 0 && (
          <span className="text-text-muted ml-auto font-mono">
            {posPct.toFixed(1)}%
          </span>
        )}
      </div>
      {total > 0 ? (
        <div className="h-2 w-full rounded-full overflow-hidden bg-error/30">
          <div
            className="h-full rounded-full bg-success transition-all"
            style={{ width: `${posPct}%` }}
          />
        </div>
      ) : (
        <div className="h-2 w-full rounded-full bg-white/10" />
      )}
    </div>
  );
}

// ── 메인 컴포넌트 ─────────────────────────────────────────────────────────────

export default function OpsDashboardPage() {
  const [orgId, setOrgId]               = useState("");
  const [from, setFrom]                 = useState(getWeekFrom);
  const [to, setTo]                     = useState(getToday);
  const [alertDismissed, setAlertDismissed] = useState(false);

  const params = new URLSearchParams({ page_size: "200" });
  if (orgId) params.set("organization_id", orgId);
  if (from)  params.set("from_date", from);
  if (to)    params.set("to_date", to);

  const { data, error, isLoading } = useSWR<PagedResponse<DailyMetric>>(
    `/api/admin/metrics/daily?${params}`,
    fetcher
  );

  const { data: llmData } = useSWR<LlmMetrics>(
    "/api/admin/metrics/llm",
    fetcher
  );

  const { data: questionsData } = useSWR<PagedResponse<UnresolvedQuestion>>(
    "/api/admin/questions/unresolved?page_size=10",
    fetcher
  );

  const { data: orgsData } = useSWR<PagedResponse<Organization>>(
    "/api/admin/organizations?page_size=50",
    fetcher
  );

  const { data: pipelineData } = useSWR<PipelineLatencyData>(
    "/api/admin/metrics/pipeline-latency",
    fetcher
  );

  // 품질/보안 요약 패널용 데이터
  const { data: ragasData } = useSWR<PagedResponse<RagasEvaluation>>(
    "/api/admin/ragas-evaluations?page_size=7",
    fetcher
  );

  const { data: piiData } = useSWR<PiiCountData>(
    "/api/admin/metrics/pii-count",
    fetcher
  );

  const { data: feedbacksData } = useSWR<PagedResponse<FeedbackItem>>(
    "/api/admin/feedbacks?page_size=100",
    fetcher
  );

  const { data: feedbackTrendData } = useSWR<FeedbackTrendPoint[]>(
    "/api/admin/metrics/feedback-trend?days=7",
    fetcher
  );

  const { data: infraData } = useSWR<InfraMetrics>(
    "/api/admin/metrics/infra",
    fetcher
  );

  const { data: rateLimitData } = useSWR<RateLimitMetrics>(
    "/api/admin/metrics/rate-limits",
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
    return (
      <p className="text-error text-sm">
        데이터를 불러오지 못했습니다. 백엔드 연결을 확인하세요.
      </p>
    );
  }

  const metrics         = [...(data?.items ?? [])].sort((a, b) => a.metricDate.localeCompare(b.metricDate));
  const latest          = metrics[metrics.length - 1];
  const prev            = metrics.length >= 2 ? metrics[metrics.length - 2] : null;
  const issueQuestions = questionsData?.items ?? [];

  // orgId → name 맵
  const orgNameMap = new Map<string, string>(
    (orgsData?.items ?? []).map((o) => [o.organizationId, o.name])
  );

  const resolvedRateVal  = latest?.resolvedRate     ?? null;
  const fallbackRateVal  = latest?.fallbackRate     ?? null;
  const zeroResultRateVal= latest?.zeroResultRate   ?? null;
  const avgRespMsVal     = latest?.avgResponseTimeMs ?? null;
  const avgCostVal       = llmData?.avgCostPerQuery  ?? null;

  const resolvedTrend  = calcTrend(resolvedRateVal,   prev?.resolvedRate     ?? null, true);
  const fallbackTrend  = calcTrend(fallbackRateVal,   prev?.fallbackRate     ?? null, false);
  const latencyTrend   = calcTrend(avgRespMsVal,      prev?.avgResponseTimeMs ?? null, false);
  const gapTrend       = calcTrend(zeroResultRateVal, prev?.zeroResultRate   ?? null, false);

  const showAlert =
    !alertDismissed &&
    ((fallbackRateVal  != null && fallbackRateVal  > 10) ||
     (zeroResultRateVal != null && zeroResultRateVal > 5));

  const alertVariant =
    (fallbackRateVal  != null && fallbackRateVal  >= 15) ||
    (zeroResultRateVal != null && zeroResultRateVal >= 8)
      ? ("critical" as const)
      : ("warn" as const);

  // 기관 헬스맵
  const orgLatestMap = new Map<string, typeof latest>();
  for (const m of metrics) {
    const existing = orgLatestMap.get(m.organizationId);
    if (!existing || m.metricDate > existing.metricDate) {
      orgLatestMap.set(m.organizationId, m);
    }
  }
  const orgHealthList = Array.from(orgLatestMap.entries()).map(([oid, m]) => {
    const resolved = m.resolvedRate != null ? Number(m.resolvedRate) : null;
    const fallback = m.fallbackRate != null ? Number(m.fallbackRate) : null;
    const hasData = resolved != null || fallback != null;
    let healthStatus: "ok" | "warn" | "critical" | "nodata" = hasData ? "ok" : "nodata";
    let issue = "";
    if (hasData) {
      if      (resolved != null && resolved < 80)   { healthStatus = "critical"; issue = `응답률 ${resolved.toFixed(1)}% (목표 90%)`; }
      else if (fallback != null && fallback >= 15)   { healthStatus = "critical"; issue = `Fallback율 ${fallback.toFixed(1)}% (임계 15%)`; }
      else if (resolved != null && resolved < 90)    { healthStatus = "warn";     issue = `응답률 ${resolved.toFixed(1)}% (목표 90%)`; }
      else if (fallback != null && fallback >= 10)   { healthStatus = "warn";     issue = `Fallback율 ${fallback.toFixed(1)}% (임계 10%)`; }
    }
    return { orgId: oid, orgName: orgNameMap.get(oid) ?? oid, healthStatus, issue, resolved, fallback };
  });

  const healthColor = { ok: "text-success", warn: "text-warning", critical: "text-error", nodata: "text-text-muted" };
  const healthLabel = { ok: "정상",         warn: "주의",         critical: "위험",       nodata: "데이터 없음" };
  const healthDot   = { ok: "bg-success",   warn: "bg-warning",   critical: "bg-error",   nodata: "bg-text-muted" };

  const systemStatus = calcSystemStatus(latest);

  // 품질/보안 요약 패널 데이터 가공
  const ragasItems = [...(ragasData?.items ?? [])].sort(
    (a, b) => a.evaluatedAt.localeCompare(b.evaluatedAt)
  );
  const faithfulnessValues = ragasItems
    .map((r) => r.faithfulness)
    .filter((v): v is number => v != null);
  const latestFaithfulness =
    faithfulnessValues.length > 0
      ? faithfulnessValues[faithfulnessValues.length - 1]
      : null;
  // Hallucination Rate = 1 - Faithfulness
  const hallucinationValues = faithfulnessValues.map((v) => 1 - v);
  const latestHallucination =
    hallucinationValues.length > 0
      ? hallucinationValues[hallucinationValues.length - 1]
      : null;

  const feedbackItems = feedbacksData?.items ?? [];
  const positiveFeedbacks = feedbackItems.filter(
    (f) => f.satisfactionScore != null && f.satisfactionScore >= 4
  ).length;
  const negativeFeedbacks = feedbackItems.filter(
    (f) => f.satisfactionScore != null && f.satisfactionScore <= 2
  ).length;

  const trendPoints = feedbackTrendData ?? [];

  // 파이프라인 레이턴시 실측 연동
  const isRealLatency =
    pipelineData != null &&
    pipelineData.sampleCount > 0 &&
    (pipelineData.avgRetrievalMs != null || pipelineData.avgLlmMs != null);

  const retrievalMs   = isRealLatency ? (pipelineData!.avgRetrievalMs   ?? 0) : PIPELINE_SAMPLE.retrievalMs;
  const llmMs         = isRealLatency ? (pipelineData!.avgLlmMs          ?? 0) : PIPELINE_SAMPLE.llmMs;
  const postprocessMs = isRealLatency
    ? (pipelineData!.avgPostprocessMs != null
        ? pipelineData!.avgPostprocessMs
        : Math.max(0, (avgRespMsVal ?? 0) - retrievalMs - llmMs))
    : PIPELINE_SAMPLE.postprocessMs;
  const pipelineTotal = retrievalMs + llmMs + postprocessMs || 1;

  const pipelineStages = [
    { label: "Retrieval", ms: retrievalMs,   color: PIPELINE_COLORS.retrieval,  ratio: retrievalMs / pipelineTotal },
    { label: "LLM",       ms: llmMs,          color: PIPELINE_COLORS.llm,        ratio: llmMs / pipelineTotal },
    { label: "후처리",    ms: postprocessMs,  color: PIPELINE_COLORS.postprocess, ratio: postprocessMs / pipelineTotal },
  ];

  return (
    <div className="space-y-6">
      <PageGuide
        description="전체 시스템 상태를 한눈에 파악하는 메인 대시보드입니다."
        tips={[
          "KPI 카드를 클릭하면 해당 지표의 상세 페이지로 이동합니다.",
          "시스템 신호등이 '위험'이면 이상 징후 감지 페이지에서 원인을 확인하세요.",
          "기관 헬스맵에서 주의/위험 기관을 먼저 점검하세요.",
        ]}
      />

      {showAlert && (
        <AlertBanner
          variant={alertVariant}
          message={
            fallbackRateVal != null && fallbackRateVal > 10
              ? `Fallback율이 ${fallbackRateVal.toFixed(1)}%로 임계값(10%)을 초과했습니다.`
              : `무응답률이 ${zeroResultRateVal?.toFixed(1)}%로 임계값(5%)을 초과했습니다.`
          }
          onDismiss={() => setAlertDismissed(true)}
        />
      )}

      <div className="flex items-center justify-between flex-wrap gap-2">
        <h2 className="font-inter text-text-primary font-[510] text-[15px] tracking-tight">통합 관제</h2>
        <PageFilters
          orgId={orgId} onOrgChange={setOrgId}
          from={from}   onFromChange={setFrom}
          to={to}       onToChange={setTo}
        />
      </div>

      {/* KPI 5개 — 클릭 시 상세 페이지로 이동 */}
      <div className="grid grid-cols-2 lg:grid-cols-5 gap-4 items-stretch">
        <Link href="/ops/quality" className="block h-full">
          <KpiCard
            label="ANSWER RATE"
            value={resolvedRateVal != null ? resolvedRateVal.toFixed(1) + "%" : "-"}
            status={getKpiStatus(resolvedRateVal, { ok: (v) => v >= 90, warn: (v) => v >= 80 })}
            progressValue={resolvedRateVal ?? undefined}
            trend={resolvedTrend?.trend}
            trendValue={resolvedTrend?.trendValue}
            help="RAG 파이프라인(문서 검색 → LLM 생성)이 끝까지 완료된 비율. 답변 내용의 정확도와는 무관하며, 파이프라인이 중단된 경우(Fallback·무응답·오류)만 제외됩니다. 정상 ≥ 90% / 주의 80~90% / 위험 < 80%."
          />
        </Link>
        <Link href="/ops/anomaly" className="block h-full">
          <KpiCard
            label="ERROR RATE"
            value={fallbackRateVal != null ? fallbackRateVal.toFixed(1) + "%" : "-"}
            status={getKpiStatus(fallbackRateVal, { ok: (v) => v < 10, warn: (v) => v < 15 })}
            progressValue={fallbackRateVal != null ? (fallbackRateVal / 30) * 100 : undefined}
            trend={fallbackTrend?.trend}
            trendValue={fallbackTrend?.trendValue}
            help="answer_status = 'fallback' 건수 ÷ 전체 질문 수. Ollama 미응답·검색 실패 등으로 파이프라인이 중단되어 대체 메시지를 반환한 비율입니다. 답변 내용 없이 '서비스 불가' 응답이 나간 경우만 집계됩니다. 정상 < 10% / 주의 10~15% / 위험 ≥ 15%."
          />
        </Link>
        <Link href="/ops/anomaly" className="block h-full">
          <KpiCard
            label="E2E LATENCY"
            value={avgRespMsVal != null ? avgRespMsVal.toLocaleString() + "ms" : "-"}
            status={getKpiStatus(avgRespMsVal, { ok: (v) => v < 1500, warn: (v) => v < 2500 })}
            progressValue={avgRespMsVal != null ? (avgRespMsVal / 5000) * 100 : undefined}
            trend={latencyTrend?.trend}
            trendValue={latencyTrend?.trendValue}
            help="answers.response_time_ms 평균. 질문 수신부터 답변 반환까지 전체 소요시간이며, 문서 검색·LLM 생성·후처리를 모두 포함합니다. 답변 품질과는 무관하며 answered 건만 집계됩니다. 정상 < 1,500ms / 주의 1,500~2,500ms / 위험 ≥ 2,500ms."
          />
        </Link>
        <Link href="/ops/statistics" className="block h-full">
          <KpiCard
            label="KNOWLEDGE GAP"
            value={zeroResultRateVal != null ? zeroResultRateVal.toFixed(1) + "%" : "-"}
            status={getKpiStatus(zeroResultRateVal, { ok: (v) => v < 5, warn: (v) => v < 8 })}
            progressValue={zeroResultRateVal != null ? (zeroResultRateVal / 20) * 100 : undefined}
            trend={gapTrend?.trend}
            trendValue={gapTrend?.trendValue}
            help="answer_status = 'no_answer' 건수 ÷ 전체 질문 수. pgvector 검색 결과가 0건이어서 LLM에 전달할 문서가 없었던 경우입니다. 지식베이스에 관련 문서 자체가 없다는 의미이며, 문서 추가·재인덱싱으로 개선할 수 있습니다. 정상 < 5% / 주의 5~8% / 위험 ≥ 8%."
          />
        </Link>
        <Link href="/ops/cost" className="block h-full">
          <KpiCard
            label="COST / QUERY"
            value={avgCostVal != null ? `$${avgCostVal.toFixed(4)}` : "-"}
            status={getKpiStatus(avgCostVal, { ok: (v) => v < 0.008, warn: (v) => v < 0.012 })}
            progressValue={avgCostVal != null ? (avgCostVal / 0.02) * 100 : undefined}
            help="answers.estimated_cost_usd 합계 ÷ answered 건수. LLM 호출 시 추정된 USD 비용 기준이며, 입력·출력 토큰 수와 모델 단가로 계산됩니다. answered가 없으면 표시되지 않습니다. 정상 < $0.008 / 주의 $0.008~0.012 / 위험 ≥ $0.012."
          />
        </Link>
      </div>

      {/* 지표 추세 차트 + 시스템 상태 신호등 */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between flex-wrap gap-3">
            <CardTitle tag="7-DAY TREND">핵심 지표 추세</CardTitle>
            <div className="flex items-center gap-4">
              {[
                {
                  key: "pipeline",
                  label: "RAG 파이프라인",
                  hint: "기준 지표: Fallback율 (answer_status = 'fallback' ÷ 전체). Ollama 미응답·검색 실패 등 파이프라인이 중단된 비율입니다. 정상 < 10% / 주의 10~15% / 위험 ≥ 15%.",
                },
                {
                  key: "knowledge",
                  label: "지식베이스",
                  hint: "기준 지표: 무응답율 (answer_status = 'no_answer' ÷ 전체). pgvector 검색 결과가 0건이어서 LLM에 전달할 문서가 없었던 비율입니다. 정상 < 5% / 주의 5~8% / 위험 ≥ 8%.",
                },
                {
                  key: "quality",
                  label: "응답 품질",
                  hint: "기준 지표: 답변완료율 (answer_status = 'answered' ÷ 전체). RAG 파이프라인이 끝까지 완료된 비율이며, 답변 내용의 정확도와는 무관합니다. 정상 ≥ 90% / 주의 80~90% / 위험 < 80%.",
                },
              ].map(({ key, label, hint }) => {
                const st = systemStatus[key] as SystemStatus;
                return (
                  <div key={key} className="flex items-center gap-1.5">
                    <div className={clsx("w-2 h-2 rounded-full shrink-0", STATUS_DOT[st])} />
                    <span className="text-[11px] text-text-secondary">{label}</span>
                    <span className={clsx("font-mono text-[10px] font-bold", {
                      "text-success": st === "ok",
                      "text-warning": st === "warn",
                      "text-error":   st === "critical",
                    })}>
                      {STATUS_LABEL[st]}
                    </span>
                    <span className="group relative">
                      <span className="text-text-muted text-xs cursor-help select-none">ⓘ</span>
                      <span className="absolute top-full right-0 mt-2 w-64 rounded-lg bg-bg-prominent border border-bg-border px-3 py-2.5 text-xs leading-relaxed text-text-secondary shadow-xl opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-[100] whitespace-normal">
                        {hint}
                      </span>
                    </span>
                  </div>
                );
              })}
            </div>
          </div>
        </CardHeader>
        <div className="px-4 pb-4">
          {metrics.length >= 2 ? (
            <MetricsLineChart
              data={metrics}
              metrics={["resolvedRate", "fallbackRate", "zeroResultRate"]}
            />
          ) : (
            <div className="h-[220px] flex items-center justify-center text-text-muted text-sm">
              추세 차트를 보려면 2일 이상의 데이터가 필요합니다.
            </div>
          )}
        </div>
      </Card>

      {/* 파이프라인 레이턴시 — 전체 너비 + 단계별 상세 */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle tag={isRealLatency ? "ACTUAL" : "SAMPLE DATA"}>파이프라인 레이턴시</CardTitle>
            <span className="font-mono text-[11px] text-text-muted">
              Total: <span className="text-text-primary font-semibold">{pipelineTotal.toLocaleString()}ms</span>
            </span>
          </div>
        </CardHeader>
        <div className="px-4 pb-6">
          {/* 누적 수평 바 */}
          <div className="h-10 w-full flex rounded-md overflow-hidden ring-1 ring-white/5">
            {pipelineStages.map((stage) => {
              const pct = stage.ratio * 100;
              return (
                <div
                  key={stage.label}
                  className="h-full flex items-center justify-center border-r border-black/20 last:border-0 transition-all hover:brightness-110"
                  style={{ width: `${pct}%`, backgroundColor: stage.color }}
                  title={`${stage.label}: ${stage.ms}ms (${pct.toFixed(1)}%)`}
                >
                  {pct >= 10 && (
                    <span className="font-mono text-[11px] font-bold text-white drop-shadow">
                      {stage.ms}ms
                    </span>
                  )}
                </div>
              );
            })}
          </div>
          {/* 단계별 상세 그리드 */}
          <div className="grid grid-cols-3 divide-x divide-bg-border mt-5">
            {pipelineStages.map((stage) => {
              const pct = (stage.ratio * 100).toFixed(1);
              return (
                <div key={stage.label} className="pl-4 first:pl-0">
                  <p className="font-inter text-[10px] font-[510] uppercase tracking-[0.1em] text-text-subtle mb-1">
                    {stage.label}
                  </p>
                  <div className="flex items-baseline gap-1">
                    <span className="text-xl font-bold font-mono text-text-primary">
                      {stage.ms}
                    </span>
                    <span className="text-[11px] text-text-muted font-mono">ms</span>
                  </div>
                  <p className="font-mono text-[11px] text-text-muted mt-0.5">{pct}%</p>
                  <div className="mt-2 h-0.5 rounded-full" style={{ backgroundColor: stage.color, width: `${stage.ratio * 100}%` }} />
                </div>
              );
            })}
          </div>
          <p className="text-[10px] text-text-muted mt-4">
            {isRealLatency
              ? `※ E2E 실측: ${pipelineTotal.toLocaleString()}ms (${pipelineData!.sampleCount.toLocaleString()}건 기준)`
              : "※ 샘플 데이터 — Ollama가 실행되면 실측 데이터로 자동 업데이트됩니다."}
          </p>
        </div>
      </Card>

      {/* 기관 헬스맵 — 카드 그리드 */}
      {orgHealthList.length > 0 && (
        <div>
          <h3 className="font-inter text-text-primary font-[510] text-[13px] tracking-tight mb-3 flex items-center gap-2">
            기관 헬스맵
            <span className="font-inter text-[10px] text-text-subtle">
              {orgHealthList.filter(o => o.healthStatus !== "ok").length > 0
                ? `${orgHealthList.filter(o => o.healthStatus !== "ok").length}개 기관 주의`
                : "전체 정상"}
            </span>
          </h3>
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
            {orgHealthList.map(({ orgId: oid, orgName, healthStatus, issue, resolved, fallback }) => (
              <div
                key={oid}
                className={clsx(
                  "rounded-lg p-4 transition-colors",
                  healthStatus === "critical" ? "border-error/30" :
                  healthStatus === "warn"     ? "border-warning/30" :
                  healthStatus === "nodata"   ? "opacity-60" : ""
                )}
                style={{ background: "var(--card-bg)", border: healthStatus === "critical" ? "1px solid rgb(239 68 68 / 0.3)" : healthStatus === "warn" ? "1px solid rgb(245 158 11 / 0.3)" : "1px solid var(--card-border)" }}
              >
                <div className="flex items-center gap-2 mb-2">
                  <div className={clsx("w-2 h-2 rounded-full shrink-0", healthDot[healthStatus],
                    healthStatus === "critical" && "animate-pulse"
                  )} />
                  <span className="text-text-primary text-sm font-medium truncate">{orgName}</span>
                </div>
                <div className="flex items-baseline gap-1 mb-1">
                  <span className="font-mono text-2xl font-bold text-text-primary">
                    {resolved != null ? Number(resolved).toFixed(1) : "-"}
                  </span>
                  <span className="font-mono text-[11px] text-text-muted">%</span>
                </div>
                <p className="font-inter text-[10px] font-[510] text-text-subtle uppercase tracking-[0.1em] mb-2">응답률</p>
                {issue ? (
                  <p className={clsx("text-[11px]", healthColor[healthStatus])}>{issue}</p>
                ) : healthStatus === "nodata" ? (
                  <p className="text-[11px] text-text-muted">데이터 없음</p>
                ) : (
                  <p className="text-[11px] text-success">정상 운영 중</p>
                )}
                {fallback != null && (
                  <p className="font-inter text-[10px] text-text-subtle mt-1">
                    Fallback {Number(fallback).toFixed(1)}%
                  </p>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 품질/보안 요약 */}
      <div>
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-text-primary font-semibold text-sm">품질/보안 요약</h3>
          <Link
            href="/ops/quality-summary"
            className="text-[11px] text-accent hover:underline font-mono"
          >
            상세 보기 →
          </Link>
        </div>

        {/* Row 1: Faithfulness / Hallucination Rate / Recall@K */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-3 mb-3">

          {/* Faithfulness */}
          <Link href="/ops/quality" className="block">
            <div className="rounded-lg p-4 hover:border-accent/30 transition-colors h-full" style={{ background: "var(--card-bg)", border: "1px solid var(--card-border)" }}>
              <p className="font-inter text-[10px] font-[510] uppercase tracking-[0.1em] text-text-subtle mb-1">
                Faithfulness
              </p>
              {latestFaithfulness != null ? (
                <>
                  <div className="flex items-baseline gap-1">
                    <span className={clsx("text-2xl font-bold font-mono", {
                      "text-success": latestFaithfulness >= 0.8,
                      "text-warning": latestFaithfulness >= 0.6 && latestFaithfulness < 0.8,
                      "text-error":   latestFaithfulness < 0.6,
                    })}>
                      {(latestFaithfulness * 100).toFixed(1)}
                    </span>
                    <span className="text-[11px] text-text-muted font-mono">%</span>
                  </div>
                  <MiniSparkline values={faithfulnessValues.map((v) => v * 100)} />
                </>
              ) : (
                <p className="text-sm text-text-muted mt-1">eval-runner 배치 실행 후 표시</p>
              )}
            </div>
          </Link>

          {/* Hallucination Rate */}
          <Link href="/ops/quality" className="block">
            <div className="rounded-lg p-4 hover:border-accent/30 transition-colors h-full" style={{ background: "var(--card-bg)", border: "1px solid var(--card-border)" }}>
              <p className="font-inter text-[10px] font-[510] uppercase tracking-[0.1em] text-text-subtle mb-1">
                Hallucination Rate
              </p>
              {latestHallucination != null ? (
                <>
                  <div className="flex items-baseline gap-1">
                    <span className={clsx("text-2xl font-bold font-mono", {
                      "text-success": latestHallucination < 0.2,
                      "text-warning": latestHallucination < 0.4 && latestHallucination >= 0.2,
                      "text-error":   latestHallucination >= 0.4,
                    })}>
                      {(latestHallucination * 100).toFixed(1)}
                    </span>
                    <span className="text-[11px] text-text-muted font-mono">%</span>
                  </div>
                  <MiniSparkline values={hallucinationValues.map((v) => v * 100)} />
                </>
              ) : (
                <p className="text-sm text-text-muted mt-1">eval-runner 배치 실행 후 표시</p>
              )}
            </div>
          </Link>

          {/* Recall@K — empty state */}
          <Link href="/ops/quality" className="block">
            <div className="rounded-lg p-4 hover:border-accent/30 transition-colors h-full" style={{ background: "var(--card-bg)", border: "1px solid var(--card-border)" }}>
              <p className="font-inter text-[10px] font-[510] uppercase tracking-[0.1em] text-text-subtle mb-1">
                Recall@K
              </p>
              <p className="text-sm text-text-muted mt-1">eval-runner 배치 실행 후 표시</p>
            </div>
          </Link>
        </div>

        {/* Row 2: Session Success Rate / PII 감지 건수 */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-3 mb-3">

          {/* Session Success Rate — empty state */}
          <Link href="/ops/quality" className="block">
            <div className="rounded-lg p-4 hover:border-accent/30 transition-colors h-full" style={{ background: "var(--card-bg)", border: "1px solid var(--card-border)" }}>
              <p className="font-inter text-[10px] font-[510] uppercase tracking-[0.1em] text-text-subtle mb-1">
                Session Success Rate
              </p>
              <p className="text-sm text-text-muted mt-1">eval-runner 배치 실행 후 표시</p>
            </div>
          </Link>

          {/* PII 감지 건수 */}
          <Link href="/ops/audit" className="block">
            <div className="rounded-lg p-4 hover:border-accent/30 transition-colors h-full" style={{ background: "var(--card-bg)", border: "1px solid var(--card-border)" }}>
              <p className="font-inter text-[10px] font-[510] uppercase tracking-[0.1em] text-text-subtle mb-1">
                PII 감지 건수
              </p>
              {piiData != null ? (
                <>
                  <div className="flex items-baseline gap-1">
                    <span className={clsx("text-2xl font-bold font-mono", {
                      "text-success": piiData.count === 0,
                      "text-warning": piiData.count > 0 && piiData.count <= 5,
                      "text-error":   piiData.count > 5,
                    })}>
                      {piiData.count.toLocaleString()}
                    </span>
                    <span className="text-[11px] text-text-muted font-mono">건 (이번 달)</span>
                  </div>
                  {piiData.lastDetectedAt != null && (
                    <p className="text-[11px] text-text-muted mt-1">
                      마지막 감지:{" "}
                      {new Date(piiData.lastDetectedAt).toLocaleString("ko-KR", {
                        month: "2-digit",
                        day: "2-digit",
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </p>
                  )}
                </>
              ) : (
                <p className="text-sm text-text-muted mt-1">데이터 없음</p>
              )}
            </div>
          </Link>
        </div>

        {/* Row 3: 사용자 피드백 (full-width) */}
        <Link href="/ops/correction" className="block">
          <div className="rounded-lg p-4 hover:border-accent/30 transition-colors" style={{ background: "var(--card-bg)", border: "1px solid var(--card-border)" }}>
            <p className="font-inter text-[10px] font-[510] uppercase tracking-[0.1em] text-text-subtle mb-2">
              사용자 피드백
            </p>
            {feedbackItems.length > 0 ? (
              <>
                <FeedbackBar positive={positiveFeedbacks} negative={negativeFeedbacks} />
                {trendPoints.length > 0 && (
                  <div className="mt-4">
                    <p className="text-[10px] text-text-muted font-mono mb-1">주간 추이</p>
                    <div className="grid gap-0.5" style={{ gridTemplateColumns: `repeat(${trendPoints.length}, 1fr)` }}>
                      {trendPoints.map((pt) => {
                        const total = pt.positive + pt.negative;
                        const posPct = total > 0 ? (pt.positive / total) * 100 : 0;
                        return (
                          <div key={pt.date} className="flex flex-col items-center gap-0.5">
                            <div className="w-full h-12 relative bg-white/5 rounded-sm overflow-hidden">
                              <div
                                className="absolute bottom-0 w-full bg-success/50 rounded-sm"
                                style={{ height: `${posPct}%` }}
                              />
                            </div>
                            <span className="text-[9px] text-text-muted font-mono">
                              {new Date(pt.date).toLocaleDateString("ko-KR", { month: "numeric", day: "numeric" })}
                            </span>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                )}
              </>
            ) : (
              <p className="text-sm text-text-muted">데이터 없음</p>
            )}
          </div>
        </Link>
      </div>

      {/* 인프라 & Rate Limit */}
      <div>
        <h3 className="font-inter text-text-primary font-[510] text-[13px] tracking-tight mb-3">인프라 & Rate Limit</h3>

        {/* Row 1: CPU / Memory */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-3 mb-3">
          {(
            [
              { key: "cpuUsagePercent", label: "CPU 사용률", warn: 80, critical: 90 },
              { key: "memoryUsagePercent", label: "Memory 사용률", warn: 80, critical: 90 },
            ] as const
          ).map(({ key, label, warn, critical }) => {
            const val = infraData?.[key] ?? null;
            const st =
              val == null ? undefined
              : val >= critical ? "critical"
              : val >= warn    ? "warn"
              : "ok";
            const stColor = { ok: "text-success", warn: "text-warning", critical: "text-error" };
            return (
              <div key={key} className="rounded-lg p-4" style={{ background: "var(--card-bg)", border: "1px solid var(--card-border)" }}>
                <p className="font-inter text-[10px] font-[510] uppercase tracking-[0.1em] text-text-subtle mb-1">{label}</p>
                {val != null ? (
                  <>
                    <div className="flex items-baseline gap-1">
                      <span className={clsx("text-2xl font-bold font-mono", st ? stColor[st] : "text-text-primary")}>
                        {val.toFixed(1)}
                      </span>
                      <span className="text-[11px] text-text-muted font-mono">%</span>
                    </div>
                    <div className="mt-2 h-1.5 w-full rounded-full bg-white/10 overflow-hidden">
                      <div
                        className={clsx("h-full rounded-full transition-all", {
                          "bg-success": st === "ok",
                          "bg-warning": st === "warn",
                          "bg-error":   st === "critical",
                        })}
                        style={{ width: `${Math.min(val, 100)}%` }}
                      />
                    </div>
                  </>
                ) : (
                  <p className="text-sm text-text-muted mt-1">rag-orchestrator 미실행</p>
                )}
              </div>
            );
          })}
        </div>

        {/* Row 2: LLM Rate Limit / Embedding Rate Limit */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-3">
          {(
            [
              { rateKey: "llmRateLimitRate", hitsKey: "llmRateLimitHits", totalKey: "llmCallsTotal", label: "LLM Rate Limit 히트율" },
              { rateKey: "embeddingRateLimitRate", hitsKey: "embeddingRateLimitHits", totalKey: "embeddingCallsTotal", label: "Embedding Rate Limit 히트율" },
            ] as const
          ).map(({ rateKey, hitsKey, totalKey, label }) => {
            const rate  = rateLimitData?.[rateKey]  ?? null;
            const hits  = rateLimitData?.[hitsKey]  ?? null;
            const total = rateLimitData?.[totalKey] ?? null;
            const st =
              rate == null ? undefined
              : rate >= 2  ? "critical"
              : rate >= 0.5 ? "warn"
              : "ok";
            const stColor = { ok: "text-success", warn: "text-warning", critical: "text-error" };
            return (
              <div key={rateKey} className="rounded-lg p-4" style={{ background: "var(--card-bg)", border: "1px solid var(--card-border)" }}>
                <p className="font-inter text-[10px] font-[510] uppercase tracking-[0.1em] text-text-subtle mb-1">{label}</p>
                {rate != null ? (
                  <>
                    <div className="flex items-baseline gap-1">
                      <span className={clsx("text-2xl font-bold font-mono", st ? stColor[st] : "text-text-primary")}>
                        {rate.toFixed(2)}
                      </span>
                      <span className="text-[11px] text-text-muted font-mono">%</span>
                    </div>
                    <p className="font-inter text-[10px] text-text-subtle mt-1">
                      {hits?.toLocaleString() ?? 0}건 / {total?.toLocaleString() ?? 0}건
                    </p>
                  </>
                ) : (
                  <p className="text-sm text-text-muted mt-1">rag-orchestrator 미실행</p>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* 이슈 알림 로그 */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-1.5">
              <CardTitle tag="ALERT LOG">이슈 질문</CardTitle>
              <span className="group relative mt-3">
                <span className="text-text-muted text-xs cursor-help select-none">ⓘ</span>
                <span className="absolute top-full left-0 mt-2 w-72 rounded-lg bg-bg-prominent border border-bg-border px-3 py-2.5 text-xs leading-relaxed text-text-secondary shadow-xl opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-[100] whitespace-normal">
                  다음 조건 중 하나에 해당하는 질문을 표시합니다.<br />
                  • answer_status가 fallback·no_answer·error인 경우<br />
                  • QA 리뷰 상태가 confirmed_issue인 경우<br />
                  QA에서 resolved 또는 false_alarm 처리된 건은 제외됩니다.
                </span>
              </span>
            </div>
            {issueQuestions.length > 0 && (
              <Link href="/ops/unresolved" className="text-[11px] text-accent hover:underline font-mono">
                전체 보기 →
              </Link>
            )}
          </div>
        </CardHeader>
        <div className="overflow-hidden">
          <Table>
            <Thead>
              <Th>시각</Th>
              <Th>질문 내용</Th>
              <Th>유형</Th>
              <Th>기관</Th>
              <Th>카테고리</Th>
            </Thead>
            <Tbody>
              {issueQuestions.map((q) => (
                <Tr key={q.questionId}>
                  <Td className="text-xs text-text-muted whitespace-nowrap">
                    {new Date(q.createdAt).toLocaleString("ko-KR", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" })}
                  </Td>
                  <Td className="max-w-xs truncate text-sm">{q.questionText}</Td>
                  <Td>
                    <Badge variant={q.answerStatus === "error" ? "error" : "warning"}>
                      {q.answerStatus === "fallback"  ? "Fallback"  :
                       q.answerStatus === "no_answer" ? "무응답"    :
                       q.answerStatus === "error"     ? "오류"      : q.answerStatus ?? "-"}
                    </Badge>
                  </Td>
                  <Td className="text-xs text-text-secondary whitespace-nowrap">
                    {orgNameMap.get(q.organizationId) ?? q.organizationId}
                  </Td>
                  <Td className="text-xs text-text-secondary">{q.questionCategory ?? "-"}</Td>
                </Tr>
              ))}
              {issueQuestions.length === 0 && (
                <Tr>
                  <Td colSpan={5} className="text-center py-8">
                    <div className="flex flex-col items-center gap-2">
                      <span className="material-symbols-outlined text-success text-2xl">check_circle</span>
                      <span className="text-text-muted text-sm">현재 이슈 질문이 없습니다.</span>
                    </div>
                  </Td>
                </Tr>
              )}
            </Tbody>
          </Table>
        </div>
      </Card>
    </div>
  );
}
