"use client";

import { useState } from "react";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, UnresolvedQuestion, RootCauseCode } from "@/lib/types";
import { Card } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Spinner } from "@/components/ui/Spinner";
import { PageFilters, getWeekFrom, getToday } from "@/components/ui/PageFilters";

const FAILURE_DESCRIPTIONS: Record<RootCauseCode, string> = {
  A01: "문서 미존재",
  A02: "검색 정확도 부족",
  A03: "답변 신뢰도 낮음",
  A04: "질문 의도 파악 실패",
  A05: "복합 질문 처리 불가",
  A06: "개인정보 포함 질문",
  A07: "시스템 오류",
  A08: "타임아웃",
  A09: "언어 감지 실패",
  A10: "기타",
};

const ALL_CODES: RootCauseCode[] = ["A01","A02","A03","A04","A05","A06","A07","A08","A09","A10"];

export default function FailurePage() {
  const [orgId, setOrgId] = useState("");
  const [from, setFrom] = useState(getWeekFrom);
  const [to, setTo] = useState(getToday);

  const params = new URLSearchParams({ page_size: "100" });
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

  // failureCode 집계
  const counts: Record<RootCauseCode, number> = {} as Record<RootCauseCode, number>;
  ALL_CODES.forEach((c) => (counts[c] = 0));
  questions.forEach((q) => {
    if (q.failureCode && q.failureCode in counts) {
      counts[q.failureCode]++;
    }
  });

  const maxCount = Math.max(...Object.values(counts), 1);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between flex-wrap gap-2">
        <h2 className="text-text-primary font-semibold text-lg">실패/전환 분석</h2>
        <PageFilters
          orgId={orgId} onOrgChange={setOrgId}
          from={from} onFromChange={setFrom}
          to={to} onToChange={setTo}
        />
      </div>
      <p className="text-text-secondary text-sm">
        미결 질문 {questions.length}건의 실패 원인 코드 분포
      </p>

      <div className="grid grid-cols-2 lg:grid-cols-5 gap-3">
        {ALL_CODES.map((code) => {
          const count = counts[code];
          const ratio = count / maxCount;
          const variant =
            ratio > 0.6 ? "error" : ratio > 0.3 ? "warning" : count > 0 ? "info" : "neutral";

          return (
            <Card key={code} className="flex flex-col gap-2">
              <div className="flex items-center justify-between">
                <span className="font-mono text-xs font-bold text-text-secondary">{code}</span>
                <Badge variant={variant}>{count}건</Badge>
              </div>
              <p className="text-text-primary text-xs font-medium">
                {FAILURE_DESCRIPTIONS[code]}
              </p>
              {/* 미니 바 */}
              <div className="h-1 bg-bg-border rounded-full overflow-hidden">
                <div
                  className="h-full bg-accent rounded-full transition-all"
                  style={{ width: `${ratio * 100}%` }}
                />
              </div>
            </Card>
          );
        })}
      </div>
    </div>
  );
}
