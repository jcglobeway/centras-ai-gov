"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import clsx from "clsx";
import { useAuth, PORTAL_ROLES } from "@/lib/auth";
import { PORTALS, type NavItem, type NavEntry } from "@/lib/nav-config";
import type { RoleCode } from "@/lib/types";

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
    <aside className="w-64 shrink-0 bg-bg-base flex flex-col h-screen sticky top-0 border-r border-[rgba(255,255,255,0.05)]">
      {/* brand */}
      <div className="px-6 py-5 border-b border-[rgba(255,255,255,0.05)]">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-md bg-accent flex items-center justify-center text-white font-[590] text-sm font-inter">
            C
          </div>
          <div>
            <p className="font-inter text-text-primary text-[13px] font-[510] tracking-tight">Centras AI Gov</p>
            <p className="font-inter text-text-subtle text-[11px] mt-0.5">{active.label}</p>
          </div>
        </div>
      </div>

      {/* nav */}
      <nav className="flex-1 px-2 py-2 overflow-y-auto">
        {active.nav.map((entry, idx) => {
          if ('section' in entry) {
            return (
              <p
                key={`section-${idx}`}
                className="font-inter text-[11px] uppercase tracking-[0.1em] text-text-subtle px-4 pt-5 pb-1"
              >
                {entry.section}
              </p>
            );
          }

          const item = entry as NavItem;
          const isActive =
            item.href === '/ops' || item.href === '/client' || item.href === '/qa'
              ? pathname === item.href
              : pathname === item.href || pathname.startsWith(item.href + '/');

          return (
            <Link
              key={item.href}
              href={item.href}
              className={clsx(
                "flex items-center gap-3 px-3 py-2 text-[13px] transition-colors duration-150 rounded-md border-l-2 mx-1",
                isActive
                  ? "bg-[rgba(255,255,255,0.04)] text-text-primary border-accent"
                  : "text-text-muted hover:bg-[rgba(255,255,255,0.03)] hover:text-text-secondary border-transparent"
              )}
            >
              <span className={clsx("material-symbols-outlined text-[18px] leading-none", isActive ? "text-accent-interactive" : "text-text-subtle")}>
                {item.icon}
              </span>
              <span className="font-inter font-[510] tracking-tight">{item.label}</span>
            </Link>
          );
        })}
      </nav>

      {/* System Health */}
      <div className="mx-3 mb-4 p-3 bg-[rgba(255,255,255,0.02)] border border-[rgba(255,255,255,0.05)] rounded-lg">
        <p className="font-inter text-[10px] uppercase tracking-[0.1em] text-text-subtle mb-2">System Health</p>
        <div className="flex items-center gap-2">
          <span className="w-1.5 h-1.5 rounded-full bg-success" style={{ boxShadow: '0 0 6px var(--success)' }} />
          <span className="font-inter text-[11px] font-[510] text-text-secondary">Operational</span>
        </div>
      </div>
    </aside>
  );
}
