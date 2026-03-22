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
import type { DailyMetric } from "@/lib/types";
import { format, parseISO } from "date-fns";

interface MetricsLineChartProps {
  data: DailyMetric[];
  metrics?: Array<keyof DailyMetric>;
}

const COLORS = ["#4b9eff", "#00c47a", "#f5a623", "#ff4560", "#a78bfa"];

const METRIC_LABELS: Partial<Record<keyof DailyMetric, string>> = {
  totalQuestions: "전체 질문",
  totalSessions: "전체 세션",
  resolvedRate: "응답률(%)",
  fallbackRate: "Fallback율(%)",
  zeroResultRate: "무응답율(%)",
  avgResponseTimeMs: "평균 응답(ms)",
};

export function MetricsLineChart({
  data,
  metrics = ["resolvedRate", "fallbackRate", "zeroResultRate"],
}: MetricsLineChartProps) {
  const chartData = data.map((d) => ({
    ...d,
    date: format(parseISO(d.metricDate), "MM/dd"),
  }));

  return (
    <ResponsiveContainer width="100%" height={220}>
      <LineChart data={chartData} margin={{ top: 5, right: 10, left: -20, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#222836" />
        <XAxis
          dataKey="date"
          tick={{ fill: "#8b93a8", fontSize: 11 }}
          axisLine={{ stroke: "#222836" }}
          tickLine={false}
        />
        <YAxis
          tick={{ fill: "#8b93a8", fontSize: 11 }}
          axisLine={false}
          tickLine={false}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: "#13171f",
            border: "1px solid #222836",
            borderRadius: 8,
          }}
          labelStyle={{ color: "#dde2ec", fontSize: 12 }}
          itemStyle={{ fontSize: 12 }}
        />
        <Legend
          wrapperStyle={{ fontSize: 11, color: "#8b93a8" }}
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
