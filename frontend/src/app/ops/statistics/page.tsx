"use client";

import { useState } from "react";
import Link from "next/link";
import useSWR from "swr";
import {
  PieChart,
  Pie,
  Cell,
  Legend,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import { fetcher } from "@/lib/api";
import type { PagedResponse, DailyMetric } from "@/lib/types";
import { KpiCard } from "@/components/charts/KpiCard";
import { MetricsLineChart } from "@/components/charts/MetricsLineChart";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Spinner } from "@/components/ui/Spinner";
import { PageFilters, getWeekFrom, getToday } from "@/components/ui/PageFilters";
import { PageGuide } from "@/components/ui/PageGuide";

const CHART_COLORS = ["#2563eb", "#8b5cf6", "#10b981", "#f59e0b", "#ef4444", "#6b7280", "#06b6d4", "#f97316"];

interface CategoryItem { category: string; count: number; }
interface CategoryDistributionResponse { items: CategoryItem[]; total: number; }

function getKpiStatus(
  value: number | null,
  thresholds: { ok: (v: number) => boolean; warn: (v: number) => boolean }
): "ok" | "warn" | "critical" | undefined {
  if (value == null) return undefined;
  if (thresholds.ok(value))   return "ok";
  if (thresholds.warn(value)) return "warn";
  return "critical";
}

export default function StatisticsPage() {
  const [orgId, setOrgId] = useState("");
  const [from,  setFrom]  = useState(getWeekFrom);
  const [to,    setTo]    = useState(getToday);

  const params = new URLSearchParams({ page_size: "14" });
  if (orgId) params.set("organization_id", orgId);
  if (from)  params.set("from_date", from);
  if (to)    params.set("to_date", to);

  const { data, error, isLoading } = useSWR<PagedResponse<DailyMetric>>(
    `/api/admin/metrics/daily?${params}`,
    fetcher
  );

  const { data: categoryData } = useSWR<CategoryDistributionResponse>(
    `/api/admin/metrics/category-distribution?${params}`,
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
  const latest  = metrics[metrics.length - 1];

  const totalQuestionsVal   = latest?.totalQuestions    ?? null;
  const resolvedRateVal     = latest?.resolvedRate      ?? null;
  const zeroResultRateVal   = latest?.zeroResultRate    ?? null;

  return (
    <div className="space-y-6">
      <PageGuide
        description="일별 질의량과 카테고리 분포를 파악하는 화면입니다."
        tips={[
          "Knowledge Gap Rate가 높으면 자주 묻는 주제의 문서가 지식베이스에 없다는 신호입니다.",
          "카테고리 분포를 보고 어떤 민원이 많은지 파악해 문서 우선순위를 결정하세요.",
          "'미해결 질의 바로가기'를 클릭하면 실제 미답변 질문 목록을 확인할 수 있습니다.",
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

      {/* KPI 3개 */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <KpiCard
          label="총 질의 수"
          value={totalQuestionsVal != null ? totalQuestionsVal.toLocaleString() : "-"}
          sub="건"
          help="최신 집계일 기준 전체 기관의 총 질의 건수입니다."
        />
        <KpiCard
          label="세션 성공률"
          value={resolvedRateVal != null ? resolvedRateVal.toFixed(1) + "%" : "-"}
          status={getKpiStatus(resolvedRateVal, {
            ok:   (v) => v >= 90,
            warn: (v) => v >= 80,
          })}
          progressValue={resolvedRateVal ?? undefined}
          help="정상 답변이 제공된 세션 비율. 90% 이상이면 정상입니다."
        />
        <div className="relative">
          <KpiCard
            label="Knowledge Gap Rate"
            value={zeroResultRateVal != null ? zeroResultRateVal.toFixed(1) + "%" : "-"}
            status={getKpiStatus(zeroResultRateVal, {
              ok:   (v) => v < 5,
              warn: (v) => v < 8,
            })}
            progressValue={zeroResultRateVal != null ? (zeroResultRateVal / 20) * 100 : undefined}
            help="벡터 검색에서 관련 문서가 없어 답변 불가한 비율. 5% 초과 시 문서 추가 필요."
          />
          <div className="px-4 pb-3 -mt-1">
            <Link
              href="/ops/unresolved"
              className="text-[11px] text-accent hover:underline"
            >
              미처리 건수 → 미해결 질의 보기
            </Link>
          </div>
        </div>
      </div>

      {/* 일별 질의 수 추이 */}
      {metrics.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>일별 질의 수 추이 (14일)</CardTitle>
          </CardHeader>
          <MetricsLineChart
            data={metrics}
            metrics={["totalQuestions"]}
          />
        </Card>
      )}

      {/* 카테고리 분포 도넛 차트 */}
      <Card>
        <CardHeader>
          <CardTitle>카테고리 분포</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4">
          <ResponsiveContainer width="100%" height={260}>
            <PieChart>
              <Pie
                data={(categoryData?.items ?? []).map((item, i) => ({
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
                contentStyle={{
                  backgroundColor: "#13171f",
                  border: "1px solid #222836",
                  borderRadius: 8,
                }}
                labelStyle={{ color: "#dde2ec", fontSize: 12 }}
                itemStyle={{ fontSize: 12 }}
                formatter={(value: number) => [`${value}%`, ""]}
              />
              <Legend
                wrapperStyle={{ fontSize: 11, color: "#8b93a8" }}
              />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </Card>
    </div>
  );
}
