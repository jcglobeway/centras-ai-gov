"use client";

import { useState } from "react";
import Link from "next/link";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, DailyMetric, Question, LlmMetrics, Organization } from "@/lib/types";
import { KpiCard } from "@/components/charts/KpiCard";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Spinner } from "@/components/ui/Spinner";
import { PageFilters, getWeekFrom, getToday } from "@/components/ui/PageFilters";
import { AlertBanner } from "@/components/ui/AlertBanner";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { PageGuide } from "@/components/ui/PageGuide";
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

// ── 파이프라인 단계 비율 (고정, 총합은 실측값으로 교체) ──────────────────────

const PIPELINE_STAGE_RATIOS = [
  { label: "Retrieval",  ratio: 142 / 1184, color: "#2563eb" },
  { label: "Re-ranking", ratio:  38 / 1184, color: "#8b5cf6" },
  { label: "LLM",        ratio: 980 / 1184, color: "#10b981" },
  { label: "후처리",      ratio:  24 / 1184, color: "#f59e0b" },
];
const PIPELINE_SAMPLE_TOTAL = 1184;

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
    "/api/admin/questions?page_size=5",
    fetcher
  );

  const { data: orgsData } = useSWR<PagedResponse<Organization>>(
    "/api/admin/organizations?page_size=50",
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
  const recentQuestions = questionsData?.items ?? [];

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
  const pipelineTotal = avgRespMsVal ?? PIPELINE_SAMPLE_TOTAL;
  const pipelineStages = PIPELINE_STAGE_RATIOS.map((s) => ({
    ...s,
    ms: Math.round(s.ratio * pipelineTotal),
  }));
  const isRealLatency = avgRespMsVal != null;

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

      {/* 시스템 상태 신호등 + 파이프라인 레이턴시 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">

        {/* 시스템 상태 신호등 — 실수치 표시 */}
        <Card>
          <CardHeader>
            <CardTitle>시스템 상태</CardTitle>
          </CardHeader>
          <div className="px-4 pb-4 space-y-2">
            {[
              {
                key: "pipeline",
                label: "RAG 파이프라인",
                metricLabel: "Fallback",
                metricValue: fallbackRateVal != null ? fallbackRateVal.toFixed(1) + "%" : null,
              },
              {
                key: "knowledge",
                label: "지식베이스",
                metricLabel: "Knowledge Gap",
                metricValue: zeroResultRateVal != null ? zeroResultRateVal.toFixed(1) + "%" : null,
              },
              {
                key: "quality",
                label: "응답 품질",
                metricLabel: "응답률",
                metricValue: resolvedRateVal != null ? resolvedRateVal.toFixed(1) + "%" : null,
              },
            ].map(({ key, label, metricLabel, metricValue }) => {
              const st = systemStatus[key] as SystemStatus;
              return (
                <div
                  key={key}
                  className={clsx(
                    "flex items-center justify-between px-3 py-2.5 rounded-lg",
                    STATUS_BG[st]
                  )}
                >
                  <div className="flex items-center gap-2.5">
                    <div className={clsx("w-2 h-2 rounded-full shrink-0", STATUS_DOT[st])} />
                    <span className="text-sm font-medium">{label}</span>
                  </div>
                  <div className="flex items-center gap-3">
                    {metricValue != null && (
                      <span className="font-mono text-[11px] text-current opacity-70">
                        {metricLabel} {metricValue}
                      </span>
                    )}
                    <span className="font-mono text-[11px] font-semibold">
                      {STATUS_LABEL[st]}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        </Card>

        {/* 파이프라인 레이턴시 바 — 실측 총합 연동 */}
        <Card>
          <CardHeader>
            <CardTitle tag={isRealLatency ? "ACTUAL" : "SAMPLE"}>파이프라인 레이턴시</CardTitle>
          </CardHeader>
          <div className="px-4 pb-4">
            {/* 누적 수평 바 */}
            <div className="h-9 w-full flex rounded-md overflow-hidden ring-1 ring-white/5 mb-1">
              {pipelineStages.map((stage) => {
                const pct = stage.ratio * 100;
                return (
                  <div
                    key={stage.label}
                    className="h-full flex items-center justify-center border-r border-black/20 last:border-0 transition-all"
                    style={{ width: `${pct}%`, backgroundColor: stage.color }}
                    title={`${stage.label}: ${stage.ms}ms`}
                  >
                    {pct >= 12 && (
                      <span className="font-mono text-[10px] font-bold text-white drop-shadow">
                        {stage.ms}ms
                      </span>
                    )}
                  </div>
                );
              })}
            </div>
            {/* 레이블 행 */}
            <div className="flex w-full text-[9px] font-mono text-text-muted uppercase mt-2">
              {pipelineStages.map((stage) => {
                const pct = stage.ratio * 100;
                return (
                  <div
                    key={stage.label}
                    className="text-center truncate"
                    style={{ width: `${pct}%` }}
                  >
                    {stage.label}
                  </div>
                );
              })}
            </div>
            {/* 범례 */}
            <div className="flex items-center gap-4 mt-3 flex-wrap">
              {pipelineStages.map((stage) => (
                <div key={stage.label} className="flex items-center gap-1.5">
                  <div className="w-2 h-2 rounded-full" style={{ backgroundColor: stage.color }} />
                  <span className="font-mono text-[10px] text-text-muted">
                    {stage.label} <span className="text-text-secondary">{stage.ms}ms</span>
                  </span>
                </div>
              ))}
            </div>
            <p className="text-[10px] text-text-muted mt-3">
              {isRealLatency
                ? `※ E2E 실측: ${pipelineTotal.toLocaleString()}ms / 단계별 비율 추정 · 합계 ${pipelineTotal.toLocaleString()}ms`
                : `※ 샘플 데이터 — 실측 API 연동 예정 · 합계 ${PIPELINE_SAMPLE_TOTAL}ms`
              }
            </p>
          </div>
        </Card>
      </div>

      {/* 기관 헬스맵 — 기관명 + 핵심 수치 항상 노출 */}
      {orgHealthList.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>기관 헬스맵</CardTitle>
          </CardHeader>
          <div className="px-4 pb-4 divide-y divide-bg-border">
            {orgHealthList.map(({ orgId: oid, orgName, healthStatus, issue, resolved, fallback }) => (
              <div key={oid} className="flex items-center justify-between py-2.5">
                <div className="flex items-center gap-2.5 min-w-0">
                  <div className={`w-2 h-2 rounded-full shrink-0 ${healthDot[healthStatus]}`} />
                  <div className="min-w-0">
                    <span className="text-text-primary text-sm font-medium block truncate">{orgName}</span>
                    <span className="text-text-muted text-[10px] font-mono">{oid}</span>
                  </div>
                  {issue && (
                    <span className="text-text-muted text-xs ml-2 hidden sm:block">{issue}</span>
                  )}
                </div>
                <div className="flex items-center gap-4 shrink-0 ml-4">
                  <div className="text-right hidden sm:block">
                    {resolved != null && (
                      <span className="font-mono text-[11px] text-text-secondary">
                        응답률 {Number(resolved).toFixed(1)}%
                      </span>
                    )}
                    {fallback != null && (
                      <span className="font-mono text-[11px] text-text-muted ml-2">
                        / Fallback {Number(fallback).toFixed(1)}%
                      </span>
                    )}
                  </div>
                  <span className={`font-mono text-[11px] font-semibold ${healthColor[healthStatus]}`}>
                    {healthLabel[healthStatus]}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </Card>
      )}

      {/* 최근 질문 테이블 */}
      <Card>
        <CardHeader>
          <CardTitle>최근 질문 (5건)</CardTitle>
        </CardHeader>
        <div className="overflow-hidden">
          <Table>
            <Thead>
              <Th>내용</Th>
              <Th>카테고리</Th>
              <Th>생성일</Th>
            </Thead>
            <Tbody>
              {recentQuestions.map((q) => (
                <Tr key={q.questionId}>
                  <Td className="max-w-xs truncate text-sm">{q.questionText}</Td>
                  <Td className="text-xs text-text-secondary">{q.questionCategory ?? "-"}</Td>
                  <Td className="text-xs text-text-muted">
                    {new Date(q.createdAt).toLocaleString("ko-KR")}
                  </Td>
                </Tr>
              ))}
              {recentQuestions.length === 0 && (
                <Tr>
                  <Td colSpan={3} className="text-center text-text-muted text-sm py-6">
                    질문 데이터가 없습니다.
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
