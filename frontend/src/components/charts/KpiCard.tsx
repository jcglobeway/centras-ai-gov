import clsx from "clsx";

interface KpiCardProps {
  label: string;
  value: string | number;
  sub?: string;
  trend?: "up" | "down" | "neutral";
  trendValue?: string;
  help?: string;
  status?: "ok" | "warn" | "critical";
  progressValue?: number;
  className?: string;
}

const statusColor: Record<string, string> = {
  ok:       "bg-success",
  warn:     "bg-warning",
  critical: "bg-error",
};

const valueColor: Record<string, string> = {
  ok:       "text-success",
  warn:     "text-warning",
  critical: "text-error",
};

const pillStyle: Record<string, string> = {
  ok:       "bg-success/10 text-success",
  warn:     "bg-warning/10 text-warning",
  critical: "bg-error/10 text-error",
};

const pillLabel: Record<string, string> = {
  ok:       "OPTIMAL",
  warn:     "WARNING",
  critical: "CRITICAL",
};

const trendIcon: Record<string, string> = {
  up:      "arrow_upward",
  down:    "arrow_downward",
  neutral: "remove",
};

const trendColor: Record<string, string> = {
  up:      "text-success",
  down:    "text-error",
  neutral: "text-text-muted",
};

export function KpiCard({
  label,
  value,
  sub,
  trend,
  trendValue,
  help,
  status,
  progressValue,
  className,
}: KpiCardProps) {
  return (
    <div
      className={clsx(
        "bg-bg-elevated border border-white/5 rounded-lg p-6 relative cursor-pointer transition-all duration-150 hover:bg-bg-prominent flex flex-col h-full",
        className
      )}
      style={{ boxShadow: "var(--shadow-card)" }}
    >
      {/* 레이블 + 상태 pill */}
      <div className="flex items-start justify-between mb-2">
        <div className="flex items-center gap-1">
          <span className="font-mono text-xs tracking-tighter uppercase text-text-muted">
            {label}
          </span>
          {help && (
            <span className="group relative">
              <span className="text-text-muted text-xs cursor-help select-none">ⓘ</span>
              <span className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 w-64 rounded-lg bg-bg-prominent border border-bg-border px-3 py-2.5 text-xs leading-relaxed text-text-secondary shadow-xl opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-[100] whitespace-normal">
                {help}
                <span className="absolute top-full left-1/2 -translate-x-1/2 border-4 border-transparent border-t-bg-border" />
              </span>
            </span>
          )}
        </div>
        {status && (
          <span className={clsx("font-mono text-[10px] font-bold px-2 py-0.5 rounded-full", pillStyle[status])}>
            {pillLabel[status]}
          </span>
        )}
      </div>

      {/* 값 + 단위 (같은 행) */}
      <div className="flex items-baseline gap-1">
        <span
          className={clsx(
            "text-3xl font-bold font-mono tracking-[-1px] leading-none",
            "text-text-primary"
          )}
        >
          {value}
        </span>
        {sub && (
          <span className="text-[11px] font-mono text-text-muted">{sub}</span>
        )}
      </div>

      {/* 트렌드 */}
      {trend && trendValue && (
        <p className={clsx("text-xs font-mono mt-2 flex items-center gap-0.5", trendColor[trend])}>
          <span className="material-symbols-outlined text-[13px] leading-none">{trendIcon[trend]}</span>
          {trendValue}
        </p>
      )}

      {/* progress bar (일반 흐름) */}
      {status && progressValue !== undefined && (
        <div className="mt-3 h-1 rounded-full bg-bg-prominent overflow-hidden">
          <div
            className={clsx("h-full transition-all", statusColor[status])}
            style={{ width: `${Math.min(progressValue, 100)}%` }}
          />
        </div>
      )}
    </div>
  );
}
