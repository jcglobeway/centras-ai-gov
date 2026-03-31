"use client";

import { useState } from "react";
import Link from "next/link";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, DailyMetric, Question, UnresolvedQuestion, LlmMetrics, Organization } from "@/lib/types";
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
  retrieval:    "#2563eb",
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

// ── 메인 컴포넌트 ─────────────────────────────────────────────────────────────

export default function OpsDashboardPage() {
  const [orgId, setOrgId]               = useState("");
  const [from, setFrom]                 = useState(getWeekFrom);
  const [to, setTo]                     = useState(getToday);
  const [alertDismissed, setAlertDismissed] = useState(false);

  const params = new URLSearchParams({ page_size: "14" });
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

  const { data: questionsData } = useSWR<PagedResponse<Question>>(
    "/api/admin/questions?page_size=30",
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

  const metrics         = data?.items ?? [];
  const latest          = metrics[metrics.length - 1];
  const prev            = metrics.length >= 2 ? metrics[metrics.length - 2] : null;
  const allQuestions    = questionsData?.items ?? [];
  const issueQuestions  = allQuestions
    .filter((q) => ["fallback", "no_answer", "error"].includes(q.answerStatus ?? ""))
    .slice(0, 10);

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
    let healthStatus: "ok" | "warn" | "critical" = "ok";
    let issue = "";
    if      (resolved != null && resolved < 80)   { healthStatus = "critical"; issue = `응답률 ${resolved.toFixed(1)}% (목표 90%)`; }
    else if (fallback != null && fallback >= 15)   { healthStatus = "critical"; issue = `Fallback율 ${fallback.toFixed(1)}% (임계 15%)`; }
    else if (resolved != null && resolved < 90)    { healthStatus = "warn";     issue = `응답률 ${resolved.toFixed(1)}% (목표 90%)`; }
    else if (fallback != null && fallback >= 10)   { healthStatus = "warn";     issue = `Fallback율 ${fallback.toFixed(1)}% (임계 10%)`; }
    return { orgId: oid, orgName: orgNameMap.get(oid) ?? oid, healthStatus, issue, resolved, fallback };
  });

  const healthColor = { ok: "text-success", warn: "text-warning", critical: "text-error" };
  const healthLabel = { ok: "정상",         warn: "주의",         critical: "위험" };
  const healthDot   = { ok: "bg-success",   warn: "bg-warning",   critical: "bg-error" };

  const systemStatus = calcSystemStatus(latest);

  // 파이프라인 레이턴시 실측 연동
  const isRealLatency =
    pipelineData != null &&
    pipelineData.sampleCount > 0 &&
    (pipelineData.avgRetrievalMs != null || pipelineData.avgLlmMs != null);

  const retrievalMs   = isRealLatency ? (pipelineData!.avgRetrievalMs ?? 0) : PIPELINE_SAMPLE.retrievalMs;
  const llmMs         = isRealLatency ? (pipelineData!.avgLlmMs ?? 0)       : PIPELINE_SAMPLE.llmMs;
  const postprocessMs = isRealLatency
    ? Math.max(0, (avgRespMsVal ?? 0) - retrievalMs - llmMs)
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
        <h2 className="text-text-primary font-semibold text-lg">통합 관제</h2>
        <PageFilters
          orgId={orgId} onOrgChange={setOrgId}
          from={from}   onFromChange={setFrom}
          to={to}       onToChange={setTo}
        />
      </div>

      {/* KPI 5개 — 클릭 시 상세 페이지로 이동 */}
      <div className="grid grid-cols-2 lg:grid-cols-5 gap-4">
        <Link href="/ops/quality">
          <KpiCard
            label="ANSWER RATE"
            value={resolvedRateVal != null ? resolvedRateVal.toFixed(1) + "%" : "-"}
            status={getKpiStatus(resolvedRateVal, { ok: (v) => v >= 90, warn: (v) => v >= 80 })}
            progressValue={resolvedRateVal ?? undefined}
            trend={resolvedTrend?.trend}
            trendValue={resolvedTrend?.trendValue}
            help="전체 질문 중 정상 답변 비율. 90% 이상 정상. 클릭하면 상세 페이지로 이동합니다."
          />
        </Link>
        <Link href="/ops/anomaly">
          <KpiCard
            label="ERROR RATE"
            value={fallbackRateVal != null ? fallbackRateVal.toFixed(1) + "%" : "-"}
            status={getKpiStatus(fallbackRateVal, { ok: (v) => v < 10, warn: (v) => v < 15 })}
            progressValue={fallbackRateVal != null ? (fallbackRateVal / 30) * 100 : undefined}
            trend={fallbackTrend?.trend}
            trendValue={fallbackTrend?.trendValue}
            help="Fallback 처리된 비율. 10% 초과 시 파이프라인 점검 필요. 클릭하면 이상 징후 페이지로 이동합니다."
          />
        </Link>
        <Link href="/ops/anomaly">
          <KpiCard
            label="E2E LATENCY"
            value={avgRespMsVal != null ? avgRespMsVal.toLocaleString() + "ms" : "-"}
            status={getKpiStatus(avgRespMsVal, { ok: (v) => v < 1500, warn: (v) => v < 2500 })}
            progressValue={avgRespMsVal != null ? (avgRespMsVal / 5000) * 100 : undefined}
            trend={latencyTrend?.trend}
            trendValue={latencyTrend?.trendValue}
            help="검색 + LLM 생성 + 후처리 전체 E2E 평균 응답시간. 클릭하면 이상 징후 페이지로 이동합니다."
          />
        </Link>
        <Link href="/ops/statistics">
          <KpiCard
            label="KNOWLEDGE GAP"
            value={zeroResultRateVal != null ? zeroResultRateVal.toFixed(1) + "%" : "-"}
            status={getKpiStatus(zeroResultRateVal, { ok: (v) => v < 5, warn: (v) => v < 8 })}
            progressValue={zeroResultRateVal != null ? (zeroResultRateVal / 20) * 100 : undefined}
            trend={gapTrend?.trend}
            trendValue={gapTrend?.trendValue}
            help="벡터 검색에서 관련 문서 없음으로 답변 불가한 비율. 클릭하면 서비스 통계 페이지로 이동합니다."
          />
        </Link>
        <Link href="/ops/cost">
          <KpiCard
            label="COST / QUERY"
            value={avgCostVal != null ? `$${avgCostVal.toFixed(4)}` : "-"}
            status={getKpiStatus(avgCostVal, { ok: (v) => v < 0.008, warn: (v) => v < 0.012 })}
            progressValue={avgCostVal != null ? (avgCostVal / 0.02) * 100 : undefined}
            help="질의 1건당 평균 LLM API 비용. $0.008 미만 정상. 클릭하면 비용 분석 페이지로 이동합니다."
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
                { key: "pipeline",  label: "RAG 파이프라인" },
                { key: "knowledge", label: "지식베이스" },
                { key: "quality",   label: "응답 품질" },
              ].map(({ key, label }) => {
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
                  <p className="font-mono text-[10px] uppercase tracking-widest text-text-muted mb-1">
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
          <h3 className="text-text-primary font-semibold text-sm mb-3 flex items-center gap-2">
            기관 헬스맵
            <span className="font-mono text-[10px] text-text-muted font-normal">
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
                  "bg-bg-elevated border rounded-lg p-4 transition-colors",
                  healthStatus === "critical" ? "border-error/30" :
                  healthStatus === "warn"     ? "border-warning/30" :
                                               "border-white/5"
                )}
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
                <p className="font-mono text-[10px] text-text-muted uppercase tracking-wider mb-2">응답률</p>
                {issue ? (
                  <p className={clsx("text-[11px]", healthColor[healthStatus])}>{issue}</p>
                ) : (
                  <p className="text-[11px] text-success">정상 운영 중</p>
                )}
                {fallback != null && (
                  <p className="font-mono text-[10px] text-text-muted mt-1">
                    Fallback {Number(fallback).toFixed(1)}%
                  </p>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 이슈 알림 로그 */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle tag="ALERT LOG">이슈 질문</CardTitle>
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
                  <Td className="text-xs text-text-secondary">{q.questionCategory ?? "-"}</Td>
                </Tr>
              ))}
              {issueQuestions.length === 0 && (
                <Tr>
                  <Td colSpan={4} className="text-center py-8">
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
