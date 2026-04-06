"use client";

import { useState } from "react";
import Link from "next/link";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, UnresolvedQuestion, AnswerStatus, ReviewStatus } from "@/lib/types";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { PageFilters, getWeekFrom, getToday } from "@/components/ui/PageFilters";
import { PageGuide } from "@/components/ui/PageGuide";
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

export default function OpsUnresolvedPage() {
  const [orgId, setOrgId] = useState("");
  const [from, setFrom] = useState(getWeekFrom);
  const [to, setTo] = useState(getToday);

  const params = new URLSearchParams({ page_size: "20" });
  if (orgId) params.set("organization_id", orgId);
  if (from) params.set("from_date", from);
  if (to) params.set("to_date", to);

  const { data, error, isLoading } = useSWR<PagedResponse<UnresolvedQuestion>>(
    `/api/admin/questions/unresolved?${params}`,
    fetcher
  );

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
  const total = data?.total ?? 0;

  const totalBadgeVariant: BadgeVariant =
    total >= 20 ? "error" : total >= 5 ? "warning" : "success";

  return (
    <div className="space-y-4">
      <PageGuide
        description="챗봇이 답변하지 못하거나 QA에서 이슈로 분류된 질문 목록입니다."
        tips={[
          "같은 주제의 질문이 반복된다면 해당 문서를 지식베이스에 추가하세요.",
          "원인 코드(A01~A10)를 확인해 문제가 지식 부재인지 파이프라인 오류인지 구분하세요.",
          "A01~A02(문서 없음/최신화 필요)는 고객사가, A03~A07(파이프라인 문제)은 운영사가 처리합니다.",
        ]}
      />
      <div className="flex items-center justify-between flex-wrap gap-2">
        <div className="flex items-center gap-3">
          <h2 className="text-text-primary font-semibold text-lg">미해결 질의 관리</h2>
          <Badge variant={totalBadgeVariant}>미결 {total}건</Badge>
        </div>
        <PageFilters
          orgId={orgId} onOrgChange={setOrgId}
          from={from} onFromChange={setFrom}
          to={to} onToChange={setTo}
        />
      </div>

      <div className="bg-bg-surface border border-bg-border rounded-xl overflow-hidden">
        <Table>
          <Thead>
            <Th>질문 ID</Th>
            <Th>내용</Th>
            <Th>카테고리</Th>
            <Th>응답 상태</Th>
            <Th>최근 리뷰</Th>
            <Th>생성일</Th>
            <Th></Th>
          </Thead>
          <Tbody>
            {questions.map((q) => (
              <Tr key={q.questionId}>
                <Td className="font-mono text-xs text-text-muted">{q.questionId}</Td>
                <Td className="max-w-xs">
                  <span className="text-sm line-clamp-2">
                    {q.questionText.slice(0, 50)}
                    {q.questionText.length > 50 ? "…" : ""}
                  </span>
                </Td>
                <Td className="text-xs text-text-muted">
                  {(q as unknown as { questionCategory?: string }).questionCategory ?? "-"}
                </Td>
                <Td>
                  <Badge variant={q.answerStatus ? ANSWER_VARIANT[q.answerStatus as AnswerStatus] : "neutral"}>
                    {q.answerStatus ? ANSWER_LABEL[q.answerStatus as AnswerStatus] : "-"}
                  </Badge>
                </Td>
                <Td>
                  {q.latestReviewStatus ? (
                    <Badge variant={REVIEW_VARIANT[q.latestReviewStatus as ReviewStatus]}>
                      {REVIEW_LABEL[q.latestReviewStatus as ReviewStatus]}
                    </Badge>
                  ) : (
                    <span className="text-text-muted text-xs">없음</span>
                  )}
                </Td>
                <Td className="text-xs text-text-muted">
                  {new Date(q.createdAt).toLocaleDateString("ko-KR")}
                </Td>
                <Td>
                  <div className="flex items-center gap-2">
                    <div className="relative group">
                      <Button
                        variant="secondary"
                        size="sm"
                        disabled
                      >
                        담당자 지정
                      </Button>
                      <span className="absolute bottom-full left-1/2 -translate-x-1/2 mb-1.5 hidden group-hover:block whitespace-nowrap bg-bg-elevated border border-bg-border text-text-muted text-[10px] rounded px-2 py-1 z-10">
                        준비 중
                      </span>
                    </div>
                    <Link
                      href="/ops/upload"
                      className="text-xs text-accent hover:underline whitespace-nowrap"
                    >
                      지식베이스 추가 →
                    </Link>
                  </div>
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
        {questions.length === 0 && (
          <p className="text-center text-text-muted text-sm py-8">미결 질문이 없습니다.</p>
        )}
      </div>
    </div>
  );
}
