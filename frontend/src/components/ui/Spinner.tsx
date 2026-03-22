import clsx from "clsx";

export function Spinner({ className }: { className?: string }) {
  return (
    <div
      className={clsx(
        "w-5 h-5 border-2 border-bg-border border-t-accent rounded-full animate-spin",
        className
      )}
    />
  );
}
