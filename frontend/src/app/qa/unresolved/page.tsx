
"use client";

import { useState } from "react";
import useSWR from "swr";
import { fetcher, qaApi, adminUserApi } from "@/lib/api";
import type { PagedResponse, UnresolvedQuestion, AnswerStatus, ReviewStatus, AdminUser } from "@/lib/types";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { PageFilters, getWeekFrom, getToday } from "@/components/ui/PageFilters";
import type { ComponentProps } from "react";

type BadgeVariant = ComponentProps<typeof Badge>["variant"];

const ANSWER_LABEL: Record<AnswerStatus, string> = {
  answered: "응답",
  fallback: "Fallback",
  no_answer: "미응답",
  error: "오류",
};

const ANSWER_VARIANT: Record<AnswerStatus, BadgeVariant> = {
  answered: "success",
  fallback: "warning",
  no_answer: "error",
  error: "error",
};

const REVIEW_LABEL: Record<ReviewStatus, string> = {
  pending: "대기",
  confirmed_issue: "이슈",
  resolved: "해결",
  false_alarm: "오탐",
};

const REVIEW_VARIANT: Record<ReviewStatus, BadgeVariant> = {
  pending: "neutral",
  confirmed_issue: "error",
  resolved: "success",
  false_alarm: "neutral",
};

// A01-A10 실패 원인 코드 레이블 및 조치 주체
const FAILURE_CODE_LABEL: Record<string, string> = {
  A01: "문서 없음",
  A02: "문서 오래됨",
  A03: "파싱 실패",
  A04: "검색 실패",
  A05: "재랭킹 실패",
  A06: "생성 왜곡",
  A07: "의도 분류 실패",
  A08: "정책 제한",
  A09: "질문 모호",
  A10: "채널 문제",
};

// 고객사 조치 필요 코드 (A01, A02, A09)
const CLIENT_CODES = new Set(["A01", "A02", "A09"]);

const ROOT_CAUSE_OPTIONS = [
  { value: "missing_document",  label: "문서 없음" },
  { value: "stale_document",    label: "문서 최신 아님" },
  { value: "bad_chunking",      label: "청킹 오류" },
  { value: "retrieval_failure", label: "검색 실패" },
  { value: "generation_error",  label: "생성 오류 (환각)" },
  { value: "policy_block",      label: "정책상 제한" },
  { value: "unclear_question",  label: "질문 표현 모호" },
];

const ACTION_TYPE_OPTIONS = [
  { value: "faq_create",           label: "FAQ 추가" },
  { value: "document_fix_request", label: "문서 수정 요청" },
  { value: "reindex_request",      label: "재인덱싱 요청" },
  { value: "ops_issue",            label: "운영 이슈" },
  { value: "no_action",            label: "조치 없음" },
];

