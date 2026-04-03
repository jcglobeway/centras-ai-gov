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
      className={clsx("rounded-lg", className)}
      style={{
        background: "var(--card-bg)",
        border: "1px solid var(--card-border)",
      }}
    >
      {children}
    </div>
  );
}

export function CardHeader({ children, className }: CardProps) {
  return (
    <div className={clsx("px-5 pt-5 pb-0 mb-4", className)}>
      {children}
    </div>
  );
}

export function CardTitle({ children, tag, className }: CardTitleProps) {
  return (
    <div className={className}>
      {tag && (
        <p className="font-inter text-[11px] uppercase tracking-[0.1em] text-text-subtle mb-0.5">
          {tag}
        </p>
      )}
      <h3 className="font-inter text-text-primary font-[510] text-sm">{children}</h3>
    </div>
  );
}
