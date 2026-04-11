"use client";

import Link from "next/link";
import useSWR from "swr";
import {
  RadarChart,
  Radar,
  PolarGrid,
  PolarAngleAxis,
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  ReferenceLine,
  CartesianGrid,
} from "recharts";
import { fetcher } from "@/lib/api";
import type { PagedResponse, RagasEvaluationSummaryResponse } from "@/lib/types";
import { KpiCard } from "@/components/charts/KpiCard";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Spinner } from "@/components/ui/Spinner";
import { PageGuide } from "@/components/ui/PageGuide";
import { useFilter } from "@/lib/filter-context";

// ── 인라인 타입 ───────────────────────────────────────────────────────────────

interface Feedback {
  feedbackId: string;
  questionId: string;
  rating: number;
  createdAt: string;
}

interface FeedbackTrendItem { date: string; positive: number; negative: number; }
interface FeedbackTrendResponse { items: FeedbackTrendItem[]; }

interface PiiCountResponse { count: number; lastDetectedAt: string | null; }

interface AlertItem { level: "warning" | "critical"; message: string; }

// ── KPI 상태 계산 헬퍼 ─────────────────────────────────────────────────────────

function getKpiStatus(
  value: number | null,
  thresholds: { ok: (v: number) => boolean; warn: (v: number) => boolean }
): "ok" | "warn" | "critical" | undefined {
  if (value == null) return undefined;
  if (thresholds.ok(value))   return "ok";
  if (thresholds.warn(value)) return "warn";
  return "critical";
}

// ── 섹션 구분 헤더 ─────────────────────────────────────────────────────────────

function SectionDivider({ title }: { title: string }) {
  return (
    <div className="flex items-center gap-3 mt-2">
      <span className="text-xs font-semibold tracking-widest text-text-secondary uppercase">
        {title}
      </span>
      <div className="flex-1 h-px bg-bg-border" />
    </div>
  );
}

// ── 알림 배너 ──────────────────────────────────────────────────────────────────

function AlertBanner({ alerts }: { alerts: AlertItem[] }) {
  if (alerts.length === 0) return null;
  return (
    <div className="space-y-2">
      {alerts.map((alert, i) => {
        const isCritical = alert.level === "critical";
        return (
          <div
            key={i}
            className="flex items-start gap-3 rounded-lg px-4 py-3 border"
            style={isCritical
              ? { backgroundColor: "rgba(239,68,68,0.12)", borderColor: "rgba(239,68,68,0.35)" }
              : { backgroundColor: "rgba(245,158,11,0.10)", borderColor: "rgba(245,158,11,0.30)" }
            }
          >
            <span className={[
              "shrink-0 mt-0.5 text-[10px] font-bold tracking-wider px-1.5 py-0.5 rounded border font-mono",
              isCritical
                ? "text-error border-error/30 bg-error/10"
                : "text-warning border-warning/30 bg-warning/10",
            ].join(" ")}>
              {isCritical ? "CRIT" : "WARN"}
            </span>
            <p className="text-sm text-text-secondary leading-relaxed">{alert.message}</p>
          </div>
        );
      })}
    </div>
  );
}

// ── 피드백 7일 추이 차트 ──────────────────────────────────────────────────────

interface FeedbackTrendChartProps {
  items: FeedbackTrendItem[];
}

