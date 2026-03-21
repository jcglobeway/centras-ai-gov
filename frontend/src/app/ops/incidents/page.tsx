"use client";

import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, DailyMetric } from "@/lib/types";
import { Card } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Spinner } from "@/components/ui/Spinner";
import type { ComponentProps } from "react";

type BadgeVariant = ComponentProps<typeof Badge>["variant"];

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

export default function IncidentsPage() {
  const { data, isLoading } = useSWR<PagedResponse<DailyMetric>>(
    `/api/admin/metrics/daily?page_size=1`,
    fetcher
  );

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-48">
        <Spinner />
      </div>
    );
  }

  const latest = data?.items?.[data.items.length - 1];
  const alerts = buildAlerts(latest);

  return (
    <div className="space-y-4">
      <h2 className="text-text-primary font-semibold text-lg">장애/이슈 관리</h2>
      <Card>
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
    </div>
  );
}
