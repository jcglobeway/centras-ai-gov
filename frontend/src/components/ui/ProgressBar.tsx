import clsx from "clsx";

interface ProgressBarProps {
  label: string;
  valueMs: number;
  maxMs: number;
  color: string;
}

export function ProgressBar({ label, valueMs, maxMs, color }: ProgressBarProps) {
  const pct = Math.min((valueMs / maxMs) * 100, 100);
  return (
    <div className="flex items-center gap-3">
      <span className="w-24 text-xs text-text-secondary shrink-0">{label}</span>
      <div className="flex-1 h-5 bg-bg-border rounded overflow-hidden">
        <div
          className={clsx("h-full flex items-center px-2 text-xs font-mono font-semibold text-white", color)}
          style={{ width: `${pct}%` }}
        >
          {pct > 15 ? `${valueMs}ms` : ""}
        </div>
      </div>
      <span className="w-16 text-right text-xs font-mono text-text-secondary shrink-0">
        {valueMs}ms
      </span>
    </div>
  );
}
