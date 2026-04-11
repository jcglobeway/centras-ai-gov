"use client";

import { useState } from "react";
import useSWR from "swr";
import {
  LineChart, Line, BarChart, Bar,
  XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, Legend,
} from "recharts";
import { getISOWeek, parseISO } from "date-fns";
import { fetcher } from "@/lib/api";
import type {
  PagedResponse, DailyMetric, RagasEvaluationSummaryResponse,
  RagasEvaluationPeriodSummary, Organization,
} from "@/lib/types";
import { KpiCard } from "@/components/charts/KpiCard";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { PageGuide } from "@/components/ui/PageGuide";
import { Spinner } from "@/components/ui/Spinner";
import { useFilter } from "@/lib/filter-context";

// ── 타입 ──────────────────────────────────────────────────────────────────────

interface AggregatedPeriod {
  label: string;
  totalQuestions: number;
  totalSessions: number;
  resolvedRate: number | null;
  fallbackRate: number | null;
  zeroResultRate: number | null;
  knowledgeGapCount: number;
  unansweredCount: number;
}

interface OrgRow {
  organizationId: string;
  totalQuestions: number;
  resolvedRate: number | null;
  unansweredCount: number;
  knowledgeGapCount: number;
}

interface DeltaScoreRow {
  label: string;
  value: number | null;
  prev: number | null;
  target: number;
}

interface FeedbackTrendItem { date: string; positive: number; negative: number; }
interface FeedbackTrendResponse { items: FeedbackTrendItem[]; }

// ── 집계 헬퍼 ─────────────────────────────────────────────────────────────────

function getWeekLabel(dateStr: string): string {
  return `W${String(getISOWeek(parseISO(dateStr))).padStart(2, "0")}`;
}

function aggregateByPeriod(items: DailyMetric[], period: "weekly" | "monthly"): AggregatedPeriod[] {
  const groupKey = period === "weekly"
    ? (s: string) => getWeekLabel(s)
    : (s: string) => s.slice(0, 7);

  const map = new Map<string, {
    totalQuestions: number; totalSessions: number;
    weightedResolved: number; weightedFallback: number; weightedZero: number;
    knowledgeGapCount: number; unansweredCount: number;
  }>();

  for (const m of items) {
    const key = groupKey(m.metricDate);
    const g = map.get(key) ?? {
      totalQuestions: 0, totalSessions: 0,
      weightedResolved: 0, weightedFallback: 0, weightedZero: 0,
      knowledgeGapCount: 0, unansweredCount: 0,
    };
    g.totalQuestions   += m.totalQuestions;
    g.totalSessions    += m.totalSessions;
    g.weightedResolved += (m.resolvedRate   ?? 0) * m.totalQuestions;
    g.weightedFallback += (m.fallbackRate   ?? 0) * m.totalQuestions;
    g.weightedZero     += (m.zeroResultRate ?? 0) * m.totalQuestions;
    g.knowledgeGapCount += m.knowledgeGapCount;
    g.unansweredCount   += m.unansweredCount;
    map.set(key, g);
  }

  return Array.from(map.entries())
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([label, g]) => ({
      label,
      totalQuestions: g.totalQuestions,
      totalSessions:  g.totalSessions,
      resolvedRate:   g.totalQuestions > 0 ? g.weightedResolved / g.totalQuestions : null,
      fallbackRate:   g.totalQuestions > 0 ? g.weightedFallback / g.totalQuestions : null,
      zeroResultRate: g.totalQuestions > 0 ? g.weightedZero     / g.totalQuestions : null,
      knowledgeGapCount: g.knowledgeGapCount,
      unansweredCount:   g.unansweredCount,
    }));
}

function aggregateByOrg(items: DailyMetric[]): OrgRow[] {
  const map = new Map<string, {
    totalQuestions: number; weightedResolved: number;
    unansweredCount: number; knowledgeGapCount: number;
  }>();
  for (const m of items) {
    const g = map.get(m.organizationId) ?? {
      totalQuestions: 0, weightedResolved: 0, unansweredCount: 0, knowledgeGapCount: 0,
    };
    g.totalQuestions   += m.totalQuestions;
    g.weightedResolved += (m.resolvedRate ?? 0) * m.totalQuestions;
    g.unansweredCount  += m.unansweredCount;
    g.knowledgeGapCount += m.knowledgeGapCount;
    map.set(m.organizationId, g);
  }
  return Array.from(map.entries())
    .map(([id, g]) => ({
      organizationId: id,
      totalQuestions: g.totalQuestions,
      resolvedRate:   g.totalQuestions > 0 ? g.weightedResolved / g.totalQuestions : null,
      unansweredCount:   g.unansweredCount,
      knowledgeGapCount: g.knowledgeGapCount,
    }))
    .sort((a, b) => b.totalQuestions - a.totalQuestions);
}

// ── KPI 상태 ──────────────────────────────────────────────────────────────────

function getKpiStatus(
  value: number | null,
  thresholds: { ok: (v: number) => boolean; warn: (v: number) => boolean }
): "ok" | "warn" | "critical" | undefined {
  if (value == null) return undefined;
  return thresholds.ok(value) ? "ok" : thresholds.warn(value) ? "warn" : "critical";
}

// ── 해석 문장 생성 ─────────────────────────────────────────────────────────────

function interpretKpi(resolvedRate: number | null, zeroResultRate: number | null, faithfulness: number | null, totalQ: number): string {
  const parts: string[] = [];
  if (totalQ === 0) return "해당 기간 내 질의 데이터가 없어 분석이 불가합니다.";
  if (resolvedRate != null) {
    if (resolvedRate >= 90)
      parts.push(`세션 성공률이 ${resolvedRate.toFixed(1)}%로 목표치(90%)를 달성하였습니다.`);
    else if (resolvedRate >= 80)
      parts.push(`세션 성공률이 ${resolvedRate.toFixed(1)}%로 목표치(90%) 대비 ${(90 - resolvedRate).toFixed(1)}%p 미달입니다. 지식베이스 보강 및 파이프라인 점검이 권고됩니다.`);
    else
      parts.push(`세션 성공률이 ${resolvedRate.toFixed(1)}%로 위험 수준입니다. 즉각적인 원인 분석 및 개선 조치가 필요합니다.`);
  }
  if (zeroResultRate != null) {
    if (zeroResultRate < 5)
      parts.push(`Knowledge Gap율은 ${zeroResultRate.toFixed(1)}%로 정상 범위 이내로, 현재 지식베이스가 시민 질의를 충분히 커버하고 있습니다.`);
    else if (zeroResultRate < 8)
      parts.push(`Knowledge Gap율이 ${zeroResultRate.toFixed(1)}%로 주의 수준입니다. 관련 문서 보강을 권고합니다.`);
    else
      parts.push(`Knowledge Gap율이 ${zeroResultRate.toFixed(1)}%로 위험 수준입니다. 미응답 질의 분석을 통한 긴급 문서 보강이 필요합니다.`);
  }
  if (faithfulness != null) {
    const pct = faithfulness * 100;
    if (pct >= 90)
      parts.push(`Faithfulness는 ${pct.toFixed(1)}%로 AI 답변의 문서 충실도가 양호합니다.`);
    else if (pct >= 80)
      parts.push(`Faithfulness가 ${pct.toFixed(1)}%로 목표(90%) 미달입니다. 프롬프트 최적화 및 유사도 임계값 조정을 권고합니다.`);
    else
      parts.push(`Faithfulness가 ${pct.toFixed(1)}%로 위험 수준입니다. 환각(Hallucination) 위험이 높으며 즉각적인 점검이 필요합니다.`);
  }
  return parts.join(" ");
}

