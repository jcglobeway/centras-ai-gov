"use client";

import { useState, useEffect } from "react";
import useSWR, { mutate } from "swr";
import { fetcher } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { PageGuide } from "@/components/ui/PageGuide";
import { Spinner } from "@/components/ui/Spinner";
import type { OrgRagConfig, Organization, PagedResponse } from "@/lib/types";

export default function RagParamsPage() {
  const { session } = useAuth();

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

  const configKey = selectedOrgId ? `/api/admin/organizations/${selectedOrgId}/rag-config` : null;
  const { data: config, isLoading: configLoading } = useSWR<OrgRagConfig>(configKey, fetcher);

  const [threshold, setThreshold] = useState(0.7);
  const [topK, setTopK] = useState(10);
  const [reranker, setReranker] = useState(false);
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  useEffect(() => {
    if (config) {
      setThreshold(Number(config.similarityThreshold));
      setTopK(config.topK);
      setReranker(config.rerankerEnabled);
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
          topK,
          similarityThreshold: threshold,
          rerankerEnabled: reranker,
          llmModel: config.llmModel,
          llmTemperature: config.llmTemperature,
          llmMaxTokens: config.llmMaxTokens,
          changeNote: "RAG 파라미터 업데이트",
        }),
      });
      if (!res.ok) throw new Error("저장 실패");
      mutate(configKey);
      showToast("RAG 파라미터가 저장되었습니다.");
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
        description="RAG 검색의 유사도 임계값, Top-K, Reranker를 조정하는 화면입니다."
        tips={[
          "유사도 임계값을 높이면 정밀도가 오르지만 Fallback율도 함께 오를 수 있습니다.",
          "Top-K를 늘리면 더 많은 문서를 참조해 답변 품질이 높아지지만 비용이 증가합니다.",
          "변경 후 평가 지표 페이지에서 Recall@K 수치 변화를 확인하세요.",
        ]}
      />

      <div className="flex items-center justify-between">
        <h2 className="text-text-primary font-semibold text-lg">RAG 파라미터 튜닝</h2>

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

      {configLoading ? (
        <div className="flex justify-center py-12"><Spinner /></div>
      ) : (
        <>
          <Card>
            <CardHeader>
              <CardTitle>파라미터 설정</CardTitle>
            </CardHeader>
            <div className="px-4 pb-4 space-y-6">
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="text-xs text-text-secondary">유사도 임계값 (Cosine Similarity)</label>
                  <span className="font-mono text-sm text-accent">{threshold.toFixed(2)}</span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={1}
                  step={0.01}
                  value={threshold}
                  onChange={(e) => setThreshold(Number(e.target.value))}
                  className="w-full h-1.5 bg-bg-prominent rounded-full appearance-none cursor-pointer accent-accent"
                />
                <div className="flex justify-between mt-1">
                  <span className="font-mono text-[9px] text-text-muted">0.0 (낮음)</span>
                  <span className="font-mono text-[9px] text-text-muted">1.0 (높음)</span>
                </div>
              </div>

              <div>
                <label className="block text-xs text-text-secondary mb-1">Top-K (최대 검색 청크 수)</label>
                <input
                  type="number"
                  value={topK}
                  onChange={(e) => setTopK(Number(e.target.value))}
                  min={1}
                  max={20}
                  className="w-32 bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent"
                />
              </div>

              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-text-secondary">Reranker</p>
                  <p className="text-[11px] text-text-muted mt-0.5">Cross-encoder 기반 재정렬 활성화</p>
                </div>
                <button
                  onClick={() => setReranker((v) => !v)}
                  className={`relative w-11 h-6 rounded-full transition-colors duration-200 ${
                    reranker ? 'bg-accent' : 'bg-bg-prominent'
                  }`}
                >
                  <span
                    className={`absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full shadow transition-transform duration-200 ${
                      reranker ? 'translate-x-5' : 'translate-x-0'
                    }`}
                  />
                </button>
              </div>

              <p className="text-[11px] text-text-muted">저장 후 평가 지표에서 효과를 확인하세요.</p>

              <button
                onClick={handleSave}
                disabled={saving || !selectedOrgId || !config}
                className="px-4 py-2 rounded-lg bg-accent text-white text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {saving ? "저장 중..." : "저장"}
              </button>
            </div>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>현재 저장된 설정</CardTitle>
            </CardHeader>
            <div className="px-4 pb-4">
              {!config ? (
                <p className="text-xs text-text-muted py-4">저장된 설정이 없습니다.</p>
              ) : (
                <div className="grid grid-cols-2 gap-4">
                  <div className="bg-bg-prominent rounded-lg p-4">
                    <p className="font-mono text-[10px] uppercase tracking-wider text-text-muted mb-3">저장된 설정</p>
                    <div className="space-y-1.5 text-xs text-text-secondary">
                      <p><span className="text-text-muted">threshold:</span> <span className="font-mono text-text-primary">{Number(config.similarityThreshold).toFixed(2)}</span></p>
                      <p><span className="text-text-muted">K:</span> <span className="font-mono text-text-primary">{config.topK}</span></p>
                      <p><span className="text-text-muted">Reranker:</span> <span className={`font-mono ${config.rerankerEnabled ? 'text-success' : 'text-text-muted'}`}>{config.rerankerEnabled ? 'on' : 'off'}</span></p>
                    </div>
                    <div className="mt-3 pt-3 border-t border-bg-border">
                      <p className="text-[10px] text-text-muted">버전</p>
                      <p className="font-mono text-lg font-bold text-text-primary">v{config.version}</p>
                    </div>
                  </div>
                  <div className="bg-bg-prominent rounded-lg p-4">
                    <p className="font-mono text-[10px] uppercase tracking-wider text-text-muted mb-3">시뮬레이션</p>
                    <p className="text-[11px] text-text-muted mt-2">시뮬레이션은 이번 변경 범위에서 제외됩니다.</p>
                    <p className="text-[11px] text-text-muted mt-1">평가 지표 페이지에서 효과를 확인하세요.</p>
                  </div>
                </div>
              )}
            </div>
          </Card>
        </>
      )}
    </div>
  );
}
