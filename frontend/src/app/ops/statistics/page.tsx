"use client";

import { useState } from "react";
import useSWR from "swr";
import {
  PieChart,
  Pie,
  Cell,
  Legend,
  Tooltip,
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
} from "recharts";
import { fetcher } from "@/lib/api";
import type { PagedResponse, DailyMetric } from "@/lib/types";
import { KpiCard } from "@/components/charts/KpiCard";
import { MetricsLineChart } from "@/components/charts/MetricsLineChart";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Spinner } from "@/components/ui/Spinner";
import { PageFilters, getWeekFrom, getToday } from "@/components/ui/PageFilters";
import { PageGuide } from "@/components/ui/PageGuide";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";

const CHART_COLORS = ["#2563eb", "#8b5cf6", "#10b981", "#f59e0b", "#ef4444", "#6b7280", "#06b6d4", "#f97316"];

interface CategoryItem { category: string; count: number; }
interface CategoryDistributionResponse { items: CategoryItem[]; total: number; }

interface DuplicateQuestionItem { questionText: string; count: number; }
interface DuplicateQuestionsResponse { items: DuplicateQuestionItem[]; total: number; }

interface FeedbackTrendItem { date: string; positive: number; negative: number; }
interface FeedbackTrendResponse { items: FeedbackTrendItem[]; }

interface QuestionLengthDistributionResponse {
  veryShort: number;
  short: number;
  long: number;
  total: number;
}

interface Insight {
  level: "critical" | "warn" | "info";
  title: string;
  desc: string;
  metric: string;
}

function getKpiStatus(
  value: number | null,
  thresholds: { ok: (v: number) => boolean; warn: (v: number) => boolean }
): "ok" | "warn" | "critical" | undefined {
  if (value == null) return undefined;
  if (thresholds.ok(value))   return "ok";
  if (thresholds.warn(value)) return "warn";
  return "critical";
}

function SectionTitle({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex items-center gap-3 text-[11px] font-mono uppercase tracking-widest text-text-muted">
      {children}
      <div className="flex-1 h-px bg-bg-border" />
    </div>
  );
}

function InsightCard({ insight }: { insight: Insight }) {
  const borderClass =
    insight.level === "critical"
      ? "border-l-4 border-error bg-error/5"
      : insight.level === "warn"
      ? "border-l-4 border-warning bg-warning/5"
      : "border-l-4 border-accent bg-accent/5";

  const badgeClass =
    insight.level === "critical"
      ? "bg-error/20 text-error"
      : insight.level === "warn"
      ? "bg-warning/20 text-warning"
      : "bg-accent/20 text-accent";

  const badgeLabel =
    insight.level === "critical" ? "CRITICAL" : insight.level === "warn" ? "WARN" : "INFO";

  return (
    <div className={`rounded-lg px-4 py-3.5 ${borderClass}`}>
      <div className="flex items-start gap-3">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <span className={`text-[10px] font-mono font-bold px-1.5 py-0.5 rounded ${badgeClass}`}>
              {badgeLabel}
            </span>
            <span className="text-sm font-semibold text-text-primary">{insight.title}</span>
          </div>
          <p className="text-xs text-text-secondary leading-relaxed">{insight.desc}</p>
          {insight.metric && (
            <p className="text-[11px] font-mono text-warning mt-1.5">{insight.metric}</p>
          )}
        </div>
      </div>
    </div>
  );
}

