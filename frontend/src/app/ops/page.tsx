"use client";

import { useState } from "react";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, DailyMetric, Question, AnswerStatus } from "@/lib/types";
import { KpiCard } from "@/components/charts/KpiCard";
import { MetricsLineChart } from "@/components/charts/MetricsLineChart";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Spinner } from "@/components/ui/Spinner";
import { PageFilters, getWeekFrom, getToday } from "@/components/ui/PageFilters";
import { AlertBanner } from "@/components/ui/AlertBanner";
import { ProgressBar } from "@/components/ui/ProgressBar";
import { Badge } from "@/components/ui/Badge";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import type { ComponentProps } from "react";

type BadgeVariant = ComponentProps<typeof Badge>["variant"];

const ANSWER_STATUS_LABEL: Record<AnswerStatus, string> = {
  answered: "답변 완료",
  fallback: "Fallback",
  no_answer: "무응답",
  error: "오류",
};
const ANSWER_STATUS_VARIANT: Record<AnswerStatus, BadgeVariant> = {
  answered: "success",
  fallback: "warning",
  no_answer: "neutral",
  error: "error",
};

function getKpiStatus(
  value: number | null,
  thresholds: { ok: (v: number) => boolean; warn: (v: number) => boolean }
): "ok" | "warn" | "critical" | undefined {
  if (value == null) return undefined;
  if (thresholds.ok(value)) return "ok";
  if (thresholds.warn(value)) return "warn";
  return "critical";
}

// 파이프라인 레이턴시 고정값 (실제 OpenTelemetry 연동 전 seed 값)
const PIPELINE_STEPS = [
  { label: "Retrieval", valueMs: 438, color: "bg-blue-500" },
  { label: "LLM 호출", valueMs: 1128, color: "bg-violet-500" },
  { label: "후처리", valueMs: 114, color: "bg-teal-500" },
];
const PIPELINE_TOTAL_MS = PIPELINE_STEPS.reduce((s, x) => s + x.valueMs, 0);

