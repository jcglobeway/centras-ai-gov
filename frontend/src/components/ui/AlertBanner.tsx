"use client";

import clsx from "clsx";

interface AlertBannerProps {
  variant: "warn" | "critical";
  message: string;
  time?: string;
  onDismiss: () => void;
}

const styles = {
  warn: "bg-warning/10 border-warning/30 text-warning",
  critical: "bg-error/10 border-error/30 text-error",
};

const dotStyles = {
  warn: "bg-warning",
  critical: "bg-error",
};

export function AlertBanner({ variant, message, time, onDismiss }: AlertBannerProps) {
  return (
    <div
      className={clsx(
        "flex items-center gap-3 border rounded-lg px-4 py-3 text-sm",
        styles[variant]
      )}
    >
      <span className={clsx("w-2 h-2 rounded-full shrink-0", dotStyles[variant])} />
      <span className="flex-1">
        {message}
        {time && <span className="ml-2 opacity-60 text-xs">{time}</span>}
      </span>
      <button
        onClick={onDismiss}
        className="opacity-60 hover:opacity-100 transition-opacity text-base leading-none"
        aria-label="닫기"
      >
        ✕
      </button>
    </div>
  );
}
