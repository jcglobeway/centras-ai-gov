"use client";

import { useState } from "react";
import { useParams, useRouter } from "next/navigation";
import useSWR, { useSWRConfig } from "swr";
import { fetcher, qaApi } from "@/lib/api";
import type {
  PagedResponse,
  Question,
  QuestionContext,
  QAReview,
  ReviewStatus,
  RootCauseCode,
  ActionType,
  ChatSession,
} from "@/lib/types";
import { Card } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Spinner } from "@/components/ui/Spinner";
import type { ComponentProps } from "react";

type BadgeVariant = ComponentProps<typeof Badge>["variant"];

// ── 상수 ─────────────────────────────────────────────────────────────────────

const ROOT_CAUSE_LABELS: Record<RootCauseCode, string> = {
  missing_document: "문서 없음",
  stale_document:   "문서 최신 아님",
  bad_chunking:     "파싱/청킹 실패",
  retrieval_failure:"검색 실패",
  generation_error: "생성 왜곡(환각)",
  policy_block:     "정책 제한",
  unclear_question: "질문 모호",
};

const ACTION_TYPE_LABELS: Record<ActionType, string> = {
  faq_create: "FAQ 생성",
  document_fix_request: "문서 수정 요청",
  reindex_request: "재인덱싱 요청",
  ops_issue: "운영 이슈",
  no_action: "조치 없음",
};

const REVIEW_STATUS_VARIANT: Record<ReviewStatus, BadgeVariant> = {
  pending: "neutral",
  confirmed_issue: "error",
  resolved: "success",
  false_alarm: "neutral",
};

const REVIEW_STATUS_LABEL: Record<ReviewStatus, string> = {
  pending: "검토 대기",
  confirmed_issue: "이슈 확인",
  resolved: "해결 완료",
  false_alarm: "오탐지",
};

// ── 헬퍼 ─────────────────────────────────────────────────────────────────────

function answerStatusBadge(q: Question): { label: string; variant: BadgeVariant } {
  if (q.isEscalated) return { label: "상담 전환", variant: "warning" };
  if (q.answerStatus === "fallback") return { label: "Fallback", variant: "warning" };
  if (q.answerStatus === "no_answer" || q.answerStatus === "error") return { label: "미응답", variant: "error" };
  if (q.failureReasonCode) return { label: "미해결", variant: "error" };
  return { label: "정상", variant: "success" };
}

function ConfidenceBar({ value }: { value: number | null }) {
  if (value == null) return <span className="text-text-muted text-xs">-</span>;
  const pct = Math.round(value * 100);
  const color = pct >= 70 ? "bg-success" : pct >= 40 ? "bg-warning" : "bg-error";
  return (
    <div className="flex items-center gap-1.5 min-w-[64px]">
      <div className="flex-1 h-1 bg-bg-border rounded-full overflow-hidden">
        <div className={`h-full ${color} rounded-full`} style={{ width: `${pct}%` }} />
      </div>
      <span className="text-[11px] text-text-muted tabular-nums">{pct}%</span>
    </div>
  );
}

function HelpIcon({ tip }: { tip: string }) {
  return (
    <span className="relative group inline-flex shrink-0">
      <span className="w-3.5 h-3.5 rounded-full bg-bg-border text-[9px] text-text-muted flex items-center justify-center cursor-help font-mono leading-none hover:bg-accent/20 hover:text-accent transition-colors">
        ?
      </span>
      <span className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 w-60 bg-bg-elevated border border-white/10 rounded-lg px-3 py-2.5 text-[11px] text-text-secondary leading-relaxed shadow-xl opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity duration-150 z-50 whitespace-pre-wrap">
        {tip}
      </span>
    </span>
  );
}

