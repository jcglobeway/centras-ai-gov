"use client";

import { useState } from "react";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, DailyMetric } from "@/lib/types";
import { KpiCard } from "@/components/charts/KpiCard";
import { MetricsLineChart } from "@/components/charts/MetricsLineChart";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { PageGuide } from "@/components/ui/PageGuide";
import { useFilter } from "@/lib/filter-context";
import type { ComponentProps } from "react";

type BadgeVariant = ComponentProps<typeof Badge>["variant"];

interface DuplicateQuestionItem { questionText: string; count: number; }
interface DuplicateQuestionsResponse { items: DuplicateQuestionItem[]; total: number; }

interface ThresholdItem {
  metricKey: string;
  warnValue: number;
  criticalValue: number;
  updatedAt: string;
}
interface ThresholdsResponse { items: ThresholdItem[]; }

interface AlertEventItem {
  id: string;
  metricKey: string;
  currentValue: number;
  severity: "warn" | "critical";
  triggeredAt: string;
}
interface AlertEventsResponse { items: AlertEventItem[]; total: number; }

interface DriftSummaryItem {
  metricKey: string;
  rollingAvg: number | null;
  latestValue: number | null;
  deviationPct: number | null;
}
interface DriftSummaryResponse { items: DriftSummaryItem[]; }

const SEVERITY_VARIANT: Record<"warn" | "critical", BadgeVariant> = {
  warn: "warning",
  critical: "error",
};

const METRIC_LABEL: Record<string, string> = {
  fallback_rate: "Fallback율",
  zero_result_rate: "무응답률",
  avg_response_time_ms: "평균 응답시간",
};

function formatValue(key: string, value: number): string {
  if (key === "avg_response_time_ms") return value.toLocaleString() + "ms";
  return value.toFixed(1) + "%";
}

