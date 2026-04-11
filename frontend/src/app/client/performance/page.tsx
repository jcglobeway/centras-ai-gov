"use client";

import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, Question } from "@/lib/types";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Spinner } from "@/components/ui/Spinner";
import { useFilter } from "@/lib/filter-context";

export default function PerformancePage() {
  const { orgId, from, to } = useFilter();

  const params = new URLSearchParams({ page_size: "20" });
  if (orgId) params.set("organization_id", orgId);
  if (from) params.set("from_date", from);
  if (to) params.set("to_date", to);

  const { data, error, isLoading } = useSWR<PagedResponse<Question>>(
    `/api/admin/questions?${params}`,
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
      <div className="flex items-center gap-4">
        <h2 className="text-text-primary font-semibold text-lg">최근 응대 현황</h2>
        <span className="text-text-muted text-xs">총 {data?.total ?? 0}건</span>
      </div>

      <div className="bg-bg-surface border border-bg-border rounded-xl overflow-hidden">
        <Table>
          <Thead>
            <Th>질문 ID</Th>
            <Th>내용</Th>
            <Th>카테고리</Th>
            <Th>신뢰도</Th>
            <Th>생성일</Th>
          </Thead>
          <Tbody>
            {questions.map((q) => (
              <Tr key={q.questionId}>
                <Td className="font-mono text-xs text-text-muted">{q.questionId}</Td>
                <Td className="max-w-xs truncate text-sm">{q.questionText}</Td>
                <Td className="text-xs text-text-secondary">{q.questionCategory ?? "-"}</Td>
                <Td className="text-xs">
                  {q.answerConfidence != null ? Number(q.answerConfidence).toFixed(2) : "-"}
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
