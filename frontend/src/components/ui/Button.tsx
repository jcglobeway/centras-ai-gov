import clsx from "clsx";
import type { ButtonHTMLAttributes } from "react";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary" | "ghost" | "danger";
  size?: "sm" | "md";
}

const variantClass = {
  primary:   "bg-accent hover:bg-accent-hover text-white",
  secondary: "bg-[rgba(255,255,255,0.02)] hover:bg-[rgba(255,255,255,0.05)] text-text-primary border border-[rgba(255,255,255,0.08)]",
  ghost:     "hover:bg-[rgba(255,255,255,0.04)] text-text-secondary hover:text-text-primary",
  danger:    "bg-error/10 hover:bg-error/20 text-error",
};

const sizeClass = {
  sm: "px-3 py-1.5 text-xs",
  md: "px-4 py-2 text-sm",
};

export function Button({
  variant = "primary",
  size = "md",
  className,
  children,
  ...props
}: ButtonProps) {
  return (
    <button
      className={clsx(
        "inline-flex items-center gap-1.5 font-inter font-[510] rounded-md transition-colors disabled:opacity-50",
        variantClass[variant],
        sizeClass[size],
        className
      )}
      {...props}
    >
      {children}
    </button>
  );
}
