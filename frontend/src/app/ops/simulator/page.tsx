"use client";

import { useEffect, useRef, useState } from "react";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { PageGuide } from "@/components/ui/PageGuide";

interface OrgItem {
  organizationId: string;
  name: string;
}
interface ServiceItem {
  serviceId: string;
  name: string;
}
interface Message {
  id: string;
  role: "user" | "assistant";
  content: string;
}
interface ChunkMeta {
  filename: string;
  preview: string;
  score: number;
}
interface Metadata {
  answerStatus: string;
  responseTimeMs: number;
  citationCount: number;
  confidenceScore: number;
  retrievedChunks: ChunkMeta[];
}

function getSessionId(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("sessionId");
}

function scoreColor(score: number): string {
  if (score >= 0.8) return "text-success";
  if (score >= 0.6) return "text-warning";
  return "text-text-muted";
}

export default function SimulatorPage() {
  const [orgId, setOrgId] = useState("");
  const [serviceId, setServiceId] = useState("");
  const [chatSessionId, setChatSessionId] = useState<string | null>(null);
  const [starting, setStarting] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [metadata, setMetadata] = useState<Metadata | null>(null);
  const bottomRef = useRef<HTMLDivElement>(null);

  const { data: orgs } = useSWR<{ items: OrgItem[] }>(
    "/api/admin/organizations?page_size=50",
    fetcher
  );
  const { data: services } = useSWR<{ items: ServiceItem[] }>(
    orgId ? `/api/admin/organizations/${orgId}/services` : null,
    fetcher
  );

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isLoading]);

  async function startSession() {
    setStarting(true);
    try {
      const res = await fetch("/api/admin/simulator/sessions", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-Admin-Session-Id": getSessionId() ?? "",
        },
        body: JSON.stringify({ organizationId: orgId, serviceId }),
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const { sessionId: id } = await res.json();
      setChatSessionId(id);
    } catch {
      alert("세션 생성에 실패했습니다. 백엔드를 확인하세요.");
    } finally {
      setStarting(false);
    }
  }

  function resetSession() {
    setChatSessionId(null);
    setOrgId("");
    setServiceId("");
    setMessages([]);
    setInput("");
    setMetadata(null);
  }

  async function sendMessage(e: React.FormEvent) {
    e.preventDefault();
    if (!input.trim() || isLoading) return;

    const userMsg: Message = {
      id: `user_${Date.now()}`,
      role: "user",
      content: input.trim(),
    };
    setMessages((prev) => [...prev, userMsg]);
    setInput("");
    setIsLoading(true);

    const assistantId = `asst_${Date.now()}`;
    setMessages((prev) => [
      ...prev,
      { id: assistantId, role: "assistant", content: "" },
    ]);

    try {
      const res = await fetch("/api/simulator/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          messages: [...messages, userMsg].map((m) => ({
            role: m.role,
            content: m.content,
          })),
          organizationId: orgId,
          serviceId,
          sessionId: chatSessionId,
        }),
      });

      if (!res.body) throw new Error("응답 본문이 없습니다.");

      const reader = res.body.getReader();
      const decoder = new TextDecoder();
      let buffer = "";

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split("\n");
        buffer = lines.pop() ?? "";

        for (const line of lines.filter(Boolean)) {
          try {
            const parsed = JSON.parse(line);
            if (typeof parsed.content === "string") {
              setMessages((prev) =>
                prev.map((m) =>
                  m.id === assistantId
                    ? { ...m, content: m.content + parsed.content }
                    : m
                )
              );
            }
            if (parsed.done) {
              setMetadata({
                answerStatus: parsed.answer_status ?? "unknown",
                responseTimeMs: parsed.response_time_ms ?? 0,
                citationCount: parsed.citation_count ?? 0,
                confidenceScore: parsed.confidence_score ?? 0,
                retrievedChunks: parsed.retrieved_chunks ?? [],
              });
            }
          } catch {
            // incomplete JSON line — skip
          }
        }
      }
    } catch {
      setMessages((prev) =>
        prev.map((m) =>
          m.id === assistantId
            ? { ...m, content: "답변 생성 중 오류가 발생했습니다." }
            : m
        )
      );
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div className="flex flex-col" style={{ height: "calc(100vh - 8rem)" }}>
      {/* 페이지 헤더 */}
      <div className="mb-2 flex items-center justify-between">
        <div>
          <h1 className="text-text-primary font-semibold text-base">
            시뮬레이션 룸
          </h1>
          <p className="text-text-muted text-xs mt-0.5">
            RAG 파이프라인을 실시간으로 테스트합니다
          </p>
        </div>
        {chatSessionId && (
          <Button onClick={resetSession} variant="secondary">
            대화 초기화
          </Button>
        )}
      </div>

      <div className="mb-3">
        <PageGuide
          description="프롬프트·파라미터 변경 사항을 배포 전에 A/B 비교 테스트하는 화면입니다."
          tips={[
            "Version A(현재 설정)와 Version B(변경 설정)에 같은 질문을 입력해 답변 품질을 비교하세요.",
            "Retrieved Chunks에서 어떤 문서가 검색됐는지, 유사도 점수가 적절한지 확인하세요.",
            "테스트 통과 후에는 RAG 파라미터 또는 프롬프트 페이지에서 설정을 저장하세요.",
          ]}
        />
      </div>

      {/* 기관/서비스 선택 */}
      {!chatSessionId ? (
        <div className="flex gap-3 items-end">
          <div className="flex flex-col gap-1">
            <label className="text-xs text-text-muted">기관</label>
            <select
              value={orgId}
              onChange={(e) => {
                setOrgId(e.target.value);
                setServiceId("");
              }}
              className="bg-bg-surface border border-bg-border rounded px-3 py-2 text-sm text-text-primary min-w-[160px]"
            >
              <option value="">선택하세요</option>
              {orgs?.items?.map((o) => (
                <option key={o.organizationId} value={o.organizationId}>
                  {o.name}
                </option>
              ))}
            </select>
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-xs text-text-muted">서비스</label>
            <select
              value={serviceId}
              onChange={(e) => setServiceId(e.target.value)}
              disabled={!orgId}
              className="bg-bg-surface border border-bg-border rounded px-3 py-2 text-sm text-text-primary min-w-[160px] disabled:opacity-50"
            >
              <option value="">선택하세요</option>
              {services?.items?.map((s) => (
                <option key={s.serviceId} value={s.serviceId}>
                  {s.name}
                </option>
              ))}
            </select>
          </div>
          <Button
            onClick={startSession}
            disabled={!orgId || !serviceId || starting}
          >
            {starting ? "시작 중..." : "대화 시작"}
          </Button>
        </div>
      ) : (
        <div className="flex-1 flex gap-3 overflow-hidden">
          {/* 채팅 패널 */}
          <div className="flex-1 flex flex-col overflow-hidden bg-bg-surface border border-white/5 rounded-lg">
            {/* 세션 헤더 */}
            <div className="px-4 py-2 border-b border-white/5 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-xs text-text-muted">세션</span>
                <code className="text-xs text-accent">{chatSessionId}</code>
              </div>
              {metadata && (
                <div className="flex items-center gap-2 font-mono text-[11px] text-text-secondary">
                  <span>Latency: {metadata.responseTimeMs}ms</span>
                  <span className="w-1 h-1 rounded-full bg-bg-border" />
                  <span>Docs: {metadata.citationCount}</span>
                  <span className="w-1 h-1 rounded-full bg-bg-border" />
                  <span>
                    Confidence: {(metadata.confidenceScore * 100).toFixed(0)}%
                  </span>
                </div>
              )}
            </div>

            {/* 메시지 영역 */}
            <div className="flex-1 overflow-y-auto p-4 space-y-4">
              {messages.length === 0 && (
                <p className="text-center text-text-muted text-sm mt-8">
                  질문을 입력해 RAG 파이프라인을 테스트하세요
                </p>
              )}
              {messages.map((m) => (
                <div
                  key={m.id}
                  className={`flex ${m.role === "user" ? "justify-end" : "justify-start"}`}
                >
                  <div
                    className={`max-w-[75%] px-4 py-3 text-sm leading-relaxed whitespace-pre-wrap rounded-xl ${
                      m.role === "user"
                        ? "bg-accent text-white rounded-br-sm"
                        : "bg-bg-elevated border border-white/5 text-text-primary rounded-bl-sm"
                    }`}
                  >
                    {m.content || (
                      <span className="text-text-muted text-xs">생성 중...</span>
                    )}
                  </div>
                </div>
              ))}
              {isLoading && messages.at(-1)?.role === "user" && (
                <div className="flex justify-start">
                  <div className="bg-bg-elevated border border-white/5 rounded-xl rounded-bl-sm px-4 py-3">
                    <div className="flex gap-1 items-center">
                      <span className="w-1.5 h-1.5 bg-text-muted rounded-full animate-bounce [animation-delay:0ms]" />
                      <span className="w-1.5 h-1.5 bg-text-muted rounded-full animate-bounce [animation-delay:150ms]" />
                      <span className="w-1.5 h-1.5 bg-text-muted rounded-full animate-bounce [animation-delay:300ms]" />
                    </div>
                  </div>
                </div>
              )}
              <div ref={bottomRef} />
            </div>

            {/* 입력 폼 */}
            <form
              onSubmit={sendMessage}
              className="flex gap-2 p-3 border-t border-white/5"
            >
              <input
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder="질문을 입력하세요..."
                disabled={isLoading}
                className="flex-1 bg-bg-base border border-white/5 rounded-lg px-3 py-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:border-accent disabled:opacity-50"
              />
              <Button type="submit" disabled={isLoading || !input.trim()}>
                전송
              </Button>
            </form>
          </div>

          {/* Retrieved Chunks 패널 */}
          <div className="w-72 flex-shrink-0 flex flex-col overflow-hidden rounded-lg bg-bg-base border border-white/5">
            <div className="px-3 py-2.5 bg-bg-surface border-b border-white/5">
              <span className="text-[11px] font-bold text-text-secondary uppercase tracking-widest">
                Retrieved Chunks
              </span>
            </div>
            <div className="px-3 py-1.5 border-b border-white/5 flex items-center justify-between">
              <span className="text-[10px] font-mono text-text-muted">
                Source Retrieval Trace
              </span>
              <div className="flex gap-1">
                <div className="w-2 h-2 rounded-full bg-error/40" />
                <div className="w-2 h-2 rounded-full bg-warning/40" />
                <div className="w-2 h-2 rounded-full bg-success/40" />
              </div>
            </div>
            <div className="flex-1 overflow-y-auto divide-y divide-white/5">
              {!metadata ? (
                <p className="text-[11px] text-text-muted p-4 text-center mt-4">
                  질문 후 참조 문서가
                  <br />
                  여기에 표시됩니다
                </p>
              ) : metadata.retrievedChunks.length === 0 ? (
                <p className="text-[11px] text-text-muted p-4 text-center mt-4">
                  검색된 문서가 없습니다
                </p>
              ) : (
                metadata.retrievedChunks.map((chunk, i) => (
                  <div
                    key={i}
                    className="p-3 hover:bg-bg-elevated transition-colors"
                  >
                    <div className="flex justify-between mb-1.5">
                      <div className="flex items-center gap-2 min-w-0">
                        <span className="font-mono text-[11px] text-accent shrink-0">
                          #{String(i + 1).padStart(3, "0")}
                        </span>
                        <span className="text-[10px] text-text-secondary flex items-center gap-0.5 truncate">
                          <span className="material-symbols-outlined text-[12px] shrink-0">
                            description
                          </span>
                          <span className="truncate">{chunk.filename}</span>
                        </span>
                      </div>
                      <div className="flex items-center gap-1 shrink-0 ml-2">
                        <span className="text-[10px] text-text-muted">Sim:</span>
                        <span
                          className={`font-mono text-[11px] font-medium ${scoreColor(chunk.score)}`}
                        >
                          {chunk.score.toFixed(3)}
                        </span>
                      </div>
                    </div>
                    <p className="text-[11px] font-mono leading-tight text-text-secondary/60 line-clamp-2">
                      &ldquo;{chunk.preview}&rdquo;
                    </p>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
