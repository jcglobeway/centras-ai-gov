"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import clsx from "clsx";
import { useAuth, PORTAL_ROLES } from "@/lib/auth";
import type { RoleCode } from "@/lib/types";

interface NavItem {
  href: string;
  label: string;
  icon: string;
}

const OPS_NAV: NavItem[] = [
  { href: "/ops", label: "운영 대시보드", icon: "◈" },
  { href: "/ops/organizations", label: "기관 관리", icon: "⊞" },
  { href: "/ops/indexing", label: "RAG 인덱싱", icon: "⊛" },
  { href: "/ops/quality", label: "품질 모니터링", icon: "◎" },
  { href: "/ops/cost", label: "비용 & 건강도", icon: "◉" },
  { href: "/ops/safety", label: "안전성", icon: "◐" },
  { href: "/ops/incidents", label: "장애 관리", icon: "⚠" },
];

const CLIENT_NAV: NavItem[] = [
  { href: "/client", label: "기관 대시보드", icon: "◈" },
  { href: "/client/performance", label: "민원응대 성과", icon: "◉" },
  { href: "/client/failure", label: "실패/전환 분석", icon: "◎" },
  { href: "/client/knowledge", label: "지식 현황", icon: "⊞" },
];

const QA_NAV: NavItem[] = [
  { href: "/qa", label: "검수 대시보드", icon: "◈" },
  { href: "/qa/unresolved", label: "미응답/오답 관리", icon: "⊛" },
  { href: "/qa/documents", label: "문서 관리", icon: "⊞" },
  { href: "/qa/approvals", label: "승인 워크플로우", icon: "◎" },
];

const PORTALS = [
  { key: "ops", label: "운영사 어드민", basePath: "/ops", nav: OPS_NAV },
  { key: "client", label: "기관 어드민", basePath: "/client", nav: CLIENT_NAV },
  { key: "qa", label: "QA 어드민", basePath: "/qa", nav: QA_NAV },
] as const;

function getActivePortal(role: RoleCode, pathname: string) {
  const accessible = PORTALS.filter((p) => PORTAL_ROLES[p.key].includes(role));
  return accessible.find((p) => pathname.startsWith(p.basePath)) ?? accessible[0];
}

export function Sidebar() {
  const { session } = useAuth();
  const pathname = usePathname();

  if (!session) return null;

  const active = getActivePortal(session.roleCode, pathname);

  return (
    <aside className="w-56 shrink-0 bg-bg-surface border-r border-bg-border flex flex-col h-screen sticky top-0">
      {/* brand */}
      <div className="px-4 py-4 border-b border-bg-border">
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 rounded bg-accent flex items-center justify-center text-white font-bold text-xs">
            C
          </div>
          <div>
            <p className="text-text-primary text-xs font-semibold">Centras AI Gov</p>
            <p className="text-text-muted text-[10px]">{active.label}</p>
          </div>
        </div>
      </div>

      {/* nav */}
      <nav className="flex-1 px-2 py-2 space-y-0.5 overflow-y-auto">
        {active.nav.map((item) => {
          const isActive =
            item.href === "/ops" || item.href === "/client" || item.href === "/qa"
              ? pathname === item.href
              : pathname.startsWith(item.href);

          return (
            <Link
              key={item.href}
              href={item.href}
              className={clsx(
                "flex items-center gap-2.5 px-3 py-2 rounded-lg text-xs transition-colors",
                isActive
                  ? "bg-accent/10 text-accent font-medium"
                  : "text-text-secondary hover:bg-bg-elevated hover:text-text-primary"
              )}
            >
              <span className="text-sm w-4 text-center">{item.icon}</span>
              {item.label}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