function ScoreCell({ label, value, target, tooltip }: { label: string; value: number | null; target: number; tooltip?: string }) {
  if (value == null) return null;
  const ok = value >= target;
  return (
    <div className="bg-bg-muted rounded p-2">
      <div className="flex items-center gap-1 mb-1">
        <p className="text-[10px] text-text-muted">{label}</p>
        {tooltip && <HelpIcon tip={tooltip} />}
      </div>
      <p className={`font-mono text-sm font-semibold ${ok ? "text-success" : "text-warning"}`}>{value.toFixed(3)}</p>
      <p className="text-[10px] text-text-muted">목표 ≥ {target}</p>
    </div>
  );
}

function SectionLabel({ children, help }: { children: React.ReactNode; help?: string }) {
  return (
    <div className="flex items-center gap-1.5 mb-2.5">
      <p className="font-mono text-[10px] uppercase tracking-[0.6px] text-text-muted">{children}</p>
      {help && <HelpIcon tip={help} />}
    </div>
  );
}

function SectionDivider() {
  return <div className="border-t border-bg-border my-4" />;
}

// ── QA 검수 폼 ────────────────────────────────────────────────────────────────

function QaReviewForm({ questionId, onSaved }: { questionId: string; onSaved: () => void }) {
  const [reviewStatus, setReviewStatus] = useState<"confirmed_issue" | "false_alarm">("confirmed_issue");
  const [rootCauseCode, setRootCauseCode] = useState<RootCauseCode>("missing_document");
  const [actionType, setActionType] = useState<ActionType>("no_action");
  const [comment, setComment] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSave() {
    setSaving(true);
    setError(null);
    try {
      await qaApi.createReview({
        questionId,
        reviewStatus,
        rootCauseCode: reviewStatus === "confirmed_issue" ? rootCauseCode : undefined,
        actionType: reviewStatus === "confirmed_issue" ? actionType : "no_action",
        reviewComment: comment || undefined,
      });
      onSaved();
    } catch {
      setError("저장에 실패했습니다. 다시 시도해 주세요.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="space-y-3">
      <div>
        <p className="text-[11px] text-text-muted mb-1.5">검수 결과</p>
        <div className="flex gap-3">
          {(["confirmed_issue", "false_alarm"] as const).map((s) => (
            <label key={s} className="flex items-center gap-1.5 cursor-pointer">
              <input type="radio" name="reviewStatus" value={s} checked={reviewStatus === s}
                onChange={() => setReviewStatus(s)} className="accent-accent" />
              <span className="text-xs text-text-primary">{REVIEW_STATUS_LABEL[s]}</span>
            </label>
          ))}
        </div>
      </div>
      {reviewStatus === "confirmed_issue" && (
        <>
          <div>
            <p className="text-[11px] text-text-muted mb-1">원인 코드</p>
            <select value={rootCauseCode} onChange={(e) => setRootCauseCode(e.target.value as RootCauseCode)}
              className="w-full bg-bg-muted border border-bg-border rounded px-2 py-1.5 text-xs text-text-primary">
              {(Object.keys(ROOT_CAUSE_LABELS) as RootCauseCode[]).map((code) => (
                <option key={code} value={code}>{ROOT_CAUSE_LABELS[code]}</option>
              ))}
            </select>
          </div>
          <div>
            <p className="text-[11px] text-text-muted mb-1">조치 유형</p>
            <select value={actionType} onChange={(e) => setActionType(e.target.value as ActionType)}
              className="w-full bg-bg-muted border border-bg-border rounded px-2 py-1.5 text-xs text-text-primary">
              {(Object.keys(ACTION_TYPE_LABELS) as ActionType[]).map((t) => (
                <option key={t} value={t}>{ACTION_TYPE_LABELS[t]}</option>
              ))}
            </select>
          </div>
        </>
      )}
      <div>
        <p className="text-[11px] text-text-muted mb-1">코멘트 (선택)</p>
        <textarea value={comment} onChange={(e) => setComment(e.target.value)} rows={2}
          placeholder="검수 내용을 입력하세요"
          className="w-full bg-bg-muted border border-bg-border rounded px-2 py-1.5 text-xs text-text-primary resize-none placeholder:text-text-muted" />
      </div>
      {error && <p className="text-[11px] text-error">{error}</p>}
      <button onClick={handleSave} disabled={saving}
        className="w-full bg-accent text-white text-xs font-medium py-1.5 rounded hover:bg-accent/80 disabled:opacity-50 transition-colors">
        {saving ? "저장 중…" : "검수 등록"}
      </button>
    </div>
  );
}

// ── 상세 패널 ─────────────────────────────────────────────────────────────────

function DetailPanel({ q, onClose }: { q: Question; onClose: () => void }) {
  const { mutate } = useSWRConfig();
  const status = answerStatusBadge(q);
  const [qaOpen, setQaOpen] = useState(false);
  const [answerExpanded, setAnswerExpanded] = useState(false);

  const { data: ctx } = useSWR<QuestionContext>(`/api/admin/questions/${q.questionId}/context`, fetcher);
  const { data: reviewData, mutate: mutateReview } = useSWR<PagedResponse<QAReview>>(
    `/api/admin/qa-reviews?question_id=${q.questionId}`, fetcher,
  );

  const existingReview = reviewData?.items?.[0] ?? null;
  const hasRagas = q.faithfulness != null || q.answerRelevancy != null || q.contextPrecision != null || q.contextRecall != null;
  const hasLatency = ctx && (ctx.latencyMs != null || ctx.llmMs != null || ctx.postprocessMs != null);

  function handleReviewSaved() {
    mutateReview();
    mutate((key: unknown) => typeof key === "string" && key.includes("/api/admin/questions/unresolved"));
    setQaOpen(false);
  }

  return (
    <div className="fixed inset-0 z-50 flex justify-end" onClick={onClose}>
      <div className="w-full max-w-[600px] bg-bg-surface border-l border-bg-border h-full flex flex-col shadow-2xl"
        onClick={(e) => e.stopPropagation()}>
        {/* 헤더 */}
        <div className="flex items-center justify-between px-5 py-3.5 border-b border-bg-border bg-bg-surface shrink-0">
          <div className="flex items-center gap-2.5">
            <h3 className="font-semibold text-text-primary text-sm">대화 상세</h3>
            <Badge variant={status.variant}>{status.label}</Badge>
            {existingReview && (
              <Badge variant={REVIEW_STATUS_VARIANT[existingReview.reviewStatus as ReviewStatus]}>
                {REVIEW_STATUS_LABEL[existingReview.reviewStatus as ReviewStatus]}
              </Badge>
            )}
          </div>
          <button onClick={onClose} className="text-text-muted hover:text-text-primary transition-colors p-1">
            <span className="material-symbols-outlined text-[18px]">close</span>
          </button>
        </div>

        {/* 본문 */}
        <div className="overflow-y-auto flex-1 px-5 py-4 text-sm">
          <section>
            <SectionLabel>질문</SectionLabel>
            <div className="bg-bg-elevated border border-white/5 rounded-lg px-3.5 py-3">
              <p className="text-text-primary leading-relaxed text-sm">{q.questionText}</p>
            </div>
            {q.answerText && (
              <div className="mt-3">
                <div className="flex items-center justify-between mb-2">
                  <SectionLabel>답변</SectionLabel>
                  <button onClick={() => setAnswerExpanded((v) => !v)}
                    className="text-[11px] text-accent hover:underline font-mono">
                    {answerExpanded ? "접기 ↑" : "전체 보기 ↓"}
                  </button>
                </div>
                <div className={`bg-bg-elevated border border-white/5 rounded-lg px-3.5 py-3 overflow-hidden transition-all ${answerExpanded ? "" : "max-h-24"}`}>
                  <p className="text-text-secondary leading-relaxed whitespace-pre-wrap text-sm">{q.answerText}</p>
                </div>
                {!answerExpanded && (
                  <div className="h-6 -mt-6 bg-gradient-to-t from-bg-surface to-transparent rounded-b-lg pointer-events-none" />
                )}
              </div>
            )}
          </section>

          <SectionDivider />

          <section>
            <SectionLabel>지표</SectionLabel>
            <div className="grid grid-cols-2 gap-2">
              <div className="bg-bg-elevated border border-white/5 rounded-lg px-3 py-2.5">
                <p className="text-[10px] text-text-muted mb-1.5">카테고리</p>
                <span className="text-text-primary text-xs">{q.questionCategory ?? "-"}</span>
              </div>
              <div className="bg-bg-elevated border border-white/5 rounded-lg px-3 py-2.5">
                <div className="flex items-center gap-1 mb-1.5">
                  <p className="text-[10px] text-text-muted">신뢰도</p>
                  <HelpIcon tip={"Vector + BM25 두 검색의 RRF 점수 평균을\n이론적 최댓값(2/61 ≈ 0.033)으로 정규화\n\n• ≥ 80%  양쪽 검색 상위 랭크\n• 40~80%  한쪽 상위 또는 중위권\n• < 40%   검색 약함 → 에스컬레이션"} />
                </div>
                <ConfidenceBar value={q.answerConfidence} />
              </div>
              {q.responseTimeMs != null && (
                <div className="bg-bg-elevated border border-white/5 rounded-lg px-3 py-2.5">
                  <div className="flex items-center gap-1 mb-1.5">
                    <p className="text-[10px] text-text-muted">E2E 응답 시간</p>
                    <HelpIcon tip={"RAG 파이프라인 전체 응답 시간\nRetrieval + LLM + 후처리 합계"} />
                  </div>
                  <span className="text-text-primary font-mono text-xs">{q.responseTimeMs.toLocaleString()}ms</span>
                </div>
              )}
              {q.failureReasonCode && (
                <div className="bg-bg-elevated border border-white/5 rounded-lg px-3 py-2.5">
                  <div className="flex items-center gap-1 mb-1.5">
                    <p className="text-[10px] text-text-muted">실패 코드</p>
                    <HelpIcon tip={"A01 문서없음 · A02 오래된문서\nA03 파싱실패 · A04 검색실패\nA05 재랭킹실패 · A06 생성왜곡(환각)\nA07 의도분류실패 · A08 정책제한\nA09 질문모호 · A10 채널문제"} />
                  </div>
                  <span className="font-mono text-xs text-error">{q.failureReasonCode}</span>
                </div>
              )}
            </div>
            {hasLatency && (
              <div className="mt-2 grid grid-cols-3 gap-2">
                {ctx!.latencyMs != null && (
                  <div className="bg-bg-elevated border border-white/5 rounded-lg p-2.5 text-center">
                    <p className="text-[10px] text-text-muted mb-1">검색</p>
                    <p className="font-mono text-xs text-text-primary">{ctx!.latencyMs.toLocaleString()}ms</p>
                  </div>
                )}
                {ctx!.llmMs != null && (
                  <div className="bg-bg-elevated border border-white/5 rounded-lg p-2.5 text-center">
                    <p className="text-[10px] text-text-muted mb-1">LLM</p>
                    <p className="font-mono text-xs text-text-primary">{ctx!.llmMs.toLocaleString()}ms</p>
                  </div>
                )}
                {ctx!.postprocessMs != null && (
                  <div className="bg-bg-elevated border border-white/5 rounded-lg p-2.5 text-center">
                    <p className="text-[10px] text-text-muted mb-1">후처리</p>
                    <p className="font-mono text-xs text-text-primary">{ctx!.postprocessMs.toLocaleString()}ms</p>
                  </div>
                )}
              </div>
            )}
            {hasRagas && (
              <>
                <div className="flex items-center gap-1.5 mt-3 mb-1.5">
                  <p className="font-mono text-[10px] uppercase tracking-[0.6px] text-text-muted">RAGAS 평가</p>
                  <HelpIcon tip={"LLM이 자동 평가한 RAG 품질 지표.\n실제 답변·문서·질문 간 관계를 0~1로 측정."} />
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <ScoreCell label="Faithfulness"      value={q.faithfulness}     target={0.90} tooltip={"답변이 검색된 문서에 충실한 정도\n목표: ≥ 0.90"} />
                  <ScoreCell label="Answer Relevancy"  value={q.answerRelevancy}  target={0.85} tooltip={"답변이 질문에 관련된 정도\n목표: ≥ 0.85"} />
                  <ScoreCell label="Context Precision" value={q.contextPrecision} target={0.70} tooltip={"검색된 문서 중 관련 문서 비율\n목표: ≥ 0.70"} />
                  <ScoreCell label="Context Recall"    value={q.contextRecall}    target={0.75} tooltip={"필요한 정보가 검색 결과에 포함된 비율\n목표: ≥ 0.75"} />
                </div>
              </>
            )}
          </section>

          {ctx && (ctx.queryRewriteText || ctx.retrievedChunks.length > 0) && (
            <>
              <SectionDivider />
              <section>
                <SectionLabel help={"pgvector Hybrid 검색(Vector + BM25 RRF)으로\n가져온 문서 청크 목록.\n\n파란 테두리는 최종 답변에 인용된 청크입니다."}>
                  검색 컨텍스트 {ctx.retrievedChunks.length > 0 && `· ${ctx.retrievedChunks.length}건`}
                </SectionLabel>
                {ctx.queryRewriteText && (
                  <div className="mb-3">
                    <p className="text-[10px] text-text-muted mb-1.5">검색 쿼리 (재작성)</p>
                    <p className="text-xs text-text-secondary bg-bg-elevated border border-white/5 rounded-lg px-3 py-2.5 leading-relaxed">
                      {ctx.queryRewriteText}
                    </p>
                  </div>
                )}
                {ctx.retrievedChunks.length > 0 && (
                  <div className="space-y-2 max-h-52 overflow-y-auto">
                    {ctx.retrievedChunks.map((chunk, idx) => (
                      <div key={`${chunk.rank ?? idx}-${chunk.chunkId ?? idx}`}
                        className={`rounded-lg px-3 py-2.5 text-xs border ${chunk.usedInCitation ? "border-accent/30 bg-accent/5" : "border-white/5 bg-bg-elevated"}`}>
                        <div className="flex items-center gap-2 mb-1.5">
                          <span className="font-mono text-[10px] text-text-muted">#{chunk.rank}</span>
                          {chunk.score != null && (
                            <span className="flex items-center gap-0.5 font-mono text-[10px] text-text-muted">
                              score: {chunk.score.toFixed(4)}
                              <HelpIcon tip={"Hybrid RRF 검색 점수\nVector + BM25 융합 결과"} />
                            </span>
                          )}
                          {chunk.usedInCitation && <span className="ml-auto text-[10px] text-accent font-medium">인용됨</span>}
                        </div>
                        <p className="text-text-secondary leading-relaxed line-clamp-3">{chunk.chunkText ?? "(청크 텍스트 없음)"}</p>
                      </div>
                    ))}
                  </div>
                )}
              </section>
            </>
          )}

          <SectionDivider />

          <section>
            <SectionLabel>QA 검수</SectionLabel>
            {existingReview ? (
              <div className="bg-bg-elevated border border-white/5 rounded-lg px-3.5 py-3 space-y-2">
                <div className="flex flex-wrap gap-2 items-center">
                  <Badge variant={REVIEW_STATUS_VARIANT[existingReview.reviewStatus as ReviewStatus]}>
                    {REVIEW_STATUS_LABEL[existingReview.reviewStatus as ReviewStatus]}
                  </Badge>
                  {existingReview.rootCauseCode && (
                    <span className="text-[11px] text-text-muted font-mono">
                      {ROOT_CAUSE_LABELS[existingReview.rootCauseCode as RootCauseCode] ?? existingReview.rootCauseCode}
                    </span>
                  )}
                  {existingReview.actionType && existingReview.actionType !== "no_action" && (
                    <span className="text-[11px] text-text-secondary">
                      → {ACTION_TYPE_LABELS[existingReview.actionType as ActionType] ?? existingReview.actionType}
                    </span>
                  )}
                </div>
                {existingReview.reviewComment && (
                  <p className="text-xs text-text-secondary bg-bg-prominent rounded px-2.5 py-2 leading-relaxed">
                    {existingReview.reviewComment}
                  </p>
                )}
                <p className="text-[10px] text-text-muted">{new Date(existingReview.createdAt).toLocaleString("ko-KR")}</p>
              </div>
            ) : (
              <div>
                <div className="flex items-center justify-between mb-2">
                  <p className="text-xs text-text-muted">검수 미등록</p>
                  <button onClick={() => setQaOpen((v) => !v)}
                    className="text-[11px] text-accent hover:underline font-mono">
                    {qaOpen ? "닫기 ↑" : "검수 등록 ↓"}
                  </button>
                </div>
                {qaOpen && (
                  <div className="bg-bg-elevated border border-white/5 rounded-lg px-3.5 py-3">
                    <QaReviewForm questionId={q.questionId} onSaved={handleReviewSaved} />
                  </div>
                )}
              </div>
            )}
          </section>

          <SectionDivider />

          <section className="pb-4">
            <SectionLabel>메타</SectionLabel>
            <div className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1.5 text-[11px]">
              <span className="text-text-muted">ID</span>
              <span className="text-text-secondary font-mono truncate">{q.questionId}</span>
              <span className="text-text-muted">채널</span>
              <span className="text-text-secondary">{q.channel}</span>
              <span className="text-text-muted">기관</span>
              <span className="text-text-secondary font-mono">{q.organizationId}</span>
              <span className="text-text-muted">시각</span>
              <span className="text-text-secondary">{new Date(q.createdAt).toLocaleString("ko-KR")}</span>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}

// ── 세션 상세 페이지 ──────────────────────────────────────────────────────────

export default function SessionDetailPage() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const router = useRouter();
  const [selected, setSelected] = useState<Question | null>(null);

  const { data: sessionData } = useSWR<ChatSession>(
    sessionId ? `/api/admin/chat-sessions/${sessionId}` : null,
    fetcher,
  );

  const { data: questionsData, isLoading } = useSWR<PagedResponse<Question>>(
    sessionId ? `/api/admin/chat-sessions/${sessionId}/questions` : null,
    fetcher,
  );

  const questions = questionsData?.items ?? [];

  const sessionMeta = sessionData
    ? `${s(sessionData.channel)} · ${new Date(sessionData.startedAt).toLocaleString("ko-KR")}`
    : sessionId;

  function s(channel: string) {
    return channel === "web" ? "Web" : channel;
  }

  return (
    <div className="space-y-4">
      {selected && <DetailPanel q={selected} onClose={() => setSelected(null)} />}

      {/* Breadcrumb */}
      <div className="flex items-center gap-2 text-sm">
        <button
          onClick={() => router.push("/ops/chat-history")}
          className="flex items-center gap-1 text-text-muted hover:text-accent transition-colors"
        >
          <span className="material-symbols-outlined text-[16px]">arrow_back</span>
          대화 이력
        </button>
        <span className="text-text-muted">/</span>
        <span className="text-text-secondary font-mono text-xs">{sessionId}</span>
      </div>

      {/* 세션 메타 카드 */}
      {sessionData && (
        <div className="bg-bg-elevated border border-white/5 rounded-lg px-5 py-3 flex flex-wrap gap-x-6 gap-y-1.5">
          <div>
            <p className="text-[10px] text-text-muted mb-0.5">채널</p>
            <p className="text-xs text-text-primary">{sessionData.channel}</p>
          </div>
          <div>
            <p className="text-[10px] text-text-muted mb-0.5">시작</p>
            <p className="text-xs text-text-primary">{new Date(sessionData.startedAt).toLocaleString("ko-KR")}</p>
          </div>
          {sessionData.endedAt && (
            <div>
              <p className="text-[10px] text-text-muted mb-0.5">종료</p>
              <p className="text-xs text-text-primary">{new Date(sessionData.endedAt).toLocaleString("ko-KR")}</p>
            </div>
          )}
          <div>
            <p className="text-[10px] text-text-muted mb-0.5">질문 수</p>
            <p className="text-xs text-text-primary font-mono">{sessionData.totalQuestionCount}</p>
          </div>
        </div>
      )}

      {/* 대화 스레드 */}
      <Card>
        <div className="px-5 py-4">
          {isLoading ? (
            <div className="flex items-center justify-center h-24"><Spinner /></div>
          ) : questions.length === 0 ? (
            <p className="text-sm text-text-muted text-center py-8">이 세션에 질문이 없습니다.</p>
          ) : (
            <div className="space-y-1">
              {questions.map((q, idx) => {
                const status = answerStatusBadge(q);
                return (
                  <div key={q.questionId}>
                    {/* 턴 구분 헤더 */}
                    <div className="flex items-center gap-3 py-3">
                      <div className="flex-1 border-t border-bg-border" />
                      <span className="text-[10px] text-text-muted font-mono shrink-0">#{idx + 1}</span>
                      <div className="flex-1 border-t border-bg-border" />
                    </div>

                    <div className="relative pr-28 space-y-3 pb-2">
                      {/* 질문 버블 (오른쪽 — 사용자) */}
                      <div className="flex justify-end items-end gap-2">
                        <p className="text-[10px] text-text-muted shrink-0">
                          {new Date(q.createdAt).toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" })}
                        </p>
                        <div
                          className="max-w-[75%] bg-accent text-white rounded-2xl rounded-tr-sm px-4 py-2.5 cursor-pointer hover:bg-accent/85 transition-colors shadow-sm shadow-accent/20"
                          onClick={() => setSelected(q)}
                        >
                          <p className="text-sm leading-relaxed">{q.questionText}</p>
                        </div>
                        <div className="w-6 h-6 rounded-full bg-accent/20 border border-accent/30 flex items-center justify-center shrink-0">
                          <span className="material-symbols-outlined text-[12px] text-accent">person</span>
                        </div>
                      </div>

                      {/* 답변 버블 (왼쪽 — 챗봇) */}
                      <div className="flex justify-start gap-2">
                        <div className="w-6 h-6 rounded-full bg-bg-elevated border border-white/10 flex items-center justify-center shrink-0 mt-1">
                          <span className="material-symbols-outlined text-[12px] text-text-muted">smart_toy</span>
                        </div>
                        <div className="flex-1">
                          <div className="bg-bg-elevated border border-white/8 rounded-2xl rounded-tl-sm px-4 py-2.5">
                            {q.answerText ? (
                              <p className="text-sm text-text-secondary leading-relaxed line-clamp-4">{q.answerText}</p>
                            ) : (
                              <p className="text-sm text-text-muted italic">응답 없음</p>
                            )}
                          </div>
                          <div className="flex items-center gap-2 mt-2 ml-1">
                            <Badge variant={status.variant}>{status.label}</Badge>
                            {q.answerConfidence != null && <ConfidenceBar value={q.answerConfidence} />}
                          </div>
                        </div>
                      </div>

                      {/* 상세 보기 버튼 — 턴 박스 오른쪽 중앙 */}
                      <button
                        onClick={() => setSelected(q)}
                        className="absolute right-0 top-1/2 -translate-y-1/2 flex flex-col items-center gap-1 text-[11px] text-text-muted border border-bg-border rounded-lg px-2.5 py-2 hover:border-accent/40 hover:text-accent hover:bg-accent/5 transition-colors w-20"
                      >
                        <span className="material-symbols-outlined text-[18px]">open_in_new</span>
                        상세 보기
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </Card>
    </div>
  );
}