export default function OpsDashboardPage() {
  const [orgId, setOrgId] = useState("");
  const [from, setFrom] = useState(getWeekFrom);
  const [to, setTo] = useState(getToday);
  const [alertDismissed, setAlertDismissed] = useState(false);

  const params = new URLSearchParams({ page_size: "14" });
  if (orgId) params.set("organization_id", orgId);
  if (from) params.set("from_date", from);
  if (to) params.set("to_date", to);

  const { data, error, isLoading } = useSWR<PagedResponse<DailyMetric>>(
    `/api/admin/metrics/daily?${params}`,
    fetcher
  );

  const { data: questionsData } = useSWR<PagedResponse<Question>>(
    `/api/admin/questions?page_size=5`,
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
  const latest = metrics[metrics.length - 1];

  const resolvedRateVal = latest?.resolvedRate ?? null;
  const fallbackRateVal = latest?.fallbackRate ?? null;
  const zeroResultRateVal = latest?.zeroResultRate ?? null;
  const avgRespMsVal = latest?.avgResponseTimeMs ?? null;

  const showAlert =
    !alertDismissed &&
    ((fallbackRateVal != null && fallbackRateVal > 10) ||
      (zeroResultRateVal != null && zeroResultRateVal > 5));

  const alertVariant =
    (fallbackRateVal != null && fallbackRateVal >= 15) ||
    (zeroResultRateVal != null && zeroResultRateVal >= 8)
      ? ("critical" as const)
      : ("warn" as const);

  const recentQuestions = questionsData?.items ?? [];

  // 기관 헬스맵: 전체 metrics에서 기관별 최신 row 추출 후 상태 계산
  const orgLatestMap = new Map<string, typeof latest>();
  for (const m of metrics) {
    const existing = orgLatestMap.get(m.organizationId);
    if (!existing || m.metricDate > existing.metricDate) {
      orgLatestMap.set(m.organizationId, m);
    }
  }
  const orgHealthList = Array.from(orgLatestMap.entries()).map(([orgId, m]) => {
    const resolved = m.resolvedRate != null ? Number(m.resolvedRate) : null;
    const fallback = m.fallbackRate != null ? Number(m.fallbackRate) : null;
    let healthStatus: "ok" | "warn" | "critical" = "ok";
    let issue = "";
    if (resolved != null && resolved < 80) { healthStatus = "critical"; issue = `응답률 ${resolved.toFixed(1)}% (목표 90%)`; }
    else if (fallback != null && fallback >= 15) { healthStatus = "critical"; issue = `Fallback율 ${fallback.toFixed(1)}% (임계 15%)`; }
    else if (resolved != null && resolved < 90) { healthStatus = "warn"; issue = `응답률 ${resolved.toFixed(1)}% (목표 90%)`; }
    else if (fallback != null && fallback >= 10) { healthStatus = "warn"; issue = `Fallback율 ${fallback.toFixed(1)}% (임계 10%)`; }
    return { orgId, healthStatus, issue };
  });

  const healthColor = { ok: "text-success", warn: "text-warning", critical: "text-error" };
  const healthLabel = { ok: "정상", warn: "주의", critical: "위험" };
  const healthDot = { ok: "bg-success", warn: "bg-warning", critical: "bg-error" };

  return (
    <div className="space-y-6">
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
        <h2 className="text-text-primary font-semibold text-lg">운영 대시보드</h2>
        <PageFilters
          orgId={orgId} onOrgChange={setOrgId}
          from={from} onFromChange={setFrom}
          to={to} onToChange={setTo}
        />
      </div>

      {/* KPI 그리드 (5개) */}
      <div className="grid grid-cols-2 lg:grid-cols-5 gap-4">
        <KpiCard
          label="응답률"
          value={resolvedRateVal != null ? resolvedRateVal.toFixed(1) + "%" : "-"}
          status={getKpiStatus(resolvedRateVal, {
            ok: (v) => v >= 90,
            warn: (v) => v >= 80,
          })}
          help="전체 질문 중 챗봇이 정상 답변을 제공한 비율입니다. 90% 이상이면 정상, 80% 미만이면 문서 품질 점검이 필요합니다."
        />
        <KpiCard
          label="Fallback율"
          value={fallbackRateVal != null ? fallbackRateVal.toFixed(1) + "%" : "-"}
          status={getKpiStatus(fallbackRateVal, {
            ok: (v) => v < 10,
            warn: (v) => v < 15,
          })}
          help="RAG가 신뢰도 낮은 답변을 감지해 일반 안내문으로 대체한 비율입니다. 10% 초과 시 관련 문서 품질 또는 검색 설정을 점검하세요."
        />
        <KpiCard
          label="무응답률"
          value={zeroResultRateVal != null ? zeroResultRateVal.toFixed(1) + "%" : "-"}
          status={getKpiStatus(zeroResultRateVal, {
            ok: (v) => v < 5,
            warn: (v) => v < 8,
          })}
          help="벡터 검색에서 관련 문서가 전혀 없어 답변 자체를 생성하지 못한 비율입니다. 5% 초과 시 해당 주제의 문서 추가를 고객사에 요청하세요."
        />
        <KpiCard
          label="평균 응답시간"
          value={avgRespMsVal != null ? avgRespMsVal.toLocaleString() + "ms" : "-"}
          status={getKpiStatus(avgRespMsVal, {
            ok: (v) => v < 1500,
            warn: (v) => v < 2500,
          })}
          help="검색(Retrieval) + LLM 생성 + 후처리 전체 E2E 시간의 평균값입니다. 1.5초 미만이면 정상, 2.5초 초과 시 파이프라인 병목 구간을 점검하세요."
        />
        <KpiCard
          label="전체 질문 (최신일)"
          value={latest?.totalQuestions ?? "-"}
          sub="건"
          help="당일 집계 기준으로 전체 기관에서 시민이 챗봇에 입력한 질문의 총 건수입니다."
        />
      </div>

      {/* 기관 헬스맵 */}
      {orgHealthList.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>기관 헬스맵</CardTitle>
          </CardHeader>
          <div className="px-4 pb-4 divide-y divide-bg-border">
            {orgHealthList.map(({ orgId: oid, healthStatus, issue }) => (
              <div key={oid} className="flex items-center justify-between py-2.5">
                <div className="flex items-center gap-2.5">
                  <div className={`w-2 h-2 rounded-full ${healthDot[healthStatus]}`} />
                  <span className="text-text-primary text-sm font-medium font-mono">{oid}</span>
                  {issue && (
                    <span className="text-text-muted text-xs">{issue}</span>
                  )}
                </div>
                <span className={`font-mono text-[11px] font-semibold ${healthColor[healthStatus]}`}>
                  {healthLabel[healthStatus]}
                </span>
              </div>
            ))}
          </div>
        </Card>
      )}

      {/* 파이프라인 레이턴시 */}
      <Card>
        <CardHeader>
          <CardTitle>파이프라인 레이턴시 (P95)</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4 space-y-3">
          {PIPELINE_STEPS.map((step) => (
            <ProgressBar
              key={step.label}
              label={step.label}
              valueMs={step.valueMs}
              maxMs={PIPELINE_TOTAL_MS}
              color={step.color}
            />
          ))}
          <p className="text-xs text-text-muted mt-2">
            * 실측 기반 고정값. OpenTelemetry 연동 후 실시간 교체 예정.
          </p>
        </div>
      </Card>

      {/* 최근 질문 테이블 */}
      <Card>
        <CardHeader>
          <CardTitle>최근 질문 (5건)</CardTitle>
        </CardHeader>
        <div className="overflow-hidden">
          <Table>
            <Thead>
              <Th>내용</Th>
              <Th>답변 상태</Th>
              <Th>생성일</Th>
            </Thead>
            <Tbody>
              {recentQuestions.map((q) => (
                <Tr key={q.questionId}>
                  <Td className="max-w-xs truncate text-sm">{q.questionText}</Td>
                  <Td>
                    <Badge variant={ANSWER_STATUS_VARIANT[q.answerStatus]}>
                      {ANSWER_STATUS_LABEL[q.answerStatus]}
                    </Badge>
                  </Td>
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

      {/* 추세 차트 */}
      {metrics.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>최근 질문 추세</CardTitle>
          </CardHeader>
          <MetricsLineChart
            data={metrics}
            metrics={["resolvedRate", "fallbackRate", "zeroResultRate"]}
          />
        </Card>
      )}
    </div>
  );
}
