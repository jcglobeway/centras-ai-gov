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
  error:   "bg-error/10 text-error border border-error/30",
  info:    "bg-accent/10 text-accent border border-accent/30",
  neutral: "bg-[rgba(255,255,255,0.05)] text-text-secondary border border-[rgba(255,255,255,0.05)]",
};

export function Badge({ variant = "neutral", children, className }: BadgeProps) {
  return (
    <span
      className={clsx(
        "inline-flex items-center font-inter text-[10px] font-[510] px-2 py-0.5 rounded-[2px] whitespace-nowrap",
        variantClass[variant],
        className
      )}
    >
      {children}
    </span>
  );
}
