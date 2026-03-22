"use client";

import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, Organization } from "@/lib/types";

export function getWeekFrom(): string {
  const today = new Date();
  const day = today.getDay();
  const diff = day === 0 ? -6 : 1 - day; // 이번 주 월요일
  const monday = new Date(today);
  monday.setDate(today.getDate() + diff);
  return monday.toISOString().slice(0, 10);
}

export function getToday(): string {
  return new Date().toISOString().slice(0, 10);
}

interface PageFiltersProps {
  orgId: string;
  onOrgChange: (id: string) => void;
  from: string;
  onFromChange: (d: string) => void;
  to: string;
  onToChange: (d: string) => void;
}

const selectClass =
  "bg-bg-elevated border border-bg-border text-text-secondary text-xs rounded px-2 py-1 focus:outline-none focus:border-accent";

export function PageFilters({
  orgId,
  onOrgChange,
  from,
  onFromChange,
  to,
  onToChange,
}: PageFiltersProps) {
  const { data } = useSWR<PagedResponse<Organization>>(
    "/api/admin/organizations?page_size=50",
    fetcher
  );

  const orgs = data?.items ?? [];

  return (
    <div className="flex items-center gap-2 flex-wrap">
      {orgs.length > 1 && (
        <select
          value={orgId}
          onChange={(e) => onOrgChange(e.target.value)}
          className={selectClass}
        >
          <option value="">전체 기관</option>
          {orgs.map((org) => (
            <option key={org.organizationId} value={org.organizationId}>
              {org.name}
            </option>
          ))}
        </select>
      )}

      <input
        type="date"
        value={from}
        onChange={(e) => onFromChange(e.target.value)}
        className={selectClass}
        placeholder="시작일"
      />
      <span className="text-text-muted text-xs">~</span>
      <input
        type="date"
        value={to}
        onChange={(e) => onToChange(e.target.value)}
        className={selectClass}
        placeholder="종료일"
      />
    </div>
  );
}
