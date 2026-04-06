"use client";

import Link from "next/link";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Table, Thead, Th, Tbody, Tr, Td } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { PageGuide } from "@/components/ui/PageGuide";

const MOCK_API_KEYS = [
  { id: "key_001", name: "홈페이지 챗봇",  key: "sk-gov-****-1234", createdAt: "2026-03-01", status: "active" },
  { id: "key_002", name: "모바일 앱 연동", key: "sk-gov-****-5678", createdAt: "2026-03-10", status: "active" },
];

export default function ApiKeysPage() {
  return (
    <div className="space-y-6">
      <PageGuide
        description="외부 서비스(홈페이지, 앱) 연동 키를 발급·관리하고 Webhook을 설정하는 화면입니다."
        tips={[
          "API 키가 노출됐다면 즉시 '폐기' 후 새 키를 발급하세요 — 기존 키는 즉시 무효화됩니다.",
          "Webhook을 Slack과 연동하면 이상 징후 감지 임계값 초과 시 자동으로 알림을 받을 수 있습니다.",
          "API 호출량이 예상보다 많다면 외부 서비스의 호출 로직에 버그가 있는지 확인하세요.",
        ]}
      />
      <div className="flex items-center gap-3">
        <h2 className="text-text-primary font-semibold text-lg">연동 API 관리</h2>
        <span className="text-[10px] font-mono text-warning bg-warning/10 border border-warning/20 rounded px-2 py-0.5">
          목업 데이터
        </span>
      </div>

      {/* 외부 연동 키 */}
      <Card>
        <CardHeader>
          <CardTitle tag="API KEYS">외부 연동 키</CardTitle>
        </CardHeader>
        <div className="overflow-hidden">
          <Table>
            <Thead>
              <Th>이름</Th>
              <Th>키</Th>
              <Th>생성일</Th>
              <Th>상태</Th>
              <Th></Th>
            </Thead>
            <Tbody>
              {MOCK_API_KEYS.map((k) => (
                <Tr key={k.id}>
                  <Td className="text-sm font-medium">{k.name}</Td>
                  <Td className="font-mono text-xs text-text-muted">{k.key}</Td>
                  <Td className="text-xs text-text-muted">{k.createdAt}</Td>
                  <Td>
                    <Badge variant={k.status === "active" ? "success" : "neutral"}>
                      {k.status === "active" ? "활성" : "비활성"}
                    </Badge>
                  </Td>
                  <Td>
                    <Button variant="secondary" size="sm" disabled>폐기</Button>
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        </div>
        <div className="px-4 pb-4 pt-3">
          <Button disabled>새 키 발급</Button>
        </div>
      </Card>

      {/* API 호출량 */}
      <Card>
        <CardHeader>
          <CardTitle tag="USAGE">API 호출량</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4">
          <div className="grid grid-cols-2 gap-3">
            <div className="bg-bg-prominent rounded-lg p-4">
              <p className="font-mono text-[10px] uppercase tracking-widest text-text-muted mb-1">오늘</p>
              <p className="font-mono text-[24px] font-bold text-text-primary">1,234건</p>
            </div>
            <div className="bg-bg-prominent rounded-lg p-4">
              <p className="font-mono text-[10px] uppercase tracking-widest text-text-muted mb-1">이번 달</p>
              <p className="font-mono text-[24px] font-bold text-text-primary">23,456건</p>
            </div>
          </div>
          <p className="text-[10px] text-text-muted mt-2">실시간 집계는 API 연동 후 활성화됩니다.</p>
        </div>
      </Card>

      {/* Webhook 설정 */}
      <Card>
        <CardHeader>
          <CardTitle tag="WEBHOOK">Webhook 설정</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4 space-y-3">
          {[
            { name: "Slack", desc: "이상 징후 알림을 Slack 채널로 전송" },
            { name: "PagerDuty", desc: "긴급 이벤트 발생 시 온콜 알림" },
          ].map((wh) => (
            <div
              key={wh.name}
              className="flex items-center justify-between p-3 bg-bg-elevated rounded-lg border border-bg-border"
            >
              <div>
                <p className="text-sm font-medium text-text-primary">{wh.name}</p>
                <p className="text-xs text-text-muted">{wh.desc}</p>
              </div>
              <div className="flex items-center gap-3">
                <Badge variant="neutral">미설정</Badge>
                <Button variant="secondary" size="sm" disabled>설정</Button>
              </div>
            </div>
          ))}
          <p className="text-xs text-text-muted">
            임계값 설정과 연동 →{" "}
            <Link href="/ops/anomaly" className="text-accent hover:underline">
              이상 징후 감지
            </Link>
          </p>
        </div>
      </Card>
    </div>
  );
}
