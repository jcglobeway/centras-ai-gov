"use client";

import { useState, useEffect } from "react";
import useSWR, { mutate } from "swr";
import { fetcher } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { PageGuide } from "@/components/ui/PageGuide";
import { Spinner } from "@/components/ui/Spinner";
import type { ModelServingStatus, OrgRagConfig, Organization, PagedResponse } from "@/lib/types";
import clsx from "clsx";

const statusBadge = (status: string) => {
  const styles: Record<string, string> = {
    ok:       'bg-success/10 text-success border border-success/30',
    warn:     'bg-warning/10 text-warning border border-warning/30',
    critical: 'bg-error/10 text-error border border-error/30',
    disabled: 'bg-bg-prominent text-text-muted border border-bg-border',
    error:    'bg-error/10 text-error border border-error/30',
  };
  const labels: Record<string, string> = {
    ok: '연결됨', warn: '미연결', critical: '오류', disabled: '비활성', error: '오류',
  };
  return (
    <span className={`text-[10px] font-mono px-1.5 py-0.5 rounded-full ${styles[status] ?? styles.warn}`}>
      {labels[status] ?? status}
    </span>
  );
};

export default function ModelServingPage() {
  const { session } = useAuth();

  // LLM API 연동 상태
  const { data, isLoading } = useSWR<ModelServingStatus>(
    "/api/admin/model-serving/status",
    fetcher,
  );

  // 기관 목록
  const { data: orgsData } = useSWR<PagedResponse<Organization>>(
    "/api/admin/organizations",
    fetcher,
  );
  const orgList = orgsData?.items ?? [];
  const defaultOrgId = session?.organizationId ?? orgList[0]?.organizationId ?? "";
  const [selectedOrgId, setSelectedOrgId] = useState<string>("");

  useEffect(() => {
    if (!selectedOrgId && defaultOrgId) {
      setSelectedOrgId(defaultOrgId);
    }
  }, [defaultOrgId, selectedOrgId]);

  // RAG Config (LLM 파라미터 포함)
  const configKey = selectedOrgId ? `/api/admin/organizations/${selectedOrgId}/rag-config` : null;
  const { data: config, isLoading: configLoading } = useSWR<OrgRagConfig>(configKey, fetcher);

  const [llmModel, setLlmModel] = useState("");
  const [llmTemperature, setLlmTemperature] = useState(0.3);
  const [llmMaxTokens, setLlmMaxTokens] = useState(500);
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  useEffect(() => {
    if (config) {
      setLlmModel(config.llmModel ?? "");
      setLlmTemperature(Number(config.llmTemperature));
      setLlmMaxTokens(config.llmMaxTokens);
    }
  }, [config]);

  function showToast(msg: string) {
    setToast(msg);
    setTimeout(() => setToast(null), 3000);
  }

  async function handleSave() {
    if (!selectedOrgId || !config) return;
    setSaving(true);
    try {
      const sessionId = localStorage.getItem("sessionId");
      const res = await fetch(`/api/admin/organizations/${selectedOrgId}/rag-config`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          ...(sessionId ? { "X-Admin-Session-Id": sessionId } : {}),
        },
        body: JSON.stringify({
          systemPrompt: config.systemPrompt,
          tone: config.tone,
          topK: config.topK,
          similarityThreshold: config.similarityThreshold,
          rerankerEnabled: config.rerankerEnabled,
          llmModel,
          llmTemperature,
          llmMaxTokens,
          changeNote: "LLM 파라미터 업데이트",
        }),
      });
      if (!res.ok) throw new Error("저장 실패");
      mutate(configKey);
      showToast("LLM 파라미터가 저장되었습니다.");
    } catch {
      showToast("저장 중 오류가 발생했습니다.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="space-y-6">
      {toast && (
        <div className="fixed top-4 right-4 z-50 bg-success/90 text-white text-sm px-4 py-2 rounded-lg shadow-lg">
          {toast}
        </div>
      )}

      <PageGuide
        description="LLM 모델과 생성 파라미터를 기관별로 설정하는 화면입니다."
        tips={[
          "모델명은 Ollama에 pull된 모델 이름과 정확히 일치해야 합니다. (예: qwen2.5:7b, llama3.2)",
          "Temperature가 높을수록 창의적이지만 환각이 증가합니다. 공공기관 챗봇은 0.1~0.4를 권장합니다.",
          "Max Tokens를 늘리면 긴 답변이 가능하지만 응답 레이턴시가 함께 증가합니다.",
        ]}
      />

      <div className="flex items-center justify-between">
        <h2 className="text-text-primary font-semibold text-lg">모델 설정</h2>
      </div>

      {/* LLM API 연동 상태 */}
      <Card>
        <CardHeader>
          <CardTitle>LLM API 연동 상태</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4">
          {isLoading ? (
            <div className="flex justify-center py-8"><Spinner /></div>
          ) : (
            <>
              <div className="mb-3 flex items-center gap-2">
                <span className="text-xs text-text-muted">Orchestrator:</span>
                {statusBadge(data?.orchestratorStatus ?? 'error')}
              </div>
              {!data?.models?.length ? (
                <p className="text-xs text-text-muted py-2">연결된 모델이 없거나 rag-orchestrator가 실행되지 않았습니다.</p>
              ) : (
                <>
                  <p className="text-[10px] font-mono uppercase tracking-wider text-text-muted mb-2">사용 가능한 모델 (Ollama)</p>
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-bg-border">
                        <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted">모델명</th>
                        <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted">상태</th>
                        <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted">계열</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-bg-border">
                      {data.models.map((m) => (
                        <tr key={m.name}>
                          <td className="py-2.5 text-sm font-medium text-text-primary font-mono">{m.name}</td>
                          <td className="py-2.5">{statusBadge(m.status)}</td>
                          <td className="py-2.5 font-mono text-xs text-text-muted">{m.version ?? '-'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </>
              )}
            </>
          )}
        </div>
      </Card>

      {/* LLM 파라미터 설정 */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between w-full">
            <CardTitle>LLM 파라미터 설정</CardTitle>
            {orgList.length > 1 && (
              <select
                value={selectedOrgId}
                onChange={(e) => setSelectedOrgId(e.target.value)}
                className="bg-bg-surface border border-bg-border rounded-lg px-3 py-1.5 text-sm text-text-primary focus:outline-none focus:border-accent"
              >
                {orgList.map((o) => (
                  <option key={o.organizationId} value={o.organizationId}>
                    {o.name}
                  </option>
                ))}
              </select>
            )}
          </div>
        </CardHeader>
        <div className="px-4 pb-4">
          {configLoading ? (
            <div className="flex justify-center py-8"><Spinner /></div>
          ) : (
            <div className="space-y-6">
              {/* LLM 모델 */}
              <div>
                <label className="block text-xs text-text-secondary mb-1">
                  LLM 모델명
                </label>
                <input
                  type="text"
                  value={llmModel}
                  onChange={(e) => setLlmModel(e.target.value)}
                  placeholder="qwen2.5:7b"
                  className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent font-mono"
                />
                <p className="text-[10px] text-text-muted mt-1">
                  위 연동 상태 테이블의 모델명을 그대로 입력하세요. 오타 시 챗봇 응답이 실패합니다.
                </p>
              </div>

              {/* Temperature */}
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="text-xs text-text-secondary">Temperature</label>
                  <span className="font-mono text-sm text-accent">{llmTemperature.toFixed(2)}</span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={1}
                  step={0.05}
                  value={llmTemperature}
                  onChange={(e) => setLlmTemperature(Number(e.target.value))}
                  className="w-full h-1.5 bg-bg-prominent rounded-full appearance-none cursor-pointer accent-accent"
                />
                <div className="flex justify-between mt-1">
                  <span className="font-mono text-[9px] text-text-muted">0.0 (결정적)</span>
                  <span className="font-mono text-[9px] text-text-muted">1.0 (창의적)</span>
                </div>
              </div>

              {/* Max Tokens */}
              <div>
                <label className="block text-xs text-text-secondary mb-1">Max Tokens (최대 생성 토큰 수)</label>
                <input
                  type="number"
                  value={llmMaxTokens}
                  onChange={(e) => setLlmMaxTokens(Number(e.target.value))}
                  min={100}
                  max={4000}
                  step={100}
                  className="w-32 bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent"
                />
                <p className="text-[10px] text-text-muted mt-1">
                  100~4000 범위 권장. 늘릴수록 응답 레이턴시가 증가합니다.
                </p>
              </div>

              {/* 현재 저장값 요약 */}
              {config && (
                <div className="bg-bg-prominent rounded-lg p-3 text-xs text-text-secondary space-y-1">
                  <p className="font-mono text-[10px] uppercase tracking-wider text-text-muted mb-2">현재 저장된 설정 (v{config.version})</p>
                  <p><span className="text-text-muted">model:</span> <span className="font-mono text-text-primary">{config.llmModel ?? '-'}</span></p>
                  <p><span className="text-text-muted">temperature:</span> <span className="font-mono text-text-primary">{Number(config.llmTemperature).toFixed(2)}</span></p>
                  <p><span className="text-text-muted">max_tokens:</span> <span className="font-mono text-text-primary">{config.llmMaxTokens}</span></p>
                </div>
              )}

              <button
                onClick={handleSave}
                disabled={saving || !selectedOrgId || !config}
                className="px-4 py-2 rounded-lg bg-accent text-white text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {saving ? "저장 중..." : "저장"}
              </button>
            </div>
          )}
        </div>
      </Card>
    </div>
  );
}
