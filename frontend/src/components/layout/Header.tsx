"use client";

import { useRef, useState, useEffect } from "react";
import Link from "next/link";
import { useAuth, PORTAL_ROLES } from "@/lib/auth";
import { useTheme } from "@/lib/theme";

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
  const { theme, toggle } = useTheme();
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
    <header className="h-14 border-b border-[rgba(255,255,255,0.05)] bg-bg-base/80 backdrop-blur-md flex items-center px-6 gap-4">
      <h1 className="font-inter text-text-primary text-[13px] font-[510] flex-1 tracking-tight">{title}</h1>

      {session && (
        <div className="flex items-center gap-2">
          {/* 테마 토글 */}
          <button
            onClick={toggle}
            className="p-1.5 rounded-md text-text-subtle hover:text-text-secondary hover:bg-[rgba(255,255,255,0.04)] transition-colors"
          >
            <span className="material-symbols-outlined text-[18px]">
              {theme === 'dark' ? 'light_mode' : 'dark_mode'}
            </span>
          </button>

          {/* 유저 메뉴 */}
          <div className="relative" ref={ref}>
            <button
              onClick={() => setOpen((v) => !v)}
              className="flex items-center gap-2 px-2.5 py-1.5 rounded-md hover:bg-[rgba(255,255,255,0.04)] transition-colors"
            >
              <div className="text-right">
                <p className="font-inter text-text-primary text-[12px] font-[510] leading-none">
                  {session.displayName}
                </p>
                <p className="font-inter text-text-subtle text-[11px] mt-0.5">
                  {ROLE_LABEL[session.roleCode] ?? session.roleCode}
                </p>
              </div>
              <span className="text-text-subtle text-[10px]">▾</span>
            </button>

            {open && (
              <div className="absolute right-0 top-full mt-1 w-44 bg-bg-surface border border-[rgba(255,255,255,0.08)] rounded-lg shadow-lg z-50 overflow-hidden">
                <div className="px-3 py-2.5 border-b border-[rgba(255,255,255,0.05)]">
                  <p className="font-inter text-text-primary text-[12px] font-[510]">{session.displayName}</p>
                  <p className="font-inter text-text-subtle text-[11px] mt-0.5">{ROLE_LABEL[session.roleCode] ?? session.roleCode}</p>
                </div>

                {accessiblePortals.length > 1 && (
                  <>
                    <div className="py-1">
                      {accessiblePortals.map((p) => (
                        <Link
                          key={p.key}
                          href={p.basePath}
                          onClick={() => setOpen(false)}
                          className="block px-3 py-1.5 font-inter text-[12px] text-text-secondary hover:bg-[rgba(255,255,255,0.04)] hover:text-text-primary transition-colors"
                        >
                          {p.label}
                        </Link>
                      ))}
                    </div>
                    <div className="border-t border-[rgba(255,255,255,0.05)]" />
                  </>
                )}

                <div className="py-1">
                  <button
                    onClick={() => { setOpen(false); logout(); }}
                    className="w-full text-left px-3 py-1.5 font-inter text-[12px] text-error hover:bg-[rgba(255,255,255,0.04)] transition-colors"
                  >
                    로그아웃
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </header>
  );
}
