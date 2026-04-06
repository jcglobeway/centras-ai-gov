"use client";

import { useState } from "react";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse } from "@/lib/types";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { PageGuide } from "@/components/ui/PageGuide";

interface Feedback {
  feedbackId: string;
  questionId: string;
  sessionId?: string;
  rating: number;
  feedbackText?: string;
  createdAt: string;
}

const MOCK_CORRECTION_HISTORY = [
  {
    id: "cor_001",
    questionId: "question_abc1",
    correctedBy: "qa@jcg.com",
    correctedAt: "2026-03-28",
    before: "정보를 찾을 수 없습니다.",
    after: "복지급여는 읍면동 주민센터에서 신청하실 수 있습니다.",
  },
];

function starRating(n: number) {
  return "★".repeat(n) + "☆".repeat(5 - n);
}

export default function CorrectionPage() {
  const [groundTruthQuestion, setGroundTruthQuestion] = useState("");
  const [groundTruthAnswer, setGroundTruthAnswer] = useState("");

  const { data, isLoading } = useSWR<PagedResponse<Feedback>>(
    `/api/admin/feedbacks?page_size=20`,
    fetcher
  );

  const lowRatingFeedbacks = (data?.items ?? []).filter((f) => f.rating <= 2);

  return (
    <div className="space-y-6">
      <PageGuide
        description="낮은 평점을 받은 답변을 검토하고 정답을 직접 입력하는 화면입니다."
        tips={[
          "👎 답변 목록에서 패턴을 찾아 프롬프트 또는 문서를 개선하세요.",
          "Ground Truth를 입력하면 이후 RAGAS 평가와 파인튜닝 데이터셋으로 활용됩니다.",
          "교정 이력을 정기적으로 검토해 반복 오류 유형을 파악하세요.",
        ]}
      />
      <div className="flex items-center gap-3">
        <h2 className="text-text-primary font-semibold text-lg">답변 교정</h2>
        <span className="text-[10px] font-mono text-warning bg-warning/10 border border-warning/20 rounded px-2 py-0.5">
          목업 데이터
        </span>
      </div>

      {/* 낮은 평점 답변 목록 */}
      <Card>
        <CardHeader>
          <CardTitle tag="FEEDBACK">부정 피드백 답변 목록 (평점 1~2)</CardTitle>
        </CardHeader>
        <div className="overflow-hidden">
          {isLoading ? (
            <div className="flex items-center justify-center h-24">
              <Spinner />
            </div>
          ) : lowRatingFeedbacks.length === 0 ? (
            <p className="text-center text-text-muted text-sm py-8">
              부정 피드백이 없습니다.
            </p>
          ) : (
            <Table>
              <Thead>
                <Th>피드백 ID</Th>
                <Th>질문 ID</Th>
                <Th>평점</Th>
                <Th>피드백 텍스트</Th>
                <Th>생성일</Th>
              </Thead>
              <Tbody>
                {lowRatingFeedbacks.map((f) => (
                  <Tr key={f.feedbackId}>
                    <Td className="font-mono text-xs text-text-muted">{f.feedbackId}</Td>
                    <Td className="font-mono text-xs text-text-muted">{f.questionId}</Td>
                    <Td>
                      <span className="text-error text-xs font-mono">{starRating(f.rating)}</span>
                    </Td>
                    <Td className="max-w-xs">
                      <span className="text-xs text-text-secondary line-clamp-2">
                        {f.feedbackText ?? "-"}
                      </span>
                    </Td>
                    <Td className="text-xs text-text-muted">
                      {new Date(f.createdAt).toLocaleDateString("ko-KR")}
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          )}
        </div>
      </Card>

      {/* Ground Truth 입력 */}
      <Card>
        <CardHeader>
          <CardTitle tag="GROUND TRUTH">정답 데이터셋 입력</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4 space-y-3">
          <div className="space-y-1">
            <label className="text-xs text-text-muted font-mono">질문 ID</label>
            <input
              value={groundTruthQuestion}
              onChange={(e) => setGroundTruthQuestion(e.target.value)}
              placeholder="question_xxxxxxxx"
              className="w-full bg-bg-base border border-bg-border rounded px-3 py-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:border-accent font-mono"
            />
          </div>
          <div className="space-y-1">
            <label className="text-xs text-text-muted font-mono">정답 직접 입력</label>
            <textarea
              value={groundTruthAnswer}
              onChange={(e) => setGroundTruthAnswer(e.target.value)}
              placeholder="올바른 답변을 입력하세요..."
              rows={4}
              className="w-full bg-bg-base border border-bg-border rounded px-3 py-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:border-accent resize-none"
            />
          </div>
          <div className="relative group inline-block">
            <Button disabled>데이터셋에 추가</Button>
            <span className="absolute bottom-full left-0 mb-1.5 hidden group-hover:block whitespace-nowrap bg-bg-elevated border border-bg-border text-text-muted text-[10px] rounded px-2 py-1 z-10">
              Ground Truth 저장 API 연동 예정
            </span>
          </div>
        </div>
      </Card>

      {/* 교정 이력 */}
      <Card>
        <CardHeader>
          <CardTitle tag="HISTORY">교정 이력</CardTitle>
        </CardHeader>
        <div className="overflow-hidden">
          <Table>
            <Thead>
              <Th>교정 ID</Th>
              <Th>질문 ID</Th>
              <Th>교정자</Th>
              <Th>교정일</Th>
              <Th>교정 전</Th>
              <Th>교정 후</Th>
            </Thead>
            <Tbody>
              {MOCK_CORRECTION_HISTORY.map((item) => (
                <Tr key={item.id}>
                  <Td className="font-mono text-xs text-text-muted">{item.id}</Td>
                  <Td className="font-mono text-xs text-text-muted">{item.questionId}</Td>
                  <Td className="text-xs">{item.correctedBy}</Td>
                  <Td className="text-xs text-text-muted">{item.correctedAt}</Td>
                  <Td className="max-w-[160px]">
                    <span className="text-xs text-error line-clamp-2">{item.before}</span>
                  </Td>
                  <Td className="max-w-[160px]">
                    <span className="text-xs text-success line-clamp-2">{item.after}</span>
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        </div>
      </Card>
    </div>
  );
}