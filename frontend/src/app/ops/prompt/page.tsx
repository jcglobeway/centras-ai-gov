"use client";

import { useState, useEffect } from "react";
import useSWR, { mutate } from "swr";
import { fetcher } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { PageGuide } from "@/components/ui/PageGuide";
import { Spinner } from "@/components/ui/Spinner";
import type { OrgRagConfig, OrgRagConfigVersionListResponse, Organization, PagedResponse } from "@/lib/types";

type Tone = 'formal' | 'friendly' | 'neutral';

export default function PromptPage() {
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
  const versionsKey = selectedOrgId ? `/api/admin/organizations/${selectedOrgId}/rag-config/versions` : null;

  const { data: config, isLoading: configLoading } = useSWR<OrgRagConfig>(configKey, fetcher);
  const { data: versionsData } = useSWR<OrgRagConfigVersionListResponse>(versionsKey, fetcher);

  const [prompt, setPrompt] = useState("");
  const [tone, setTone] = useState<Tone>('formal');
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const [changeNote, setChangeNote] = useState("");

  useEffect(() => {
    if (config) {
      setPrompt(config.systemPrompt);
      setTone(config.tone);
    }
  }, [config]);

  function showToast(msg: string) {
    setToast(msg);
    setTimeout(() => setToast(null), 3000);
  }

  async function handleSave() {
    if (!selectedOrgId) return;
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
          systemPrompt: prompt,
          tone,
          topK: config?.topK ?? 10,
          similarityThreshold: config?.similarityThreshold ?? 0.7,
          rerankerEnabled: config?.rerankerEnabled ?? false,
          llmModel: config?.llmModel ?? "qwen2.5:7b",
          llmTemperature: config?.llmTemperature ?? 0.3,
          llmMaxTokens: config?.llmMaxTokens ?? 500,
          changeNote: changeNote || null,
        }),
      });
      if (!res.ok) throw new Error("저장 실패");
      mutate(configKey);
      mutate(versionsKey);
      setChangeNote("");
      showToast("저장되었습니다.");
    } catch {
      showToast("저장 중 오류가 발생했습니다.");
    } finally {
      setSaving(false);
    }
  }

  async function handleRollback(version: number) {
    if (!selectedOrgId) return;
    try {
      const sessionId = localStorage.getItem("sessionId");
      const res = await fetch(`/api/admin/organizations/${selectedOrgId}/rag-config/rollback/${version}`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...(sessionId ? { "X-Admin-Session-Id": sessionId } : {}),
        },
      });
      if (!res.ok) throw new Error("롤백 실패");
      mutate(configKey);
      mutate(versionsKey);
      showToast(`v${version}으로 롤백되었습니다.`);
    } catch {
      showToast("롤백 중 오류가 발생했습니다.");
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
        description="챗봇의 성격, 톤앤매너, 답변 지침을 설정하는 화면입니다."
        tips={[
          "변경 사항은 저장 후 반드시 시뮬레이션 룸에서 검증하고 배포하세요.",
          "버전 이력에서 이전 프롬프트로 즉시 롤백할 수 있습니다.",
          "공식 기관 챗봇이므로 '공식체'를 기본으로 유지하고 변경 시 팀 협의를 권장합니다.",
        ]}
      />

      <div className="flex items-center justify-between">
        <h2 className="text-text-primary font-semibold text-lg">프롬프트 엔지니어링</h2>

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
              <CardTitle>시스템 프롬프트 편집기</CardTitle>
            </CardHeader>
            <div className="px-4 pb-4 space-y-4">
              <textarea
                value={prompt}
                onChange={(e) => setPrompt(e.target.value)}
                rows={20}
                className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2.5 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:border-accent resize-y font-mono leading-relaxed min-h-[200px]"
              />
              <p className="text-[10px] text-text-muted text-right">{prompt.length.toLocaleString()}자</p>

              <div>
                <p className="text-xs text-text-secondary mb-2">톤앤매너</p>
                <div className="flex gap-6">
                  {([
                    { value: 'formal',   label: '공식체' },
                    { value: 'friendly', label: '친근체' },
                    { value: 'neutral',  label: '중립체' },
                  ] as { value: Tone; label: string }[]).map((opt) => (
                    <label key={opt.value} className="flex items-center gap-2 cursor-pointer">
                      <input
                        type="radio"
                        name="tone"
                        value={opt.value}
                        checked={tone === opt.value}
                        onChange={() => setTone(opt.value)}
                        className="accent-accent"
                      />
                      <span className="text-sm text-text-secondary">{opt.label}</span>
                    </label>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-xs text-text-secondary mb-1">변경 메모 (선택)</label>
                <input
                  type="text"
                  value={changeNote}
                  onChange={(e) => setChangeNote(e.target.value)}
                  placeholder="예: 톤 친근하게 수정"
                  className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent"
                />
              </div>

              <div className="flex items-start gap-2 bg-warning/10 border border-warning/20 rounded-lg px-3 py-2.5">
                <span className="material-symbols-outlined text-warning text-[18px] mt-px shrink-0">warning</span>
                <p className="text-[11px] text-warning leading-relaxed">
                  저장 후 시뮬레이션 룸에서 반드시 검증하세요
                </p>
              </div>

              <button
                onClick={handleSave}
                disabled={saving || !selectedOrgId}
                className="px-4 py-2 rounded-lg bg-accent text-white text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {saving ? "저장 중..." : "저장"}
              </button>

              {config && (
                <p className="text-[11px] text-text-muted">현재 버전: v{config.version}</p>
              )}
            </div>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>버전 이력</CardTitle>
            </CardHeader>
            <div className="px-4 pb-4">
              {!versionsData?.items?.length ? (
                <p className="text-xs text-text-muted py-4 text-center">저장된 버전이 없습니다.</p>
              ) : (
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-bg-border">
                      <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted w-16">버전</th>
                      <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted w-28">저장일</th>
                      <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted">작성자</th>
                      <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted">노트</th>
                      <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted w-20">작업</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-bg-border">
                    {versionsData.items.map((v) => (
                      <tr key={v.id}>
                        <td className="py-2.5 font-mono text-xs text-accent">v{v.version}</td>
                        <td className="py-2.5 text-xs text-text-muted">{new Date(v.createdAt).toLocaleDateString('ko-KR')}</td>
                        <td className="py-2.5 text-xs text-text-secondary">{v.changedBy ?? '-'}</td>
                        <td className="py-2.5 text-xs text-text-secondary">{v.changeNote ?? '-'}</td>
                        <td className="py-2.5">
                          {config && v.version !== config.version ? (
                            <button
                              onClick={() => handleRollback(v.version)}
                              className="text-[11px] text-accent hover:underline"
                            >
                              롤백
                            </button>
                          ) : (
                            <span className="text-[11px] text-success font-mono">현재</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </Card>
        </>
      )}
    </div>
  );
}
