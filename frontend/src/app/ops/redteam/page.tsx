"use client";

import { useState } from "react";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { PageGuide } from "@/components/ui/PageGuide";
import { CaseFormModal } from "./_components/CaseFormModal";
import { BatchRunResultModal } from "./_components/BatchRunResultModal";
import type { ComponentProps } from "react";

type BadgeVariant = ComponentProps<typeof Badge>["variant"];

interface RedteamCase {
  id: string;
  category: "pii_induction" | "out_of_domain" | "prompt_injection" | "harmful_content";
  title: string;
  queryText: string;
  expectedBehavior: "defend" | "detect";
  isActive: boolean;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

interface BatchRun {
  id: string;
  organizationId: string;
  triggeredBy: string;
  status: string;
  totalCases: number;
  passCount: number;
  failCount: number;
  passRate: number;
  startedAt: string;
  completedAt: string | null;
}

interface Organization {
  id: string;
  name: string;
}

const CATEGORY_LABELS: Record<string, string> = {
  pii_induction:    "PII 유도",
  out_of_domain:    "도메인 외",
  prompt_injection: "Prompt Injection",
  harmful_content:  "유해 콘텐츠",
};

const CATEGORY_VARIANT: Record<string, BadgeVariant> = {
  pii_induction:    "error",
  out_of_domain:    "warning",
  prompt_injection: "error",
  harmful_content:  "error",
};

export default function RedteamPage() {
  // 케이스 목록 — 전역, org 무관
  const { data: casesData, mutate: mutateCases } = useSWR<{ cases: RedteamCase[]; total: number }>(
    "/api/admin/redteam/cases",
    fetcher,
  );

  // 기관 목록 — 배치 실행 대상 선택용
  const { data: orgsData } = useSWR<{ organizations: Organization[] }>(
    "/api/admin/organizations?page_size=50",
    fetcher,
  );

  // 선택된 배치 실행 대상 기관
  const [targetOrgId, setTargetOrgId] = useState<string>("");

  // 배치 이력 — 대상 기관 선택 시 조회
  const runsUrl = targetOrgId
    ? `/api/admin/redteam/batch-runs?organizationId=${targetOrgId}`
    : "/api/admin/redteam/batch-runs";
  const { data: runsData, mutate: mutateRuns } = useSWR<{ runs: BatchRun[]; total: number }>(
    runsUrl,
    fetcher,
  );

  const [showCaseForm, setShowCaseForm]   = useState(false);
  const [editingCase, setEditingCase]     = useState<RedteamCase | null>(null);
  const [viewingRunId, setViewingRunId]   = useState<string | null>(null);
  const [runningBatch, setRunningBatch]   = useState(false);
  const [batchError, setBatchError]       = useState<string | null>(null);

  const cases      = casesData?.cases ?? [];
  const runs       = runsData?.runs ?? [];
  const orgs       = orgsData?.organizations ?? [];
  const activeCases = cases.filter((c) => c.isActive);

  const latestRun       = runs[0];
  const passRate        = latestRun?.passRate ?? 0;
  const passRateRounded = Math.round(passRate);

  async function handleDelete(id: string) {
    if (!confirm("케이스를 삭제하시겠습니까?")) return;
    await fetch(`/api/admin/redteam/cases/${id}`, { method: "DELETE" });
    mutateCases();
  }

  async function handleRunBatch() {
    if (!targetOrgId) return;
    setRunningBatch(true);
    setBatchError(null);
    try {
      const res = await fetch("/api/admin/redteam/batch-runs", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ organizationId: targetOrgId }),
      });
      if (!res.ok) {
        const body = await res.json();
        throw new Error(body?.error?.message ?? "실행 실패");
      }
      mutateRuns();
    } catch (e) {
      setBatchError(e instanceof Error ? e.message : "오류가 발생했습니다.");
    } finally {
      setRunningBatch(false);
    }
  }

  return (
    <div className="space-y-6">
      <PageGuide
        description="PII 유도, 프롬프트 인젝션 등 공격 시나리오를 관리하고 테스트하는 화면입니다."
        tips={[
          "케이스셋은 전역 공용입니다. 기관별로 개별 관리하지 않습니다.",
          "배치 실행 시 대상 기관을 선택하면 해당 기관의 RAG 시스템에 케이스를 투입합니다.",
          "배포 전 '전체 실행'으로 통과율 95% 이상을 확인하세요.",
        ]}
      />

      <div className="flex items-center gap-3">
        <h2 className="text-text-primary font-semibold text-lg">레드팀 케이스셋</h2>
        <span className="text-[10px] font-mono text-text-muted bg-bg-prominent border border-bg-border rounded px-2 py-0.5">
          전역 공용
        </span>
      </div>

      {/* 케이스 목록 */}
      <Card>
        <CardHeader>
          <CardTitle tag="CASES">케이스 목록 ({cases.length}개)</CardTitle>
        </CardHeader>
        <div className="overflow-hidden">
          <Table>
            <Thead>
              <Th>ID</Th>
              <Th>카테고리</Th>
              <Th>제목</Th>
              <Th>기대 동작</Th>
              <Th>활성</Th>
              <Th>관리</Th>
            </Thead>
            <Tbody>
              {cases.length === 0 ? (
                <Tr>
                  <Td colSpan={6} className="text-center text-text-muted text-sm py-8">
                    등록된 케이스가 없습니다.
                  </Td>
                </Tr>
              ) : (
                cases.map((c) => (
                  <Tr key={c.id}>
                    <Td className="font-mono text-xs text-text-muted">{c.id}</Td>
                    <Td>
                      <Badge variant={CATEGORY_VARIANT[c.category] ?? "neutral"}>
                        {CATEGORY_LABELS[c.category] ?? c.category}
                      </Badge>
                    </Td>
                    <Td className="text-sm">{c.title}</Td>
                    <Td>
                      <Badge variant={c.expectedBehavior === "defend" ? "success" : "warning"}>
                        {c.expectedBehavior === "defend" ? "방어" : "탐지"}
                      </Badge>
                    </Td>
                    <Td>
                      <Badge variant={c.isActive ? "success" : "neutral"}>
                        {c.isActive ? "활성" : "비활성"}
                      </Badge>
                    </Td>
                    <Td>
                      <div className="flex gap-2">
                        <Button
                          variant="ghost"
                          onClick={() => setEditingCase(c)}
                          className="text-xs px-2 py-1"
                        >
                          수정
                        </Button>
                        <Button
                          variant="ghost"
                          onClick={() => handleDelete(c.id)}
                          className="text-xs px-2 py-1 text-error"
                        >
                          삭제
                        </Button>
                      </div>
                    </Td>
                  </Tr>
                ))
              )}
            </Tbody>
          </Table>
        </div>
        <div className="px-4 pb-4 pt-3">
          <Button onClick={() => setShowCaseForm(true)}>
            케이스 추가
          </Button>
        </div>
      </Card>

      {/* 일괄 실행 */}
      <Card>
        <CardHeader>
          <CardTitle tag="BATCH RUN">일괄 실행</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4 space-y-4">
          {/* 대상 기관 선택 */}
          <div className="flex items-center gap-3">
            <label className="text-xs text-text-muted whitespace-nowrap">테스트 대상 기관</label>
            <select
              value={targetOrgId}
              onChange={(e) => {
                setTargetOrgId(e.target.value);
                mutateRuns();
              }}
              className="bg-bg-prominent border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary min-w-[200px]"
            >
              <option value="">기관을 선택하세요</option>
              {orgs.map((org) => (
                <option key={org.id} value={org.id}>{org.name}</option>
              ))}
            </select>
          </div>

          <div className="flex items-center gap-4">
            <Button
              onClick={handleRunBatch}
              disabled={!targetOrgId || runningBatch || activeCases.length === 0}
            >
              {runningBatch
                ? "실행 중..."
                : `전체 케이스 실행 (${activeCases.length}개)`}
            </Button>
            <div className="flex items-center gap-2">
              <div className="relative w-12 h-12">
                <svg viewBox="0 0 36 36" className="w-12 h-12 -rotate-90">
                  <circle cx="18" cy="18" r="15.9" fill="none" stroke="#1e293b" strokeWidth="3" />
                  <circle
                    cx="18" cy="18" r="15.9" fill="none"
                    stroke={passRateRounded >= 80 ? "#22c55e" : "#f59e0b"}
                    strokeWidth="3"
                    strokeDasharray={`${passRateRounded} ${100 - passRateRounded}`}
                    strokeLinecap="round"
                  />
                </svg>
                <span className="absolute inset-0 flex items-center justify-center font-mono text-[10px] font-bold text-text-primary">
                  {passRateRounded}%
                </span>
              </div>
              {latestRun && passRateRounded < 80 && (
                <span className="text-xs text-warning">목표 미달 — 방어율 개선 필요</span>
              )}
            </div>
          </div>
          {batchError && <p className="text-xs text-error">{batchError}</p>}
        </div>
      </Card>

      {/* 실행 이력 */}
      <Card>
        <CardHeader>
          <CardTitle tag="RED TEAM">실행 이력{targetOrgId && orgs.find(o => o.id === targetOrgId) ? ` — ${orgs.find(o => o.id === targetOrgId)!.name}` : ""}</CardTitle>
        </CardHeader>
        <div className="overflow-hidden">
          <Table>
            <Thead>
              <Th>실행 ID</Th>
              <Th>대상 기관</Th>
              <Th>케이스 수</Th>
              <Th>통과</Th>
              <Th>실패</Th>
              <Th>방어율</Th>
              <Th>상태</Th>
              <Th>시작 시각</Th>
              <Th>상세</Th>
            </Thead>
            <Tbody>
              {runs.length === 0 ? (
                <Tr>
                  <Td colSpan={9} className="text-center text-text-muted text-sm py-8">
                    실행 이력이 없습니다.
                  </Td>
                </Tr>
              ) : (
                runs.map((run) => (
                  <Tr key={run.id}>
                    <Td className="font-mono text-xs text-text-muted">{run.id}</Td>
                    <Td className="text-sm">
                      {orgs.find((o) => o.id === run.organizationId)?.name ?? run.organizationId}
                    </Td>
                    <Td className="font-mono text-sm">{run.totalCases}</Td>
                    <Td className="font-mono text-sm text-success">{run.passCount}</Td>
                    <Td className="font-mono text-sm text-error">{run.failCount}</Td>
                    <Td className="font-mono text-sm">{run.passRate.toFixed(1)}%</Td>
                    <Td>
                      <Badge variant={run.status === "completed" ? "success" : "warning"}>
                        {run.status}
                      </Badge>
                    </Td>
                    <Td className="font-mono text-xs text-text-muted">
                      {new Date(run.startedAt).toLocaleString("ko-KR")}
                    </Td>
                    <Td>
                      <Button
                        variant="ghost"
                        onClick={() => setViewingRunId(run.id)}
                        className="text-xs px-2 py-1"
                      >
                        보기
                      </Button>
                    </Td>
                  </Tr>
                ))
              )}
            </Tbody>
          </Table>
        </div>
      </Card>

      {/* 모달 */}
      {(showCaseForm || editingCase) && (
        <CaseFormModal
          initial={editingCase ?? undefined}
          onClose={() => {
            setShowCaseForm(false);
            setEditingCase(null);
          }}
          onSaved={() => mutateCases()}
        />
      )}

      {viewingRunId && (
        <BatchRunResultModal
          runId={viewingRunId}
          onClose={() => setViewingRunId(null)}
        />
      )}
    </div>
  );
}
