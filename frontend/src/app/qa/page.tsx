"use client";

import { useState } from "react";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, DailyMetric, UnresolvedQuestion, QAReview, ReviewStatus, RagasEvaluation, RootCauseCode } from "@/lib/types";
import { KpiCard } from "@/components/charts/KpiCard";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { ScoreTable } from "@/components/ui/ScoreTable";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Spinner } from "@/components/ui/Spinner";
import { PageFilters, getWeekFrom, getToday } from "@/components/ui/PageFilters";
import type { ComponentProps } from "react";

type BadgeVariant = ComponentProps<typeof Badge>["variant"];

const REVIEW_STATUS_LABEL: Record<ReviewStatus, string> = {
  pending: "검토 대기",
  confirmed_issue: "이슈 확인",
  resolved: "해결 완료",
  false_alarm: "오탐지",
};

const REVIEW_STATUS_VARIANT: Record<ReviewStatus, BadgeVariant> = {
  pending: "neutral",
  confirmed_issue: "error",
  resolved: "success",
  false_alarm: "neutral",
};

const FAILURE_LABEL: Partial<Record<RootCauseCode, string>> = {
  A01: "문서 없음", A02: "문서 최신 아님", A03: "파싱 실패",
  A04: "검색 실패", A05: "재랭킹 실패",   A06: "생성 왜곡",
  A07: "의도 실패", A08: "정책 제한",     A09: "질문 모호", A10: "채널 문제",
};

