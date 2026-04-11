"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";

type Category = "pii_induction" | "out_of_domain" | "prompt_injection" | "harmful_content";
type ExpectedBehavior = "defend" | "detect";

const CATEGORY_LABELS: Record<Category, string> = {
  pii_induction:    "PII 유도",
  out_of_domain:    "도메인 외 질의",
  prompt_injection: "Prompt Injection",
  harmful_content:  "유해 콘텐츠",
};

export interface CaseFormData {
  category: Category;
  title: string;
  queryText: string;
  expectedBehavior: ExpectedBehavior;
}

interface CaseFormModalProps {
  initial?: {
    id: string;
    category: Category;
    title: string;
    queryText: string;
    expectedBehavior: ExpectedBehavior;
    isActive: boolean;
  };
  onClose: () => void;
  onSaved: () => void;
}

export function CaseFormModal({ initial, onClose, onSaved }: CaseFormModalProps) {
  const [category, setCategory]               = useState<Category>(initial?.category ?? "out_of_domain");
  const [title, setTitle]                     = useState(initial?.title ?? "");
  const [queryText, setQueryText]             = useState(initial?.queryText ?? "");
  const [expectedBehavior, setExpectedBehavior] = useState<ExpectedBehavior>(initial?.expectedBehavior ?? "defend");
  const [loading, setLoading]                 = useState(false);
  const [error, setError]                     = useState<string | null>(null);

  const isEdit = !!initial;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim() || !queryText.trim()) {
      setError("제목과 질의 내용을 입력하세요.");
      return;
    }
    setLoading(true);
    setError(null);

    try {
      if (isEdit) {
        const res = await fetch(`/api/admin/redteam/cases/${initial.id}`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ title, queryText, expectedBehavior }),
        });
        if (!res.ok) throw new Error("수정 실패");
      } else {
        const res = await fetch("/api/admin/redteam/cases", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ category, title, queryText, expectedBehavior }),
        });
        if (!res.ok) throw new Error("등록 실패");
      }
      onSaved();
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : "오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
      <div className="bg-bg-elevated border border-bg-border rounded-xl w-full max-w-lg p-6 space-y-5">
        <h3 className="text-text-primary font-semibold text-base">
          {isEdit ? "케이스 수정" : "케이스 등록"}
        </h3>

        <form onSubmit={handleSubmit} className="space-y-4">
          {!isEdit && (
            <div className="space-y-1">
              <label className="text-xs text-text-muted">카테고리</label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value as Category)}
                className="w-full bg-bg-prominent border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary"
              >
                {(Object.entries(CATEGORY_LABELS) as [Category, string][]).map(([val, label]) => (
                  <option key={val} value={val}>{label}</option>
                ))}
              </select>
            </div>
          )}

          <div className="space-y-1">
            <label className="text-xs text-text-muted">제목</label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              maxLength={100}
              placeholder="케이스 제목을 입력하세요"
              className="w-full bg-bg-prominent border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary placeholder:text-text-muted"
            />
          </div>

          <div className="space-y-1">
            <label className="text-xs text-text-muted">공격 질의문</label>
            <textarea
              value={queryText}
              onChange={(e) => setQueryText(e.target.value)}
              rows={3}
              placeholder="RAG에 투입할 공격 질의문을 입력하세요"
              className="w-full bg-bg-prominent border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary placeholder:text-text-muted resize-none"
            />
          </div>

          <div className="space-y-2">
            <label className="text-xs text-text-muted">기대 동작</label>
            <div className="flex gap-4">
              {(["defend", "detect"] as ExpectedBehavior[]).map((b) => (
                <label key={b} className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="radio"
                    name="expectedBehavior"
                    value={b}
                    checked={expectedBehavior === b}
                    onChange={() => setExpectedBehavior(b)}
                    className="accent-accent"
                  />
                  <span className="text-sm text-text-primary">
                    {b === "defend" ? "방어 (완전 차단)" : "탐지 (no_answer/fallback)"}
                  </span>
                </label>
              ))}
            </div>
          </div>

          {error && <p className="text-xs text-error">{error}</p>}

          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="ghost" onClick={onClose} disabled={loading}>
              취소
            </Button>
            <Button type="submit" disabled={loading}>
              {loading ? "저장 중..." : isEdit ? "수정" : "등록"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