function FeedbackTrendChart({ items }: FeedbackTrendChartProps) {
  const chartData = items.map((d) => {
    const t = d.positive + d.negative;
    return {
      date: d.date.substring(5).replace("-", "/"),
      pct: t > 0 ? Math.round((d.positive / t) * 100) : 0,
      total: t,
    };
  });

  return (
    <ResponsiveContainer width="100%" height={160}>
      <AreaChart data={chartData} margin={{ top: 8, right: 8, bottom: 0, left: -16 }}>
        <defs>
          <linearGradient id="posGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%"  stopColor="#22c55e" stopOpacity={0.3} />
            <stop offset="95%" stopColor="#22c55e" stopOpacity={0.02} />
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="3 3" stroke="#1e2d42" vertical={false} />
        <XAxis dataKey="date" tick={{ fill: "#6b7f99", fontSize: 10 }} tickLine={false} axisLine={false} />
        <YAxis domain={[0, 100]} tick={{ fill: "#6b7f99", fontSize: 10 }} tickLine={false} axisLine={false}
          tickFormatter={(v) => `${v}%`} />
        <ReferenceLine y={70} stroke="#f59e0b" strokeDasharray="4 3" strokeWidth={1}
          label={{ value: "목표 70%", position: "insideTopRight", fill: "#f59e0b", fontSize: 10 }} />
        <Tooltip
          contentStyle={{ background: "#0d1424", border: "1px solid #1e2d42", borderRadius: 8, fontSize: 11 }}
          labelStyle={{ color: "#8899aa" }}
          formatter={(v: number, _: string, props: { payload?: { total: number } }) => [
            `${v}% (${props.payload?.total ?? 0}건)`, "긍정 비율"
          ]}
        />
        <Area type="monotone" dataKey="pct" stroke="#22c55e" strokeWidth={2}
          fill="url(#posGrad)" dot={{ r: 3, fill: "#22c55e", strokeWidth: 0 }}
          activeDot={{ r: 5, fill: "#22c55e" }} />
      </AreaChart>
    </ResponsiveContainer>
  );
}

// ── RAGAS 레이더 차트 ──────────────────────────────────────────────────────────

interface RadarDataPoint { axis: string; value: number; fullMark: number; }

function RagasRadarChart({ data }: { data: RadarDataPoint[] }) {
  return (
    <ResponsiveContainer width="100%" height={260}>
      <RadarChart data={data} margin={{ top: 10, right: 30, bottom: 10, left: 30 }}>
        <PolarGrid stroke="#2a3448" />
        <PolarAngleAxis
          dataKey="axis"
          tick={{ fill: "#8899aa", fontSize: 11 }}
        />
        <Radar
          name="score"
          dataKey="value"
          stroke="#3B82F6"
          fill="#3B82F6"
          fillOpacity={0.25}
          dot={{ r: 3, fill: "#3B82F6" }}
        />
      </RadarChart>
    </ResponsiveContainer>
  );
}

// ── RAGAS 스코어 바 ────────────────────────────────────────────────────────────

interface DeltaScoreRow {
  label: string;
  value: number | null;
  prev: number | null;
  target: number;
}

function ScoreBar({ row }: { row: DeltaScoreRow }) {
  const pct = row.value != null ? Math.min(row.value * 100, 100) : 0;
  const targetPct = Math.min(row.target * 100, 100);
  const delta = row.value != null && row.prev != null ? row.value - row.prev : null;
  const ok = row.value != null && row.value >= row.target;
  const near = row.value != null && !ok && row.value >= row.target * 0.9;
  const barColor = ok ? "bg-success" : near ? "bg-warning" : "bg-error";
  const statusCls = ok ? "text-success bg-success/10 border-success/20"
    : near ? "text-warning bg-warning/10 border-warning/20"
    : "text-error bg-error/10 border-error/20";
  const statusText = row.value == null ? "N/A" : ok ? "달성" : near ? "근접" : "미달";

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
            {statusText}
          </span>
        </div>
      </div>
      <div className="relative h-2 rounded-full bg-bg-prominent overflow-visible">
        <div className={`h-full rounded-full transition-all ${barColor}`} style={{ width: `${pct}%` }} />
        {/* 목표선 */}
        <div
          className="absolute top-1/2 -translate-y-1/2 w-0.5 h-4 bg-white/30 rounded-full"
          style={{ left: `${targetPct}%` }}
        />
      </div>
      <p className="text-[10px] text-text-muted text-right">목표 {(row.target * 100).toFixed(0)}%</p>
    </div>
  );
}


