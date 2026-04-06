"use client";

import { useState } from "react";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, DailyMetric } from "@/lib/types";
import { KpiCard } from "@/components/charts/KpiCard";
import { MetricsLineChart } from "@/components/charts/MetricsLineChart";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Spinner } from "@/components/ui/Spinner";
import { PageFilters, getWeekFrom, getToday } from "@/components/ui/PageFilters";

function getKpiStatus(
  value: number | null,
  thresholds: { ok: (v: number) => boolean; warn: (v: number) => boolean }
): "ok" | "warn" | "critical" | undefined {
  if (value == null) return undefined;
  if (thresholds.ok(value)) return "ok";
  if (thresholds.warn(value)) return "warn";
  return "critical";
}

export default function ClientDashboardPage() {
  const [orgId, setOrgId] = useState("");
  const [from, setFrom] = useState(getWeekFrom);
  const [to, setTo] = useState(getToday);

  const params = new URLSearchParams({ page_size: "14" });
  if (orgId) params.set("organization_id", orgId);
  if (from) params.set("from_date", from);
  if (to) params.set("to_date", to);

  const { data, error, isLoading } = useSWR<PagedResponse<DailyMetric>>(
    `/api/admin/metrics/daily?${params}`,
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
    return <p className="text-error text-sm">데이터를 불러오지 못했습니다.</p>;
  }

  const metrics = data?.items ?? [];
  const latest = metrics[metrics.length - 1];

  const autoResolutionVal = latest?.autoResolutionRate != null ? Number(latest.autoResolutionRate) * 100 : null;
  const escalationVal = latest?.escalationRate != null ? Number(latest.escalationRate) * 100 : null;
  const revisitVal = latest?.revisitRate != null ? Number(latest.revisitRate) * 100 : null;
  const afterHoursVal = latest?.afterHoursRate != null ? Number(latest.afterHoursRate) * 100 : null;
  const totalQuestions = latest?.totalQuestions ?? null;
  const avgResponseMsVal = latest?.avgResponseTimeMs ?? null;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-2">
        <h2 className="text-text-primary font-semibold text-lg">기관 대시보드</h2>
        <PageFilters
          orgId={orgId} onOrgChange={setOrgId}
          from={from} onFromChange={setFrom}
          to={to} onToChange={setTo}
        />
      </div>

      {/* 민원응대 성과 KPI — Row 1 */}
      <div className="grid grid-cols-2 lg:grid-cols-3 gap-4 items-stretch">
        <KpiCard
          label="총 문의 수"
          value={totalQuestions != null ? totalQuestions.toLocaleString() : "-"}
          sub="건 (최신일)"
          help="당일 시민이 챗봇에 입력한 전체 질문 건수입니다. 채널(웹·모바일·키오스크)을 합산한 수치입니다."
        />
        <KpiCard
          label="자동응대 완료율"
          value={autoResolutionVal != null ? autoResolutionVal.toFixed(1) + "%" : "-"}
          status={getKpiStatus(autoResolutionVal, {
            ok: (v) => v >= 70,
            warn: (v) => v >= 60,
          })}
          help="상담원 연결 없이 챗봇만으로 대화가 완료된 비율입니다. 70% 이상이면 정상, 60% 미만이면 지식베이스 보강이 필요합니다."
        />
        <KpiCard
          label="상담 전환율"
          value={escalationVal != null ? escalationVal.toFixed(1) + "%" : "-"}
          status={getKpiStatus(escalationVal, {
            ok: (v) => v < 20,
            warn: (v) => v < 30,
          })}
          help="챗봇이 답변하지 못해 상담원·콜센터로 넘어간 비율입니다. 20% 미만이면 정상, 30% 이상이면 자주 실패하는 민원 유형을 확인해야 합니다."
        />
      </div>

      {/* 민원응대 성과 KPI — Row 2 */}
      <div className="grid grid-cols-2 lg:grid-cols-3 gap-4 items-stretch">
        <KpiCard
          label="평균 응답시간"
          value={avgResponseMsVal != null ? avgResponseMsVal.toLocaleString() + "ms" : "-"}
          status={getKpiStatus(avgResponseMsVal, {
            ok: (v) => v < 1500,
            warn: (v) => v < 2500,
          })}
          help="챗봇이 답변을 생성하는 데 걸린 평균 시간입니다. 1.5초 미만이면 정상, 2.5초를 초과하면 시스템 성능 점검이 필요합니다."
        />
        <KpiCard
          label="재방문율"
          value={revisitVal != null ? revisitVal.toFixed(1) + "%" : "-"}
          status={getKpiStatus(revisitVal, {
            ok: (v) => v < 10,
            warn: (v) => v < 15,
          })}
          help="동일 민원 건으로 재방문한 세션 비율입니다. 10% 미만이면 정상, 15% 이상이면 답변 품질 점검이 필요합니다."
        />
        <KpiCard
          label="업무시간 외 응대율"
          value={afterHoursVal != null ? afterHoursVal.toFixed(1) + "%" : "-"}
          help="야간·주말에 챗봇이 자동으로 처리한 질문 비율입니다. 챗봇이 없으면 모두 다음 영업일에 처리해야 하는 민원입니다."
        />
      </div>

      {/* 추세 차트 */}
      {metrics.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>자동응대율 / 상담 전환율 추세</CardTitle>
          </CardHeader>
          <MetricsLineChart
            data={metrics.map((m) => ({
              ...m,
              autoResolutionRate: m.autoResolutionRate != null ? Number(m.autoResolutionRate) * 100 : null,
              escalationRate: m.escalationRate != null ? Number(m.escalationRate) * 100 : null,
            }))}
            metrics={["autoResolutionRate", "escalationRate"]}
          />
        </Card>
      )}
    </div>
  );
}
