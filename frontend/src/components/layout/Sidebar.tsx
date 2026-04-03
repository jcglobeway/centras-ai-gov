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

interface NavSection {
  section: string;
}

type NavEntry = NavItem | NavSection;

const OPS_NAV: NavEntry[] = [
  { section: '대시보드' },
  { href: '/ops',                  label: '통합 관제',      icon: 'dashboard' },
  { href: '/ops/statistics',       label: '서비스 통계',    icon: 'bar_chart' },
  { href: '/ops/quality-summary',  label: '품질/보안 요약', icon: 'shield' },
  { section: '지식베이스' },
  { href: '/ops/upload',           label: '데이터 업로드',  icon: 'upload_file' },
  { href: '/ops/preprocessing',    label: '데이터 전처리',  icon: 'tune' },
  { href: '/ops/vector-db',        label: '벡터 DB 관리',   icon: 'hub' },
  { href: '/ops/dictionary',       label: '동의어/금칙어',  icon: 'spellcheck' },
  { section: '챗봇 엔진' },
  { href: '/ops/prompt',           label: '프롬프트',       icon: 'chat' },
  { href: '/ops/rag-params',       label: 'RAG 파라미터',   icon: 'settings_suggest' },
  { href: '/ops/model-serving',    label: '모델 서빙',      icon: 'memory' },
  { section: '품질 관리' },
  { href: '/ops/quality',          label: '평가 지표',      icon: 'analytics' },
  { href: '/ops/unresolved',       label: '미해결 질의',    icon: 'help_center' },
  { href: '/ops/correction',       label: '답변 교정',      icon: 'edit_note' },
  { href: '/ops/simulator',        label: '시뮬레이션 룸',  icon: 'science' },
  { href: '/ops/redteam',          label: '레드팀 케이스셋',icon: 'security' },
  { section: '서비스 모니터링' },
  { href: '/ops/chat-history',     label: '대화 이력',      icon: 'history' },
  { href: '/ops/audit',            label: '보안 감사 로그', icon: 'policy' },
  { href: '/ops/anomaly',          label: '이상 징후 감지', icon: 'warning' },
  { section: '통계 및 보고서' },
  { href: '/ops/reports',          label: '성과 분석 리포트',icon: 'summarize' },
  { href: '/ops/cost',             label: '비용 분석',      icon: 'payments' },
  { section: '시스템' },
  { href: '/ops/users',            label: '사용자/권한 관리',icon: 'manage_accounts' },
  { href: '/ops/api-keys',         label: '연동 API 관리',  icon: 'api' },
  { section: '운영' },
  { href: '/ops/indexing',         label: 'RAG 인덱싱',     icon: 'storage' },
  { href: '/ops/organizations',    label: '기관 관리',      icon: 'domain' },
];

const CLIENT_NAV: NavItem[] = [
  { href: '/client',             label: '기관 대시보드',   icon: 'dashboard' },
  { href: '/client/performance', label: '민원응대 성과',   icon: 'trending_up' },
  { href: '/client/failure',     label: '실패/전환 분석',  icon: 'error_outline' },
  { href: '/client/knowledge',   label: '지식 현황',       icon: 'menu_book' },
];

const QA_NAV: NavItem[] = [
  { href: '/qa',             label: '검수 대시보드',    icon: 'dashboard' },
  { href: '/qa/unresolved',  label: '미응답/오답 관리', icon: 'help_center' },
  { href: '/qa/documents',   label: '문서 관리',        icon: 'folder_open' },
  { href: '/qa/approvals',   label: '승인 워크플로우',  icon: 'task_alt' },
];

const PORTALS = [
  { key: 'ops',    label: '운영사 어드민', basePath: '/ops',    nav: OPS_NAV as NavEntry[] },
  { key: 'client', label: '기관 어드민',   basePath: '/client', nav: CLIENT_NAV as NavEntry[] },
  { key: 'qa',     label: 'QA 어드민',     basePath: '/qa',     nav: QA_NAV as NavEntry[] },
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