export default function StatisticsPage() {
  const [orgId, setOrgId] = useState("");
  const [from,  setFrom]  = useState(getWeekFrom);
  const [to,    setTo]    = useState(getToday);

  const params = new URLSearchParams({ page_size: "14" });
  if (orgId) params.set("organization_id", orgId);
  if (from)  params.set("from_date", from);
  if (to)    params.set("to_date", to);

  const dupParams = new URLSearchParams({ limit: "10", min_count: "2" });
  if (orgId) dupParams.set("organization_id", orgId);
  if (from)  dupParams.set("from_date", from);
  if (to)    dupParams.set("to_date", to);

  const feedbackParams = new URLSearchParams({ days: "7" });
  if (orgId) feedbackParams.set("organization_id", orgId);

  const { data, error, isLoading } = useSWR<PagedResponse<DailyMetric>>(
    `/api/admin/metrics/daily?${params}`,
    fetcher
  );
  const { data: categoryData } = useSWR<CategoryDistributionResponse>(
    `/api/admin/metrics/category-distribution?${params}`,
    fetcher
  );
  const { data: dupData } = useSWR<DuplicateQuestionsResponse>(
    `/api/admin/metrics/duplicate-questions?${dupParams}`,
    fetcher
  );
  const { data: feedbackData } = useSWR<FeedbackTrendResponse>(
    `/api/admin/metrics/feedback-trend?${feedbackParams}`,
    fetcher
  );
  const { data: lengthData } = useSWR<QuestionLengthDistributionResponse>(
    `/api/admin/metrics/question-length-distribution?${params}`,
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

  const metrics = data?.items ?? [];

  const totalQuestionsVal = metrics.length > 0
    ? metrics.reduce((sum, m) => sum + (m.totalQuestions ?? 0), 0)
    : null;
  const unansweredCountVal = metrics.length > 0
    ? metrics.reduce((sum, m) => sum + (m.unansweredCount ?? 0), 0)
    : null;

  const totalQ = totalQuestionsVal ?? 0;

  const totalWeightedResolved = metrics.reduce((sum, m) =>
    sum + (m.resolvedRate != null && m.totalQuestions != null ? Number(m.resolvedRate) * m.totalQuestions : 0), 0);
  const totalWeightedZero = metrics.reduce((sum, m) =>
    sum + (m.zeroResultRate != null && m.totalQuestions != null ? Number(m.zeroResultRate) * m.totalQuestions : 0), 0);
  const totalWeightedFallback = metrics.reduce((sum, m) =>
    sum + (m.fallbackRate != null && m.totalQuestions != null ? Number(m.fallbackRate) * m.totalQuestions : 0), 0);
  const totalWeightedMs = metrics.reduce((sum, m) =>
    sum + (m.avgResponseTimeMs != null && m.totalQuestions != null ? m.avgResponseTimeMs * m.totalQuestions : 0), 0);

  const resolvedRateVal   = totalQ > 0 ? totalWeightedResolved / totalQ : null;
  const zeroResultRateVal = totalQ > 0 ? totalWeightedZero / totalQ     : null;
  const fallbackRateVal   = totalQ > 0 ? totalWeightedFallback / totalQ : null;
  const avgResponseMsVal  = totalQ > 0 ? totalWeightedMs / totalQ       : null;

  const feedbackItems = feedbackData?.items ?? [];

  // 인사이트 자동 생성
  const insights: Insight[] = [];

  if (fallbackRateVal != null && fallbackRateVal > 10)
    insights.push({
      level: "critical",
      title: "Fallback율 임계값 초과",
      desc: "RAG 파이프라인이 답변을 생성하지 못하고 fallback으로 처리된 비율이 높습니다. 문서 품질 또는 검색 파라미터를 점검하세요.",
      metric: `Fallback ${fallbackRateVal.toFixed(1)}%`,
    });

  if (zeroResultRateVal != null && zeroResultRateVal > 5)
    insights.push({
      level: "critical",
      title: "지식베이스 문서 부족 신호",
      desc: "pgvector 검색 결과가 0건인 질문 비율이 높습니다. 자주 묻는 주제의 문서가 지식베이스에 없는 상태입니다. 문서 추가가 필요합니다.",
      metric: `무응답 ${zeroResultRateVal.toFixed(1)}%`,
    });

  if (avgResponseMsVal != null && avgResponseMsVal > 2000)
    insights.push({
      level: "warn",
      title: "평균 응답시간 초과",
      desc: "전체 평균 응답시간이 2초를 넘었습니다. LLM 레이턴시 또는 검색 컨텍스트 크기를 점검하세요. 스트리밍 응답 적용을 권장합니다.",
      metric: `평균 ${Math.round(avgResponseMsVal).toLocaleString()}ms`,
    });

  if ((dupData?.items?.[0]?.count ?? 0) > 5)
    insights.push({
      level: "warn",
      title: "반복 질의 다수 감지",
      desc: "같은 질문이 반복되고 있습니다. 답변 불만족이거나 UI에서 답변이 명확히 전달되지 않는 신호일 수 있습니다. FAQ 등록을 검토하세요.",
      metric: `"${(dupData?.items[0].questionText ?? "").slice(0, 20)}..." ${dupData?.items[0].count}회`,
    });

  if (lengthData && lengthData.total > 0 && lengthData.veryShort / lengthData.total > 0.2)
    insights.push({
      level: "info",
      title: "단답형 입력 비율 높음",
      desc: "5자 이하의 단답형 질문 비율이 높습니다. 키워드만 입력하는 사용자가 많으며, RAG 검색 정확도가 낮아질 수 있습니다. 입력 가이드 UI를 검토하세요.",
      metric: `단답(≤5자) ${Math.round(lengthData.veryShort / lengthData.total * 100)}%`,
    });

  if (insights.length === 0)
    insights.push({
      level: "info",
      title: "현재 주요 이슈 없음",
      desc: "모든 지표가 정상 범위 내에 있습니다.",
      metric: "",
    });

  // 질문 길이 분포 차트 데이터
  const lengthChartData = [
    { label: "≤5자 (단답/명사만)", count: lengthData?.veryShort ?? 0, fill: "#ef4444" },
    { label: "6~20자 (일반 질문)", count: lengthData?.short ?? 0, fill: "#3b82f6" },
    { label: "21자+ (구체적 질문)", count: lengthData?.long ?? 0, fill: "#10b981" },
  ];

  return (
    <div className="space-y-6">
      <PageGuide
        description="일별 질의량과 카테고리 분포를 파악하는 화면입니다."
        tips={[
          "Knowledge Gap Rate가 높으면 자주 묻는 주제의 문서가 지식베이스에 없다는 신호입니다.",
          "중복 질의 TOP N을 보고 FAQ 추가나 문서 보강 우선순위를 결정하세요.",
          "피드백 추이에서 부정 피드백이 늘어나면 답변 품질 점검이 필요합니다.",
        ]}
      />
      <div className="flex items-center justify-between flex-wrap gap-2">
        <h2 className="text-text-primary font-semibold text-lg">서비스 통계</h2>
        <PageFilters
          orgId={orgId} onOrgChange={setOrgId}
          from={from}   onFromChange={setFrom}
          to={to}       onToChange={setTo}
        />
      </div>

      {/* KPI 5개 */}
      <div className="grid grid-cols-2 lg:grid-cols-5 gap-4">
        <KpiCard
          label="총 질의 수"
          value={totalQuestionsVal != null ? totalQuestionsVal.toLocaleString() : "-"}
          sub="건"
          help="선택한 날짜 범위 내 전체 기관·서비스의 총 질의 건수 합산입니다."
        />
        <KpiCard
          label="세션 성공률"
          value={resolvedRateVal != null ? resolvedRateVal.toFixed(1) + "%" : "-"}
          status={getKpiStatus(resolvedRateVal, { ok: (v) => v >= 90, warn: (v) => v >= 80 })}
          progressValue={resolvedRateVal ?? undefined}
          help="answer_status = 'answered' 비율의 가중 평균 (질의 수 기준). RAG 파이프라인이 끝까지 완료된 비율이며 답변 품질과는 무관합니다. 정상 ≥ 90%."
        />
        <KpiCard
          label="Knowledge Gap"
          value={zeroResultRateVal != null ? zeroResultRateVal.toFixed(1) + "%" : "-"}
          status={getKpiStatus(zeroResultRateVal, { ok: (v) => v < 5, warn: (v) => v < 8 })}
          progressValue={zeroResultRateVal != null ? (zeroResultRateVal / 20) * 100 : undefined}
          help="pgvector 검색 결과 0건(answer_status = 'no_answer') 비율의 가중 평균. 지식베이스에 관련 문서 자체가 없는 경우입니다. 정상 < 5%."
        />
        <KpiCard
          label="평균 응답시간"
          value={avgResponseMsVal != null ? Math.round(avgResponseMsVal).toLocaleString() + "ms" : "-"}
          status={getKpiStatus(avgResponseMsVal, { ok: (v) => v < 1500, warn: (v) => v < 2500 })}
          progressValue={avgResponseMsVal != null ? (avgResponseMsVal / 5000) * 100 : undefined}
          help="answers.response_time_ms의 가중 평균. 질문 수신부터 답변 반환까지 전체 소요시간입니다. 정상 < 1,500ms."
        />
        <KpiCard
          label="미응답 건수"
          value={unansweredCountVal != null ? unansweredCountVal.toLocaleString() : "-"}
          sub="건"
          status={getKpiStatus(unansweredCountVal, { ok: (v) => v === 0, warn: (v) => v < 10 })}
          help="answer_status가 fallback 또는 no_answer인 건수 합산. 문서 추가·파이프라인 점검으로 줄일 수 있습니다."
        />
      </div>

      {/* 일별 질의 수 추이 */}
      {metrics.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle tag="TREND">일별 질의 수 추이</CardTitle>
          </CardHeader>
          <MetricsLineChart
            data={metrics}
            metrics={["totalQuestions"]}
          />
        </Card>
      )}

      {/* 섹션 A: 질문 패턴 분석 */}
      <SectionTitle>질문 패턴 분석</SectionTitle>
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

        {/* 중복 질의 TOP 10 */}
        <Card>
          <CardHeader>
            <div className="flex items-center gap-1.5">
              <CardTitle tag="DUPLICATE QUESTIONS">중복 질의 TOP 10</CardTitle>
              <span className="group relative mt-3">
                <span className="text-text-muted text-xs cursor-help select-none">ⓘ</span>
                <span className="absolute bottom-full left-0 mb-2 w-64 rounded-lg bg-bg-prominent border border-bg-border px-3 py-2.5 text-xs leading-relaxed text-text-secondary shadow-xl opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-[100] whitespace-normal">
                  같은 질문 텍스트가 2회 이상 반복된 건을 집계합니다. 자주 반복되는 질문은 FAQ 등록이나 관련 문서 보강 대상입니다.
                </span>
              </span>
            </div>
          </CardHeader>
          <div className="overflow-hidden">
            {(dupData?.items ?? []).length === 0 ? (
              <p className="px-5 pb-5 text-sm text-text-muted">중복 질의가 없습니다.</p>
            ) : (
              <Table>
                <Thead>
                  <Th>질문 내용</Th>
                  <Th>횟수</Th>
                </Thead>
                <Tbody>
                  {(dupData?.items ?? []).map((item, i) => (
                    <Tr key={i}>
                      <Td>
                        <span className="text-text-primary text-xs line-clamp-2">{item.questionText}</span>
                      </Td>
                      <Td>
                        <span className="font-mono text-sm font-bold text-accent">{item.count}</span>
                      </Td>
                    </Tr>
                  ))}
                </Tbody>
              </Table>
            )}
          </div>
        </Card>

        {/* 질문 길이 분포 */}
        <Card>
          <CardHeader>
            <div className="flex items-center gap-1.5">
              <CardTitle tag="QUESTION LENGTH">질문 길이 분포</CardTitle>
              <span className="group relative mt-3">
                <span className="text-text-muted text-xs cursor-help select-none">ⓘ</span>
                <span className="absolute bottom-full left-0 mb-2 w-64 rounded-lg bg-bg-prominent border border-bg-border px-3 py-2.5 text-xs leading-relaxed text-text-secondary shadow-xl opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-[100] whitespace-normal">
                  질문 텍스트 글자 수 기준. 단답형(≤5자)이 많으면 RAG 검색 정확도가 낮아질 수 있습니다.
                </span>
              </span>
            </div>
          </CardHeader>
          <div className="px-4 pb-4">
            {!lengthData || lengthData.total === 0 ? (
              <p className="text-sm text-text-muted py-4">질문 데이터가 없습니다.</p>
            ) : (
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={lengthChartData} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#1e2533" />
                  <XAxis
                    dataKey="label"
                    tick={{ fontSize: 10, fill: "#8b93a8" }}
                    tickFormatter={(v: string) => v.split(" ")[0]}
                  />
                  <YAxis tick={{ fontSize: 10, fill: "#8b93a8" }} />
                  <Tooltip
                    contentStyle={{ backgroundColor: "#13171f", border: "1px solid #222836", borderRadius: 8 }}
                    labelStyle={{ color: "#dde2ec", fontSize: 11 }}
                    itemStyle={{ fontSize: 11 }}
                    formatter={(value: number) => [`${value.toLocaleString()}건`, "질문 수"]}
                  />
                  <Bar dataKey="count" name="질문 수" radius={[4, 4, 0, 0]}>
                    {lengthChartData.map((entry, i) => (
                      <Cell key={i} fill={entry.fill} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            )}
            {lengthData && lengthData.total > 0 && (
              <div className="flex gap-4 mt-2 text-[11px] text-text-muted font-mono">
                <span style={{ color: "#ef4444" }}>≤5자 {lengthData.veryShort.toLocaleString()}건</span>
                <span style={{ color: "#3b82f6" }}>6~20자 {lengthData.short.toLocaleString()}건</span>
                <span style={{ color: "#10b981" }}>21자+ {lengthData.long.toLocaleString()}건</span>
              </div>
            )}
          </div>
        </Card>
      </div>

      {/* 피드백 만족도 추이 */}
      <Card>
        <CardHeader>
          <div className="flex items-center gap-1.5">
            <CardTitle tag="FEEDBACK TREND">피드백 만족도 추이 (7일)</CardTitle>
            <span className="group relative mt-3">
              <span className="text-text-muted text-xs cursor-help select-none">ⓘ</span>
              <span className="absolute bottom-full left-0 mb-2 w-64 rounded-lg bg-bg-prominent border border-bg-border px-3 py-2.5 text-xs leading-relaxed text-text-secondary shadow-xl opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-[100] whitespace-normal">
                긍정: rating ≥ 4 / 부정: rating ≤ 2. 부정 피드백이 증가하면 답변 품질 점검이 필요합니다.
              </span>
            </span>
          </div>
        </CardHeader>
        <div className="px-4 pb-4">
          {feedbackItems.length === 0 ? (
            <p className="text-sm text-text-muted py-4">피드백 데이터가 없습니다.</p>
          ) : (
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={feedbackItems} margin={{ top: 4, right: 8, left: -16, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e2533" />
                <XAxis dataKey="date" tick={{ fontSize: 10, fill: "#8b93a8" }} tickFormatter={(v) => v.slice(5)} />
                <YAxis tick={{ fontSize: 10, fill: "#8b93a8" }} />
                <Tooltip
                  contentStyle={{ backgroundColor: "#13171f", border: "1px solid #222836", borderRadius: 8 }}
                  labelStyle={{ color: "#dde2ec", fontSize: 11 }}
                  itemStyle={{ fontSize: 11 }}
                />
                <Bar dataKey="positive" name="긍정" fill="#10b981" radius={[3, 3, 0, 0]} />
                <Bar dataKey="negative" name="부정" fill="#ef4444" radius={[3, 3, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>
      </Card>

      {/* 카테고리 분포 도넛 차트 */}
      <Card>
        <CardHeader>
          <CardTitle tag="CATEGORY">카테고리 분포</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4">
          <ResponsiveContainer width="100%" height={260}>
            <PieChart>
              <Pie
                data={(categoryData?.items ?? []).map((item) => ({
                  name: item.category,
                  value: item.count,
                }))}
                cx="50%"
                cy="50%"
                innerRadius={60}
                outerRadius={100}
                paddingAngle={2}
                dataKey="value"
              >
                {(categoryData?.items ?? []).map((_, i) => (
                  <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{ backgroundColor: "#13171f", border: "1px solid #222836", borderRadius: 8 }}
                labelStyle={{ color: "#dde2ec", fontSize: 12 }}
                itemStyle={{ fontSize: 12 }}
                formatter={(value: number) => [`${value}건`, ""]}
              />
              <Legend wrapperStyle={{ fontSize: 11, color: "#8b93a8" }} />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </Card>

      {/* 섹션 B: RAG 개선 포인트 도출 */}
      <SectionTitle>RAG 개선 포인트 도출</SectionTitle>
      <div className="flex flex-col gap-3">
        {insights.map((insight, i) => (
          <InsightCard key={i} insight={insight} />
        ))}
      </div>
    </div>
  );
}
