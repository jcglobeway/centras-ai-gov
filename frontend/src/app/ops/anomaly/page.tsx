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
import type { ComponentProps } from "react";

type BadgeVariant = ComponentProps<typeof Badge>["variant"];

interface DuplicateQuestionItem { questionText: string; count: number; }
interface DuplicateQuestionsResponse { items: DuplicateQuestionItem[]; total: number; }

interface AlertRow {
  metric: string;
  current: string;
  threshold: string;
  severity: "warn" | "critical";
  status: "발생 중";
}

function buildAlerts(latest: DailyMetric | undefined): AlertRow[] {
  if (!latest) return [];
  const alerts: AlertRow[] = [];
  if (latest.fallbackRate != null && latest.fallbackRate > 10) {
    alerts.push({
      metric: "Fallback율",
      current: latest.fallbackRate.toFixed(1) + "%",
      threshold: "10%",
      severity: latest.fallbackRate >= 15 ? "critical" : "warn",
      status: "발생 중",
    });
  }
  if (latest.zeroResultRate != null && latest.zeroResultRate > 5) {
    alerts.push({
      metric: "무응답률",
      current: latest.zeroResultRate.toFixed(1) + "%",
      threshold: "5%",
      severity: latest.zeroResultRate >= 8 ? "critical" : "warn",
      status: "발생 중",
    });
  }
  if (latest.avgResponseTimeMs != null && latest.avgResponseTimeMs > 1500) {
    alerts.push({
      metric: "평균 응답시간",
      current: latest.avgResponseTimeMs.toLocaleString() + "ms",
      threshold: "1,500ms",
      severity: latest.avgResponseTimeMs >= 2500 ? "critical" : "warn",
      status: "발생 중",
    });
  }
  return alerts;
}

const SEVERITY_VARIANT: Record<"warn" | "critical", BadgeVariant> = {
  warn: "warning",
  critical: "error",
};

export default function AnomalyPage() {
  const [warnFallback, setWarnFallback] = useState("10");
  const [critFallback, setCritFallback] = useState("15");
  const [warnZero, setWarnZero] = useState("5");
  const [critZero, setCritZero] = useState("8");

  const { data, isLoading } = useSWR<PagedResponse<DailyMetric>>(
    `/api/admin/metrics/daily?page_size=14`,
    fetcher
  );

  const { data: dupData } = useSWR<DuplicateQuestionsResponse>(
    `/api/admin/metrics/duplicate-questions?min_count=2&limit=10`,
    fetcher
  );

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-48">
        <Spinner />
      </div>
    );
  }

  const metrics = data?.items ?? [];
  const latest = metrics[metrics.length - 1];
  const alerts = buildAlerts(latest);
  const dupItems = dupData?.items ?? [];

  // 7일 평균 계산
  const recent7 = metrics.slice(-7);
  let queryDrift: string = "-";
  let recallDeviation: string = "-";

  if (recent7.length >= 2) {
    const prevAvgFallback =
      recent7.slice(0, -1).reduce((s, m) => s + (m.fallbackRate ?? 0), 0) /
      (recent7.length - 1);
    const latestFallback = recent7[recent7.length - 1]?.fallbackRate ?? null;
    if (latestFallback != null && prevAvgFallback > 0) {
      const drift = ((latestFallback - prevAvgFallback) / prevAvgFallback) * 100;
      queryDrift = drift.toFixed(1) + "%";
    }

    const prevAvgZero =
      recent7.slice(0, -1).reduce((s, m) => s + (m.zeroResultRate ?? 0), 0) /
      (recent7.length - 1);
    const latestZero = recent7[recent7.length - 1]?.zeroResultRate ?? null;
    if (latestZero != null && prevAvgZero > 0) {
      const dev = ((latestZero - prevAvgZero) / prevAvgZero) * 100;
      recallDeviation = dev.toFixed(1) + "%";
    }
  }

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

      {/* 임계값 초과 알림 */}
      <Card>
        <CardHeader>
          <CardTitle tag="ALERTS">임계값 초과 알림</CardTitle>
        </CardHeader>
        <div className="overflow-hidden">
          <Table>
            <Thead>
              <Th>지표</Th>
              <Th>현재값</Th>
              <Th>임계값</Th>
              <Th>심각도</Th>
              <Th>상태</Th>
            </Thead>
            <Tbody>
              {alerts.map((alert, i) => (
                <Tr key={i}>
                  <Td className="text-sm font-medium">{alert.metric}</Td>
                  <Td className="font-mono text-sm">{alert.current}</Td>
                  <Td className="font-mono text-sm text-text-muted">{alert.threshold}</Td>
                  <Td>
                    <Badge variant={SEVERITY_VARIANT[alert.severity]}>
                      {alert.severity === "critical" ? "긴급" : "경고"}
                    </Badge>
                  </Td>
                  <Td>
                    <Badge variant="error">{alert.status}</Badge>
                  </Td>
                </Tr>
              ))}
              {alerts.length === 0 && (
                <Tr>
                  <Td colSpan={5} className="text-center text-text-muted text-sm py-8">
                    임계값을 초과한 지표가 없습니다. 정상 운영 중입니다.
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
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
            {[
              { label: "Fallback율 경고 (%)", value: warnFallback, onChange: setWarnFallback },
              { label: "Fallback율 긴급 (%)", value: critFallback, onChange: setCritFallback },
              { label: "무응답률 경고 (%)",   value: warnZero,     onChange: setWarnZero },
              { label: "무응답률 긴급 (%)",   value: critZero,     onChange: setCritZero },
            ].map((field) => (
              <div key={field.label} className="space-y-1">
                <label className="text-[10px] font-mono text-text-muted uppercase tracking-wider">
                  {field.label}
                </label>
                <input
                  type="number"
                  value={field.value}
                  onChange={(e) => field.onChange(e.target.value)}
                  className="w-full bg-bg-base border border-bg-border rounded px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent font-mono"
                />
              </div>
            ))}
          </div>
          <Button disabled>저장 (API 연동 예정)</Button>
        </div>
      </Card>

    </div>
  );
}
