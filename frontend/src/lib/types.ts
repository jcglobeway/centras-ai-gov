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
  collectionName?: string;
}

export interface IngestionJob {
  id: string;
  crawlSourceId: string;
  documentId: string | null;
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
  | "missing_document"
  | "stale_document"
  | "bad_chunking"
  | "retrieval_failure"
  | "generation_error"
  | "policy_block"
  | "unclear_question";

export type ActionType =
  | "faq_create"
  | "document_fix_request"
  | "reindex_request"
  | "ops_issue"
  | "no_action";

export interface QAReview {
  qaReviewId: string;
  questionId: string;
  reviewerId: string;
  reviewStatus: ReviewStatus;
  rootCauseCode: RootCauseCode | null;
  actionType: ActionType | null;
  reviewComment: string | null;
  createdAt: string;
  resolvedAt: string | null;
}

// ── 채팅 세션 ─────────────────────────────────────────────────────────────────

export interface ChatSession {
  sessionId: string;
  organizationId: string;
  serviceId: string;
  channel: string;
  startedAt: string;
  endedAt: string | null;
  sessionEndType: string | null;
  totalQuestionCount: number;
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
  answerText: string | null;
  answerStatus: string | null;
  responseTimeMs: number | null;
  faithfulness: number | null;
  answerRelevancy: number | null;
  contextPrecision: number | null;
  contextRecall: number | null;
}

export interface RetrievedChunk {
  rank: number;
  score: number | null;
  usedInCitation: boolean;
  chunkId: string | null;
  chunkText: string | null;
}

export interface QuestionContext {
  queryText: string | null;
  queryRewriteText: string | null;
  latencyMs: number | null;
  llmMs: number | null;
  postprocessMs: number | null;
  retrievalStatus: string | null;
  retrievedChunks: RetrievedChunk[];
}

export interface RagSearchLogStats {
  total: number;
  avgLatencyMs: number | null;
  p50LatencyMs: number | null;
  p95LatencyMs: number | null;
  zeroResultRate: number;
  avgTopK: number | null;
  retrievalStatusDistribution: Record<string, number>;
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
  latestReviewId: string | null;
  assigneeId: string | null;
  createdAt: string;
}

export interface AdminUser {
  id: string;
  email: string;
  displayName: string;
  status: string;
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
  id: string;
  documentId: string;
  versionLabel: string;
  contentHash: string | null;
  changeDetected: boolean;
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
  citationCoverage: number | null;
  citationCorrectness: number | null;
  evaluatedAt: string;
  judgeProvider: string | null;
  judgeModel: string | null;
}

export interface RagasEvaluationPeriodSummary {
  avgFaithfulness: number | null;
  avgAnswerRelevancy: number | null;
  avgContextPrecision: number | null;
  avgContextRecall: number | null;
  avgCitationCoverage: number | null;
  avgCitationCorrectness: number | null;
  count: number;
  from: string;
  to: string;
}

export interface RagasEvaluationSummaryResponse {
  current: RagasEvaluationPeriodSummary;
  previous: RagasEvaluationPeriodSummary;
  generatedAt: string;
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

// ── 기관 RAG 설정 ─────────────────────────────────────────────────────────────

export interface OrgRagConfig {
  id: string;
  organizationId: string;
  systemPrompt: string;
  tone: 'formal' | 'friendly' | 'neutral';
  topK: number;
  similarityThreshold: number;
  rerankerEnabled: boolean;
  llmModel: string;
  llmTemperature: number;
  llmMaxTokens: number;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface OrgRagConfigVersion {
  id: string;
  organizationId: string;
  version: number;
  systemPrompt: string;
  tone: string;
  topK: number;
  similarityThreshold: number;
  rerankerEnabled: boolean;
  llmModel: string;
  llmTemperature: number;
  llmMaxTokens: number;
  changeNote: string | null;
  changedBy: string | null;
  createdAt: string;
}

export interface OrgRagConfigVersionListResponse {
  items: OrgRagConfigVersion[];
  total: number;
}

export interface ModelServingStatus {
  orchestratorStatus: string;
  models: ModelInfo[];
}

export interface ModelInfo {
  name: string;
  status: string;
  version: string | null;
  latencyMs: number | null;
}

// ── 메트릭 ────────────────────────────────────────────────────────────────────

export interface CacheHitTrendItem {
  date: string;
  hits: number;
  total: number;
  hitRate: number;
}

export interface CacheHitTrendResponse {
  items: CacheHitTrendItem[];
}

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
