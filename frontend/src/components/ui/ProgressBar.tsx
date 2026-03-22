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
    <div className="flex items-center gap-2.5">
      <span className="w-[82px] font-mono text-[10px] text-text-secondary shrink-0">{label}</span>
      <div className="flex-1 h-[22px] bg-bg-border rounded overflow-hidden">
        <div
          className={clsx("h-full flex items-center px-2 font-mono text-[10px] font-semibold", color)}
          style={{ width: `${pct}%` }}
        >
          {pct > 12 ? `${valueMs}ms` : ""}
        </div>
      </div>
      <span className="w-[55px] text-right font-mono text-[10px] text-text-secondary shrink-0">
        {valueMs}ms
      </span>
    </div>
  );
}