function interpretTrend(trendData: AggregatedPeriod[], period: "weekly" | "monthly"): string {
  if (trendData.length < 2) return "추이 분석을 위한 충분한 데이터가 없습니다.";
  const first = trendData[0];
  const last  = trendData[trendData.length - 1];
  const rDelta = (last.resolvedRate ?? 0) - (first.resolvedRate ?? 0);
  const fDelta = (last.fallbackRate ?? 0) - (first.fallbackRate ?? 0);
  const unit = period === "weekly" ? "주" : "개월";
  const trend = rDelta >= 0 && fDelta <= 0
    ? "전반적으로 서비스 품질이 개선되는 추세입니다."
    : rDelta < 0
    ? "세션 성공률이 하락하고 있어 지속적인 모니터링과 개선 조치가 필요합니다."
    : "일부 지표가 혼조세를 보이고 있어 면밀한 분석이 필요합니다.";
  return `분석 기간(${trendData.length}${unit}) 동안 세션 성공률은 ${(first.resolvedRate ?? 0).toFixed(1)}%에서 ${(last.resolvedRate ?? 0).toFixed(1)}%로 ${rDelta >= 0 ? "+" : ""}${rDelta.toFixed(1)}%p 변동하였으며, Fallback율은 ${fDelta >= 0 ? "+" : ""}${fDelta.toFixed(1)}%p 변동하였습니다. ${trend}`;
}

function interpretOrg(orgRows: OrgRow[]): string {
  if (orgRows.length === 0) return "기관별 데이터가 없습니다.";
  if (orgRows.length === 1) {
    const r = orgRows[0];
    return `${r.organizationId} 기관의 총 질의 수는 ${r.totalQuestions.toLocaleString()}건이며, 세션 성공률은 ${r.resolvedRate != null ? r.resolvedRate.toFixed(1) + "%" : "집계 불가"}입니다.`;
  }
  const best  = [...orgRows].sort((a, b) => (b.resolvedRate ?? 0) - (a.resolvedRate ?? 0))[0];
  const worst = [...orgRows].sort((a, b) => (a.resolvedRate ?? 100) - (b.resolvedRate ?? 100))[0];
  const topGap = [...orgRows].sort((a, b) => b.knowledgeGapCount - a.knowledgeGapCount)[0];
  let text = `총 ${orgRows.length}개 기관을 분석하였습니다. 세션 성공률이 가장 높은 기관은 ${best.organizationId}(${(best.resolvedRate ?? 0).toFixed(1)}%)이며, ${worst.organizationId}(${(worst.resolvedRate ?? 0).toFixed(1)}%)는 가장 낮은 성과를 보였습니다.`;
  if (topGap.knowledgeGapCount > 0)
    text += ` Knowledge Gap 발생이 가장 많은 기관은 ${topGap.organizationId}(${topGap.knowledgeGapCount}건)으로, 해당 기관의 지식베이스 보강을 우선 권고합니다.`;
  return text;
}

function interpretRagas(current: RagasEvaluationPeriodSummary | null, previous: RagasEvaluationPeriodSummary | null): string {
  if (!current) return "RAGAS 평가 데이터가 없습니다. 평가를 실행한 후 재확인하시기 바랍니다.";

  const metrics = [
    { key: "Faithfulness",      cur: current.avgFaithfulness,     prev: previous?.avgFaithfulness,     target: 0.90, okMsg: "AI 답변의 문서 충실도가 양호한 수준으로, 환각(Hallucination) 위험이 낮습니다.", warnMsg: "목표(90%) 미달로, 프롬프트 최적화 및 RAG 유사도 임계값 조정을 권고합니다.", critMsg: "환각 위험이 높은 위험 수준으로 즉각적인 점검이 필요합니다." },
    { key: "Answer Relevancy",  cur: current.avgAnswerRelevancy,  prev: previous?.avgAnswerRelevancy,  target: 0.85, okMsg: "AI 답변이 시민 질문 의도에 충실히 부합하고 있습니다.", warnMsg: "목표(85%) 미달로, 검색 파라미터 및 답변 생성 프롬프트 개선이 필요합니다.", critMsg: "답변과 질문 간 관련성이 낮아 서비스 품질 전반에 대한 점검이 필요합니다." },
    { key: "Context Precision", cur: current.avgContextPrecision, prev: previous?.avgContextPrecision, target: 0.70, okMsg: "검색된 문서 중 실제로 유용한 문서 비율이 목표를 달성하였습니다.", warnMsg: "목표(70%) 미달로, 검색 쿼리 처리 및 재랭킹 설정 점검을 권고합니다.", critMsg: "검색 정밀도가 낮아 불필요한 문서가 답변 생성에 과다 개입될 위험이 있습니다." },
    { key: "Context Recall",    cur: current.avgContextRecall,    prev: previous?.avgContextRecall,    target: 0.75, okMsg: "정답 도출에 필요한 핵심 문서가 충분히 검색되고 있습니다.", warnMsg: "목표(75%) 미달로, 지식베이스 색인 범위 및 청크 분할 전략 재검토를 권고합니다.", critMsg: "필수 문서가 누락되어 답변 완전성이 크게 저하될 위험이 있습니다." },
  ] as const;

  const parts: string[] = [];
  for (const m of metrics) {
    if (m.cur == null) continue;
    const pct   = m.cur * 100;
    const prev  = m.prev != null ? m.prev * 100 : null;
    const delta = prev != null ? pct - prev : null;
    const deltaStr = delta != null ? `(전기 대비 ${delta >= 0 ? "+" : ""}${delta.toFixed(1)}%p)` : "";
    const evaluation = pct >= m.target * 100 ? m.okMsg : pct >= m.target * 100 * 0.9 ? m.warnMsg : m.critMsg;
    parts.push(`${m.key}는 ${pct.toFixed(1)}% ${deltaStr}: ${evaluation}`);
  }
  return parts.join("\n") || "평가 수치 없음.";
}

