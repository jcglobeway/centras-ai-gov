"use client";

import { useState } from "react";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import { useFilter } from "@/lib/filter-context";
import { useAuth } from "@/lib/auth";
import type { PagedResponse, Organization, Service } from "@/lib/types";

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

const RESTRICTED_ORG_ROLES = ["client_org_admin", "client_viewer"];

type Preset = "today" | "week" | "month" | "custom";

function derivePreset(from: string, to: string): Preset {
  const today = getToday();
  if (from === today && to === today) return "today";
  if (from === getDaysAgo(6) && to === today) return "week";
  if (from === getDaysAgo(29) && to === today) return "month";
  return "custom";
}

const selectClass =
  "bg-[rgba(255,255,255,0.04)] border border-[rgba(255,255,255,0.1)] text-text-primary font-inter text-[12px] font-[510] rounded-md px-2.5 h-8 focus:outline-none focus:border-accent transition-colors cursor-pointer min-w-[130px]";

const presetBtnBase =
  "px-3 h-8 font-inter text-[12px] font-[510] rounded-md border transition-colors";
const presetBtnActive =
  "border-accent text-accent bg-accent/15";
const presetBtnInactive =
  "border-[rgba(255,255,255,0.1)] text-text-secondary hover:border-[rgba(255,255,255,0.2)] hover:text-text-primary hover:bg-[rgba(255,255,255,0.04)]";

const dateInputClass =
  "bg-[rgba(255,255,255,0.04)] border border-[rgba(255,255,255,0.1)] text-text-primary font-inter text-[12px] rounded-md px-2 h-8 focus:outline-none focus:border-accent transition-colors w-[120px]";

interface PageFiltersProps {
  showOrg?: boolean;
  showService?: boolean;
  showDate?: boolean;
}

export function PageFilters({ showOrg = true, showService = false, showDate = true }: PageFiltersProps) {
  const { session } = useAuth();
  const { orgId, setOrgId, serviceId, setServiceId, from, setFrom, to, setTo } = useFilter();
  const [showDatePicker, setShowDatePicker] = useState(false);

  const { data } = useSWR<PagedResponse<Organization>>(
    session ? "/api/admin/organizations?page_size=50" : null,
    fetcher
  );

  const allOrgs = data?.items ?? [];
  const visibleOrgs =
    session && RESTRICTED_ORG_ROLES.includes(session.roleCode)
      ? allOrgs.filter((o) => session.organizationIds.includes(o.organizationId))
      : allOrgs;

  const { data: servicesData } = useSWR<PagedResponse<Service>>(
    session && showService && orgId ? `/api/admin/organizations/${orgId}/services` : null,
    fetcher
  );
  const visibleServices = servicesData?.items ?? [];

  const preset = derivePreset(from, to);

  function applyPreset(p: Preset) {
    const today = getToday();
    if (p === "today")  { setFrom(today);         setTo(today);  setShowDatePicker(false); }
    if (p === "week")   { setFrom(getDaysAgo(6));  setTo(today);  setShowDatePicker(false); }
    if (p === "month")  { setFrom(getDaysAgo(29)); setTo(today);  setShowDatePicker(false); }
    if (p === "custom") { setShowDatePicker((v) => !v); }
  }

  const presets: { key: Preset; label: string }[] = [
    { key: "today",  label: "오늘" },
    { key: "week",   label: "1주" },
    { key: "month",  label: "1개월" },
    { key: "custom", label: "날짜선택" },
  ];

  const isCustomActive = preset === "custom" || showDatePicker;

  if (!showOrg && !showService && !showDate) return null;

  return (
    <div className="flex items-center gap-2 flex-wrap">
      {/* 기관 드롭다운 */}
      {showOrg && visibleOrgs.length > 1 && (
        <select
          value={orgId}
          onChange={(e) => {
            setOrgId(e.target.value);
            setServiceId("");
          }}
          className={selectClass}
        >
          <option value="">전체 기관</option>
          {visibleOrgs.map((org) => (
            <option key={org.organizationId} value={org.organizationId}>
              {org.name}
            </option>
          ))}
        </select>
      )}
      {showOrg && visibleOrgs.length === 1 && (
        <span className="font-inter text-[12px] font-[510] text-text-primary px-2.5 h-8 flex items-center border border-[rgba(255,255,255,0.1)] rounded-md bg-[rgba(255,255,255,0.04)]">
          {visibleOrgs[0].name}
        </span>
      )}

      {showService && (
        <select
          value={serviceId}
          onChange={(e) => setServiceId(e.target.value)}
          disabled={!orgId}
          className={selectClass}
        >
          <option value="">전체 서비스</option>
          {visibleServices.map((svc) => (
            <option key={svc.serviceId} value={svc.serviceId}>
              {svc.name}
            </option>
          ))}
        </select>
      )}

      {/* 날짜 프리셋 + 직접 입력 */}
      {showDate && (
        <>
          <div className="flex items-center gap-1">
            {presets.map(({ key, label }) => {
              const isActive = key === "custom" ? isCustomActive : preset === key;
              return (
                <button
                  key={key}
                  onClick={() => applyPreset(key)}
                  className={`${presetBtnBase} ${isActive ? presetBtnActive : presetBtnInactive}`}
                >
                  {label}
                </button>
              );
            })}
          </div>

          {isCustomActive && (
            <div className="flex items-center gap-1.5">
              <input
                type="date"
                value={from}
                onChange={(e) => setFrom(e.target.value)}
                className={dateInputClass}
              />
              <span className="text-text-muted text-xs">~</span>
              <input
                type="date"
                value={to}
                onChange={(e) => setTo(e.target.value)}
                className={dateInputClass}
              />
            </div>
          )}
        </>
      )}
    </div>
  );
}
