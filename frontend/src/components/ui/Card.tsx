import clsx from "clsx";

interface CardProps {
  children: React.ReactNode;
  className?: string;
}

interface CardTitleProps extends CardProps {
  tag?: string;
}

export function Card({ children, className }: CardProps) {
  return (
    <div
      className={clsx(
        "bg-bg-surface border border-bg-border rounded-xl overflow-hidden transition-colors hover:border-bg-elevated",
        className
      )}
    >
      {children}
    </div>
  );
}

export function CardHeader({ children, className }: CardProps) {
  return (
    <div className={clsx("px-4 pt-4 pb-0 mb-3", className)}>
      {children}
    </div>
  );
}

export function CardTitle({ children, tag, className }: CardTitleProps) {
  return (
    <div className={className}>
      {tag && (
        <p className="font-mono text-[10px] uppercase tracking-[0.5px] text-text-muted mb-0.5">
          {tag}
        </p>
      )}
      <h3 className="text-text-primary font-semibold text-[12px]">{children}</h3>
    </div>
  );
}
