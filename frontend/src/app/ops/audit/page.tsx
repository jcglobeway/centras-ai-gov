"use client";

import useSWR from "swr";
import { fetcher } from "@/lib/api";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";
import { Spinner } from "@/components/ui/Spinner";
import { PageGuide } from "@/components/ui/PageGuide";
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

const MOCK_PII_EVENTS = [
  { id: "pii_001", type: "전화번호", masked: true, sessionId: "sess_abc1", detectedAt: "2026-03-28 09:14" },
  { id: "pii_002", type: "주민번호", masked: true, sessionId: "sess_abc2", detectedAt: "2026-03-27 15:32" },
];

const ACTION_CODE_LABEL: Record<string, string> = {
  ADMIN_LOGIN:         "로그인",
  ADMIN_LOGOUT:        "로그아웃",
  QA_REVIEW_UPDATE:    "QA 리뷰 업데이트",
  INGESTION_JOB_RUN:   "인덱싱 잡 실행",
  INGESTION_JOB_CANCEL:"인덱싱 잡 취소",
  RESOURCE_ACCESS:     "리소스 조회",
};

function resultBadgeVariant(resultCode: string): BadgeVariant {
  if (resultCode === "success") return "success";
  if (resultCode === "failure") return "error";
  return "neutral";
}

export default function AuditPage() {
  const { data, isLoading } = useSWR<AuditLogListResponse>(
    `/api/admin/audit-logs?page_size=20`,
    fetcher,
  );

  return (
    <div className="space-y-6">
      <PageGuide
        description="PII 감지 이벤트, 관리자 접근 이력, 금칙어 차단 기록을 확인하는 화면입니다."
        tips={[
          "PII 감지 이벤트에서 마스킹이 '미처리'인 건이 있으면 즉시 데이터 전처리 규칙을 점검하세요.",
          "관리자 접근 이력에서 비정상적인 시간대의 접근이 있으면 계정을 즉시 점검하세요.",
          "금칙어 차단 건수가 급증하면 특정 질의 패턴의 공격 가능성을 의심하세요.",
        ]}
      />
      <h2 className="text-text-primary font-semibold text-lg">보안 감사 로그</h2>

      {/* PII 감지 이벤트 — 샘플 데이터 (PII 분류기 미구현) */}
      <Card>
        <CardHeader>
          <CardTitle tag="PII">PII 감지 이벤트</CardTitle>
        </CardHeader>
        <div className="px-4 pb-2">
          <p className="text-[10px] text-text-muted mb-2">※ 샘플 데이터 — PII 감지 분류기 구현 후 실데이터로 교체 예정</p>
        </div>
        <div className="overflow-hidden">
          <Table>
            <Thead>
              <Th>이벤트 ID</Th>
              <Th>감지 유형</Th>
              <Th>마스킹 여부</Th>
              <Th>세션 ID</Th>
              <Th>감지 시각</Th>
            </Thead>
            <Tbody>
              {MOCK_PII_EVENTS.map((e) => (
                <Tr key={e.id}>
                  <Td className="font-mono text-xs text-text-muted">{e.id}</Td>
                  <Td><Badge variant="error">{e.type}</Badge></Td>
                  <Td>
                    <Badge variant={e.masked ? "success" : "error"}>
                      {e.masked ? "마스킹 완료" : "미처리"}
                    </Badge>
                  </Td>
                  <Td className="font-mono text-xs text-text-muted">{e.sessionId}</Td>
                  <Td className="text-xs text-text-muted">{e.detectedAt}</Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        </div>
      </Card>

      {/* 관리자 접근 이력 — 실데이터 */}
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
                        {log.resultCode === "success" ? "성공" : log.resultCode}
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

      {/* 금칙어 차단 로그 요약 — 샘플 데이터 (사전 관리 미구현) */}
      <Card>
        <CardHeader>
          <CardTitle tag="BLOCKLIST">금칙어 차단 로그</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4">
          <p className="text-[10px] text-text-muted mb-3">※ 샘플 데이터 — 금칙어 사전 관리 API 구현 후 실데이터로 교체 예정</p>
          <div className="bg-bg-prominent rounded-lg p-4 flex items-center gap-4">
            <div className="text-center">
              <p className="font-mono text-[28px] font-bold text-warning">4</p>
              <p className="text-[10px] font-mono uppercase tracking-widest text-text-muted mt-1">
                이번 달 차단
              </p>
            </div>
            <div className="h-10 w-px bg-bg-border" />
            <div className="space-y-1">
              <p className="text-xs text-text-secondary">금칙어 필터 정상 동작 중</p>
              <p className="text-[10px] text-text-muted">차단 상세 목록은 금칙어 사전 API 연동 후 활성화됩니다.</p>
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
}
