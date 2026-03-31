"use client";

import { useState } from "react";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse } from "@/lib/types";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";
import { Spinner } from "@/components/ui/Spinner";
import { PageFilters, getWeekFrom, getToday } from "@/components/ui/PageFilters";
import { PageGuide } from "@/components/ui/PageGuide";
import type { ComponentProps } from "react";

type BadgeVariant = ComponentProps<typeof Badge>["variant"];

interface QuestionItem {
  questionId: string;
  organizationId: string;
  chatSessionId: string;
  questionText: string;
  questionCategory: string | null;
  failureReasonCode: string | null;
  isEscalated: boolean;
  createdAt: string;
}

function answerStatusBadge(failureReasonCode: string | null, isEscalated: boolean): { label: string; variant: BadgeVariant } {
  if (isEscalated) return { label: "상담 전환", variant: "warning" };
  if (failureReasonCode) return { label: "미해결", variant: "error" };
  return { label: "정상 응답", variant: "success" };
}

export default function ChatHistoryPage() {
  const [orgId, setOrgId] = useState("");
  const [from, setFrom] = useState(getWeekFrom);
  const [to, setTo] = useState(getToday);

  const params = new URLSearchParams({ page_size: "30" });
  if (orgId) params.set("organization_id", orgId);
  if (from) params.set("from_date", from);
  if (to) params.set("to_date", to);

  const { data, isLoading } = useSWR<PagedResponse<QuestionItem>>(
    `/api/admin/questions?${params}`,
    fetcher,
  );

  const questions = data?.items ?? [];

  return (
    <div className="space-y-4">
      <PageGuide
        description="실제 시민 질문 이력을 조회하고 품질 문제를 파악하는 화면입니다."
        tips={[
          "미해결·상담 전환 건을 확인해 자주 실패하는 질의 유형을 파악하세요.",
          "기관별 필터로 특정 고객사의 대화 패턴을 집중 분석할 수 있습니다.",
          "개인정보 보호를 위해 원문 조회는 감사 로그에 기록됩니다.",
        ]}
      />
      <div className="flex items-center justify-between flex-wrap gap-2">
        <h2 className="text-text-primary font-semibold text-lg">대화 이력</h2>
        <PageFilters
          orgId={orgId} onOrgChange={setOrgId}
          from={from} onFromChange={setFrom}
          to={to} onToChange={setTo}
        />
      </div>

      <Card>
        <CardHeader>
          <CardTitle tag="QUESTIONS">질문 이력</CardTitle>
        </CardHeader>
        {isLoading ? (
          <div className="flex items-center justify-center h-24">
            <Spinner />
          </div>
        ) : (
          <div className="overflow-hidden">
            <Table>
              <Thead>
                <Th>질문 ID</Th>
                <Th>기관</Th>
                <Th>질문 내용</Th>
                <Th>카테고리</Th>
                <Th>응답 상태</Th>
                <Th>시각</Th>
              </Thead>
              <Tbody>
                {questions.map((q) => {
                  const status = answerStatusBadge(q.failureReasonCode, q.isEscalated);
                  return (
                    <Tr key={q.questionId}>
                      <Td className="font-mono text-xs text-text-muted">{q.questionId}</Td>
                      <Td className="text-xs text-text-muted">{q.organizationId}</Td>
                      <Td className="text-sm max-w-xs truncate" title={q.questionText}>
                        {q.questionText}
                      </Td>
                      <Td className="text-xs text-text-muted">{q.questionCategory ?? "-"}</Td>
                      <Td>
                        <Badge variant={status.variant}>{status.label}</Badge>
                      </Td>
                      <Td className="text-xs text-text-muted">
                        {new Date(q.createdAt).toLocaleString("ko-KR")}
                      </Td>
                    </Tr>
                  );
                })}
                {questions.length === 0 && (
                  <Tr>
                    <Td colSpan={6} className="text-center text-text-muted text-sm py-8">
                      조회된 질문이 없습니다.
                    </Td>
                  </Tr>
                )}
              </Tbody>
            </Table>
          </div>
        )}
        {questions.length > 0 && (
          <div className="px-4 py-3 border-t border-bg-border">
            <p className="text-[11px] text-text-muted">
              총 {data?.total ?? questions.length}건 중 최신 {questions.length}건 표시
            </p>
          </div>
        )}
      </Card>
    </div>
  );
}
