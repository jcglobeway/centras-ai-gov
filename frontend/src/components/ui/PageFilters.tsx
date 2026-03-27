"use client";

import { useState } from "react";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, Organization } from "@/lib/types";

export function getToday(): string {
  return new Date().toISOString().slice(0, 10);
}

export function getDaysAgo(n: number): string {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return d.toISOString().slice(0, 10);
}

/** @deprecated Use getDaysAgo(6) instead */
export function getWeekFrom(): string {
  return getDaysAgo(6);
}

type Preset = "today" | "week" | "month" | "custom";

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

const presetBtnBase =
  "px-3 py-1 text-xs rounded border transition-colors";
const presetBtnActive =
  "border-accent text-accent bg-accent/10";
const presetBtnInactive =
  "border-bg-border text-text-secondary hover:border-accent/50 hover:text-text-primary";

export function PageFilters({
  orgId,
  onOrgChange,
  from,
  onFromChange,
  to,
  onToChange,
}: PageFiltersProps) {
  const [preset, setPreset] = useState<Preset>("today");

  const { data } = useSWR<PagedResponse<Organization>>(
    "/api/admin/organizations?page_size=50",
    fetcher
  );

  const orgs = data?.items ?? [];

  function applyPreset(p: Preset) {
    setPreset(p);
    const today = getToday();
    if (p === "today") {
      onFromChange(today);
      onToChange(today);
    } else if (p === "week") {
      onFromChange(getDaysAgo(6));
      onToChange(today);
    } else if (p === "month") {
      onFromChange(getDaysAgo(29));
      onToChange(today);
    }
    // custom: keep current from/to, just show inputs
  }

  const presets: { key: Preset; label: string }[] = [
    { key: "today", label: "오늘" },
    { key: "week", label: "최근 일주일" },
    { key: "month", label: "1개월" },
    { key: "custom", label: "날짜선택" },
  ];

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

      <div className="flex items-center gap-1">
        {presets.map(({ key, label }) => (
          <button
            key={key}
            onClick={() => applyPreset(key)}
            className={`${presetBtnBase} ${preset === key ? presetBtnActive : presetBtnInactive}`}
          >
            {label}
          </button>
        ))}
      </div>

      {preset === "custom" && (
        <div className="flex items-center gap-1">
          <input
            type="date"
            value={from}
            onChange={(e) => onFromChange(e.target.value)}
            className={selectClass}
          />
          <span className="text-text-muted text-xs">~</span>
          <input
            type="date"
            value={to}
            onChange={(e) => onToChange(e.target.value)}
            className={selectClass}
          />
        </div>
      )}
    </div>
  );
}
