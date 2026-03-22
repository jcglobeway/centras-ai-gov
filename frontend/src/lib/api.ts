import type {
  LoginRequest,
  LoginResponse,
  PagedResponse,
  Organization,
  Service,
  CrawlSource,
  IngestionJob,
  QAReview,
  UnresolvedQuestion,
  Document,
  DocumentVersion,
  DailyMetric,
} from "./types";

const BASE = "/api/admin";

function getSessionId(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("sessionId");
}

async function request<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const sessionId = getSessionId();
  const headers: HeadersInit = {
    "Content-Type": "application/json",
    ...(sessionId ? { "X-Admin-Session-Id": sessionId } : {}),
    ...options.headers,
  };

  const res = await fetch(`${BASE}${path}`, { ...options, headers });

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw { status: res.status, ...body };
  }

  return res.json();
}

// ── 인증 ──────────────────────────────────────────────────────────────────────

export const authApi = {
  login: (body: LoginRequest) =>
    request<LoginResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify(body),
    }),

  logout: () =>
    request<void>("/auth/logout", { method: "POST" }),
};

// ── 기관 ──────────────────────────────────────────────────────────────────────

export const orgApi = {
  list: (params?: Record<string, string>) =>
    request<PagedResponse<Organization>>(
      "/organizations?" + new URLSearchParams(params).toString()
    ),

  services: (orgId: string) =>
    request<PagedResponse<Service>>(`/organizations/${orgId}/services`),
};

// ── 인제스션 ──────────────────────────────────────────────────────────────────

export const ingestionApi = {
  sources: (params?: Record<string, string>) =>
    request<PagedResponse<CrawlSource>>(
      "/crawl-sources?" + new URLSearchParams(params).toString()
    ),

  jobs: (params?: Record<string, string>) =>
    request<PagedResponse<IngestionJob>>(
      "/ingestion-jobs?" + new URLSearchParams(params).toString()
    ),

  getJob: (id: string) => request<IngestionJob>(`/ingestion-jobs/${id}`),
};

// ── QA 리뷰 ───────────────────────────────────────────────────────────────────

export const qaApi = {
  unresolved: (params?: Record<string, string>) =>
    request<PagedResponse<UnresolvedQuestion>>(
      "/questions/unresolved?" + new URLSearchParams(params).toString()
    ),

  reviews: (params?: Record<string, string>) =>
    request<PagedResponse<QAReview>>(
      "/qa-reviews?" + new URLSearchParams(params).toString()
    ),

  createReview: (body: {
    questionId: string;
    reviewStatus: string;
    rootCauseCode?: string;
    actionType?: string;
    reviewNote?: string;
  }) =>
    request<QAReview>("/qa-reviews", {
      method: "POST",
      body: JSON.stringify(body),
    }),

  updateReview: (id: string, body: Partial<QAReview>) =>
    request<QAReview>(`/qa-reviews/${id}`, {
      method: "PATCH",
      body: JSON.stringify(body),
    }),
};

// ── 문서 ──────────────────────────────────────────────────────────────────────

export const documentApi = {
  list: (params?: Record<string, string>) =>
    request<PagedResponse<Document>>(
      "/documents?" + new URLSearchParams(params).toString()
    ),

  versions: (docId: string) =>
    request<PagedResponse<DocumentVersion>>(`/documents/${docId}/versions`),
};

// ── 메트릭 ────────────────────────────────────────────────────────────────────

export const metricsApi = {
  daily: (params?: Record<string, string>) =>
    request<PagedResponse<DailyMetric>>(
      "/metrics/daily?" + new URLSearchParams(params).toString()
    ),
};

// ── SWR fetcher ───────────────────────────────────────────────────────────────

export async function fetcher<T = unknown>(url: string): Promise<T> {
  const sessionId = getSessionId();
  const headers: HeadersInit = {
    "Content-Type": "application/json",
    ...(sessionId ? { "X-Admin-Session-Id": sessionId } : {}),
  };

  const res = await fetch(url, { headers });

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw { status: res.status, ...body };
  }

  return res.json();
}
