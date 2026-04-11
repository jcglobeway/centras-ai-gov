"use client";

import { useState } from "react";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";
import { Spinner } from "@/components/ui/Spinner";
import { PageGuide } from "@/components/ui/PageGuide";
import { useFilter } from "@/lib/filter-context";
import type { ComponentProps } from "react";

type BadgeVariant = ComponentProps<typeof Badge>["variant"];

interface AuditLogItem {
  id: string;
  actorUserId: string | null;
  actorRoleCode: string | null;
  organizationId: string | null;
  actionCode: string;
  resourceType: string | null;
  resourceId: string | null;
  resultCode: string;
  createdAt: string;
}

interface AuditLogListResponse {
  items: AuditLogItem[];
  total: number;
}

const ACTION_CODE_LABEL: Record<string, string> = {
  ADMIN_LOGIN:         "로그인",
  ADMIN_LOGOUT:        "로그아웃",
  QA_REVIEW_UPDATE:    "QA 리뷰 업데이트",
  INGESTION_JOB_RUN:   "인덱싱 잡 실행",
  INGESTION_JOB_CANCEL:"인덱싱 잡 취소",
  RESOURCE_ACCESS:     "리소스 조회",
  PII_DETECTED:        "PII 감지",
  BLOCKLIST_HIT:       "금칙어 차단",
};

const ACTION_CODE_OPTIONS = [
  { value: "", label: "전체" },
  { value: "ADMIN_LOGIN", label: "로그인" },
  { value: "ADMIN_LOGOUT", label: "로그아웃" },
  { value: "QA_REVIEW_UPDATE", label: "QA 리뷰 업데이트" },
  { value: "INGESTION_JOB_RUN", label: "인덱싱 잡 실행" },
  { value: "INGESTION_JOB_CANCEL", label: "인덱싱 잡 취소" },
  { value: "PII_DETECTED", label: "PII 감지" },
  { value: "BLOCKLIST_HIT", label: "금칙어 차단" },
];

function resultBadgeVariant(resultCode: string): BadgeVariant {
  if (resultCode === "success") return "success";
  if (resultCode === "masked") return "warning";
  if (resultCode === "failure") return "error";
  return "neutral";
}

function buildAuditLogsQuery(params: {
  from?: string;
  to?: string;
  actionCode?: string;
  pageSize?: number;
}) {
  const p = new URLSearchParams();
  if (params.from) p.set("from", params.from);
  if (params.to) p.set("to", params.to);
  if (params.actionCode) p.set("action_code", params.actionCode);
  p.set("page_size", String(params.pageSize ?? 20));
  return `/api/admin/audit-logs?${p.toString()}`;
}

function buildExportUrl(params: {
  from?: string;
  to?: string;
  actionCode?: string;
}) {
  const p = new URLSearchParams();
  if (params.from) p.set("from", params.from);
  if (params.to) p.set("to", params.to);
  if (params.actionCode) p.set("action_code", params.actionCode);
  return `/api/admin/audit-logs/export.csv?${p.toString()}`;
}

