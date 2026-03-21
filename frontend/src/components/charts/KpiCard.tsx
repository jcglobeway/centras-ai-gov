import clsx from "clsx";

interface KpiCardProps {
  label: string;
  value: string | number;
  sub?: string;
  trend?: "up" | "down" | "neutral";
  trendValue?: string;
  help?: string;
  status?: "ok" | "warn" | "critical";
  className?: string;
}

const statusStripe: Record<string, string> = {
  ok: "bg-success",
  warn: "bg-warning",
  critical: "bg-error",
};

export function KpiCard({
  label,
  value,
  sub,
  trend,
  trendValue,
  help,
  status,
  className,
}: KpiCardProps) {
  return (
    <div
      className={clsx(
        "bg-bg-surface border border-bg-border rounded-xl overflow-hidden",
        className
      )}
    >
      {status && (
        <div className={clsx("h-0.5 w-full", statusStripe[status])} />
      )}
      <div className="p-4">
      <div className="flex items-center gap-1 mb-2">
        <p className="text-text-secondary text-xs">{label}</p>
        {help && (
          <span className="group relative">
            <span className="text-text-muted text-xs cursor-help select-none">ⓘ</span>
            <span className="absolute bottom-full left-1/2 -translate-x-1/2 mb-1 w-52 rounded-lg bg-bg-surface border border-bg-border px-3 py-2 text-xs text-text-secondary shadow-lg opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-50 whitespace-normal">
              {help}
            </span>
          </span>
        )}
      </div>
      <p className="text-text-primary text-2xl font-semibold font-mono">{value}</p>
      {(sub || trend) && (
        <div className="flex items-center gap-2 mt-1">
          {sub && <span className="text-text-muted text-xs">{sub}</span>}
          {trend && trendValue && (
            <span
              className={clsx("text-xs font-medium", {
                "text-success": trend === "up",
                "text-error": trend === "down",
                "text-text-muted": trend === "neutral",
              })}
            >
              {trend === "up" ? "▲" : trend === "down" ? "▼" : "–"}{" "}
              {trendValue}
            </span>
          )}
        </div>
      )}
      </div>
    </div>
  );
}
