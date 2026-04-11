"use client";

import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";
import { format, parseISO } from "date-fns";

const COLORS = ["var(--accent)", "#00c47a", "#f5a623", "#ff4560", "#a78bfa"];

const METRIC_LABELS: Record<string, string> = {
  totalQuestions: "전체 질문",
  totalSessions: "전체 세션",
  resolvedRate: "응답률(%)",
  fallbackRate: "Fallback율(%)",
  zeroResultRate: "무응답율(%)",
  avgResponseTimeMs: "평균 응답(ms)",
  autoResolutionRate: "자동응대율(%)",
  escalationRate: "상담전환율(%)",
  revisitRate: "재문의율(%)",
  afterHoursRate: "업무시간외 응대율(%)",
};

export function MetricsLineChart<T extends { metricDate: string }>({
  data,
  metrics = ["resolvedRate", "fallbackRate", "zeroResultRate"],
}: {
  data: T[];
  metrics?: string[];
}) {
  const chartData = [...data]
    .sort((a, b) => a.metricDate.localeCompare(b.metricDate))
    .map((d) => ({
      ...d,
      date: format(parseISO(d.metricDate), "MM/dd"),
    }));

  return (
    <ResponsiveContainer width="100%" height={220}>
      <LineChart data={chartData} margin={{ top: 5, right: 10, left: -20, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="var(--bg-border)" />
        <XAxis
          dataKey="date"
          tick={{ fill: "var(--text-muted)", fontSize: 11 }}
          axisLine={{ stroke: "var(--bg-border)" }}
          tickLine={false}
        />
        <YAxis
          tick={{ fill: "var(--text-muted)", fontSize: 11 }}
          axisLine={false}
          tickLine={false}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: "var(--bg-surface)",
            border: "1px solid var(--bg-border)",
            borderRadius: 8,
          }}
          labelStyle={{ color: "var(--text-primary)", fontSize: 12 }}
          itemStyle={{ fontSize: 12 }}
        />
        <Legend
          wrapperStyle={{ fontSize: 11, color: "var(--text-muted)" }}
        />
        {metrics.map((key, i) => (
          <Line
            key={String(key)}
            type="monotone"
            dataKey={key as string}
            name={METRIC_LABELS[key] ?? String(key)}
            stroke={COLORS[i % COLORS.length]}
            strokeWidth={2}
            dot={false}
            activeDot={{ r: 4 }}
          />
        ))}
      </LineChart>
    </ResponsiveContainer>
  );
}
