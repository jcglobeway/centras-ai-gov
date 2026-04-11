"use client";

import { useEffect, useState } from "react";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";

interface CaseResult {
  id: string;
  caseId: string;
  queryText: string;
  responseText: string;
  answerStatus: string;
  judgment: "pass" | "fail" | "skip";
  judgmentDetail: string | null;
  executedAt: string;
}

interface BatchRunDetail {
  run: {
    id: string;
    status: string;
    totalCases: number;
    passCount: number;
    failCount: number;
    passRate: number;
    startedAt: string;
    completedAt: string | null;
  };
  results: CaseResult[];
}

interface BatchRunResultModalProps {
  runId: string;
  onClose: () => void;
}

export function BatchRunResultModal({ runId, onClose }: BatchRunResultModalProps) {
  const [detail, setDetail] = useState<BatchRunDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch(`/api/admin/redteam/batch-runs/${runId}`)
      .then((r) => r.json())
      .then(setDetail)
      .catch(() => setError("결과를 불러오지 못했습니다."))
      .finally(() => setLoading(false));
  }, [runId]);

  const judgmentVariant = (j: string) => {
    if (j === "pass") return "success" as const;
    if (j === "fail") return "error" as const;
    return "neutral" as const;
  };

  const judgmentLabel = (j: string) => {
    if (j === "pass") return "통과";
    if (j === "fail") return "실패";
    return "건너뜀";
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
      <div className="bg-bg-elevated border border-bg-border rounded-xl w-full max-w-3xl max-h-[80vh] flex flex-col">
        <div className="flex items-center justify-between px-6 py-4 border-b border-bg-border">
          <h3 className="text-text-primary font-semibold text-base">배치런 상세 결과</h3>
          <Button variant="ghost" onClick={onClose}>닫기</Button>
        </div>

        <div className="overflow-auto flex-1 p-6">
          {loading && <p className="text-text-muted text-sm">불러오는 중...</p>}
          {error && <p className="text-error text-sm">{error}</p>}
          {detail && (
            <div className="space-y-4">
              <div className="flex gap-6 text-sm">
                <span className="text-text-muted">총 케이스: <span className="text-text-primary font-mono">{detail.run.totalCases}</span></span>
                <span className="text-text-muted">통과: <span className="text-success font-mono">{detail.run.passCount}</span></span>
                <span className="text-text-muted">실패: <span className="text-error font-mono">{detail.run.failCount}</span></span>
                <span className="text-text-muted">방어율: <span className="text-text-primary font-mono">{detail.run.passRate.toFixed(1)}%</span></span>
              </div>

              <Table>
                <Thead>
                  <Th>질의</Th>
                  <Th>응답 상태</Th>
                  <Th>판정</Th>
                  <Th>판정 근거</Th>
                </Thead>
                <Tbody>
                  {detail.results.map((r) => (
                    <Tr key={r.id}>
                      <Td className="text-sm max-w-xs truncate">{r.queryText}</Td>
                      <Td className="font-mono text-xs text-text-muted">{r.answerStatus}</Td>
                      <Td>
                        <Badge variant={judgmentVariant(r.judgment)}>
                          {judgmentLabel(r.judgment)}
                        </Badge>
                      </Td>
                      <Td className="text-xs text-text-muted max-w-xs truncate">
                        {r.judgmentDetail ?? "-"}
                      </Td>
                    </Tr>
                  ))}
                </Tbody>
              </Table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
