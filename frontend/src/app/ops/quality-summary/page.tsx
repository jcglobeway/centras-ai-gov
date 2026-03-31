"use client";

import Link from "next/link";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, RagasEvaluation } from "@/lib/types";
import { KpiCard } from "@/components/charts/KpiCard";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { ScoreTable } from "@/components/ui/ScoreTable";
import { Spinner } from "@/components/ui/Spinner";
import { PageGuide } from "@/components/ui/PageGuide";

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

export default function QualitySummaryPage() {
  const { data: ragasData, isLoading: ragasLoading } =
    useSWR<PagedResponse<RagasEvaluation>>(
      "/api/admin/ragas-evaluations?page_size=1",
      fetcher
    );

  const { data: feedbacksData } = useSWR<PagedResponse<Feedback>>(
    "/api/admin/feedbacks?page_size=100",
    fetcher
  );

  const { data: trendData } = useSWR<FeedbackTrendResponse>(
    "/api/admin/metrics/feedback-trend?days=7",
    fetcher
  );

  const { data: piiData } = useSWR<PiiCountResponse>(
    "/api/admin/metrics/pii-count",
    fetcher
  );

  if (ragasLoading) {
    return (
      <div className="flex items-center justify-center h-48">
        <Spinner />
      </div>
    );
  }

  const latestRagas = ragasData?.items?.[0] ?? null;

  const faithfulness       = latestRagas?.faithfulness       ?? null;
  const answerRelevancy    = latestRagas?.answerRelevancy    ?? null;
  const contextPrecision   = latestRagas?.contextPrecision   ?? null;
  const contextRecall      = latestRagas?.contextRecall      ?? null;

  const hallucinationRate = faithfulness != null ? (1 - faithfulness) * 100 : null;

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
          "사용자 만족도(👍 비율)가 70% 미만이면 미응답 질의 목록에서 주요 패턴을 분석하세요.",
        ]}
      />
      <h2 className="text-text-primary font-semibold text-lg">품질/보안 요약</h2>

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
            ok:   (v) => v < 10,
            warn: (v) => v < 20,
          })}
          progressValue={hallucinationRate != null ? (hallucinationRate / 50) * 100 : undefined}
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
          help="rating 4 이상(👍) / 전체 평가 비율. 70% 이상이면 정상입니다."
        />
        <div className="relative">
          <KpiCard
            label="PII 감지 건수"
            value={piiData != null ? piiData.count.toLocaleString() + "건" : "-"}
            sub="이번 달 누적"
            status={piiData != null ? (piiData.count === 0 ? "ok" : piiData.count < 5 ? "warn" : "critical") : undefined}
            help="audit_logs의 PII_DETECTED 이벤트 이번 달 누적 건수. 0건이면 정상입니다."
          />
          <div className="px-4 pb-3 -mt-1">
            <Link href="/ops/audit" className="text-[11px] text-accent hover:underline">
              감사 로그 바로가기 →
            </Link>
          </div>
        </div>
      </div>

      {/* RAGAS 스코어카드 */}
      <Card>
        <CardHeader>
          <CardTitle>RAGAS 스코어카드</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4">
          {latestRagas == null ? (
            <p className="text-text-muted text-sm py-4">
              평가 데이터가 없습니다. RAGAS 평가를 먼저 실행하세요.
            </p>
          ) : (
            <ScoreTable
              rows={[
                { label: "Faithfulness",     value: faithfulness,     target: 0.90 },
                { label: "Answer Relevance", value: answerRelevancy,  target: 0.85 },
                { label: "Context Precision",value: contextPrecision, target: 0.70 },
                { label: "Context Recall",   value: contextRecall,    target: 0.75 },
              ]}
            />
          )}
          {latestRagas && (
            <p className="text-[10px] text-text-muted mt-3">
              마지막 평가: {new Date(latestRagas.evaluatedAt).toLocaleString("ko-KR")}
            </p>
          )}
        </div>
      </Card>

      {/* 피드백 요약 */}
      <Card>
        <CardHeader>
          <CardTitle>사용자 피드백 요약</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4 space-y-4">
          {/* 👍 / 👎 요약 */}
          <div className="grid grid-cols-3 gap-4">
            <div className="bg-bg-prominent rounded-lg p-4 text-center">
              <p className="text-2xl mb-1">👍</p>
              <p className="font-mono text-xl font-bold text-success">
                {feedbacks.length > 0 ? thumbsUp : trendTotal.up}
              </p>
              <p className="text-[10px] text-text-muted">긍정 (rating 4~5)</p>
            </div>
            <div className="bg-bg-prominent rounded-lg p-4 text-center">
              <p className="text-2xl mb-1">👎</p>
              <p className="font-mono text-xl font-bold text-error">
                {feedbacks.length > 0 ? thumbsDown : trendTotal.down}
              </p>
              <p className="text-[10px] text-text-muted">부정 (rating 1~2)</p>
            </div>
            <div className="bg-bg-prominent rounded-lg p-4 text-center">
              <p className="text-2xl mb-1">📊</p>
              <p className="font-mono text-xl font-bold text-text-primary">
                {satisfactionPct != null
                  ? satisfactionPct.toFixed(1) + "%"
                  : (() => {
                      const t = trendTotal.up + trendTotal.down;
                      return t > 0 ? ((trendTotal.up / t) * 100).toFixed(1) + "%" : "N/A";
                    })()}
              </p>
              <p className="text-[10px] text-text-muted">만족도 비율</p>
            </div>
          </div>

          {/* 최근 7일 추이 */}
          {trendItems.length > 0 && (
            <div>
              <p className="text-xs text-text-secondary mb-2 font-medium">최근 7일 추이</p>
              <div className="space-y-1.5">
                {trendItems.map((d) => {
                  const t = d.positive + d.negative;
                  const upPct = t > 0 ? (d.positive / t) * 100 : 0;
                  const label = d.date.substring(5).replace("-", "/");
                  return (
                    <div key={d.date} className="flex items-center gap-3">
                      <span className="font-mono text-[10px] text-text-muted w-10 shrink-0">
                        {label}
                      </span>
                      <div className="flex-1 h-2 rounded-full bg-bg-prominent overflow-hidden">
                        <div
                          className="h-full bg-success rounded-full transition-all"
                          style={{ width: `${upPct}%` }}
                        />
                      </div>
                      <span className="font-mono text-[10px] text-text-secondary w-8 text-right shrink-0">
                        {upPct.toFixed(0)}%
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      </Card>
    </div>
  );
}
