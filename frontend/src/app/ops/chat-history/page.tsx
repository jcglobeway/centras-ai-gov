"use client";

import { useRouter } from "next/navigation";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, ChatSession } from "@/lib/types";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Spinner } from "@/components/ui/Spinner";
import { useFilter } from "@/lib/filter-context";
import { PageGuide } from "@/components/ui/PageGuide";

// ── 헬퍼 ─────────────────────────────────────────────────────────────────────

function sessionEndTypeLabel(t: string | null): string {
  if (!t) return "-";
  const map: Record<string, string> = {
    user_closed: "사용자 종료",
    timeout: "타임아웃",
    escalated: "상담 전환",
    resolved: "해결 완료",
  };
  return map[t] ?? t;
}

function durationLabel(startedAt: string, endedAt: string | null): string {
  if (!endedAt) return "진행 중";
  const ms = new Date(endedAt).getTime() - new Date(startedAt).getTime();
  const min = Math.floor(ms / 60000);
  const sec = Math.floor((ms % 60000) / 1000);
  return min > 0 ? `${min}분 ${sec}초` : `${sec}초`;
}

// ── 세션 목록 페이지 ──────────────────────────────────────────────────────────

export default function ChatHistoryPage() {
  const router = useRouter();
  const { orgId, from, to } = useFilter();

  const params = new URLSearchParams();
  if (orgId) params.set("organization_id", orgId);
  if (from) params.set("from_date", from);
  if (to) params.set("to_date", to);

  const { data, isLoading } = useSWR<PagedResponse<ChatSession>>(
    `/api/admin/chat-sessions?${params}`,
    fetcher,
  );

  const sessions = data?.items ?? [];

  return (
    <div className="space-y-4">
      <PageGuide
        description="시민과의 대화 세션 이력을 조회합니다. 세션을 클릭하면 전체 대화 흐름과 각 질문의 품질 지표를 확인할 수 있습니다."
        tips={[
          "세션 행을 클릭하면 해당 세션의 전체 Q&A 대화 스레드로 이동합니다.",
          "각 대화 항목을 클릭하면 RAGAS 지표, 검색 컨텍스트, QA 검수 상세 패널이 열립니다.",
          "기관 필터와 날짜 범위를 조합해 특정 기간의 세션을 조회하세요.",
        ]}
      />
      <h2 className="text-text-primary font-semibold text-lg">대화 이력</h2>

      <Card>
        <CardHeader>
          <CardTitle tag="SESSIONS">세션 목록 (클릭 시 대화 상세)</CardTitle>
        </CardHeader>
        {isLoading ? (
          <div className="flex items-center justify-center h-24"><Spinner /></div>
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <Thead>
                <Th>시작 시각</Th>
                <Th>채널</Th>
                <Th>질문 수</Th>
                <Th>소요 시간</Th>
                <Th>종료 유형</Th>
              </Thead>
              <Tbody>
                {sessions.map((s) => (
                  <Tr
                    key={s.sessionId}
                    className="cursor-pointer"
                    onClick={() => router.push(`/ops/chat-history/${s.sessionId}`)}
                  >
                    <Td className="text-xs text-text-secondary whitespace-nowrap">
                      {new Date(s.startedAt).toLocaleString("ko-KR")}
                    </Td>
                    <Td className="text-xs text-text-muted">{s.channel}</Td>
                    <Td className="text-xs text-text-primary font-mono">{s.totalQuestionCount}</Td>
                    <Td className="text-xs text-text-muted">{durationLabel(s.startedAt, s.endedAt)}</Td>
                    <Td className="text-xs text-text-muted">{sessionEndTypeLabel(s.sessionEndType)}</Td>
                  </Tr>
                ))}
                {sessions.length === 0 && (
                  <Tr>
                    <Td colSpan={5} className="text-center text-text-muted text-sm py-8">
                      조회된 세션이 없습니다.
                    </Td>
                  </Tr>
                )}
              </Tbody>
            </Table>
          </div>
        )}
        {sessions.length > 0 && (
          <div className="px-4 py-3 border-t border-bg-border">
            <p className="text-[11px] text-text-muted">총 {data?.total ?? sessions.length}개 세션</p>
          </div>
        )}
      </Card>
    </div>
  );
}
