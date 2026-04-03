import clsx from "clsx";

interface TableProps {
  children?: React.ReactNode;
  className?: string;
  colSpan?: number;
  title?: string;
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
    <th className={clsx("px-4 py-3 text-left font-inter text-[11px] font-[510] uppercase tracking-[0.1em] text-text-subtle", className)}>
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
  onClick,
}: {
  children: React.ReactNode;
  className?: string;
  onClick?: () => void;
}) {
  return (
    <tr
      className={clsx(
        "hover:bg-bg-elevated transition-colors",
        className
      )}
      onClick={onClick}
    >
      {children}
    </tr>
  );
}

export function Td({ children, className, colSpan, title }: TableProps) {
  return (
    <td colSpan={colSpan} title={title} className={clsx("px-4 py-3 text-sm text-text-secondary", className)}>
      {children}
    </td>
  );
}
