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
          help="전체 질문 중 정상적으로 답변이 제공된 비율입니다."
        />
        <KpiCard
          label="Fallback율"
          value={fallbackRateVal != null ? fallbackRateVal.toFixed(1) + "%" : "-"}
          status={getKpiStatus(fallbackRateVal, {
            ok: (v) => v < 10,
            warn: (v) => v < 15,
          })}
          help="신뢰도가 낮아 일반 안내문으로 대체된 비율입니다."
        />
        <KpiCard
          label="무응답률"
          value={zeroResultRateVal != null ? zeroResultRateVal.toFixed(1) + "%" : "-"}
          status={getKpiStatus(zeroResultRateVal, {
            ok: (v) => v < 5,
            warn: (v) => v < 8,
          })}
          help="검색 결과가 전혀 없어 답변을 제공하지 못한 비율입니다."
        />
        <KpiCard
          label="평균 응답시간"
          value={avgRespMsVal != null ? avgRespMsVal.toLocaleString() + "ms" : "-"}
          status={getKpiStatus(avgRespMsVal, {
            ok: (v) => v < 1500,
            warn: (v) => v < 2500,
          })}
          help="RAG 파이프라인이 답변을 생성하는 데 걸린 평균 시간입니다."
        />
        <KpiCard
          label="전체 질문 (최신일)"
          value={latest?.totalQuestions ?? "-"}
          sub="건"
          help="당일 시민이 챗봇에 입력한 질문의 총 건수입니다."
        />
      </div>

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