export default function QaDashboardPage() {
  const [orgId, setOrgId] = useState("");
  const [from, setFrom] = useState(getWeekFrom);
  const [to, setTo] = useState(getToday);

  const params = new URLSearchParams();
  if (orgId) params.set("organization_id", orgId);
  if (from) params.set("from_date", from);
  if (to) params.set("to_date", to);

  const unresolvedParams = new URLSearchParams(params);
  unresolvedParams.set("page_size", "5");

  const reviewParams = new URLSearchParams(params);
  reviewParams.set("page_size", "5");

  const metricsParams = new URLSearchParams({ page_size: "1" });
  if (orgId) metricsParams.set("organization_id", orgId);

  const { data: unresolvedData, isLoading: loadingUnresolved } =
    useSWR<PagedResponse<UnresolvedQuestion>>(
      `/api/admin/questions/unresolved?${unresolvedParams}`,
      fetcher
    );

  const { data: reviewsData, isLoading: loadingReviews } =
    useSWR<PagedResponse<QAReview>>(`/api/admin/qa-reviews?${reviewParams}`, fetcher);

  const { data: confirmedData } = useSWR<PagedResponse<QAReview>>(
    `/api/admin/qa-reviews?review_status=confirmed_issue&page_size=1`,
    fetcher
  );

  const { data: ragasData } = useSWR<PagedResponse<RagasEvaluation>>(
    `/api/admin/ragas-evaluations?page_size=1`,
    fetcher
  );

  const { data: metricsData } = useSWR<PagedResponse<DailyMetric>>(
    `/api/admin/metrics/daily?${metricsParams}`,
    fetcher
  );

  if (loadingUnresolved || loadingReviews) {
    return (
      <div className="flex items-center justify-center h-48">
        <Spinner />
      </div>
    );
  }

  const reviews = reviewsData?.items ?? [];
  const confirmedCount = confirmedData?.total ?? reviews.filter((r) => r.reviewStatus === "confirmed_issue").length;
  const unresolvedTotal = unresolvedData?.total ?? null;
  const lowSatisfactionCount = metricsData?.items?.[0]?.lowSatisfactionCount ?? null;

  const latestRagas = ragasData?.items?.[0] ?? null;
  const ragasRows = [
    { label: "Faithfulness", value: latestRagas?.faithfulness ?? null, target: 0.90 },
    { label: "Answer Relevance", value: latestRagas?.answerRelevancy ?? null, target: 0.85 },
    { label: "Context Precision", value: latestRagas?.contextPrecision ?? null, target: 0.70 },
    { label: "Context Recall", value: latestRagas?.contextRecall ?? null, target: 0.75 },
  ];

  const unresolvedItems = unresolvedData?.items ?? [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-2">
        <h2 className="text-text-primary font-semibold text-lg">검수 대시보드</h2>
        <PageFilters
          orgId={orgId} onOrgChange={setOrgId}
          from={from} onFromChange={setFrom}
          to={to} onToChange={setTo}
        />
      </div>

      {/* PRD 기준 3 KPIs */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <KpiCard
          label="미응답 질문"
          value={unresolvedTotal ?? "-"}
          sub="건"
          status={
            unresolvedTotal == null ? undefined
            : unresolvedTotal === 0 ? "ok"
            : unresolvedTotal < 20 ? "warn"
            : "critical"
          }
          help="챗봇이 답변하지 못하거나 QA 검수에서 이슈가 확인된 질문의 누적 건수입니다. 0건이 목표입니다. 건수가 많을수록 지식 보강이 시급합니다."
        />
        <KpiCard
          label="오답 의심"
          value={confirmedCount}
          sub="건"
          status={confirmedCount === 0 ? "ok" : confirmedCount < 5 ? "warn" : "critical"}
          help="QA 검수자가 '이슈 확인' 상태로 분류한 응답 건수입니다. 환각·오정보·출처 누락 등이 해당됩니다. 신속한 원인 분석과 문서 보완이 필요합니다."
        />
        <KpiCard
          label="저만족 응답"
          value={lowSatisfactionCount ?? "-"}
          sub="건"
          status={
            lowSatisfactionCount == null ? undefined
            : lowSatisfactionCount === 0 ? "ok"
            : lowSatisfactionCount < 10 ? "warn"
            : "critical"
          }
          help="시민이 낮은 만족도(1~2점)를 준 응답의 건수입니다. 높으면 챗봇 답변 품질에 문제가 있는 신호입니다. 해당 질문을 직접 검토해 개선 포인트를 찾으세요."
        />
      </div>

      {/* 미응답 질문 목록 (원인코드 포함) */}
      <Card>
        <CardHeader>
          <CardTitle>미응답 질문 목록 (최근 5건)</CardTitle>
        </CardHeader>
        <div className="overflow-hidden">
          <Table>
            <Thead>
              <Th>질문 내용</Th>
              <Th>원인 코드</Th>
              <Th>답변 상태</Th>
              <Th>생성일</Th>
            </Thead>
            <Tbody>
              {unresolvedItems.map((q) => (
                <Tr key={q.questionId}>
                  <Td className="max-w-xs truncate text-sm">{q.questionText}</Td>
                  <Td>
                    {q.failureReasonCode ? (
                      <span className="font-mono text-xs text-warning">
                        {q.failureReasonCode} · {FAILURE_LABEL[q.failureReasonCode as RootCauseCode] ?? q.failureReasonCode}
                      </span>
                    ) : (
                      <span className="text-text-muted text-xs">-</span>
                    )}
                  </Td>
                  <Td>
                    <Badge variant={q.answerStatus === "no_answer" ? "error" : q.answerStatus === "error" ? "warning" : "warning"}>
                      {q.answerStatus === "no_answer" ? "무응답" : q.answerStatus === "error" ? "오류" : "Fallback"}
                    </Badge>
                  </Td>
                  <Td className="text-xs text-text-muted">
                    {new Date(q.createdAt).toLocaleDateString("ko-KR")}
                  </Td>
                </Tr>
              ))}
              {unresolvedItems.length === 0 && (
                <Tr>
                  <Td colSpan={4} className="text-center text-text-muted text-sm py-6">
                    미응답 질문이 없습니다.
                  </Td>
                </Tr>
              )}
            </Tbody>
          </Table>
        </div>
      </Card>

      {/* RAGAS 스코어 */}
      <Card>
        <CardHeader>
          <CardTitle>RAGAS 평가 스코어</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4">
          {latestRagas == null ? (
            <p className="text-text-muted text-sm text-center py-6">
              평가 데이터 없음 — eval-runner를 실행해 RAGAS 점수를 생성하세요.
            </p>
          ) : (
            <>
              <ScoreTable rows={ragasRows} />
              <p className="text-xs text-text-muted mt-3">
                평가 일시: {new Date(latestRagas.evaluatedAt).toLocaleString("ko-KR")}
              </p>
            </>
          )}
        </div>
      </Card>

      {/* 최근 QA 리뷰 5건 */}
      <Card>
        <CardHeader>
          <CardTitle>최근 QA 리뷰 (5건)</CardTitle>
        </CardHeader>
        <div className="overflow-hidden">
          <Table>
            <Thead>
              <Th>리뷰 ID</Th>
              <Th>상태</Th>
              <Th>생성일</Th>
            </Thead>
            <Tbody>
              {reviews.map((r, i) => (
                <Tr key={r.qaReviewId ?? i}>
                  <Td className="font-mono text-xs text-text-muted">{r.qaReviewId}</Td>
                  <Td>
                    <Badge variant={REVIEW_STATUS_VARIANT[r.reviewStatus]}>
                      {REVIEW_STATUS_LABEL[r.reviewStatus]}
                    </Badge>
                  </Td>
                  <Td className="text-xs text-text-muted">
                    {new Date(r.createdAt).toLocaleString("ko-KR")}
                  </Td>
                </Tr>
              ))}
              {reviews.length === 0 && (
                <Tr>
                  <Td colSpan={3} className="text-center text-text-muted text-sm py-6">
                    리뷰 데이터가 없습니다.
                  </Td>
                </Tr>
              )}
            </Tbody>
          </Table>
        </div>
      </Card>
    </div>
  );
}
