"use client";

import { usePathname } from "next/navigation";
import { resolveBreadcrumb } from "@/lib/nav-config";
import { PageFilters } from "@/components/ui/PageFilters";

export function Breadcrumb() {
  const pathname = usePathname();
  const crumb = resolveBreadcrumb(pathname);

  if (!crumb) return null;

  const { filters } = crumb;
  const hasFilter = filters.org || filters.service || filters.date;

  return (
    <div className="border-b border-[rgba(255,255,255,0.06)] bg-bg-base/60 px-8 py-3">
      <div className="max-w-[1400px] flex items-center gap-2">
        {/* 브레드크럼 — 좌측 */}
        <span className="font-inter text-[12px] text-text-muted">{crumb.portalLabel}</span>
        <span className="text-text-muted text-[11px]">/</span>
        {crumb.section && (
          <>
            <span className="font-inter text-[12px] text-text-muted">{crumb.section}</span>
            <span className="text-text-muted text-[11px]">/</span>
          </>
        )}
        <span className="font-inter text-[13px] font-[600] text-text-primary">{crumb.pageLabel}</span>

        {hasFilter && (
          <>
            <div className="flex-1" />
            <PageFilters showOrg={filters.org} showService={filters.service} showDate={filters.date} />
          </>
        )}
      </div>
    </div>
  );
}
