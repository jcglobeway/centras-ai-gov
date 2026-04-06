"use client";

import { useEffect, useRef, useState } from "react";
import useSWR from "swr";
import ReactMarkdown from "react-markdown";
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
interface Message {
  id: string;
  role: "user" | "assistant";
  content: string;
  metadata?: Metadata;
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

function scoreBadgeBg(score: number): string {
  if (score >= 0.8) return "bg-success/10 text-success";
  if (score >= 0.6) return "bg-warning/10 text-warning";
  return "bg-bg-border/30 text-text-muted";
}

export default function SimulatorPage() {
  const [orgId, setOrgId] = useState("");
  const [serviceId, setServiceId] = useState("");
  const [chatSessionId, setChatSessionId] = useState<string | null>(null);
  const [starting, setStarting] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [selectedMsgId, setSelectedMsgId] = useState<string | null>(null);
  const [expandedChunks, setExpandedChunks] = useState<Set<number>>(new Set());
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

  // selectedMsgId가 바뀌면 청크 펼침 상태 초기화
  useEffect(() => {
    setExpandedChunks(new Set());
  }, [selectedMsgId]);

  const selectedMetadata =
    messages.find((m) => m.id === selectedMsgId)?.metadata ?? null;

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
    setSelectedMsgId(null);
    setExpandedChunks(new Set());
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
              const meta: Metadata = {
                answerStatus: parsed.answer_status ?? "unknown",
                responseTimeMs: parsed.response_time_ms ?? 0,
                citationCount: parsed.citation_count ?? 0,
                confidenceScore: parsed.confidence_score ?? 0,
                retrievedChunks: parsed.retrieved_chunks ?? [],
              };
              setMessages((prev) =>
                prev.map((m) =>
                  m.id === assistantId ? { ...m, metadata: meta } : m
                )
              );
              setSelectedMsgId(assistantId);
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

  function toggleChunk(idx: number) {
    setExpandedChunks((prev) => {
      const next = new Set(prev);
      if (next.has(idx)) next.delete(idx);
      else next.add(idx);
      return next;
    });
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
              {selectedMetadata && (
                <div className="flex items-center gap-2 font-mono text-[11px] text-text-secondary">
                  <span>{selectedMetadata.responseTimeMs}ms</span>
                  <span className="w-1 h-1 rounded-full bg-bg-border" />
                  <span>Docs: {selectedMetadata.citationCount}</span>
                  <span className="w-1 h-1 rounded-full bg-bg-border" />
                  <span>
                    Conf: {(selectedMetadata.confidenceScore * 100).toFixed(0)}%
                  </span>
                  <span className="w-1 h-1 rounded-full bg-bg-border" />
                  <span
                    className={
                      selectedMetadata.answerStatus === "answered"
                        ? "text-success"
                        : "text-warning"
                    }
                  >
                    {selectedMetadata.answerStatus}
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
                  className={`flex flex-col ${m.role === "user" ? "items-end" : "items-start"}`}
                >
                  <div
                    className={`max-w-[75%] px-4 py-3 text-sm leading-relaxed rounded-xl ${
                      m.role === "user"
                        ? "bg-accent text-white rounded-br-sm whitespace-pre-wrap"
                        : "bg-bg-elevated border border-white/5 text-text-primary rounded-bl-sm"
                    }`}
                  >
                    {m.content ? (
                      m.role === "assistant" ? (
                        <ReactMarkdown
                          components={{
                            p: ({ children }) => <p className="mb-2 last:mb-0">{children}</p>,
                            ul: ({ children }) => <ul className="list-disc pl-4 mb-2 space-y-1">{children}</ul>,
                            ol: ({ children }) => <ol className="list-decimal pl-4 mb-2 space-y-1">{children}</ol>,
                            li: ({ children }) => <li>{children}</li>,
                            strong: ({ children }) => <strong className="font-semibold">{children}</strong>,
                            h1: ({ children }) => <h1 className="text-base font-bold mb-1">{children}</h1>,
                            h2: ({ children }) => <h2 className="text-sm font-bold mb-1">{children}</h2>,
                            h3: ({ children }) => <h3 className="text-sm font-semibold mb-1">{children}</h3>,
                            code: ({ children }) => <code className="bg-bg-base px-1 py-0.5 rounded text-xs font-mono text-accent">{children}</code>,
                            pre: ({ children }) => <pre className="bg-bg-base p-2 rounded text-xs font-mono overflow-x-auto mb-2">{children}</pre>,
                          }}
                        >
                          {m.content}
                        </ReactMarkdown>
                      ) : (
                        m.content
                      )
                    ) : (
                      <span className="text-text-muted text-xs">생성 중...</span>
                    )}
                  </div>
                  {/* 어시스턴트 메시지의 소스 뱃지 */}
                  {m.role === "assistant" && m.metadata && (
                    <button
                      onClick={() => setSelectedMsgId(m.id)}
                      className={`mt-1 text-[11px] px-2 py-0.5 rounded-full border transition-colors ${
                        selectedMsgId === m.id
                          ? "border-accent text-accent bg-accent/10"
                          : "border-white/10 text-text-muted hover:border-accent/50 hover:text-accent/70"
                      }`}
                    >
                      소스 {m.metadata.citationCount}개 · {m.metadata.responseTimeMs}ms
                    </button>
                  )}
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
          <div className="w-80 flex-shrink-0 flex flex-col overflow-hidden rounded-lg bg-bg-base border border-white/5">
            <div className="px-3 py-2.5 bg-bg-surface border-b border-white/5 flex items-center justify-between">
              <span className="text-[11px] font-bold text-text-secondary uppercase tracking-widest">
                Retrieved Chunks
              </span>
              {selectedMetadata && (
                <span className="text-[10px] text-text-muted font-mono">
                  {selectedMetadata.retrievedChunks.length}개 검색됨
                </span>
              )}
            </div>

            {/* 메타 요약 */}
            {selectedMetadata && (
              <div className="px-3 py-2 bg-bg-surface/50 border-b border-white/5 grid grid-cols-3 gap-2">
                <div className="text-center">
                  <div className="text-[10px] text-text-muted mb-0.5">Latency</div>
                  <div className="text-[12px] font-mono text-text-primary">
                    {selectedMetadata.responseTimeMs}ms
                  </div>
                </div>
                <div className="text-center">
                  <div className="text-[10px] text-text-muted mb-0.5">Confidence</div>
                  <div
                    className={`text-[12px] font-mono ${scoreColor(selectedMetadata.confidenceScore)}`}
                  >
                    {(selectedMetadata.confidenceScore * 100).toFixed(1)}%
                  </div>
                </div>
                <div className="text-center">
                  <div className="text-[10px] text-text-muted mb-0.5">Status</div>
                  <div
                    className={`text-[11px] font-mono ${
                      selectedMetadata.answerStatus === "answered"
                        ? "text-success"
                        : "text-warning"
                    }`}
                  >
                    {selectedMetadata.answerStatus}
                  </div>
                </div>
              </div>
            )}

            <div className="flex-1 overflow-y-auto divide-y divide-white/5">
              {!selectedMetadata ? (
                <p className="text-[11px] text-text-muted p-4 text-center mt-4">
                  답변 아래 소스 뱃지를 클릭하면
                  <br />
                  참조 문서가 여기에 표시됩니다
                </p>
              ) : selectedMetadata.retrievedChunks.length === 0 ? (
                <p className="text-[11px] text-text-muted p-4 text-center mt-4">
                  검색된 문서가 없습니다
                </p>
              ) : (
                selectedMetadata.retrievedChunks.map((chunk, i) => {
                  const isExpanded = expandedChunks.has(i);
                  return (
                    <div key={i} className="hover:bg-bg-elevated transition-colors">
                      {/* 청크 헤더 (클릭 시 토글) */}
                      <button
                        onClick={() => toggleChunk(i)}
                        className="w-full text-left p-3"
                      >
                        <div className="flex items-start justify-between gap-2 mb-1.5">
                          <div className="flex items-center gap-1.5 min-w-0">
                            <span className="font-mono text-[11px] text-accent shrink-0">
                              #{String(i + 1).padStart(2, "0")}
                            </span>
                            <span className="text-[10px] text-text-secondary truncate">
                              {chunk.filename}
                            </span>
                          </div>
                          <div className="flex items-center gap-1 shrink-0">
                            <span
                              className={`text-[10px] font-mono font-semibold px-1.5 py-0.5 rounded ${scoreBadgeBg(chunk.score)}`}
                            >
                              {chunk.score.toFixed(3)}
                            </span>
                            <span className="text-[10px] text-text-muted">
                              {isExpanded ? "▲" : "▼"}
                            </span>
                          </div>
                        </div>
                        <p
                          className={`text-[11px] font-mono leading-relaxed text-text-secondary/70 ${
                            isExpanded ? "" : "line-clamp-2"
                          }`}
                        >
                          {chunk.preview}
                        </p>
                      </button>

                      {/* 확장 시 추가 정보 */}
                      {isExpanded && (
                        <div className="px-3 pb-3 space-y-1.5 border-t border-white/5 pt-2">
                          <div className="flex gap-3 text-[10px] text-text-muted font-mono">
                            <span>Sim Score: <span className={scoreColor(chunk.score)}>{chunk.score.toFixed(4)}</span></span>
                            <span>Rank: #{i + 1}</span>
                          </div>
                          <div className="text-[10px] text-text-muted truncate">
                            파일: {chunk.filename}
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