export default function UnresolvedPage() {
  const [orgId, setOrgId] = useState("");
  const [from, setFrom]   = useState(getWeekFrom);
  const [to, setTo]       = useState(getToday);

  // 리뷰 작성 모달 상태
  const [modalQuestion, setModalQuestion] = useState<UnresolvedQuestion | null>(null);
  const [reviewStatus, setReviewStatus]   = useState("confirmed_issue");
  const [rootCauseCode, setRootCauseCode] = useState("");
  const [actionType, setActionType]       = useState("");
  const [reviewComment, setReviewComment] = useState("");
  const [reviewAssigneeId, setReviewAssigneeId] = useState("");
  const [submitting, setSubmitting]       = useState(false);
  const [submitError, setSubmitError]     = useState("");

  // 담당자 지정 모달 상태 (이미 리뷰 있는 건)
  const [assignModal, setAssignModal] = useState<UnresolvedQuestion | null>(null);
  const [assigneeId, setAssigneeId]   = useState("");
  const [assigning, setAssigning]     = useState(false);

  const params = new URLSearchParams({ page_size: "50" });
  if (orgId) params.set("organization_id", orgId);
  if (from)  params.set("from_date", from);
  if (to)    params.set("to_date", to);

  const { data, error, isLoading, mutate } = useSWR<PagedResponse<UnresolvedQuestion>>(
    `/api/admin/questions/unresolved?${params}`,
    fetcher
  );

  const { data: usersData } = useSWR<{ items: AdminUser[]; total: number }>(
    "/api/admin/users",
    fetcher
  );
  const users = usersData?.items ?? [];

  function getUserName(id: string | null): string {
    if (!id) return "";
    return users.find((u) => u.id === id)?.displayName ?? id;
  }

  // ── 리뷰 작성 모달 ───────────────────────────────────────────────────────────

  function openReviewModal(q: UnresolvedQuestion) {
    setModalQuestion(q);
    setReviewStatus("confirmed_issue");
    setRootCauseCode("");
    setActionType("");
    setReviewComment("");
    setReviewAssigneeId("");
    setSubmitError("");
  }

  function closeReviewModal() {
    setModalQuestion(null);
    setSubmitError("");
  }

  async function handleReviewSubmit() {
    if (!modalQuestion) return;
    if (reviewStatus === "confirmed_issue" && (!rootCauseCode || !actionType)) {
      setSubmitError("근본 원인과 조치 유형을 선택하세요.");
      return;
    }
    setSubmitting(true);
    setSubmitError("");
    try {
      await qaApi.createReview({
        questionId:    modalQuestion.questionId,
        reviewStatus,
        rootCauseCode: reviewStatus === "confirmed_issue" ? rootCauseCode : undefined,
        actionType:    reviewStatus === "confirmed_issue" ? actionType
                     : reviewStatus === "false_alarm"    ? "no_action"
                     : undefined,
        reviewComment: reviewComment || undefined,
        assigneeId:   reviewAssigneeId || undefined,
      });
      closeReviewModal();
      mutate();
    } catch {
      setSubmitError("저장 중 오류가 발생했습니다. 다시 시도하세요.");
    } finally {
      setSubmitting(false);
    }
  }

  // ── 담당자 지정 모달 ─────────────────────────────────────────────────────────

  function openAssignModal(q: UnresolvedQuestion) {
    setAssignModal(q);
    setAssigneeId(q.assigneeId ?? "");
  }

  function closeAssignModal() {
    setAssignModal(null);
  }

  async function handleAssignSubmit() {
    if (!assignModal?.latestReviewId) return;
    setAssigning(true);
    try {
      await qaApi.assignReview(assignModal.latestReviewId, assigneeId || null);
      closeAssignModal();
      mutate();
    } catch {
      // 조용히 실패 (토스트 없음 — 사용자가 재시도 가능)
    } finally {
      setAssigning(false);
    }
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-48">
        <Spinner />
      </div>
    );
  }

  if (error) {
    return <p className="text-error text-sm">데이터를 불러오지 못했습니다.</p>;
  }

  const questions = data?.items ?? [];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between flex-wrap gap-2">
        <div className="flex items-center gap-4">
          <h2 className="text-text-primary font-semibold text-lg">미응답/오답 관리</h2>
          <span className="text-text-muted text-xs">미결 {data?.total ?? 0}건</span>
        </div>
        <PageFilters
          orgId={orgId} onOrgChange={setOrgId}
          from={from}   onFromChange={setFrom}
          to={to}       onToChange={setTo}
        />
      </div>

      <div className="bg-bg-surface border border-bg-border rounded-xl overflow-hidden">
        <Table>
          <Thead>
            <Th>질문 내용</Th>
            <Th>근거</Th>
            <Th>카테고리</Th>
            <Th>리뷰 상태</Th>
            <Th>담당자</Th>
            <Th>생성일</Th>
            <Th></Th>
          </Thead>
          <Tbody>
            {questions.map((q) => (
              <Tr key={q.questionId}>
                {/* 질문 내용 */}
                <Td className="max-w-xs">
                  <div className="space-y-1">
                    <span className="text-sm line-clamp-2">
                      {q.questionText.slice(0, 60)}
                      {q.questionText.length > 60 ? "…" : ""}
                    </span>
                    {q.isEscalated && (
                      <span className="inline-flex items-center gap-1 text-[10px] text-warning font-medium">
                        <span className="material-symbols-outlined text-[12px]">priority_high</span>
                        에스컬레이션
                      </span>
                    )}
                  </div>
                </Td>

                {/* 근거: answerStatus + failureReasonCode */}
                <Td>
                  <div className="flex flex-col gap-1">
                    <Badge variant={q.answerStatus ? ANSWER_VARIANT[q.answerStatus as AnswerStatus] : "neutral"}>
                      {q.answerStatus ? ANSWER_LABEL[q.answerStatus as AnswerStatus] : "-"}
                    </Badge>
                    {q.failureReasonCode && (
                      <span className={`text-[10px] font-mono font-medium ${CLIENT_CODES.has(q.failureReasonCode) ? "text-warning" : "text-error"}`}>
                        {q.failureReasonCode} {FAILURE_CODE_LABEL[q.failureReasonCode] ?? ""}
                      </span>
                    )}
                  </div>
                </Td>

                {/* 카테고리 */}
                <Td>
                  {q.questionCategory ? (
                    <span className="text-xs text-text-secondary bg-bg-prominent px-2 py-0.5 rounded">
                      {q.questionCategory}
                    </span>
                  ) : (
                    <span className="text-text-muted text-xs">-</span>
                  )}
                </Td>

                {/* 리뷰 상태 */}
                <Td>
                  {q.latestReviewStatus ? (
                    <Badge variant={REVIEW_VARIANT[q.latestReviewStatus as ReviewStatus]}>
                      {REVIEW_LABEL[q.latestReviewStatus as ReviewStatus]}
                    </Badge>
                  ) : (
                    <span className="text-text-muted text-xs">없음</span>
                  )}
                </Td>

                {/* 담당자 */}
                <Td>
                  {q.assigneeId ? (
                    <button
                      onClick={() => openAssignModal(q)}
                      className="text-xs text-accent hover:underline text-left"
                    >
                      {getUserName(q.assigneeId)}
                    </button>
                  ) : (
                    <button
                      onClick={() => q.latestReviewId ? openAssignModal(q) : openReviewModal(q)}
                      className="text-xs text-text-muted hover:text-accent transition-colors"
                    >
                      미배정
                    </button>
                  )}
                </Td>

                {/* 생성일 */}
                <Td className="text-xs text-text-muted whitespace-nowrap">
                  {new Date(q.createdAt).toLocaleDateString("ko-KR")}
                </Td>

                {/* 액션 */}
                <Td>
                  <Button variant="secondary" size="sm" onClick={() => openReviewModal(q)}>
                    리뷰 작성
                  </Button>
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
        {questions.length === 0 && (
          <p className="text-center text-text-muted text-sm py-8">미결 질문이 없습니다.</p>
        )}
      </div>

      {/* ── 리뷰 작성 모달 ──────────────────────────────────────────────────── */}
      {modalQuestion && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
          <div className="bg-bg-elevated border border-bg-border rounded-xl shadow-2xl w-full max-w-lg mx-4">
            <div className="flex items-center justify-between px-5 py-4 border-b border-bg-border">
              <h3 className="text-text-primary font-semibold text-sm">QA 리뷰 작성</h3>
              <button
                onClick={closeReviewModal}
                className="text-text-muted hover:text-text-primary transition-colors"
              >
                <span className="material-symbols-outlined text-[20px]">close</span>
              </button>
            </div>

            <div className="px-5 py-4 space-y-4">
              {/* 질문 컨텍스트 — 근거 패널 */}
              <div className="bg-bg-prominent rounded-lg px-3 py-2.5 space-y-2">
                <p className="text-[10px] font-mono uppercase text-text-muted">질문</p>
                <p className="text-sm text-text-secondary leading-relaxed">
                  {modalQuestion.questionText}
                </p>
                <div className="flex flex-wrap gap-2 pt-1">
                  {modalQuestion.answerStatus && (
                    <Badge variant={ANSWER_VARIANT[modalQuestion.answerStatus as AnswerStatus]}>
                      {ANSWER_LABEL[modalQuestion.answerStatus as AnswerStatus]}
                    </Badge>
                  )}
                  {modalQuestion.failureReasonCode && (
                    <span className="text-[10px] font-mono text-error font-medium bg-bg-surface px-1.5 py-0.5 rounded">
                      {modalQuestion.failureReasonCode} {FAILURE_CODE_LABEL[modalQuestion.failureReasonCode] ?? ""}
                    </span>
                  )}
                  {modalQuestion.questionCategory && (
                    <span className="text-[10px] text-text-muted bg-bg-surface px-1.5 py-0.5 rounded">
                      {modalQuestion.questionCategory}
                    </span>
                  )}
                  {modalQuestion.isEscalated && (
                    <span className="text-[10px] text-warning font-medium">에스컬레이션</span>
                  )}
                </div>
              </div>

              <div>
                <label className="block text-xs text-text-secondary mb-1.5">
                  리뷰 상태 <span className="text-error">*</span>
                </label>
                <select
                  value={reviewStatus}
                  onChange={(e) => { setReviewStatus(e.target.value); setSubmitError(""); }}
                  className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent"
                >
                  <option value="confirmed_issue">confirmed_issue — 이슈 확인됨</option>
                  <option value="false_alarm">false_alarm — 오탐</option>
                  <option value="pending">pending — 검토 대기</option>
                </select>
              </div>

              {reviewStatus === "confirmed_issue" && (
                <>
                  <div>
                    <label className="block text-xs text-text-secondary mb-1.5">
                      근본 원인 <span className="text-error">*</span>
                    </label>
                    <select
                      value={rootCauseCode}
                      onChange={(e) => { setRootCauseCode(e.target.value); setSubmitError(""); }}
                      className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent"
                    >
                      <option value="">선택하세요</option>
                      {ROOT_CAUSE_OPTIONS.map((o) => (
                        <option key={o.value} value={o.value}>{o.label}</option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs text-text-secondary mb-1.5">
                      조치 유형 <span className="text-error">*</span>
                    </label>
                    <select
                      value={actionType}
                      onChange={(e) => { setActionType(e.target.value); setSubmitError(""); }}
                      className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent"
                    >
                      <option value="">선택하세요</option>
                      {ACTION_TYPE_OPTIONS.map((o) => (
                        <option key={o.value} value={o.value}>{o.label}</option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs text-text-secondary mb-1.5">
                      담당자 지정 (선택)
                    </label>
                    <select
                      value={reviewAssigneeId}
                      onChange={(e) => setReviewAssigneeId(e.target.value)}
                      className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent"
                    >
                      <option value="">미지정</option>
                      {users.filter((u) => u.status === "active").map((u) => (
                        <option key={u.id} value={u.id}>{u.displayName} ({u.email})</option>
                      ))}
                    </select>
                  </div>
                </>
              )}

              <div>
                <label className="block text-xs text-text-secondary mb-1.5">검토 의견 (선택)</label>
                <textarea
                  value={reviewComment}
                  onChange={(e) => setReviewComment(e.target.value)}
                  rows={3}
                  placeholder="추가 의견을 입력하세요"
                  className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:border-accent resize-none"
                />
              </div>

              {submitError && (
                <p className="text-error text-xs">{submitError}</p>
              )}
            </div>

            <div className="flex items-center justify-end gap-2 px-5 py-4 border-t border-bg-border">
              <Button variant="ghost" size="sm" onClick={closeReviewModal} disabled={submitting}>
                취소
              </Button>
              <Button size="sm" onClick={handleReviewSubmit} disabled={submitting}>
                {submitting ? "저장 중…" : "저장"}
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* ── 담당자 지정 모달 ─────────────────────────────────────────────────── */}
      {assignModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
          <div className="bg-bg-elevated border border-bg-border rounded-xl shadow-2xl w-full max-w-sm mx-4">
            <div className="flex items-center justify-between px-5 py-4 border-b border-bg-border">
              <h3 className="text-text-primary font-semibold text-sm">담당자 지정</h3>
              <button
                onClick={closeAssignModal}
                className="text-text-muted hover:text-text-primary transition-colors"
              >
                <span className="material-symbols-outlined text-[20px]">close</span>
              </button>
            </div>

            <div className="px-5 py-4 space-y-4">
              <div className="bg-bg-prominent rounded-lg px-3 py-2">
                <p className="text-[10px] font-mono uppercase text-text-muted mb-1">질문</p>
                <p className="text-sm text-text-secondary line-clamp-2">
                  {assignModal.questionText}
                </p>
              </div>

              <div>
                <label className="block text-xs text-text-secondary mb-1.5">담당자</label>
                <select
                  value={assigneeId}
                  onChange={(e) => setAssigneeId(e.target.value)}
                  className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent"
                >
                  <option value="">미지정</option>
                  {users.filter((u) => u.status === "active").map((u) => (
                    <option key={u.id} value={u.id}>{u.displayName} ({u.email})</option>
                  ))}
                </select>
              </div>
            </div>

            <div className="flex items-center justify-end gap-2 px-5 py-4 border-t border-bg-border">
              <Button variant="ghost" size="sm" onClick={closeAssignModal} disabled={assigning}>
                취소
              </Button>
              <Button size="sm" onClick={handleAssignSubmit} disabled={assigning}>
                {assigning ? "저장 중…" : "저장"}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