function interpretFeedback(items: FeedbackTrendItem[]): string {
  if (items.length === 0) return "피드백 데이터가 없습니다.";
  const total    = items.reduce((s, d) => s + d.positive + d.negative, 0);
  const positive = items.reduce((s, d) => s + d.positive, 0);
  const rate     = total > 0 ? (positive / total) * 100 : 0;
  const recent   = items.slice(-7);
  const recentPos = recent.reduce((s, d) => s + d.positive, 0);
  const recentTot = recent.reduce((s, d) => s + d.positive + d.negative, 0);
  const recentRate = recentTot > 0 ? (recentPos / recentTot) * 100 : 0;
  const trend = recentRate > rate ? "상승" : recentRate < rate ? "하락" : "유지";
  return `최근 30일간 총 ${total}건의 피드백이 수집되었으며, 긍정 비율은 ${rate.toFixed(1)}%입니다. 최근 7일 기준 긍정 비율은 ${recentRate.toFixed(1)}%로 ${trend} 추세입니다. ${rate >= 70 ? "전반적으로 시민 만족도가 양호한 수준입니다." : rate >= 50 ? "만족도 개선을 위해 미응답 질의 분석이 권고됩니다." : "사용자 만족도가 낮은 수준으로, 서비스 품질 전반에 대한 점검이 필요합니다."}`;
}

// ── ScoreBar (화면용) ──────────────────────────────────────────────────────────

function ScoreBar({ row }: { row: DeltaScoreRow }) {
  const pct       = row.value != null ? Math.min(row.value * 100, 100) : 0;
  const targetPct = Math.min(row.target * 100, 100);
  const delta     = row.value != null && row.prev != null ? row.value - row.prev : null;
  const ok        = row.value != null && row.value >= row.target;
  const near      = row.value != null && !ok && row.value >= row.target * 0.9;
  const barColor  = ok ? "bg-success" : near ? "bg-warning" : "bg-error";
  const statusCls = ok ? "text-success bg-success/10 border-success/20"
    : near ? "text-warning bg-warning/10 border-warning/20"
    : "text-error bg-error/10 border-error/20";

  return (
    <div className="space-y-1.5">
      <div className="flex items-center justify-between">
        <span className="text-xs text-text-primary font-medium">{row.label}</span>
        <div className="flex items-center gap-2">
          {delta != null && (
            <span className={`text-[10px] font-mono ${delta >= 0 ? "text-success" : "text-error"}`}>
              {delta >= 0 ? "+" : ""}{(delta * 100).toFixed(1)}pp
            </span>
          )}
          <span className="font-mono text-sm text-text-primary tabular-nums">
            {row.value != null ? (row.value * 100).toFixed(1) + "%" : "–"}
          </span>
          <span className={`text-[10px] font-semibold border rounded px-1.5 py-0.5 ${statusCls}`}>
            {row.value == null ? "N/A" : ok ? "달성" : near ? "근접" : "미달"}
          </span>
        </div>
      </div>
      <div className="relative h-2 rounded-full bg-bg-prominent overflow-visible">
        <div className={`h-full rounded-full transition-all ${barColor}`} style={{ width: `${pct}%` }} />
        <div className="absolute top-1/2 -translate-y-1/2 w-0.5 h-4 bg-white/30 rounded-full" style={{ left: `${targetPct}%` }} />
      </div>
      <p className="text-[10px] text-text-muted text-right">목표 {(row.target * 100).toFixed(0)}%</p>
    </div>
  );
}

// ── 인쇄 전용 공공기관 보고서 ────────────────────────────────────────────────────

const PS: Record<string, React.CSSProperties> = {
  page:     { fontFamily: "'Malgun Gothic', '맑은 고딕', sans-serif", color: "#111", background: "#fff", fontSize: 11 },
  h1:       { fontSize: 20, fontWeight: 700, textAlign: "center", marginBottom: 4, letterSpacing: "-0.5px" },
  subtitle: { fontSize: 12, textAlign: "center", color: "#555", marginBottom: 16 },
  divider:  { borderTop: "2px solid #1a2a5e", margin: "12px 0" },
  dividerThin: { borderTop: "1px solid #ccc", margin: "10px 0" },
  sectionTitle: { fontSize: 13, fontWeight: 700, color: "#1a2a5e", marginBottom: 6, marginTop: 14, borderLeft: "4px solid #1a2a5e", paddingLeft: 8 },
  label:    { fontSize: 10, color: "#555", marginBottom: 2 },
  analysis: { fontSize: 10.5, color: "#222", lineHeight: 1.75, background: "#f8f9fc", border: "1px solid #dde1ea", borderRadius: 4, padding: "8px 10px", marginTop: 8 },
  kpiGrid:  { display: "grid", gridTemplateColumns: "1fr 1fr 1fr 1fr", gap: 8, marginBottom: 8 },
  kpiBox:   { border: "1px solid #cdd3e0", borderRadius: 4, padding: "8px 10px", textAlign: "center" as const, background: "#fafbfe" },
  kpiVal:   { fontSize: 20, fontWeight: 700, color: "#1a2a5e" },
  kpiLbl:   { fontSize: 9.5, color: "#555", marginTop: 2 },
  kpiTarget:{ fontSize: 9, color: "#888", marginTop: 1 },
  kpiStatus:{ fontSize: 9, fontWeight: 700, marginTop: 3 },
  table:    { width: "100%", borderCollapse: "collapse" as const, fontSize: 10.5, marginTop: 6 },
  th:       { background: "#1a2a5e", color: "#fff", padding: "5px 8px", textAlign: "left" as const, fontWeight: 600, fontSize: 10 },
  td:       { padding: "4px 8px", borderBottom: "1px solid #e5e7ec", verticalAlign: "top" as const },
  tdAlt:    { padding: "4px 8px", borderBottom: "1px solid #e5e7ec", background: "#f5f7fb", verticalAlign: "top" as const },
  footer:   { marginTop: 20, borderTop: "1px solid #ccc", paddingTop: 8, fontSize: 9, color: "#888", textAlign: "center" as const },
  pageBreak:{ pageBreakBefore: "always" as const },
};

