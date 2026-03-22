"use client";

import { useState } from "react";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, UnresolvedQuestion, QAReview, ReviewStatus, RagasEvaluation } from "@/lib/types";
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

export default function QaDashboardPage() {
  const [orgId, setOrgId] = useState("");
  const [from, setFrom] = useState(getWeekFrom);
  const [to, setTo] = useState(getToday);

  const params = new URLSearchParams();
  if (orgId) params.set("organization_id", orgId);
  if (from) params.set("from_date", from);
  if (to) params.set("to_date", to);

  const unresolvedParams = new URLSearchParams(params);
  unresolvedParams.set("page_size", "1");

  const reviewParams = new URLSearchParams(params);
  reviewParams.set("page_size", "5");

  const { data: unresolvedData, isLoading: loadingUnresolved } =
    useSWR<PagedResponse<UnresolvedQuestion>>(
      `/api/admin/questions/unresolved?${unresolvedParams}`,
      fetcher
    );

  const { data: reviewsData, isLoading: loadingReviews } =
    useSWR<PagedResponse<QAReview>>(`/api/admin/qa-reviews?${reviewParams}`, fetcher);

  const { data: ragasData } = useSWR<PagedResponse<RagasEvaluation>>(
    `/api/admin/ragas-evaluations?page_size=1`,
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
  const confirmedCount = reviews.filter((r) => r.reviewStatus === "confirmed_issue").length;
  const resolvedCount = reviews.filter((r) => r.reviewStatus === "resolved").length;
  const falseAlarmCount = reviews.filter((r) => r.reviewStatus === "false_alarm").length;

  const latestRagas = ragasData?.items?.[0] ?? null;
  const ragasRows = [
    { label: "Faithfulness", value: latestRagas?.faithfulness ?? null, target: 0.90 },
    { label: "Answer Relevance", value: latestRagas?.answerRelevancy ?? null, target: 0.85 },
    { label: "Context Precision", value: latestRagas?.contextPrecision ?? null, target: 0.70 },
  ];

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

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <KpiCard
          label="미검수 건수"
          value={unresolvedData?.total ?? "-"}
          sub="건"
          trend="down"
          trendValue="처리 필요"
        />
        <KpiCard label="확인된 이슈" value={confirmedCount} sub="건" />
        <KpiCard label="해결 완료" value={resolvedCount} sub="건" trend="up" trendValue="누적" />
        <KpiCard label="오탐지" value={falseAlarmCount} sub="건" />
      </div>

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
