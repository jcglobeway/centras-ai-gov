"use client";

import { useState } from "react";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, UnresolvedQuestion, AnswerStatus } from "@/lib/types";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";
import { Spinner } from "@/components/ui/Spinner";
import { PageFilters, getWeekFrom, getToday } from "@/components/ui/PageFilters";
import type { ComponentProps } from "react";

type BadgeVariant = ComponentProps<typeof Badge>["variant"];

const STATUS_LABEL: Record<AnswerStatus, string> = {
  answered: "응답",
  fallback: "Fallback",
  no_answer: "미응답",
  error: "오류",
};

const STATUS_VARIANT: Record<AnswerStatus, BadgeVariant> = {
  answered: "success",
  fallback: "warning",
  no_answer: "error",
  error: "error",
};

export default function PerformancePage() {
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

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between flex-wrap gap-2">
        <div className="flex items-center gap-4">
          <h2 className="text-text-primary font-semibold text-lg">민원응대 성과</h2>
          <span className="text-text-muted text-xs">총 {data?.total ?? 0}건</span>
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
            <Th>만족도</Th>
            <Th>생성일</Th>
          </Thead>
          <Tbody>
            {questions.map((q) => (
              <Tr key={q.questionId}>
                <Td className="font-mono text-xs text-text-muted">{q.questionId}</Td>
                <Td className="max-w-xs truncate text-sm">{q.questionText}</Td>
                <Td className="text-xs text-text-secondary">
                  {q.categoryL1 ?? "-"}
                  {q.categoryL2 ? ` / ${q.categoryL2}` : ""}
                </Td>
                <Td>
                  <Badge variant={STATUS_VARIANT[q.answerStatus]}>
                    {STATUS_LABEL[q.answerStatus]}
                  </Badge>
                </Td>
                <Td className="text-xs">
                  {q.satisfactionScore != null ? q.satisfactionScore : "-"}
                </Td>
                <Td className="text-xs text-text-muted">
                  {new Date(q.createdAt).toLocaleDateString("ko-KR")}
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
        {questions.length === 0 && (
          <p className="text-center text-text-muted text-sm py-8">질문이 없습니다.</p>
        )}
      </div>
    </div>
  );
}
