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

const stripeColor: Record<string, string> = {
  ok: "bg-success",
  warn: "bg-warning",
  critical: "bg-error",
};

const valueColor: Record<string, string> = {
  ok: "text-success",
  warn: "text-warning",
  critical: "text-error",
};

const badgeStyle: Record<string, string> = {
  ok: "bg-success/10 text-success border border-success/30",
  warn: "bg-warning/10 text-warning border border-warning/30",
  critical: "bg-error/10 text-error border border-error/30",
};

const badgeLabel: Record<string, string> = {
  ok: "정상",
  warn: "경고",
  critical: "긴급",
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
        "bg-bg-surface border border-bg-border rounded-xl overflow-hidden relative cursor-pointer transition-all duration-150 hover:-translate-y-px hover:border-bg-elevated",
        className
      )}
    >
      {/* 상단 상태 스트라이프 */}
      {status && (
        <div className={clsx("absolute top-0 left-0 right-0 h-0.5", stripeColor[status])} />
      )}
      <div className="p-4 pt-5">
        {/* 레이블 + 상태 뱃지 */}
        <div className="flex items-center justify-between mb-2">
          <div className="flex items-center gap-1">
            <span className="font-mono text-[10px] tracking-[0.4px] uppercase text-text-secondary">
              {label}
            </span>
            {help && (
              <span className="group relative">
                <span className="text-text-muted text-[10px] cursor-help select-none">ⓘ</span>
                <span className="absolute bottom-full left-1/2 -translate-x-1/2 mb-1 w-52 rounded-lg bg-bg-surface border border-bg-border px-3 py-2 text-xs text-text-secondary shadow-lg opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-50 whitespace-normal">
                  {help}
                </span>
              </span>
            )}
          </div>
          {status && (
            <span className={clsx("font-mono text-[10px] px-1.5 py-0.5 rounded-full", badgeStyle[status])}>
              {badgeLabel[status]}
            </span>
          )}
        </div>

        {/* 값 */}
        <p
          className={clsx(
            "text-[26px] font-bold tracking-[-1px] leading-none mb-2",
            status ? valueColor[status] : "text-text-primary"
          )}
        >
          {value}
          {sub && (
            <span className="text-[13px] font-normal text-text-secondary ml-1">
              {sub}
            </span>
          )}
        </p>

        {/* 푸터: 트렌드 */}
        {trendValue && (
          <div className="flex items-center justify-between">
            <span
              className={clsx("font-mono text-[10px]", {
                "text-success": trend === "up",
                "text-error": trend === "down",
                "text-text-muted": !trend || trend === "neutral",
              })}
            >
              {trend === "up" ? "▲ " : trend === "down" ? "▼ " : ""}
              {trendValue}
            </span>
          </div>
        )}
      </div>
    </div>
  );
}
