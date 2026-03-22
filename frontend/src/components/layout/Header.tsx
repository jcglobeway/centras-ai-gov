"use client";

import { useRef, useState, useEffect } from "react";
import Link from "next/link";
import { useAuth, PORTAL_ROLES } from "@/lib/auth";

const ROLE_LABEL: Record<string, string> = {
  super_admin: "최고관리자",
  ops_admin: "운영관리자",
  client_admin: "기관관리자",
  client_org_admin: "기관관리자",
  client_viewer: "기관뷰어",
  qa_manager: "품질관리자",
  knowledge_editor: "지식편집자",
};

const PORTAL_INFO = [
  { key: "ops", label: "운영사 포털", basePath: "/ops" },
  { key: "client", label: "기관 포털", basePath: "/client" },
  { key: "qa", label: "QA 포털", basePath: "/qa" },
] as const;

interface HeaderProps {
  title: string;
}

export function Header({ title }: HeaderProps) {
  const { session, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, []);

  const accessiblePortals = session
    ? PORTAL_INFO.filter((p) => PORTAL_ROLES[p.key].includes(session.roleCode))
    : [];

  return (
    <header className="h-12 border-b border-bg-border bg-bg-surface flex items-center px-4 gap-4">
      <h1 className="text-text-primary text-sm font-semibold flex-1">{title}</h1>

      {session && (
        <div className="relative" ref={ref}>
          <button
            onClick={() => setOpen((v) => !v)}
            className="flex items-center gap-2 px-2 py-1 rounded-lg hover:bg-bg-elevated transition-colors"
          >
            <div className="text-right">
              <p className="text-text-primary text-xs font-medium leading-none">
                {session.displayName}
              </p>
              <p className="text-text-muted text-[10px] mt-0.5">
                {ROLE_LABEL[session.roleCode] ?? session.roleCode}
              </p>
            </div>
            <span className="text-text-muted text-xs">▾</span>
          </button>

          {open && (
            <div className="absolute right-0 top-full mt-1 w-44 bg-bg-surface border border-bg-border rounded-xl shadow-lg z-50 overflow-hidden">
              <div className="px-3 py-2.5 border-b border-bg-border">
                <p className="text-text-primary text-xs font-semibold">{session.displayName}</p>
                <p className="text-text-muted text-[10px] mt-0.5">{ROLE_LABEL[session.roleCode] ?? session.roleCode}</p>
              </div>

              {accessiblePortals.length > 1 && (
                <>
                  <div className="py-1">
                    {accessiblePortals.map((p) => (
                      <Link
                        key={p.key}
                        href={p.basePath}
                        onClick={() => setOpen(false)}
                        className="block px-3 py-1.5 text-xs text-text-secondary hover:bg-bg-elevated hover:text-text-primary transition-colors"
                      >
                        {p.label}
                      </Link>
                    ))}
                  </div>
                  <div className="border-t border-bg-border" />
                </>
              )}

              <div className="py-1">
                <button
                  onClick={() => { setOpen(false); logout(); }}
                  className="w-full text-left px-3 py-1.5 text-xs text-error hover:bg-bg-elevated transition-colors"
                >
                  로그아웃
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </header>
  );
}