function statusLabel(value: number | null, ok: (v: number) => boolean, warn: (v: number) => boolean): { text: string; color: string } {
  if (value == null) return { text: "집계 불가", color: "#888" };
  if (ok(value))   return { text: "정상",  color: "#166534" };
  if (warn(value)) return { text: "주의",  color: "#92400e" };
  return { text: "위험", color: "#991b1b" };
}

interface PrintReportProps {
  period: "weekly" | "monthly";
  fromDate: string;
  today: string;
  orgId: string;
  orgName: string;
  totalQ: number;
  resolvedRateKpi: number | null;
  zeroResultRateKpi: number | null;
  faithfulness: number | null;
  trendData: AggregatedPeriod[];
  orgRows: OrgRow[];
  ragasData: RagasEvaluationSummaryResponse | undefined;
  feedbackItems: FeedbackTrendItem[];
}

function PrintReport({
  period, fromDate, today, orgId, orgName,
  totalQ, resolvedRateKpi, zeroResultRateKpi, faithfulness,
  trendData, orgRows, ragasData, feedbackItems,
}: PrintReportProps) {
  const now = new Date().toLocaleString("ko-KR", { year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" });
  const periodLabel = period === "weekly" ? `최근 8주 (주간)` : `최근 6개월 (월간)`;
  const current  = ragasData?.current  ?? null;
  const previous = ragasData?.previous ?? null;

  const rSt  = statusLabel(resolvedRateKpi,   (v) => v >= 90, (v) => v >= 80);
  const kgSt = statusLabel(zeroResultRateKpi, (v) => v < 5,  (v) => v < 8);
  const fSt  = statusLabel(faithfulness != null ? faithfulness * 100 : null, (v) => v >= 90, (v) => v >= 80);

  // 피드백 집계
  const totalFb  = feedbackItems.reduce((s, d) => s + d.positive + d.negative, 0);
  const posFb    = feedbackItems.reduce((s, d) => s + d.positive, 0);
  const posRate  = totalFb > 0 ? (posFb / totalFb) * 100 : null;

  return (
    <div style={PS.page}>
      {/* ── 표지 헤더 ─────────────────────────────────────────────── */}
      <div style={{ textAlign: "center", paddingBottom: 12, borderBottom: "3px solid #1a2a5e" }}>
        {orgName && orgName !== "전체" && (
          <div style={{ fontSize: 13, color: "#1a2a5e", fontWeight: 700, marginBottom: 6, letterSpacing: 1 }}>
            {orgName}
          </div>
        )}
        <div style={PS.h1}>인공지능 기반 민원상담 서비스</div>
        <div style={PS.h1}>성과 분석 보고서</div>
        <div style={PS.subtitle}>{periodLabel} · 분석 기준일: {now}</div>
      </div>

      {/* ── 분석 개요 ─────────────────────────────────────────────── */}
      <table style={{ width: "100%", marginTop: 10, fontSize: 10.5, borderCollapse: "collapse" }}>
        <tbody>
          <tr>
            <td style={{ width: "25%", color: "#555", padding: "2px 0" }}>분석 기간</td>
            <td style={{ fontWeight: 600 }}>{fromDate} ~ {today}</td>
            <td style={{ width: "20%", color: "#555" }}>분석 주기</td>
            <td style={{ fontWeight: 600 }}>{period === "weekly" ? "주간" : "월간"}</td>
          </tr>
          <tr>
            <td style={{ color: "#555", padding: "2px 0" }}>대상 기관</td>
            <td style={{ fontWeight: 600 }}>{orgName}{orgId && orgName !== orgId ? ` (${orgId})` : ""}</td>
            <td style={{ color: "#555" }}>총 질의 수</td>
            <td style={{ fontWeight: 600 }}>{totalQ.toLocaleString()}건</td>
          </tr>
        </tbody>
      </table>

      <div style={PS.divider} />

      {/* ── 1. 핵심 성과 지표 ────────────────────────────────────── */}
      <div style={PS.sectionTitle}>1. 핵심 성과 지표 (KPI) 요약</div>

      <div style={PS.kpiGrid}>
        {/* 총 질의 수 */}
        <div style={PS.kpiBox}>
          <div style={{ ...PS.kpiVal, fontSize: 22 }}>{totalQ > 0 ? totalQ.toLocaleString() : "–"}</div>
          <div style={PS.kpiLbl}>총 질의 수</div>
          <div style={PS.kpiTarget}>선택 기간 합산</div>
        </div>
        {/* 세션 성공률 */}
        <div style={{ ...PS.kpiBox, borderColor: rSt.color + "66" }}>
          <div style={{ ...PS.kpiVal, color: rSt.color }}>{resolvedRateKpi != null ? resolvedRateKpi.toFixed(1) + "%" : "–"}</div>
          <div style={PS.kpiLbl}>세션 성공률</div>
          <div style={PS.kpiTarget}>목표 ≥ 90%</div>
          <div style={{ ...PS.kpiStatus, color: rSt.color }}>[ {rSt.text} ]</div>
        </div>
        {/* Knowledge Gap */}
        <div style={{ ...PS.kpiBox, borderColor: kgSt.color + "66" }}>
          <div style={{ ...PS.kpiVal, color: kgSt.color }}>{zeroResultRateKpi != null ? zeroResultRateKpi.toFixed(1) + "%" : "–"}</div>
          <div style={PS.kpiLbl}>Knowledge Gap율</div>
          <div style={PS.kpiTarget}>목표 &lt; 5%</div>
          <div style={{ ...PS.kpiStatus, color: kgSt.color }}>[ {kgSt.text} ]</div>
        </div>
        {/* Faithfulness */}
        <div style={{ ...PS.kpiBox, borderColor: fSt.color + "66" }}>
          <div style={{ ...PS.kpiVal, color: fSt.color }}>{faithfulness != null ? (faithfulness * 100).toFixed(1) + "%" : "–"}</div>
          <div style={PS.kpiLbl}>Faithfulness</div>
          <div style={PS.kpiTarget}>목표 ≥ 90%</div>
          <div style={{ ...PS.kpiStatus, color: fSt.color }}>[ {fSt.text} ]</div>
        </div>
      </div>

      <div style={PS.analysis}>
        <strong>■ 분석 의견</strong><br />
        {interpretKpi(resolvedRateKpi, zeroResultRateKpi, faithfulness, totalQ)}
      </div>

      <div style={PS.dividerThin} />

      {/* ── 2. 기간별 추이 ────────────────────────────────────────── */}
      <div style={PS.sectionTitle}>2. {period === "weekly" ? "주간" : "월간"} 품질 지표 추이</div>

      {trendData.length > 0 ? (
        <table style={PS.table}>
          <thead>
            <tr>
              <th style={PS.th}>기간</th>
              <th style={{ ...PS.th, textAlign: "right" }}>질의 수</th>
              <th style={{ ...PS.th, textAlign: "right" }}>세션 성공률</th>
              <th style={{ ...PS.th, textAlign: "right" }}>Fallback율</th>
              <th style={{ ...PS.th, textAlign: "right" }}>미응답 건수</th>
            </tr>
          </thead>
          <tbody>
            {trendData.map((row, i) => (
              <tr key={row.label}>
                <td style={i % 2 === 0 ? PS.td : PS.tdAlt}>{row.label}</td>
                <td style={{ ...(i % 2 === 0 ? PS.td : PS.tdAlt), textAlign: "right" }}>{row.totalQuestions.toLocaleString()}</td>
                <td style={{ ...(i % 2 === 0 ? PS.td : PS.tdAlt), textAlign: "right", fontWeight: 600, color: (row.resolvedRate ?? 0) >= 90 ? "#166534" : (row.resolvedRate ?? 0) >= 80 ? "#92400e" : "#991b1b" }}>
                  {row.resolvedRate != null ? row.resolvedRate.toFixed(1) + "%" : "–"}
                </td>
                <td style={{ ...(i % 2 === 0 ? PS.td : PS.tdAlt), textAlign: "right" }}>
                  {row.fallbackRate != null ? row.fallbackRate.toFixed(1) + "%" : "–"}
                </td>
                <td style={{ ...(i % 2 === 0 ? PS.td : PS.tdAlt), textAlign: "right" }}>{row.unansweredCount}</td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        <p style={{ color: "#888", fontSize: 10.5 }}>데이터가 없습니다.</p>
      )}

      <div style={PS.analysis}>
        <strong>■ 분석 의견</strong><br />
        {interpretTrend(trendData, period)}
      </div>

      <div style={PS.dividerThin} />

      {/* ── 3. 기관별 성과 ────────────────────────────────────────── */}
      <div style={PS.sectionTitle}>3. 기관별 성과 분석</div>

      {orgRows.length > 0 ? (
        <table style={PS.table}>
          <thead>
            <tr>
              <th style={PS.th}>기관 ID</th>
              <th style={{ ...PS.th, textAlign: "right" }}>질의 수</th>
              <th style={{ ...PS.th, textAlign: "right" }}>세션 성공률</th>
              <th style={{ ...PS.th, textAlign: "right" }}>미응답 건수</th>
              <th style={{ ...PS.th, textAlign: "right" }}>Knowledge Gap</th>
            </tr>
          </thead>
          <tbody>
            {orgRows.map((row, i) => {
              const st = statusLabel(row.resolvedRate, (v) => v >= 90, (v) => v >= 80);
              return (
                <tr key={row.organizationId}>
                  <td style={i % 2 === 0 ? PS.td : PS.tdAlt}>{row.organizationId}</td>
                  <td style={{ ...(i % 2 === 0 ? PS.td : PS.tdAlt), textAlign: "right" }}>{row.totalQuestions.toLocaleString()}</td>
                  <td style={{ ...(i % 2 === 0 ? PS.td : PS.tdAlt), textAlign: "right", fontWeight: 600, color: st.color }}>
                    {row.resolvedRate != null ? row.resolvedRate.toFixed(1) + "%" : "–"}
                  </td>
                  <td style={{ ...(i % 2 === 0 ? PS.td : PS.tdAlt), textAlign: "right" }}>{row.unansweredCount}</td>
                  <td style={{ ...(i % 2 === 0 ? PS.td : PS.tdAlt), textAlign: "right" }}>{row.knowledgeGapCount}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      ) : (
        <p style={{ color: "#888", fontSize: 10.5 }}>데이터가 없습니다.</p>
      )}

      <div style={PS.analysis}>
        <strong>■ 분석 의견</strong><br />
        {interpretOrg(orgRows)}
      </div>

      {/* ── 4. RAG 품질 평가 ──────────────────────────────────────── */}
      <div style={PS.sectionTitle}>4. RAG 품질 평가 (RAGAS)</div>

      {current ? (
        <>
          <table style={PS.table}>
            <thead>
              <tr>
                <th style={PS.th}>지표</th>
                <th style={{ ...PS.th, textAlign: "right" }}>현재 기간</th>
                <th style={{ ...PS.th, textAlign: "right" }}>이전 기간</th>
                <th style={{ ...PS.th, textAlign: "right" }}>증감</th>
                <th style={{ ...PS.th, textAlign: "right" }}>목표</th>
                <th style={PS.th}>달성 여부</th>
              </tr>
            </thead>
            <tbody>
              {([
                { label: "Faithfulness",    cur: current.avgFaithfulness,    prev: previous?.avgFaithfulness,    target: 0.90 },
                { label: "Answer Relevancy", cur: current.avgAnswerRelevancy, prev: previous?.avgAnswerRelevancy, target: 0.85 },
                { label: "Context Precision", cur: current.avgContextPrecision, prev: previous?.avgContextPrecision, target: 0.70 },
                { label: "Context Recall",   cur: current.avgContextRecall,   prev: previous?.avgContextRecall,   target: 0.75 },
              ] as const).map((row, i) => {
                const curPct  = row.cur  != null ? row.cur  * 100 : null;
                const prevPct = (row.prev ?? null) != null ? (row.prev! * 100) : null;
                const delta   = curPct != null && prevPct != null ? curPct - prevPct : null;
                const achieved = curPct != null && curPct >= row.target * 100;
                return (
                  <tr key={row.label}>
                    <td style={i % 2 === 0 ? PS.td : PS.tdAlt}>{row.label}</td>
                    <td style={{ ...(i % 2 === 0 ? PS.td : PS.tdAlt), textAlign: "right", fontWeight: 600 }}>
                      {curPct != null ? curPct.toFixed(1) + "%" : "–"}
                    </td>
                    <td style={{ ...(i % 2 === 0 ? PS.td : PS.tdAlt), textAlign: "right", color: "#555" }}>
                      {prevPct != null ? prevPct.toFixed(1) + "%" : "–"}
                    </td>
                    <td style={{ ...(i % 2 === 0 ? PS.td : PS.tdAlt), textAlign: "right", color: delta != null ? (delta >= 0 ? "#166534" : "#991b1b") : "#888" }}>
                      {delta != null ? (delta >= 0 ? "+" : "") + delta.toFixed(1) + "%p" : "–"}
                    </td>
                    <td style={{ ...(i % 2 === 0 ? PS.td : PS.tdAlt), textAlign: "right", color: "#555" }}>
                      {(row.target * 100).toFixed(0)}%
                    </td>
                    <td style={{ ...(i % 2 === 0 ? PS.td : PS.tdAlt), fontWeight: 700, color: achieved ? "#166534" : "#991b1b" }}>
                      {curPct == null ? "집계 불가" : achieved ? "달성" : "미달"}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          <p style={{ fontSize: 9.5, color: "#777", marginTop: 4 }}>
            평가 기간: {current.from} ~ {current.to} ({current.count}건)
            {previous && ` / 이전 기간: ${previous.from} ~ ${previous.to} (${previous.count}건)`}
          </p>
        </>
      ) : (
        <p style={{ color: "#888", fontSize: 10.5 }}>RAGAS 평가 데이터가 없습니다.</p>
      )}

      {/* 용어 설명 */}
      <div style={{ marginTop: 10, border: "1px solid #c8d0e0", borderRadius: 4, overflow: "hidden", fontSize: 10 }}>
        <div style={{ background: "#e8ecf5", padding: "4px 10px", fontWeight: 700, color: "#1a2a5e", fontSize: 10 }}>
          ■ RAGAS 지표 용어 설명
        </div>
        <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 10 }}>
          <tbody>
            {([
              { term: "Faithfulness (충실도)", desc: "AI가 생성한 답변이 검색된 문서 내용에 근거하는 정도. 높을수록 환각(Hallucination) 위험이 낮음. 목표 ≥ 90%" },
              { term: "Answer Relevancy (답변 관련성)", desc: "생성된 답변이 시민의 질문 의도와 얼마나 잘 부합하는지를 측정. 높을수록 질문에 직접적으로 답하는 답변 생성. 목표 ≥ 85%" },
              { term: "Context Precision (검색 정밀도)", desc: "검색된 문서 중 실제로 답변 생성에 유용한 문서의 비율. 낮으면 불필요한 문서가 답변 품질을 저하시킬 수 있음. 목표 ≥ 70%" },
              { term: "Context Recall (검색 재현율)", desc: "정답 도출에 필요한 핵심 문서가 검색 결과에 포함된 비율. 낮으면 답변이 불완전해질 위험이 있음. 목표 ≥ 75%" },
            ] as const).map((row, i) => (
              <tr key={row.term}>
                <td style={{ width: "28%", padding: "4px 10px", background: i % 2 === 0 ? "#f5f7fb" : "#fff", fontWeight: 600, color: "#1a2a5e", borderBottom: "1px solid #dde3ee", verticalAlign: "top" }}>
                  {row.term}
                </td>
                <td style={{ padding: "4px 10px", background: i % 2 === 0 ? "#f5f7fb" : "#fff", color: "#333", borderBottom: "1px solid #dde3ee", lineHeight: 1.6 }}>
                  {row.desc}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div style={{ ...PS.analysis, whiteSpace: "pre-line" }}>
        <strong>■ 분석 의견</strong><br />
        {interpretRagas(current, previous ?? null)}
      </div>

      {/* ── 5. 사용자 피드백 ──────────────────────────────────────── */}
      <div style={PS.sectionTitle}>5. 사용자 피드백 분석 (최근 30일)</div>

      <table style={{ ...PS.table, width: "50%" }}>
        <thead>
          <tr>
            <th style={PS.th}>구분</th>
            <th style={{ ...PS.th, textAlign: "right" }}>건수</th>
            <th style={{ ...PS.th, textAlign: "right" }}>비율</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td style={PS.td}>긍정 피드백 (rating ≥ 4)</td>
            <td style={{ ...PS.td, textAlign: "right" }}>{posFb.toLocaleString()}</td>
            <td style={{ ...PS.td, textAlign: "right", fontWeight: 600, color: "#166534" }}>
              {posRate != null ? posRate.toFixed(1) + "%" : "–"}
            </td>
          </tr>
          <tr>
            <td style={PS.tdAlt}>부정 피드백 (rating ≤ 2)</td>
            <td style={{ ...PS.tdAlt, textAlign: "right" }}>{(totalFb - posFb).toLocaleString()}</td>
            <td style={{ ...PS.tdAlt, textAlign: "right", fontWeight: 600, color: "#991b1b" }}>
              {posRate != null ? (100 - posRate).toFixed(1) + "%" : "–"}
            </td>
          </tr>
          <tr>
            <td style={{ ...PS.td, fontWeight: 600 }}>합계</td>
            <td style={{ ...PS.td, textAlign: "right", fontWeight: 600 }}>{totalFb.toLocaleString()}</td>
            <td style={{ ...PS.td, textAlign: "right" }}>100%</td>
          </tr>
        </tbody>
      </table>

      <div style={PS.analysis}>
        <strong>■ 분석 의견</strong><br />
        {interpretFeedback(feedbackItems)}
      </div>

      <div style={PS.divider} />

      {/* ── 종합 의견 ─────────────────────────────────────────────── */}
      <div style={PS.sectionTitle}>6. 종합 평가 및 개선 권고사항</div>
      <div style={{ ...PS.analysis, background: "#f0f3fa" }}>
        {[
          resolvedRateKpi != null && resolvedRateKpi < 90 &&
            `① 세션 성공률(${resolvedRateKpi.toFixed(1)}%) 개선: 미응답·오류 질의 원인 분석 및 지식베이스 보강이 필요합니다.`,
          zeroResultRateKpi != null && zeroResultRateKpi >= 5 &&
            `② Knowledge Gap율(${zeroResultRateKpi.toFixed(1)}%) 개선: 미응답 질의의 주제 패턴을 분석하여 관련 문서를 우선 등록하시기 바랍니다.`,
          faithfulness != null && faithfulness < 0.9 &&
            `③ AI 답변 신뢰도(Faithfulness ${(faithfulness * 100).toFixed(1)}%) 개선: 프롬프트 최적화 및 RAG 검색 유사도 임계값 조정을 권고합니다.`,
          trendData.length >= 2 && ((trendData[trendData.length - 1].resolvedRate ?? 0) < (trendData[0].resolvedRate ?? 0)) &&
            `④ 성공률 하락 추세 모니터링: 지속적인 성과 추적 및 주간 단위 점검 체계 운영을 권장합니다.`,
        ].filter(Boolean).join("\n") || "현재 주요 지표가 목표치를 달성하고 있습니다. 현 운영 체계를 유지하면서 지속적인 품질 모니터링을 권장합니다."}
      </div>

      {/* ── 하단 ──────────────────────────────────────────────────── */}
      <div style={PS.footer}>
        본 보고서는 Centras AI Gov 운영 포털에서 자동 생성되었습니다. · 생성일시: {now}
      </div>
    </div>
  );
}

// ── 메인 페이지 ────────────────────────────────────────────────────────────────

export default function ReportsPage() {
  const { orgId } = useFilter();
  const [period, setPeriod] = useState<"weekly" | "monthly">("weekly");

  const today = new Date().toISOString().slice(0, 10);
  const fromDate = (() => {
    const d = new Date();
    d.setDate(d.getDate() - (period === "weekly" ? 56 : 180));
    return d.toISOString().slice(0, 10);
  })();

  const dailyParams = new URLSearchParams({ page_size: "300" });
  dailyParams.set("from_date", fromDate);
  dailyParams.set("to_date", today);
  if (orgId) dailyParams.set("organization_id", orgId);

  const ragasParams = new URLSearchParams();
  ragasParams.set("from_date", fromDate);
  ragasParams.set("to_date", today);
  if (orgId) ragasParams.set("organization_id", orgId);

  const feedbackParams = new URLSearchParams({ days: "30" });
  if (orgId) feedbackParams.set("organization_id", orgId);

  const { data: dailyData, isLoading: dailyLoading, error: dailyError } =
    useSWR<PagedResponse<DailyMetric>>(`/api/admin/metrics/daily?${dailyParams}`, fetcher);
  const { data: ragasData, isLoading: ragasLoading } =
    useSWR<RagasEvaluationSummaryResponse>(`/api/admin/ragas-evaluations/summary?${ragasParams}`, fetcher);
  const { data: feedbackData } =
    useSWR<FeedbackTrendResponse>(`/api/admin/metrics/feedback-trend?${feedbackParams}`, fetcher);
  const { data: orgsData } =
    useSWR<PagedResponse<Organization>>("/api/admin/organizations?page_size=100", fetcher);

  if (dailyLoading || ragasLoading) {
    return <div className="flex items-center justify-center h-48"><Spinner /></div>;
  }
  if (dailyError) {
    return <p className="text-error text-sm">데이터를 불러오지 못했습니다. 백엔드 연결을 확인하세요.</p>;
  }

  const orgName = (() => {
    if (!orgId) return "전체";
    const found = orgsData?.items?.find((o) => o.organizationId === orgId);
    return found ? found.name : orgId;
  })();

  const metrics          = dailyData?.items ?? [];
  const totalQ           = metrics.reduce((s, m) => s + m.totalQuestions, 0);
  const resolvedRateKpi  = totalQ > 0 ? metrics.reduce((s, m) => s + (m.resolvedRate   ?? 0) * m.totalQuestions, 0) / totalQ : null;
  const zeroResultRateKpi = totalQ > 0 ? metrics.reduce((s, m) => s + (m.zeroResultRate ?? 0) * m.totalQuestions, 0) / totalQ : null;
  const faithfulness     = ragasData?.current?.avgFaithfulness ?? null;
  const trendData        = aggregateByPeriod(metrics, period);
  const orgRows          = aggregateByOrg(metrics);
  const feedbackItems    = feedbackData?.items ?? [];

  const ragasRows: DeltaScoreRow[] = [
    { label: "Faithfulness",    value: ragasData?.current?.avgFaithfulness    ?? null, prev: ragasData?.previous?.avgFaithfulness    ?? null, target: 0.90 },
    { label: "Answer Relevancy", value: ragasData?.current?.avgAnswerRelevancy ?? null, prev: ragasData?.previous?.avgAnswerRelevancy ?? null, target: 0.85 },
  ];

  return (
    <>
      <style>{`
        @page { size: A4; margin: 16mm 18mm; }
        @media print {
          aside            { display: none !important; }
          header           { display: none !important; }
          header + div     { display: none !important; }
          main             { overflow: visible !important; padding: 0 !important; flex: unset !important; }
          main > div       { max-width: 100% !important; }
          .print-screen    { display: none !important; }
          .print-only      { display: block !important; }
        }
        @media screen {
          .print-only { display: none !important; }
        }
      `}</style>

      {/* 화면 레이아웃 */}
      <div className="print-screen space-y-6">
        <PageGuide
          description="주간·월간 기준으로 품질 지표 추이와 기관별 성과를 확인하는 화면입니다."
          tips={[
            "기간 탭이 데이터 범위를 결정합니다 (주간=최근 8주, 월간=최근 6개월).",
            "기관 필터로 특정 기관의 성과를 개별 조회할 수 있습니다.",
            "PDF 저장 버튼을 누르면 공공기관 보고서 형식으로 출력됩니다.",
          ]}
        />

        <div className="flex items-center justify-between">
          <h2 className="text-text-primary font-semibold text-lg">성과 분석 리포트</h2>
          <button
            onClick={() => window.print()}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-mono font-semibold bg-accent/10 text-accent border border-accent/30 hover:bg-accent/20 transition-colors"
          >
            <span className="material-symbols-outlined text-[16px]">picture_as_pdf</span>
            PDF 저장
          </button>
        </div>

        <div className="flex gap-0 border-b border-bg-border">
          {(["weekly", "monthly"] as const).map((p) => (
            <button
              key={p}
              onClick={() => setPeriod(p)}
              className={`px-4 py-2 text-xs font-mono transition-colors ${
                period === p
                  ? "border-b-2 border-accent text-accent font-semibold"
                  : "text-text-muted hover:text-text-secondary"
              }`}
            >
              {p === "weekly" ? "주간 리포트 (8주)" : "월간 리포트 (6개월)"}
            </button>
          ))}
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-4 gap-4">
          <KpiCard label="총 질의 수" value={totalQ > 0 ? totalQ.toLocaleString() : "-"} sub="건" help="선택 기간 내 전체 질의 건수 합산입니다." />
          <KpiCard label="세션 성공률" value={resolvedRateKpi != null ? resolvedRateKpi.toFixed(1) + "%" : "-"}
            status={getKpiStatus(resolvedRateKpi, { ok: (v) => v >= 90, warn: (v) => v >= 80 })}
            progressValue={resolvedRateKpi ?? undefined} help="answer_status = 'answered' 비율의 질의 수 가중 평균. 정상 ≥ 90%." />
          <KpiCard label="Knowledge Gap율" value={zeroResultRateKpi != null ? zeroResultRateKpi.toFixed(1) + "%" : "-"}
            status={getKpiStatus(zeroResultRateKpi, { ok: (v) => v < 5, warn: (v) => v < 8 })}
            progressValue={zeroResultRateKpi != null ? (zeroResultRateKpi / 20) * 100 : undefined} help="pgvector 검색 0건 비율. 정상 < 5%." />
          <KpiCard label="Faithfulness" value={faithfulness != null ? (faithfulness * 100).toFixed(1) + "%" : "-"}
            status={getKpiStatus(faithfulness != null ? faithfulness * 100 : null, { ok: (v) => v >= 90, warn: (v) => v >= 80 })}
            progressValue={faithfulness != null ? faithfulness * 100 : undefined} help="LLM 답변의 문서 충실도. 목표 ≥ 90%." />
        </div>

        <Card>
          <CardHeader><CardTitle tag="TREND">{period === "weekly" ? "주간" : "월간"} 품질 지표 추이</CardTitle></CardHeader>
          {trendData.length === 0 ? <p className="px-5 pb-5 text-sm text-text-muted">데이터가 없습니다.</p> : (
            <div className="px-2 pb-4">
              <ResponsiveContainer width="100%" height={220}>
                <LineChart data={trendData} margin={{ top: 5, right: 10, left: -20, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--bg-border)" />
                  <XAxis dataKey="label" tick={{ fill: "var(--text-muted)", fontSize: 11 }} axisLine={{ stroke: "var(--bg-border)" }} tickLine={false} />
                  <YAxis tick={{ fill: "var(--text-muted)", fontSize: 11 }} axisLine={false} tickLine={false} tickFormatter={(v) => `${v}%`} />
                  <Tooltip contentStyle={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--bg-border)", borderRadius: 8 }} labelStyle={{ color: "var(--text-primary)", fontSize: 12 }} itemStyle={{ fontSize: 12 }} formatter={(v: number) => [`${v.toFixed(1)}%`]} />
                  <Legend wrapperStyle={{ fontSize: 11, color: "var(--text-muted)" }} />
                  <Line type="monotone" dataKey="resolvedRate" name="세션 성공률(%)" stroke="var(--accent)" strokeWidth={2} dot={false} activeDot={{ r: 4 }} />
                  <Line type="monotone" dataKey="fallbackRate" name="Fallback율(%)" stroke="#f5a623" strokeWidth={2} dot={false} activeDot={{ r: 4 }} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          )}
        </Card>

        <Card>
          <CardHeader><CardTitle tag="BY ORG">기관별 성과</CardTitle></CardHeader>
          <div className="overflow-hidden">
            {orgRows.length === 0 ? <p className="px-5 pb-5 text-sm text-text-muted">데이터가 없습니다.</p> : (
              <Table>
                <Thead><Th>기관 ID</Th><Th>질의 수</Th><Th>세션 성공률</Th><Th>미응답 건수</Th><Th>Knowledge Gap</Th></Thead>
                <Tbody>
                  {orgRows.map((row) => (
                    <Tr key={row.organizationId}>
                      <Td className="font-mono text-xs">{row.organizationId}</Td>
                      <Td className="font-mono text-sm">{row.totalQuestions.toLocaleString()}</Td>
                      <Td><span className={`font-mono text-sm ${row.resolvedRate == null ? "text-text-muted" : row.resolvedRate >= 90 ? "text-success" : row.resolvedRate >= 80 ? "text-warning" : "text-error"}`}>{row.resolvedRate != null ? `${row.resolvedRate.toFixed(1)}%` : "–"}</span></Td>
                      <Td className="font-mono text-sm">{row.unansweredCount.toLocaleString()}</Td>
                      <Td className="font-mono text-sm">{row.knowledgeGapCount.toLocaleString()}</Td>
                    </Tr>
                  ))}
                </Tbody>
              </Table>
            )}
          </div>
        </Card>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Card>
            <CardHeader><CardTitle tag="RAGAS">RAGAS 품질 지표</CardTitle></CardHeader>
            {ragasData?.current == null ? <p className="px-5 pb-5 text-sm text-text-muted">RAGAS 평가 데이터가 없습니다.</p> : (
              <div className="px-5 py-4 space-y-5">
                {ragasRows.map((row) => <ScoreBar key={row.label} row={row} />)}
                <p className="text-[10px] text-text-muted pt-1 border-t border-bg-border">
                  {ragasData.current.from} ~ {ragasData.current.to} ({ragasData.current.count}건)
                </p>
              </div>
            )}
          </Card>

          <Card>
            <CardHeader><CardTitle tag="FEEDBACK">피드백 트렌드 (30일)</CardTitle></CardHeader>
            <div className="px-4 pb-4">
              {feedbackItems.length === 0 ? <p className="text-sm text-text-muted py-4">피드백 데이터가 없습니다.</p> : (
                <ResponsiveContainer width="100%" height={180}>
                  <BarChart data={feedbackItems} margin={{ top: 4, right: 8, left: -16, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#1e2533" />
                    <XAxis dataKey="date" tick={{ fontSize: 10, fill: "#8b93a8" }} tickFormatter={(v: string) => v.slice(5)} />
                    <YAxis tick={{ fontSize: 10, fill: "#8b93a8" }} />
                    <Tooltip contentStyle={{ backgroundColor: "#13171f", border: "1px solid #222836", borderRadius: 8 }} labelStyle={{ color: "#dde2ec", fontSize: 11 }} itemStyle={{ fontSize: 11 }} />
                    <Bar dataKey="positive" name="긍정" fill="#10b981" radius={[3, 3, 0, 0]} />
                    <Bar dataKey="negative" name="부정" fill="#ef4444" radius={[3, 3, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </div>
          </Card>
        </div>
      </div>

      {/* 인쇄 전용 공공기관 보고서 */}
      <div className="print-only">
        <PrintReport
          period={period}
          fromDate={fromDate}
          today={today}
          orgId={orgId}
          orgName={orgName}
          totalQ={totalQ}
          resolvedRateKpi={resolvedRateKpi}
          zeroResultRateKpi={zeroResultRateKpi}
          faithfulness={faithfulness}
          trendData={trendData}
          orgRows={orgRows}
          ragasData={ragasData}
          feedbackItems={feedbackItems}
        />
      </div>
    </>
  );
}
