import { Badge } from "./Badge";

interface ScoreRow {
  label: string;
  value: number | null;
  target: number;
  unit?: string;
}

interface ScoreTableProps {
  rows: ScoreRow[];
}

function getVariant(value: number | null, target: number) {
  if (value === null) return "neutral" as const;
  if (value >= target) return "success" as const;
  if (value >= target * 0.9) return "warning" as const;
  return "error" as const;
}

function getLabel(value: number | null, target: number) {
  if (value === null) return "N/A";
  if (value >= target) return "달성";
  if (value >= target * 0.9) return "근접";
  return "미달";
}

export function ScoreTable({ rows }: ScoreTableProps) {
  return (
    <table className="w-full text-sm">
      <thead>
        <tr className="text-text-secondary text-xs border-b border-bg-border">
          <th className="text-left pb-2 font-medium">지표</th>
          <th className="text-right pb-2 font-medium">점수</th>
          <th className="text-right pb-2 font-medium">목표</th>
          <th className="text-right pb-2 font-medium">상태</th>
        </tr>
      </thead>
      <tbody className="divide-y divide-bg-border">
        {rows.map((row) => (
          <tr key={row.label}>
            <td className="py-2 text-text-primary">{row.label}</td>
            <td className="py-2 text-right font-mono text-text-primary">
              {row.value !== null ? row.value.toFixed(2) : "–"}
            </td>
            <td className="py-2 text-right font-mono text-text-secondary">
              {row.target.toFixed(2)}
            </td>
            <td className="py-2 text-right">
              <Badge variant={getVariant(row.value, row.target)}>
                {getLabel(row.value, row.target)}
              </Badge>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