export default function AuditPage() {
  const { from, to } = useFilter();
  const [actionCode, setActionCode] = useState("");

  const auditLogsKey = buildAuditLogsQuery({ from, to, actionCode, pageSize: 20 });
  const { data, isLoading } = useSWR<AuditLogListResponse>(auditLogsKey, fetcher);

  const { data: piiData, isLoading: piiLoading } = useSWR<AuditLogListResponse>(
    `/api/admin/audit-logs?action_code=PII_DETECTED&page_size=10`,
    fetcher,
  );

  const { data: blocklistData, isLoading: blocklistLoading } = useSWR<AuditLogListResponse>(
    `/api/admin/audit-logs?action_code=BLOCKLIST_HIT&page_size=10`,
    fetcher,
  );

  return (
    <div className="space-y-6">
      <PageGuide
        description="PII 감지 이벤트, 관리자 접근 이력, 금칙어 차단 기록을 확인하는 화면입니다."
        tips={[
          "PII 감지 이벤트가 발생하면 데이터 전처리 규칙을 즉시 점검하세요.",
          "관리자 접근 이력에서 비정상적인 시간대의 접근이 있으면 계정을 즉시 점검하세요.",
          "금칙어 차단 건수가 급증하면 특정 질의 패턴의 공격 가능성을 의심하세요.",
        ]}
      />
      <h2 className="text-text-primary font-semibold text-lg">보안 감사 로그</h2>

      {/* 필터 영역 */}
      <Card>
        <div className="px-4 py-3 flex flex-wrap items-end gap-3">
          <div className="flex flex-col gap-1">
            <label className="text-[10px] text-text-muted uppercase tracking-widest">행동 코드</label>
            <select
              value={actionCode}
              onChange={(e) => setActionCode(e.target.value)}
              className="bg-bg-prominent border border-bg-border rounded px-2 py-1 text-xs text-text-primary focus:outline-none focus:border-accent"
            >
              {ACTION_CODE_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
          </div>
          <div className="ml-auto flex items-end">
            <a
              href={buildExportUrl({ from, to, actionCode })}
              download="audit-logs.csv"
              className="px-3 py-1.5 rounded bg-accent text-white text-xs font-medium hover:bg-accent/80 transition-colors"
            >
              CSV 내보내기
            </a>
          </div>
        </div>
      </Card>

      {/* PII 감지 이벤트 — 실데이터 연동 */}
      <Card>
        <CardHeader>
          <CardTitle tag="PII">PII 감지 이벤트</CardTitle>
        </CardHeader>
        {piiLoading ? (
          <div className="flex items-center justify-center h-24">
            <Spinner />
          </div>
        ) : (
          <div className="overflow-hidden">
            <Table>
              <Thead>
                <Th>세션 ID (resourceId)</Th>
                <Th>기관</Th>
                <Th>처리 결과</Th>
                <Th>감지 시각</Th>
              </Thead>
              <Tbody>
                {(piiData?.items ?? []).map((e) => (
                  <Tr key={e.id}>
                    <Td className="font-mono text-xs text-text-muted">{e.resourceId ?? "-"}</Td>
                    <Td className="text-xs text-text-secondary">{e.organizationId ?? "-"}</Td>
                    <Td>
                      <Badge variant={e.resultCode === "masked" ? "success" : "error"}>
                        {e.resultCode === "masked" ? "마스킹 완료" : e.resultCode}
                      </Badge>
                    </Td>
                    <Td className="text-xs text-text-muted">
                      {new Date(e.createdAt).toLocaleString("ko-KR")}
                    </Td>
                  </Tr>
                ))}
                {(piiData?.items ?? []).length === 0 && !piiLoading && (
                  <Tr>
                    <Td colSpan={4} className="text-center text-text-muted text-sm py-8">
                      PII 감지 이벤트가 없습니다.
                    </Td>
                  </Tr>
                )}
              </Tbody>
            </Table>
          </div>
        )}
      </Card>

      {/* 관리자 접근 이력 — 실데이터 + 필터 적용 */}
      <Card>
        <CardHeader>
          <CardTitle tag="ACCESS">관리자 접근 이력</CardTitle>
        </CardHeader>
        {isLoading ? (
          <div className="flex items-center justify-center h-24">
            <Spinner />
          </div>
        ) : (
          <div className="overflow-hidden">
            <Table>
              <Thead>
                <Th>사용자 ID</Th>
                <Th>역할</Th>
                <Th>행동</Th>
                <Th>대상</Th>
                <Th>결과</Th>
                <Th>시각</Th>
              </Thead>
              <Tbody>
                {(data?.items ?? []).map((log) => (
                  <Tr key={log.id}>
                    <Td className="font-mono text-xs text-text-muted">{log.actorUserId ?? "-"}</Td>
                    <Td className="text-xs text-text-secondary">{log.actorRoleCode ?? "-"}</Td>
                    <Td>
                      <Badge variant="neutral">
                        {ACTION_CODE_LABEL[log.actionCode] ?? log.actionCode}
                      </Badge>
                    </Td>
                    <Td className="font-mono text-xs text-text-muted">
                      {log.resourceType ?? "-"}
                      {log.resourceId ? ` / ${log.resourceId}` : ""}
                    </Td>
                    <Td>
                      <Badge variant={resultBadgeVariant(log.resultCode)}>
                        {log.resultCode === "success" ? "성공" : log.resultCode === "masked" ? "마스킹" : log.resultCode}
                      </Badge>
                    </Td>
                    <Td className="text-xs text-text-muted">
                      {new Date(log.createdAt).toLocaleString("ko-KR")}
                    </Td>
                  </Tr>
                ))}
                {(data?.items ?? []).length === 0 && !isLoading && (
                  <Tr>
                    <Td colSpan={6} className="text-center text-text-muted text-sm py-8">
                      감사 로그가 없습니다.
                    </Td>
                  </Tr>
                )}
              </Tbody>
            </Table>
          </div>
        )}
      </Card>

      {/* 금칙어 차단 로그 — 실데이터 연동 */}
      <Card>
        <CardHeader>
          <CardTitle tag="BLOCKLIST">금칙어 차단 로그</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4">
          <div className="bg-bg-prominent rounded-lg p-4 flex items-center gap-4 mb-4">
            <div className="text-center">
              {blocklistLoading ? (
                <Spinner />
              ) : (
                <>
                  <p className="font-mono text-[28px] font-bold text-warning">
                    {blocklistData?.total ?? 0}
                  </p>
                  <p className="text-[10px] font-mono uppercase tracking-widest text-text-muted mt-1">
                    이번 달 차단
                  </p>
                </>
              )}
            </div>
            <div className="h-10 w-px bg-bg-border" />
            <div className="space-y-1">
              <p className="text-xs text-text-secondary">금칙어 필터 동작 현황</p>
              <p className="text-[10px] text-text-muted">최근 10건의 차단 이력이 표시됩니다.</p>
            </div>
          </div>
          {!blocklistLoading && (blocklistData?.items ?? []).length > 0 && (
            <div className="overflow-hidden">
              <Table>
                <Thead>
                  <Th>리소스 ID</Th>
                  <Th>기관</Th>
                  <Th>결과</Th>
                  <Th>시각</Th>
                </Thead>
                <Tbody>
                  {(blocklistData?.items ?? []).map((log) => (
                    <Tr key={log.id}>
                      <Td className="font-mono text-xs text-text-muted">{log.resourceId ?? "-"}</Td>
                      <Td className="text-xs text-text-secondary">{log.organizationId ?? "-"}</Td>
                      <Td>
                        <Badge variant="error">{log.resultCode}</Badge>
                      </Td>
                      <Td className="text-xs text-text-muted">
                        {new Date(log.createdAt).toLocaleString("ko-KR")}
                      </Td>
                    </Tr>
                  ))}
                </Tbody>
              </Table>
            </div>
          )}
        </div>
      </Card>
    </div>
  );
}
