import clsx from "clsx";

type Variant = "success" | "warning" | "error" | "info" | "neutral";

interface BadgeProps {
  variant?: Variant;
  children: React.ReactNode;
  className?: string;
}

const variantClass: Record<Variant, string> = {
  success: "bg-success/10 text-success border border-success/30",
  warning: "bg-warning/10 text-warning border border-warning/30",
  error: "bg-error/10 text-error border border-error/30",
  info: "bg-accent/10 text-accent border border-accent/30",
  neutral: "bg-bg-elevated text-text-secondary border border-bg-border",
};

export function Badge({ variant = "neutral", children, className }: BadgeProps) {
  return (
    <span
      className={clsx(
        "inline-flex items-center font-mono text-[10px] px-1.5 py-0.5 rounded-full whitespace-nowrap",
        variantClass[variant],
        className
      )}
    >
      {children}
    </span>
  );
}
