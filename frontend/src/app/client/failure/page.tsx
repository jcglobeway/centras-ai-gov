"use client";

import { useState } from "react";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, UnresolvedQuestion, RootCauseCode } from "@/lib/types";
import { Card } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Spinner } from "@/components/ui/Spinner";
import { PageFilters, getWeekFrom, getToday } from "@/components/ui/PageFilters";

const FAILURE_DESCRIPTIONS: Record<RootCauseCode, { label: string; desc: string; owner: string }> = {
  A01: { label: "문서 없음",        desc: "질문에 해당하는 문서가 지식베이스에 등록되지 않음",           owner: "고객사" },
  A02: { label: "문서 최신 아님",   desc: "법령·조례·공고 개정 후 지식베이스에 반영되지 않은 문서 참조", owner: "고객사" },
  A03: { label: "파싱 실패",        desc: "PDF·한글 등 문서 형식 처리 오류로 내용 추출 불가",           owner: "운영사" },
  A04: { label: "검색 실패",        desc: "벡터 검색 결과가 전혀 없어 관련 문서를 찾지 못함",            owner: "운영사" },
  A05: { label: "재랭킹 실패",      desc: "검색된 문서가 있으나 관련 문서 순위 산정이 잘못됨",           owner: "운영사" },
  A06: { label: "생성 답변 왜곡",   desc: "문서 내용과 다른 답변 생성(환각·출처 이탈) 의심",            owner: "운영사" },
  A07: { label: "의도 분류 실패",   desc: "질문 카테고리·의도 파악에 실패해 잘못된 검색 경로 진입",      owner: "운영사" },
  A08: { label: "정책상 제한",      desc: "답변 불가 영역(민감 업무·법적 한계)으로 정책상 응답 차단",    owner: "협의" },
  A09: { label: "질문 표현 모호",   desc: "사용자 질문이 불분명해 의미 파악 불가 — FAQ 유도 문구 개선 필요", owner: "고객사" },
  A10: { label: "채널 UI 문제",     desc: "입력 형식·채널 연동 오류로 질문이 정상 수신되지 않음",        owner: "운영사" },
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

          const { label, desc, owner } = FAILURE_DESCRIPTIONS[code];
          const ownerVariant = owner === "고객사" ? "info" : owner === "협의" ? "warning" : "neutral";

          return (
            <Card key={code} className="flex flex-col gap-2">
              <div className="flex items-center justify-between">
                <span className="font-mono text-xs font-bold text-text-secondary">{code}</span>
                <Badge variant={variant}>{count}건</Badge>
              </div>
              <p className="text-text-primary text-xs font-medium">{label}</p>
              <p className="text-text-muted text-[10px] leading-relaxed">{desc}</p>
              <div className="flex items-center justify-between mt-0.5">
                <Badge variant={ownerVariant} className="text-[9px]">조치: {owner}</Badge>
              </div>
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
