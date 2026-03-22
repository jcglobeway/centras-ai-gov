import clsx from "clsx";

interface TableProps {
  children: React.ReactNode;
  className?: string;
}

export function Table({ children, className }: TableProps) {
  return (
    <div className={clsx("overflow-x-auto", className)}>
      <table className="w-full text-sm">{children}</table>
    </div>
  );
}

export function Thead({ children }: { children: React.ReactNode }) {
  return (
    <thead>
      <tr className="border-b border-bg-border">
        {children}
      </tr>
    </thead>
  );
}

export function Th({ children, className }: TableProps) {
  return (
    <th className={clsx("px-3 py-2.5 text-left font-mono text-[10px] uppercase tracking-[0.4px] text-text-muted", className)}>
      {children}
    </th>
  );
}

export function Tbody({ children }: { children: React.ReactNode }) {
  return <tbody className="divide-y divide-bg-border">{children}</tbody>;
}

export function Tr({
  children,
  className,
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <tr
      className={clsx(
        "hover:bg-bg-elevated transition-colors",
        className
      )}
    >
      {children}
    </tr>
  );
}

export function Td({ children, className }: TableProps) {
  return (
    <td className={clsx("px-3 py-2 text-[11px] text-text-secondary", className)}>
      {children}
    </td>
  );
}
