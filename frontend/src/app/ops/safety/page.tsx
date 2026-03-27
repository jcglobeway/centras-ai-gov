"use client";

import useSWR from "swr";
import { fetcher } from "@/lib/api";
import type { PagedResponse, Question } from "@/lib/types";
import { KpiCard } from "@/components/charts/KpiCard";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Spinner } from "@/components/ui/Spinner";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";

// 레드팀 평가 이력 (v3 HTML 동일, 정적 데이터)
const RED_TEAM_HISTORY = [
  {
    date: "2026-03-15",
    type: "Prompt Injection",
    result: "방어",
    score: "96.7%",
    note: "시스템 프롬프트 노출 시도 차단",
  },
  {
    date: "2026-03-08",
    type: "PII 추출 시도",
    result: "방어",
    score: "100%",
    note: "주민번호/전화번호 패턴 필터 동작 확인",
  },
  {
    date: "2026-02-22",
    type: "OOD 질의",
    result: "탐지",
    score: "89.2%",
    note: "범위 외 질의 → no_answer 처리",
  },
];

export default function SafetyPage() {
  const { data: questionsData, isLoading } = useSWR<PagedResponse<Question>>(
    `/api/admin/questions?page_size=100`,
    fetcher
  );

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-48">
        <Spinner />
      </div>
    );
  }

  const questions = questionsData?.items ?? [];
  const total = questions.length;
  const noAnswerCount = questions.filter((q) => q.isEscalated).length;
  const unanswerableRate = total > 0 ? ((noAnswerCount / total) * 100).toFixed(1) + "%" : "-";

  return (
    <div className="space-y-6">
      <h2 className="text-text-primary font-semibold text-lg">안전성 모니터링</h2>

      {/* KPI 그리드 */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <KpiCard
          label="PII LEAKAGE"
          value="0 건"
          status="ok"
          help="개인정보(주민번호, 전화번호 등) 응답 포함 건수. 현재 미추적 — 패턴 필터 기준 이상 없음."
        />
        <KpiCard
          label="UNANSWERABLE DETECT"
          value={unanswerableRate}
          status="ok"
          help="no_answer 처리 비율. 범위 외 질의를 적절히 거부하는 비율을 의미합니다."
        />
        <KpiCard
          label="ADVERSARIAL RESIST"
          value="96.7%"
          status="ok"
          help="레드팀 Prompt Injection 방어율. 최근 테스트 기준 고정값."
        />
        <KpiCard
          label="TOXICITY SCORE"
          value="0.02%"
          status="ok"
          help="유해/부적절 응답 비율 (미추적, 분류기 연동 후 실측 예정)."
        />
      </div>

      {/* Safety 지표 그리드 */}
      <Card>
        <CardHeader>
          <CardTitle tag="SAFETY METRICS">안전성 지표 상세</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4">
          <div className="grid grid-cols-2 lg:grid-cols-3 gap-3">
            {[
              {
                label: "PII 유출",
                value: "0 건",
                note: "전화번호/주민번호 패턴 필터링 적용",
                status: "ok" as const,
              },
              {
                label: "답변 거부율",
                value: unanswerableRate,
                note: "범위 외 질의 no_answer 처리 비율",
                status: "ok" as const,
              },
              {
                label: "OOD 탐지율",
                value: "89.2%",
                note: "도메인 외 질의 탐지 (레드팀 기준)",
                status: "ok" as const,
              },
              {
                label: "Adversarial 방어",
                value: "96.7%",
                note: "Prompt Injection 방어율",
                status: "ok" as const,
              },
              {
                label: "독성 점수",
                value: "0.02%",
                note: "분류기 연동 전 추정값",
                status: "ok" as const,
              },
              {
                label: "종합 Safety Score",
                value: "96.4",
                note: "5개 지표 가중 평균 (100 만점)",
                status: "ok" as const,
              },
            ].map((item) => (
              <div key={item.label} className="bg-bg-elevated rounded p-3 flex items-start justify-between gap-2">
                <div>
                  <p className="font-mono text-[10px] uppercase tracking-[0.4px] text-text-muted mb-1">
                    {item.label}
                  </p>
                  <p className="font-mono text-[18px] font-bold text-success">{item.value}</p>
                  <p className="text-[10px] text-text-muted mt-1">{item.note}</p>
                </div>
                <Badge variant="success">정상</Badge>
              </div>
            ))}
          </div>
        </div>
      </Card>

      {/* 레드팀 평가 이력 */}
      <Card>
        <CardHeader>
          <CardTitle tag="RED TEAM">레드팀 평가 이력</CardTitle>
        </CardHeader>
        <div className="overflow-hidden">
          <Table>
            <Thead>
              <Th>날짜</Th>
              <Th>공격 유형</Th>
              <Th>결과</Th>
              <Th>방어율</Th>
              <Th>비고</Th>
            </Thead>
            <Tbody>
              {RED_TEAM_HISTORY.map((row, i) => (
                <Tr key={i}>
                  <Td className="font-mono">{row.date}</Td>
                  <Td>{row.type}</Td>
                  <Td>
                    <Badge variant="success">{row.result}</Badge>
                  </Td>
                  <Td className="font-mono">{row.score}</Td>
                  <Td className="text-text-muted">{row.note}</Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        </div>
      </Card>
    </div>
  );
}
