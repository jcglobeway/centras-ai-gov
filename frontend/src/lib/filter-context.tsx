"use client";

import { createContext, useContext, useState, useEffect, type ReactNode } from "react";

const STORAGE_KEY = "globalFilter";

function getToday(): string {
  return new Date().toISOString().slice(0, 10);
}

function getDaysAgo(n: number): string {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return d.toISOString().slice(0, 10);
}

interface FilterState {
  orgId: string;
  serviceId: string;
  from: string;
  to: string;
}

const DEFAULT_STATE: FilterState = {
  orgId: "",
  serviceId: "",
  from: getDaysAgo(29),
  to: getToday(),
};

function loadFromStorage(): FilterState {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return { ...DEFAULT_STATE, ...JSON.parse(raw) };
  } catch {
    // ignore
  }
  return DEFAULT_STATE;
}

interface FilterContextValue extends FilterState {
  setOrgId: (id: string) => void;
  setServiceId: (id: string) => void;
  setFrom: (d: string) => void;
  setTo: (d: string) => void;
}

const FilterContext = createContext<FilterContextValue | null>(null);

export function FilterProvider({ children }: { children: ReactNode }) {
  const [hydrated, setHydrated] = useState(false);
  const [state, setState] = useState<FilterState>(DEFAULT_STATE);

  // 마운트 후 localStorage에서 복원
  useEffect(() => {
    setState(loadFromStorage());
    setHydrated(true);
  }, []);

  // hydration 완료 후에만 저장 (초기 기본값으로 덮어쓰는 것 방지)
  useEffect(() => {
    if (!hydrated) return;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  }, [state, hydrated]);

  const setOrgId = (orgId: string) => setState((s) => ({ ...s, orgId }));
  const setServiceId = (serviceId: string) => setState((s) => ({ ...s, serviceId }));
  const setFrom  = (from: string)  => setState((s) => ({ ...s, from }));
  const setTo    = (to: string)    => setState((s) => ({ ...s, to }));

  return (
    <FilterContext.Provider value={{ ...state, setOrgId, setServiceId, setFrom, setTo }}>
      {children}
    </FilterContext.Provider>
  );
}

export function useFilter(): FilterContextValue {
  const ctx = useContext(FilterContext);
  if (!ctx) throw new Error("useFilter must be used within FilterProvider");
  return ctx;
}
