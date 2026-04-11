export interface FilterConfig {
  org: boolean;
  service: boolean;
  date: boolean;
}

export interface NavItem {
  href: string;
  label: string;
  icon: string;
  filters?: FilterConfig;
}

export interface NavSection {
  section: string;
}

export type NavEntry = NavItem | NavSection;

const F_BOTH: FilterConfig = { org: true,  service: false, date: true  };
const F_ORG:  FilterConfig = { org: true,  service: false, date: false };
const F_DATE: FilterConfig = { org: false, service: false, date: true  };
const F_NONE: FilterConfig = { org: false, service: false, date: false };

const F_BOTH_OPS: FilterConfig = { org: true,  service: true, date: true  };
const F_ORG_OPS:  FilterConfig = { org: true,  service: true, date: false };

export const OPS_NAV: NavEntry[] = [
  { section: '대시보드' },
  { href: '/ops',                  label: '통합 관제',        icon: 'dashboard',        filters: F_BOTH_OPS },
  { href: '/ops/statistics',       label: '서비스 통계',      icon: 'bar_chart',        filters: F_BOTH_OPS },
  { href: '/ops/quality-summary',  label: '품질/보안 요약',   icon: 'shield',           filters: F_BOTH_OPS },
  { section: '지식베이스' },
  { href: '/ops/upload',           label: '데이터 업로드',    icon: 'upload_file',      filters: F_ORG_OPS },
  { href: '/ops/preprocessing',    label: '데이터 전처리',    icon: 'tune',             filters: F_NONE },
  { href: '/ops/vector-db',        label: '벡터 DB 관리',     icon: 'hub',              filters: F_NONE },
  { href: '/ops/dictionary',       label: '동의어/금칙어',    icon: 'spellcheck',       filters: F_NONE },
  { section: '챗봇 엔진' },
  { href: '/ops/prompt',           label: '프롬프트',         icon: 'chat',             filters: F_NONE },
  { href: '/ops/rag-params',       label: 'RAG 파라미터',     icon: 'settings_suggest', filters: F_NONE },
  { href: '/ops/model-serving',    label: '모델 설정',        icon: 'memory',           filters: F_NONE },
  { section: '품질 관리' },
  { href: '/ops/quality',          label: '평가 지표',        icon: 'analytics',        filters: F_BOTH_OPS },
  { href: '/ops/unresolved',       label: '미해결 질의',      icon: 'help_center',      filters: F_BOTH_OPS },
  { href: '/ops/correction',       label: '답변 교정',        icon: 'edit_note',        filters: F_NONE },
  { href: '/ops/simulator',        label: '시뮬레이션 룸',    icon: 'science',          filters: F_NONE },
  { href: '/ops/redteam',          label: '레드팀 케이스셋',  icon: 'security',         filters: F_NONE },
  { section: '서비스 모니터링' },
  { href: '/ops/chat-history',     label: '대화 이력',        icon: 'history',          filters: F_BOTH_OPS },
  { href: '/ops/audit',            label: '보안 감사 로그',   icon: 'policy',           filters: F_DATE },
  { href: '/ops/anomaly',          label: '이상 징후 감지',   icon: 'warning',          filters: F_BOTH_OPS },
  { section: '통계 및 보고서' },
  { href: '/ops/reports',          label: '성과 분석 리포트', icon: 'summarize',        filters: F_ORG_OPS  },
  { href: '/ops/cost',             label: '비용 분석',        icon: 'payments',         filters: F_BOTH_OPS },
  { section: '시스템' },
  { href: '/ops/users',            label: '사용자/권한 관리', icon: 'manage_accounts',  filters: F_NONE },
  { href: '/ops/api-keys',         label: '연동 API 관리',    icon: 'api',              filters: F_NONE },
  { section: '운영' },
  { href: '/ops/indexing',         label: 'RAG 인덱싱',       icon: 'storage',          filters: F_BOTH_OPS },
  { href: '/ops/organizations',    label: '기관 관리',        icon: 'domain',           filters: F_NONE },
];

export const CLIENT_NAV: NavItem[] = [
  { href: '/client',             label: '기관 대시보드',  icon: 'dashboard',    filters: F_BOTH },
  { href: '/client/performance', label: '민원응대 성과',  icon: 'trending_up',  filters: F_BOTH },
  { href: '/client/failure',     label: '실패/전환 분석', icon: 'error_outline',filters: F_BOTH },
  { href: '/client/knowledge',   label: '지식 현황',      icon: 'menu_book',    filters: F_BOTH },
];

export const QA_NAV: NavItem[] = [
  { href: '/qa',             label: '검수 대시보드',    icon: 'dashboard',  filters: F_BOTH },
  { href: '/qa/unresolved',  label: '미응답/오답 관리', icon: 'help_center',filters: F_BOTH },
  { href: '/qa/documents',   label: '문서 관리',        icon: 'folder_open',filters: F_BOTH },
  { href: '/qa/approvals',   label: '승인 워크플로우',  icon: 'task_alt',   filters: F_NONE },
];

export const PORTALS = [
  { key: 'ops',    label: '운영사 어드민', basePath: '/ops',    nav: OPS_NAV },
  { key: 'client', label: '기관 어드민',   basePath: '/client', nav: CLIENT_NAV as NavEntry[] },
  { key: 'qa',     label: 'QA 어드민',     basePath: '/qa',     nav: QA_NAV as NavEntry[] },
] as const;

export interface BreadcrumbResult {
  portalLabel: string;
  section: string;
  pageLabel: string;
  filters: FilterConfig;
}

/** pathname → breadcrumb 정보 + 필터 설정 */
export function resolveBreadcrumb(pathname: string): BreadcrumbResult | null {
  const portal = PORTALS.find((p) => pathname.startsWith(p.basePath));
  if (!portal) return null;

  let currentSection = "";
  let pageLabel = "";
  let filters: FilterConfig = F_NONE;

  for (const entry of portal.nav) {
    if ("section" in entry) {
      currentSection = entry.section;
    } else {
      const matched = entry.href === portal.basePath
        ? pathname === entry.href
        : pathname === entry.href || pathname.startsWith(entry.href + "/");
      if (matched) {
        pageLabel = entry.label;
        filters = (entry as NavItem).filters ?? F_NONE;
        break;
      }
    }
  }

  return {
    portalLabel: portal.label,
    section: currentSection,
    pageLabel: pageLabel || pathname,
    filters,
  };
}