export default function QualitySummaryPage() {
  const { orgId, from, to } = useFilter();

  const ragasParams = new URLSearchParams();
  if (orgId) ragasParams.set("organization_id", orgId);
  if (from) ragasParams.set("from_date", from);
  if (to) ragasParams.set("to_date", to);

  const feedbackParams = new URLSearchParams({ page_size: "100" });
  if (orgId) feedbackParams.set("organization_id", orgId);
  if (from) feedbackParams.set("from_date", from);
  if (to) feedbackParams.set("to_date", to);

  const trendParams = new URLSearchParams({ days: "7" });
  if (orgId) trendParams.set("organization_id", orgId);

  const piiParams = new URLSearchParams();
  if (orgId) piiParams.set("organization_id", orgId);
  if (from) piiParams.set("from_date", from);
  if (to) piiParams.set("to_date", to);

  const { data: ragasData, isLoading: ragasLoading } =
    useSWR<RagasEvaluationSummaryResponse>(
      `/api/admin/ragas-evaluations/summary?${ragasParams}`,
      fetcher
    );

  const { data: feedbacksData } = useSWR<PagedResponse<Feedback>>(
    `/api/admin/feedbacks?${feedbackParams}`,
    fetcher
  );

  const { data: trendData } = useSWR<FeedbackTrendResponse>(
    `/api/admin/metrics/feedback-trend?${trendParams}`,
    fetcher
  );

  const { data: piiData } = useSWR<PiiCountResponse>(
    `/api/admin/metrics/pii-count?${piiParams}`,
    fetcher
  );

  if (ragasLoading) {
    return (
      <div className="flex items-center justify-center h-48">
        <Spinner />
      </div>
    );
  }

  const current  = ragasData?.current  ?? null;
  const previous = ragasData?.previous ?? null;

  const faithfulness         = current?.avgFaithfulness         ?? null;
  const answerRelevancy      = current?.avgAnswerRelevancy      ?? null;
  const contextPrecision     = current?.avgContextPrecision     ?? null;
  const contextRecall        = current?.avgContextRecall        ?? null;
  const citationCoverage     = current?.avgCitationCoverage     ?? null;
  const citationCorrectness  = current?.avgCitationCorrectness  ?? null;

  const hallucinationRate = faithfulness != null ? (1 - faithfulness) * 100 : null;

  // 알림 배너 조건 계산
  const alerts: AlertItem[] = [];
  if (hallucinationRate != null) {
    if (hallucinationRate > 5) {
      alerts.push({ level: "critical", message: `Hallucination Rate ${hallucinationRate.toFixed(1)}% — 임계값(5%)을 초과했습니다. RAG 파라미터 유사도 임계값을 높이고 프롬프트를 점검하세요.` });
    } else if (hallucinationRate > 3) {
      alerts.push({ level: "warning", message: `Hallucination Rate ${hallucinationRate.toFixed(1)}% — 경고 임계값(3%)을 초과했습니다.` });
    }
  }
  if (faithfulness != null) {
    if (faithfulness < 0.80) {
      alerts.push({ level: "critical", message: `Faithfulness ${(faithfulness * 100).toFixed(1)}% — 위험 수준(80% 미만)입니다. 검색된 문서와 답변의 일치율을 즉시 점검하세요.` });
    } else if (faithfulness < 0.85) {
      alerts.push({ level: "warning", message: `Faithfulness ${(faithfulness * 100).toFixed(1)}% — 권고 수준(85%) 미만입니다.` });
    }
  }

  // 레이더 차트 데이터 (0~1 스케일 * 100)
  const radarData: RadarDataPoint[] = [
    { axis: "Faithfulness",       value: faithfulness        != null ? faithfulness        * 100 : 0, fullMark: 100 },
    { axis: "Answer Relevance",   value: answerRelevancy     != null ? answerRelevancy     * 100 : 0, fullMark: 100 },
    { axis: "Context Precision",  value: contextPrecision    != null ? contextPrecision    * 100 : 0, fullMark: 100 },
    { axis: "Context Recall",     value: contextRecall       != null ? contextRecall       * 100 : 0, fullMark: 100 },
    { axis: "Citation Coverage",  value: citationCoverage    != null ? citationCoverage    * 100 : 0, fullMark: 100 },
    { axis: "Citation Correct.",  value: citationCorrectness != null ? citationCorrectness * 100 : 0, fullMark: 100 },
  ];
  const hasRadarData = current != null &&
    (faithfulness != null || answerRelevancy != null || contextPrecision != null || contextRecall != null ||
     citationCoverage != null || citationCorrectness != null);

  // delta 스코어카드 rows
  const deltaRows: DeltaScoreRow[] = [
    { label: "Faithfulness",        value: faithfulness,        prev: previous?.avgFaithfulness        ?? null, target: 0.90 },
    { label: "Answer Relevance",    value: answerRelevancy,     prev: previous?.avgAnswerRelevancy     ?? null, target: 0.85 },
    { label: "Context Precision",   value: contextPrecision,    prev: previous?.avgContextPrecision    ?? null, target: 0.70 },
    { label: "Context Recall",      value: contextRecall,       prev: previous?.avgContextRecall       ?? null, target: 0.75 },
    { label: "Citation Coverage",   value: citationCoverage,    prev: previous?.avgCitationCoverage    ?? null, target: 0.80 },
    { label: "Citation Correctness",value: citationCorrectness, prev: previous?.avgCitationCorrectness ?? null, target: 0.85 },
  ];

  const feedbacks   = feedbacksData?.items ?? [];
  const thumbsUp    = feedbacks.filter((f) => f.rating >= 4).length;
  const thumbsDown  = feedbacks.filter((f) => f.rating <= 2).length;
  const total       = thumbsUp + thumbsDown;
  const satisfactionPct = total > 0 ? (thumbsUp / total) * 100 : null;

  const trendItems = trendData?.items ?? [];
  const trendTotal = trendItems.reduce(
    (acc, d) => ({ up: acc.up + d.positive, down: acc.down + d.negative }),
    { up: 0, down: 0 }
  );

  return (
    <div className="space-y-6">
      <PageGuide
        description="RAG 답변 품질과 사용자 만족도를 요약하는 화면입니다."
        tips={[
          "Faithfulness 0.90 미만이면 답변이 문서를 벗어나고 있다는 신호입니다 — 프롬프트를 점검하세요.",
          "Hallucination Rate가 오르면 RAG 파라미터의 유사도 임계값을 높여보세요.",
          "사용자 만족도(긍정 비율)가 70% 미만이면 미응답 질의 목록에서 주요 패턴을 분석하세요.",
        ]}
      />
      <h2 className="text-text-primary font-semibold text-lg">품질/보안 요약</h2>

      {/* 알림 배너 */}
      <AlertBanner alerts={alerts} />

      {/* KPI 4개 */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-4">
        <KpiCard
          label="FAITHFULNESS"
          value={faithfulness != null ? (faithfulness * 100).toFixed(1) + "%" : "-"}
          status={getKpiStatus(faithfulness != null ? faithfulness * 100 : null, {
            ok:   (v) => v >= 90,
            warn: (v) => v >= 80,
          })}
          progressValue={faithfulness != null ? faithfulness * 100 : undefined}
          help="LLM 답변이 검색된 문서에 충실한 비율. 0.90 이상 목표."
        />
        <KpiCard
          label="HALLUCINATION RATE"
          value={hallucinationRate != null ? hallucinationRate.toFixed(1) + "%" : "-"}
          status={getKpiStatus(hallucinationRate, {
            ok:   (v) => v < 3,
            warn: (v) => v < 5,
          })}
          progressValue={hallucinationRate != null ? (hallucinationRate / 20) * 100 : undefined}
          help="Faithfulness에서 역산한 환각(hallucination) 추정 비율. 낮을수록 좋습니다."
        />
        <KpiCard
          label="USER SATISFACTION"
          value={satisfactionPct != null ? satisfactionPct.toFixed(1) + "%" : feedbacks.length === 0 ? "N/A" : "-"}
          status={getKpiStatus(satisfactionPct, {
            ok:   (v) => v >= 70,
            warn: (v) => v >= 50,
          })}
          progressValue={satisfactionPct ?? undefined}
          help="rating 4 이상(긍정) / 전체 평가 비율. 70% 이상이면 정상입니다."
        />
        <div className="relative">
          <KpiCard
            label="PII 감지 건수"
            value={piiData != null ? piiData.count.toLocaleString() + "건" : "-"}
            sub="이번 달 누적"
            status={piiData != null ? (piiData.count === 0 ? "ok" : piiData.count < 5 ? "warn" : "critical") : undefined}
            help="audit_logs의 PII_DETECTED 이벤트 이번 달 누적 건수. 0건이면 정상입니다."
          />
          <div className="px-4 pb-3 -mt-1 space-y-1">
            <p className="text-[10px] text-text-muted">
              마지막 감지:{" "}
              {piiData?.lastDetectedAt
                ? new Date(piiData.lastDetectedAt).toLocaleString("ko-KR")
                : "감지 없음"}
            </p>
            <Link href="/ops/audit" className="text-[11px] text-accent hover:underline">
              감사 로그 바로가기 →
            </Link>
          </div>
        </div>
      </div>

      {/* RAGAS 평가 섹션 — 레이더 + 스코어 바 2-column */}
      <SectionDivider title="RAGAS 평가" />

      <Card>
        <CardHeader>
          <CardTitle>RAG 품질 지표</CardTitle>
        </CardHeader>
        {!hasRadarData ? (
          <p className="text-text-muted text-sm py-8 text-center px-4">
            평가 데이터가 없습니다. RAGAS 평가를 먼저 실행하세요.
          </p>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-0 divide-y lg:divide-y-0 lg:divide-x divide-bg-border">
            {/* 레이더 차트 */}
            <div className="px-4 pb-4">
              <RagasRadarChart data={radarData} />
            </div>
            {/* 스코어 바 */}
            <div className="px-5 py-4 space-y-5">
              {deltaRows.map((row) => (
                <ScoreBar key={row.label} row={row} />
              ))}
              {current && (
                <div className="pt-2 border-t border-bg-border space-y-0.5">
                  <p className="text-[10px] text-text-muted">
                    현재 기간: {current.from} ~ {current.to} ({current.count}건)
                  </p>
                  {previous && (
                    <p className="text-[10px] text-text-muted">
                      이전 기간: {previous.from} ~ {previous.to} ({previous.count}건, Δ 기준)
                    </p>
                  )}
                </div>
              )}
            </div>
          </div>
        )}
      </Card>

      {/* 사용자 피드백 섹션 */}
      <SectionDivider title="사용자 피드백" />

      <Card>
        <CardHeader>
          <CardTitle>사용자 피드백 요약</CardTitle>
        </CardHeader>
        <div className="px-4 pb-5 space-y-5">
          {/* 큰 수치 + 세그먼트 바 */}
          {(() => {
            const pos = feedbacks.length > 0 ? thumbsUp : trendTotal.up;
            const neg = feedbacks.length > 0 ? thumbsDown : trendTotal.down;
            const tot = pos + neg;
            const posPct = tot > 0 ? (pos / tot) * 100 : 0;
            const negPct = tot > 0 ? (neg / tot) * 100 : 0;
            if (tot === 0) return <p className="text-text-muted text-sm py-2">피드백 데이터가 없습니다.</p>;
            return (
              <div className="space-y-3">
                <div className="flex items-end gap-6">
                  <div>
                    <p className="text-[10px] text-text-muted mb-0.5">긍정</p>
                    <p className="text-3xl font-bold text-success tabular-nums">{posPct.toFixed(1)}<span className="text-base font-normal ml-0.5">%</span></p>
                    <p className="text-[11px] text-text-muted">{pos}건</p>
                  </div>
                  <div className="w-px h-10 bg-bg-border" />
                  <div>
                    <p className="text-[10px] text-text-muted mb-0.5">부정</p>
                    <p className="text-3xl font-bold text-error tabular-nums">{negPct.toFixed(1)}<span className="text-base font-normal ml-0.5">%</span></p>
                    <p className="text-[11px] text-text-muted">{neg}건</p>
                  </div>
                  <div className="w-px h-10 bg-bg-border" />
                  <div>
                    <p className="text-[10px] text-text-muted mb-0.5">총 피드백</p>
                    <p className="text-3xl font-bold text-text-primary tabular-nums">{tot}</p>
                    <p className="text-[11px] text-text-muted">건</p>
                  </div>
                </div>
                <div className="flex h-3 w-full overflow-hidden rounded-full gap-0.5">
                  <div className="bg-success rounded-l-full transition-all" style={{ width: `${posPct}%` }} />
                  <div className="bg-error rounded-r-full transition-all" style={{ width: `${negPct}%` }} />
                </div>
              </div>
            );
          })()}

          {/* 최근 7일 추이 차트 */}
          {trendItems.length > 0 && (
            <div>
              <p className="text-xs text-text-secondary mb-2 font-medium">최근 7일 긍정 비율 추이</p>
              <FeedbackTrendChart items={trendItems} />
            </div>
          )}
        </div>
      </Card>
    </div>
  );
}
