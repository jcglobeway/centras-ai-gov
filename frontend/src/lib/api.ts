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
  AdminUser,
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

  createSource: (body: {
    organizationId: string;
    serviceId: string;
    name: string;
    sourceType: string;
    sourceUri: string;
    renderMode: string;
    collectionMode: string;
    scheduleExpr: string;
    collectionName?: string;
  }) =>
    request<{ crawlSourceId: string; saved: boolean }>("/crawl-sources", {
      method: "POST",
      body: JSON.stringify(body),
    }),

  runSource: (id: string) =>
    request<{ jobId: string; status: string }>(`/crawl-sources/${id}/run`, {
      method: "POST",
    }),
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
    reviewComment?: string;
    assigneeId?: string;
  }) =>
    request<QAReview>("/qa-reviews", {
      method: "POST",
      body: JSON.stringify(body),
    }),

  assignReview: (id: string, assigneeId: string | null) =>
    request<{ qaReviewId: string; assigneeId: string | null }>(`/qa-reviews/${id}`, {
      method: "PATCH",
      body: JSON.stringify({ assigneeId }),
    }),
};

// ── 관리자 사용자 ─────────────────────────────────────────────────────────────

export const adminUserApi = {
  list: () => request<{ items: AdminUser[]; total: number }>("/users"),
};

// ── 문서 ──────────────────────────────────────────────────────────────────────

export const documentApi = {
  list: (params?: Record<string, string>) =>
    request<PagedResponse<Document>>(
      "/documents?" + new URLSearchParams(params).toString()
    ),

  versions: (docId: string) =>
    request<PagedResponse<DocumentVersion>>(`/documents/${docId}/versions`),

  upload: (
    files: File[],
    organizationId: string,
    serviceId: string,
    collectionName?: string
  ): Promise<{ documentId: string; crawlSourceId: string; jobId: string; status: string }[]> => {
    const sessionId = typeof window !== "undefined" ? localStorage.getItem("sessionId") : null;
    const formData = new FormData();
    for (const file of files) {
      formData.append("files", file);
    }
    formData.append("organizationId", organizationId);
    formData.append("serviceId", serviceId);
    if (collectionName) {
      formData.append("collectionName", collectionName);
    }
    return fetch(`${BASE}/documents/upload`, {
      method: "POST",
      headers: sessionId ? { "X-Admin-Session-Id": sessionId } : {},
      body: formData,
    }).then(async (res) => {
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw { status: res.status, ...body };
      }
      return res.json();
    });
  },
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
