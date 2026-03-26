// ── 공통 응답 구조 ────────────────────────────────────────────────────────────

export interface ApiResponse<T> {
  data: T;
  requestId: string;
  generatedAt: string;
}

export interface ApiError {
  error: {
    code: string;
    message: string;
    requestId: string;
  };
}

export interface PagedResponse<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
  requestId: string;
  generatedAt: string;
}

// ── 인증 / 세션 ───────────────────────────────────────────────────────────────

export type RoleCode =
  | "super_admin"
  | "ops_admin"
  | "client_admin"
  | "client_org_admin"
  | "client_viewer"
  | "qa_admin"
  | "qa_manager"
  | "knowledge_editor";

export interface LoginRequest {
  email: string;
  password: string;
}

// 백엔드 LoginResponse 구조 그대로 매핑
export interface LoginResponse {
  user: {
    id: string;
    email: string;
    displayName: string;
    status: string;
  };
  session: {
    token: string;
    expiresAt: string;
  };
  authorization: {
    primaryRole: RoleCode;
    organizationScope: string[];
    actions: string[];
  };
}

// 프론트엔드 내부에서 사용하는 세션 구조
export interface SessionInfo {
  sessionId: string;
  userId: string;
  email: string;
  displayName: string;
  roleCode: RoleCode;
  organizationId: string | null;
  organizationIds: string[];
  expiresAt: string;
}

// ── 기관 / 서비스 ─────────────────────────────────────────────────────────────

export interface Organization {
  organizationId: string;
  name: string;
  code: string;
  status: string;
  institutionType: string;
  createdAt: string;
}

export interface Service {
  serviceId: string;
  organizationId: string;
  name: string;
  serviceType: string;
  status: "active" | "inactive";
  createdAt: string;
}

// ── 인제스션 ──────────────────────────────────────────────────────────────────

export type IngestionJobStatus =
  | "queued"
  | "running"
  | "succeeded"
  | "partial_success"
  | "failed"
  | "cancelled";

export interface CrawlSource {
  id: string;
  organizationId: string;
  serviceId: string;
  name: string;
  sourceUri: string;
  schedule: string;
  status: string;
}

export interface IngestionJob {
  id: string;
  crawlSourceId: string;
  organizationId: string;
  serviceId: string;
  jobType: string;
  jobStage: string;
  status: IngestionJobStatus;
  triggerType: string;
  runnerType: string;
  attemptCount: number;
  errorCode: string | null;
  requestedAt: string;
  startedAt: string | null;
  finishedAt: string | null;
}

// ── QA 리뷰 ───────────────────────────────────────────────────────────────────

export type ReviewStatus = "pending" | "confirmed_issue" | "resolved" | "false_alarm";
export type RootCauseCode =
  | "A01" | "A02" | "A03" | "A04" | "A05"
  | "A06" | "A07" | "A08" | "A09" | "A10";

export interface QAReview {
  qaReviewId: string;
  questionId: string;
  reviewerId: string;
  reviewStatus: ReviewStatus;
  rootCauseCode: RootCauseCode | null;
  actionType: string | null;
  reviewNote: string | null;
  createdAt: string;
  resolvedAt: string | null;
}

// ── 질문 / 답변 ───────────────────────────────────────────────────────────────

export type AnswerStatus = "answered" | "fallback" | "no_answer" | "error";

export interface Question {
  questionId: string;
  organizationId: string;
  serviceId: string;
  chatSessionId: string;
  questionText: string;
  questionIntentLabel: string | null;
  channel: string;
  questionCategory: string | null;
  failureReasonCode: string | null;
  isEscalated: boolean;
  answerConfidence: number | null;
  createdAt: string;
}

export interface UnresolvedQuestion {
  questionId: string;
  organizationId: string;
  questionText: string;
  failureReasonCode: string | null;
  questionCategory: string | null;
  isEscalated: boolean;
  answerStatus: string | null;
  latestReviewStatus: string | null;
  createdAt: string;
}

// ── 문서 ──────────────────────────────────────────────────────────────────────

export type IngestionStatus = "pending" | "running" | "completed" | "failed";
export type IndexStatus = "not_indexed" | "indexed" | "outdated";

export interface Document {
  id: string;
  organizationId: string;
  documentType: string;
  title: string;
  sourceUri: string;
  versionLabel: string | null;
  publishedAt: string | null;
  ingestionStatus: IngestionStatus;
  indexStatus: IndexStatus;
  visibilityScope: string;
  lastIngestedAt: string | null;
  lastIndexedAt: string | null;
  createdAt: string;
}

export interface DocumentVersion {
  versionId: string;
  documentId: string;
  versionNumber: number;
  changeNote: string | null;
  createdAt: string;
}

// ── RAGAS 평가 ────────────────────────────────────────────────────────────────

export interface RagasEvaluation {
  id: string;
  questionId: string;
  faithfulness: number | null;
  answerRelevancy: number | null;
  contextPrecision: number | null;
  contextRecall: number | null;
  evaluatedAt: string;
  judgeProvider: string | null;
  judgeModel: string | null;
}

// ── LLM 메트릭 ────────────────────────────────────────────────────────────────

export interface LlmMetrics {
  answerCount: number;
  totalCostUsd: number | null;
  avgCostPerQuery: number | null;
  avgInputTokens: number | null;
  avgOutputTokens: number | null;
  generatedAt: string;
}

// ── 메트릭 ────────────────────────────────────────────────────────────────────

export interface DailyMetric {
  id: string;
  organizationId: string;
  serviceId: string;
  metricDate: string;
  totalSessions: number;
  totalQuestions: number;
  resolvedRate: number | null;
  fallbackRate: number | null;
  zeroResultRate: number | null;
  avgResponseTimeMs: number | null;
  autoResolutionRate: number | null;
  escalationRate: number | null;
  revisitRate: number | null;
  afterHoursRate: number | null;
  knowledgeGapCount: number;
  unansweredCount: number;
  lowSatisfactionCount: number;
}
