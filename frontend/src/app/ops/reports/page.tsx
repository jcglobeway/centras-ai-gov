"use client";

import { useState } from "react";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Button } from "@/components/ui/Button";
import { PageGuide } from "@/components/ui/PageGuide";

const MOCK_SATISFACTION = [
  { org: "국립아시아문화전당", score: 4.2, responses: 234 },
  { org: "중앙정부기관",       score: 3.8, responses: 156 },
];

const WEEKLY_TRENDS = [
  { week: "W1 (3/7~3/13)",  faithfulness: 0.89, sessionSuccess: 87 },
  { week: "W2 (3/14~3/20)", faithfulness: 0.91, sessionSuccess: 89 },
  { week: "W3 (3/21~3/27)", faithfulness: 0.92, sessionSuccess: 91 },
];

const MONTHLY_TRENDS = [
  { month: "2026-01", faithfulness: 0.85, sessionSuccess: 82 },
  { month: "2026-02", faithfulness: 0.88, sessionSuccess: 86 },
  { month: "2026-03", faithfulness: 0.92, sessionSuccess: 91 },
];

export default function ReportsPage() {
  const [period, setPeriod] = useState<"weekly" | "monthly">("weekly");

  const trends = period === "weekly" ? WEEKLY_TRENDS : MONTHLY_TRENDS;
  const periodKey = period === "weekly" ? "week" : "month";

  return (
    <div className="space-y-6">
      <PageGuide
        description="주간·월간 품질 지표 추이와 고객사별 만족도를 확인하는 화면입니다."
        tips={[
          "월간 리포트를 PDF로 내보내 고객사 보고 자료로 활용하세요.",
          "Faithfulness와 Session Success 추이가 모두 오르고 있다면 개선 작업이 효과적이라는 신호입니다.",
          "고객사별 만족도 차이가 크다면 해당 기관의 지식베이스 품질을 집중 점검하세요.",
        ]}
      />
      <div className="flex items-center gap-3">
        <h2 className="text-text-primary font-semibold text-lg">성과 분석 리포트</h2>
        <span className="text-[10px] font-mono text-warning bg-warning/10 border border-warning/20 rounded px-2 py-0.5">
          목업 데이터
        </span>
      </div>

      {/* 기간 탭 */}
      <div className="flex gap-0 border-b border-bg-border">
        <button
          onClick={() => setPeriod("weekly")}
          className={`px-4 py-2 text-xs font-mono transition-colors ${
            period === "weekly"
              ? "border-b-2 border-accent text-accent font-semibold"
              : "text-text-muted hover:text-text-secondary"
          }`}
        >
          주간 리포트
        </button>
        <button
          onClick={() => setPeriod("monthly")}
          className={`px-4 py-2 text-xs font-mono transition-colors ${
            period === "monthly"
              ? "border-b-2 border-accent text-accent font-semibold"
              : "text-text-muted hover:text-text-secondary"
          }`}
        >
          월간 리포트
        </button>
      </div>

      {/* 품질 지표 추이 */}
      <Card>
        <CardHeader>
          <CardTitle tag="TREND">품질 지표 추이</CardTitle>
        </CardHeader>
        <div className="overflow-hidden">
          <Table>
            <Thead>
              <Th>{period === "weekly" ? "주차" : "월"}</Th>
              <Th>Faithfulness</Th>
              <Th>Session Success</Th>
            </Thead>
            <Tbody>
              {trends.map((row) => (
                <Tr key={(row as Record<string, unknown>)[periodKey] as string}>
                  <Td className="font-mono text-xs">{(row as Record<string, unknown>)[periodKey] as string}</Td>
                  <Td className="font-mono text-sm text-success">{row.faithfulness.toFixed(2)}</Td>
                  <Td className="font-mono text-sm text-accent">{row.sessionSuccess}%</Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        </div>
        <div className="px-4 py-3 border-t border-bg-border">
          <p className="text-[10px] text-text-muted">
            Faithfulness: 0.89 → 0.91 → 0.92 | Session Success: 87% → 89% → 91% (상승 추세)
          </p>
        </div>
      </Card>

      {/* 고객사별 만족도 */}
      <Card>
        <CardHeader>
          <CardTitle tag="SATISFACTION">고객사별 만족도</CardTitle>
        </CardHeader>
        <div className="overflow-hidden">
          <Table>
            <Thead>
              <Th>기관명</Th>
              <Th>만족도 점수</Th>
              <Th>응답 수</Th>
            </Thead>
            <Tbody>
              {MOCK_SATISFACTION.map((s) => (
                <Tr key={s.org}>
                  <Td className="text-sm">{s.org}</Td>
                  <Td>
                    <span className={`font-mono text-sm font-bold ${s.score >= 4.0 ? "text-success" : "text-warning"}`}>
                      {s.score.toFixed(1)} / 5.0
                    </span>
                  </Td>
                  <Td className="font-mono text-sm text-text-muted">{s.responses}건</Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        </div>
      </Card>

      {/* 다운로드 */}
      <Card>
        <CardHeader>
          <CardTitle tag="DOWNLOAD">리포트 다운로드</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4 flex gap-3">
          <Button disabled>PDF 리포트 생성</Button>
          <Button disabled variant="secondary">PPT 리포트 생성</Button>
        </div>
      </Card>
    </div>
  );
}