export default function AnomalyPage() {
  const { orgId, from, to } = useFilter();

  const metricsParams = new URLSearchParams({ page_size: "14" });
  if (orgId) metricsParams.set("organization_id", orgId);
  if (from) metricsParams.set("from_date", from);
  if (to) metricsParams.set("to_date", to);

  const dupParams = new URLSearchParams({ min_count: "2", limit: "10" });
  if (orgId) dupParams.set("organization_id", orgId);
  if (from) dupParams.set("from_date", from);
  if (to) dupParams.set("to_date", to);

  const driftParams = new URLSearchParams();
  if (orgId) driftParams.set("organization_id", orgId);

  const { data: metricsData, isLoading: metricsLoading } = useSWR<PagedResponse<DailyMetric>>(
    `/api/admin/metrics/daily?${metricsParams}`,
    fetcher
  );

  const { data: dupData } = useSWR<DuplicateQuestionsResponse>(
    `/api/admin/metrics/duplicate-questions?${dupParams}`,
    fetcher
  );

  const { data: thresholdsData, mutate: mutateThresholds } = useSWR<ThresholdsResponse>(
    `/api/admin/anomaly/thresholds`,
    fetcher
  );

  const { data: alertEventsData } = useSWR<AlertEventsResponse>(
    `/api/admin/anomaly/alert-events?limit=50`,
    fetcher
  );

  const { data: driftData } = useSWR<DriftSummaryResponse>(
    `/api/admin/anomaly/drift-summary?${driftParams}`,
    fetcher
  );

  // 임계값 로컬 편집 상태 (API 값으로 초기화)
  const [editValues, setEditValues] = useState<Record<string, { warn: string; critical: string }>>({});
  const [saving, setSaving] = useState(false);
  const [saveMsg, setSaveMsg] = useState<string | null>(null);

  const thresholds = thresholdsData?.items ?? [];

  function getEdit(key: string, field: "warn" | "critical"): string {
    const fromEdit = editValues[key]?.[field];
    if (fromEdit !== undefined) return fromEdit;
    const t = thresholds.find((t) => t.metricKey === key);
    return t ? (field === "warn" ? String(t.warnValue) : String(t.criticalValue)) : "";
  }

  function setEdit(key: string, field: "warn" | "critical", value: string) {
    setEditValues((prev) => ({
      ...prev,
      [key]: { ...(prev[key] ?? {}), [field]: value },
    }));
  }

  async function handleSave() {
    setSaving(true);
    setSaveMsg(null);
    try {
      const payload = thresholds.map((t) => ({
        metricKey: t.metricKey,
        warnValue: parseFloat(getEdit(t.metricKey, "warn")),
        criticalValue: parseFloat(getEdit(t.metricKey, "critical")),
      }));
      const res = await fetch("/api/admin/anomaly/thresholds", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ thresholds: payload }),
        credentials: "include",
      });
      if (!res.ok) throw new Error("저장 실패");
      setEditValues({});
      await mutateThresholds();
      setSaveMsg("저장되었습니다.");
    } catch {
      setSaveMsg("저장에 실패했습니다.");
    } finally {
      setSaving(false);
      setTimeout(() => setSaveMsg(null), 3000);
    }
  }

  if (metricsLoading) {
    return (
      <div className="flex items-center justify-center h-48">
        <Spinner />
      </div>
    );
  }

  const metrics = metricsData?.items ?? [];
  const dupItems = dupData?.items ?? [];
  const alertEvents = alertEventsData?.items ?? [];
  const driftItems = driftData?.items ?? [];

  const fallbackDrift = driftItems.find((d) => d.metricKey === "fallback_rate");
  const zeroDrift = driftItems.find((d) => d.metricKey === "zero_result_rate");

  const queryDrift = fallbackDrift?.deviationPct != null
    ? fallbackDrift.deviationPct.toFixed(1) + "%"
    : "-";
  const recallDeviation = zeroDrift?.deviationPct != null
    ? zeroDrift.deviationPct.toFixed(1) + "%"
    : "-";

  return (
    <div className="space-y-6">
      <PageGuide
        description="지표 드리프트와 비정상 패턴을 실시간으로 모니터링하는 화면입니다."
        tips={[
          "Query Drift가 급격히 상승하면 새로운 민원 주제가 유입됐다는 신호 — 관련 문서를 추가하세요.",
          "임계값 초과 알림 테이블에 항목이 있으면 즉시 해당 지표의 원인을 확인하세요.",
          "Webhook 설정(연동 API 관리)으로 임계값 초과 시 Slack 알림을 받을 수 있습니다.",
        ]}
      />
      <h2 className="text-text-primary font-semibold text-lg">이상 징후 감지</h2>

      {/* KPI */}
      <div className="grid grid-cols-2 gap-4">
        <KpiCard
          label="QUERY DRIFT"
          value={queryDrift}
          help="Fallback율 7일 평균 대비 최신값 이탈률"
          status={
            queryDrift === "-"
              ? undefined
              : parseFloat(queryDrift) > 20
              ? "critical"
              : parseFloat(queryDrift) > 10
              ? "warn"
              : "ok"
          }
        />
        <KpiCard
          label="RECALL DEVIATION"
          value={recallDeviation}
          help="무응답률 7일 평균 대비 이탈률"
          status={
            recallDeviation === "-"
              ? undefined
              : parseFloat(recallDeviation) > 20
              ? "critical"
              : parseFloat(recallDeviation) > 10
              ? "warn"
              : "ok"
          }
        />
      </div>

      {/* 7일 Drift 추이 */}
      {metrics.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle tag="DRIFT TREND">7일 Drift 추이</CardTitle>
          </CardHeader>
          <MetricsLineChart
            data={metrics}
            metrics={["fallbackRate", "zeroResultRate"]}
          />
        </Card>
      )}

      {/* 임계값 초과 알림 이력 */}
      <Card>
        <CardHeader>
          <CardTitle tag="ALERTS">임계값 초과 알림 이력</CardTitle>
        </CardHeader>
        <div className="overflow-hidden">
          <Table>
            <Thead>
              <Th>지표</Th>
              <Th>현재값</Th>
              <Th>심각도</Th>
              <Th>발생 시각</Th>
            </Thead>
            <Tbody>
              {alertEvents.map((event) => (
                <Tr key={event.id}>
                  <Td className="text-sm font-medium">{METRIC_LABEL[event.metricKey] ?? event.metricKey}</Td>
                  <Td className="font-mono text-sm">{formatValue(event.metricKey, event.currentValue)}</Td>
                  <Td>
                    <Badge variant={SEVERITY_VARIANT[event.severity]}>
                      {event.severity === "critical" ? "긴급" : "경고"}
                    </Badge>
                  </Td>
                  <Td className="text-xs text-text-muted">{new Date(event.triggeredAt).toLocaleString("ko-KR")}</Td>
                </Tr>
              ))}
              {alertEvents.length === 0 && (
                <Tr>
                  <Td colSpan={4} className="text-center text-text-muted text-sm py-8">
                    임계값을 초과한 이력이 없습니다.
                  </Td>
                </Tr>
              )}
            </Tbody>
          </Table>
        </div>
      </Card>

      {/* 반복 질의 (DDoS 의심 패턴) */}
      <Card>
        <CardHeader>
          <CardTitle tag="REPEAT">비정상 반복 질의</CardTitle>
        </CardHeader>
        <div className="overflow-hidden">
          <Table>
            <Thead>
              <Th>질문 내용</Th>
              <Th>반복 횟수</Th>
            </Thead>
            <Tbody>
              {dupItems.map((item, i) => (
                <Tr key={i}>
                  <Td className="text-sm max-w-xs truncate" title={item.questionText}>
                    {item.questionText}
                  </Td>
                  <Td>
                    <Badge variant={item.count >= 10 ? "error" : "warning"}>
                      {item.count}회
                    </Badge>
                  </Td>
                </Tr>
              ))}
              {dupItems.length === 0 && (
                <Tr>
                  <Td colSpan={2} className="text-center text-text-muted text-sm py-8">
                    반복 질의 패턴이 감지되지 않았습니다.
                  </Td>
                </Tr>
              )}
            </Tbody>
          </Table>
        </div>
      </Card>

      {/* 임계값 설정 */}
      <Card>
        <CardHeader>
          <CardTitle tag="THRESHOLD">임계값 설정</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4 space-y-4">
          {thresholds.length > 0 ? (
            <div className="space-y-3">
              {thresholds.map((t) => (
                <div key={t.metricKey} className="grid grid-cols-3 gap-3 items-end">
                  <div className="space-y-1">
                    <label className="text-[10px] font-mono text-text-muted uppercase tracking-wider">
                      지표
                    </label>
                    <div className="text-sm text-text-primary py-2">{METRIC_LABEL[t.metricKey] ?? t.metricKey}</div>
                  </div>
                  <div className="space-y-1">
                    <label className="text-[10px] font-mono text-text-muted uppercase tracking-wider">
                      경고(warn)
                    </label>
                    <input
                      type="number"
                      value={getEdit(t.metricKey, "warn")}
                      onChange={(e) => setEdit(t.metricKey, "warn", e.target.value)}
                      className="w-full bg-bg-base border border-bg-border rounded px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent font-mono"
                    />
                  </div>
                  <div className="space-y-1">
                    <label className="text-[10px] font-mono text-text-muted uppercase tracking-wider">
                      긴급(critical)
                    </label>
                    <input
                      type="number"
                      value={getEdit(t.metricKey, "critical")}
                      onChange={(e) => setEdit(t.metricKey, "critical", e.target.value)}
                      className="w-full bg-bg-base border border-bg-border rounded px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent font-mono"
                    />
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm text-text-muted">임계값을 불러오는 중입니다...</p>
          )}
          <div className="flex items-center gap-3">
            <Button onClick={handleSave} disabled={saving || thresholds.length === 0}>
              {saving ? "저장 중..." : "저장"}
            </Button>
            {saveMsg && (
              <span className={`text-sm ${saveMsg.includes("실패") ? "text-red-400" : "text-green-400"}`}>
                {saveMsg}
              </span>
            )}
          </div>
        </div>
      </Card>
    </div>
  );
}
